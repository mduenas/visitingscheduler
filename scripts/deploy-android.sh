#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODE_ROOT="$(cd "$REPO_ROOT/.." && pwd)"
APP_ROOT="$REPO_ROOT/VisiScheduler"
TRACK="internal"
RELEASE_STATUS="completed"
SKIP_UPLOAD=false
SERVICE_ACCOUNT_JSON="${SERVICE_ACCOUNT_JSON:-}"
PACKAGE_NAME="${ANDROID_PACKAGE_NAME:-com.markduenas.visischeduler}"

SHARED_ENV_FILE="$CODE_ROOT/.deploy-config/deploy.env"
if [[ -f "$SHARED_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$SHARED_ENV_FILE"
fi

usage() {
  cat <<'EOF'
Usage:
  ./scripts/deploy-android.sh [options]

Options:
  --track <internal|alpha|beta|production>   Play track (default: internal)
  --release-status <draft|completed>          Release status (default: completed; use draft for new apps)
  --skip-upload                               Build only, do not upload
  --service-account <path>                    Google Play JSON key path
  --package-name <id>                         Android application ID
  -h, --help                                  Show help

Environment:
  PLAY_SERVICE_ACCOUNT
  GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH
  ANDROID_KEYSTORE_BASE64
  KEYSTORE_PATH
  KEYSTORE_PASSWORD
  KEY_ALIAS
  KEY_PASSWORD
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --track)
      TRACK="$2"
      shift 2
      ;;
    --release-status)
      RELEASE_STATUS="$2"
      shift 2
      ;;
    --skip-upload)
      SKIP_UPLOAD=true
      shift
      ;;
    --service-account)
      SERVICE_ACCOUNT_JSON="$2"
      shift 2
      ;;
    --package-name)
      PACKAGE_NAME="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$SERVICE_ACCOUNT_JSON" ]]; then
  SERVICE_ACCOUNT_JSON="${PLAY_SERVICE_ACCOUNT:-${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH:-$CODE_ROOT/play-store-key.json}}"
fi

if [[ ! -d "$APP_ROOT" ]]; then
  echo "Missing app directory: $APP_ROOT" >&2
  exit 1
fi

if [[ -n "${ANDROID_KEYSTORE_BASE64:-}" ]]; then
  TMP_KEYSTORE="${TMPDIR:-/tmp}/visischeduler-release-key.jks"
  echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$TMP_KEYSTORE"
  export KEYSTORE_PATH="$TMP_KEYSTORE"
fi

pushd "$APP_ROOT" >/dev/null

if [[ ! -x "./gradlew" ]]; then
  chmod +x ./gradlew
fi

echo "Building Android release bundle..."
./gradlew :androidApp:bundleRelease --no-daemon

AAB_PATH="$(find androidApp/build/outputs/bundle/release -name '*.aab' | sort | tail -1)"
if [[ -z "$AAB_PATH" ]]; then
  echo "Could not find release AAB output." >&2
  exit 1
fi

echo "Built AAB: $AAB_PATH"

if [[ "$SKIP_UPLOAD" == "true" ]]; then
  echo "Upload skipped (--skip-upload)."
  popd >/dev/null
  exit 0
fi

if [[ -z "$SERVICE_ACCOUNT_JSON" ]]; then
  echo "No service account JSON configured. Set --service-account, PLAY_SERVICE_ACCOUNT, or GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH." >&2
  popd >/dev/null
  exit 1
fi

if [[ ! -f "$SERVICE_ACCOUNT_JSON" ]]; then
  echo "Service account JSON not found: $SERVICE_ACCOUNT_JSON" >&2
  popd >/dev/null
  exit 1
fi

if ! command -v fastlane >/dev/null 2>&1; then
  echo "fastlane is required for Play upload. Install with: gem install fastlane" >&2
  popd >/dev/null
  exit 1
fi

echo "Uploading to Google Play track: $TRACK"
fastlane supply \
  --aab "$AAB_PATH" \
  --json_key "$SERVICE_ACCOUNT_JSON" \
  --package_name "$PACKAGE_NAME" \
  --track "$TRACK" \
  --release_status "$RELEASE_STATUS" \
  --skip_upload_apk true \
  --skip_upload_images true \
  --skip_upload_screenshots true \
  --skip_upload_metadata true \
  --skip_upload_changelogs true

popd >/dev/null

if [[ -n "${TMP_KEYSTORE:-}" && -f "${TMP_KEYSTORE:-}" ]]; then
  rm -f "$TMP_KEYSTORE"
fi

echo "Android deployment complete."
