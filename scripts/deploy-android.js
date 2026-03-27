#!/usr/bin/env node
/**
 * deploy-android.js
 *
 * Builds the Android release AAB and uploads it to the Play Store.
 * Uses the Google Play Developer API directly — no fastlane required.
 *
 * Prerequisites:
 *   1. node_modules: run `npm install` in scripts/ first
 *   2. Google Play service account JSON at ../play-store-key.json
 *      (or set PLAY_SERVICE_ACCOUNT env var to the path)
 *   3. Release signing configured in VisiScheduler/local.properties
 *
 * Usage:
 *   node scripts/deploy-android.js [--track internal|alpha|beta|production]
 *
 * Tracks: internal (default), alpha, beta, production
 */

const { google } = require('googleapis');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// ── Config ────────────────────────────────────────────────────────────────────

const PACKAGE_NAME = 'com.markduenas.visischeduler';
const REPO_ROOT = path.resolve(__dirname, '..');
const APP_ROOT = path.join(REPO_ROOT, 'VisiScheduler');
const AAB_PATH = path.join(APP_ROOT, 'androidApp/build/outputs/bundle/release/androidApp-release.aab');
const SERVICE_ACCOUNT_PATH = process.env.PLAY_SERVICE_ACCOUNT
  || path.join(REPO_ROOT, '../play-store-key.json');

const trackArg = process.argv.indexOf('--track');
const TRACK = trackArg !== -1 ? process.argv[trackArg + 1] : 'internal';

// ── Preflight checks ──────────────────────────────────────────────────────────

function preflight() {
  if (!fs.existsSync(SERVICE_ACCOUNT_PATH)) {
    console.error(`\n❌ Service account not found at: ${SERVICE_ACCOUNT_PATH}`);
    console.error('   Set PLAY_SERVICE_ACCOUNT env var or place the JSON at the path above.');
    console.error('   To create one: Play Console → Setup → API access → Create service account');
    process.exit(1);
  }

  const validTracks = ['internal', 'alpha', 'beta', 'production'];
  if (!validTracks.includes(TRACK)) {
    console.error(`❌ Invalid track "${TRACK}". Must be one of: ${validTracks.join(', ')}`);
    process.exit(1);
  }
}

// ── Release notes ─────────────────────────────────────────────────────────────

/** Generates release notes from git commits since the last version bump. */
function generateReleaseNotes() {
  try {
    const lastBumpHash = execSync(
      'git log --oneline | grep -m1 "bump\\|versionCode\\|Bump" | awk \'{print $1}\'',
      { cwd: REPO_ROOT }
    ).toString().trim();

    const log = execSync(
      `git log ${lastBumpHash ? `${lastBumpHash}..HEAD` : '-n 10'} --pretty=format:"• %s" --no-merges`,
      { cwd: REPO_ROOT }
    ).toString().trim();

    const lines = log
      .split('\n')
      .filter(l => !l.includes('bump') && !l.includes('versionCode') && !l.includes('[automated]') && l.trim());

    let notes = lines.join('\n');
    if (notes.length > 500) notes = notes.substring(0, 497) + '...';
    return notes || 'Bug fixes and improvements.';
  } catch {
    return 'Bug fixes and improvements.';
  }
}

// ── Build ─────────────────────────────────────────────────────────────────────

function buildAAB() {
  console.log('🔨 Building release AAB...');
  try {
    execSync('./gradlew :androidApp:bundleRelease', {
      cwd: APP_ROOT,
      stdio: 'inherit',
    });
  } catch {
    console.error('❌ Gradle build failed.');
    process.exit(1);
  }

  if (!fs.existsSync(AAB_PATH)) {
    console.error(`❌ AAB not found at expected path: ${AAB_PATH}`);
    process.exit(1);
  }

  console.log(`✅ AAB built: ${path.relative(REPO_ROOT, AAB_PATH)}\n`);
}

// ── Upload ────────────────────────────────────────────────────────────────────

async function upload() {
  const auth = new google.auth.GoogleAuth({
    keyFile: SERVICE_ACCOUNT_PATH,
    scopes: ['https://www.googleapis.com/auth/androidpublisher'],
  });

  const publisher = google.androidpublisher({ version: 'v3', auth });

  // 1. Open edit
  console.log('📝 Opening Play Store edit...');
  const { data: edit } = await publisher.edits.insert({ packageName: PACKAGE_NAME });
  const editId = edit.id;

  try {
    // 2. Upload AAB
    console.log(`📦 Uploading AAB to track: ${TRACK}...`);
    const { data: bundle } = await publisher.edits.bundles.upload({
      packageName: PACKAGE_NAME,
      editId,
      media: {
        mimeType: 'application/octet-stream',
        body: fs.createReadStream(AAB_PATH),
      },
    });
    console.log(`   Version code: ${bundle.versionCode}`);

    // 3. Detect if the app is still in draft state
    let releaseStatus = 'completed';
    try {
      const { data: appEdit } = await publisher.edits.details.get({ packageName: PACKAGE_NAME, editId });
      if (appEdit.details?.defaultLanguage === undefined) {
        // App has no store listing yet — it's a draft app
        releaseStatus = 'draft';
      }
    } catch {
      // If we can't determine, try completed and fall back to draft on error
    }

    // 4. Assign to track
    const notes = generateReleaseNotes();
    console.log(`📝 Release notes:\n${notes}\n`);
    console.log(`📋 Release status: ${releaseStatus}`);

    const assignToTrack = async (status) => {
      await publisher.edits.tracks.update({
        packageName: PACKAGE_NAME,
        editId,
        track: TRACK,
        requestBody: {
          track: TRACK,
          releases: [{
            status,
            versionCodes: [bundle.versionCode],
            releaseNotes: [{ language: 'en-US', text: notes }],
          }],
        },
      });
    };

    try {
      await assignToTrack(releaseStatus);
    } catch (err) {
      if (err.message && err.message.includes('draft')) {
        console.log('   App is in draft state — retrying with status: draft');
        await assignToTrack('draft');
      } else {
        throw err;
      }
    }

    // 4. Commit edit
    console.log('✅ Committing edit...');
    await publisher.edits.commit({ packageName: PACKAGE_NAME, editId });

    console.log(`\n🎉 Done — uploaded to Play Store (${TRACK} track)`);
  } catch (err) {
    // Abort edit on failure
    await publisher.edits.delete({ packageName: PACKAGE_NAME, editId }).catch(() => {});
    throw err;
  }
}

// ── Entry point ───────────────────────────────────────────────────────────────

preflight();
buildAAB();
upload().catch(err => {
  console.error('❌ Upload failed:', err.message);
  process.exit(1);
});
