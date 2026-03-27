# KindVisit — App Store Distribution Content

---

## APP STORE (Apple)

**App Name:** KindVisit
**Subtitle** *(30 chars max):* Visits for patients & residents

**Description** *(4000 chars max):*
KindVisit makes it easy to schedule and manage visits for loved ones in hospitals, care facilities, and assisted living communities — so caregivers can focus on what matters most.

Whether you're coordinating visits for a family member, managing a care facility, or visiting someone you love, KindVisit handles the scheduling so you don't have to.

**FOR VISITORS**
• Request visit time slots in seconds
• View upcoming and past visits at a glance
• Receive notifications when visits are approved or updated
• Check in and check out with a simple QR code scan
• Schedule video calls, in-person visits, and window visits

**FOR COORDINATORS & CAREGIVERS**
• Approve or deny visit requests with one tap
• Set and enforce visit restrictions — time windows, frequency limits, capacity caps, and medical clearances
• Manage visitor lists and approval levels
• View a full calendar of scheduled visits by day, week, or month
• Send messages directly to visitors from within the app

**FOR FACILITIES**
• Multi-role access: Admin, Primary Coordinator, Secondary Coordinator, Approved Visitor
• Customizable restriction engine for each resident or beneficiary
• QR code check-in for contactless visit tracking
• Automatic enforcement of capacity limits and rest periods
• Full visit history with notes and mood observations

**FEATURES**
• Secure login with biometric authentication (Face ID / Touch ID)
• Real-time notifications for visit updates
• Works offline — syncs automatically when back online
• Clean, accessible design built for all ages

KindVisit is built with care for the people who give it.

**Keywords** *(100 chars max, comma-separated):*
visit scheduling,care facility,hospital visits,caregiver,patient visits,coordinator,elder care

**Support Email:** markduenas@gmail.com
**Support URL:** https://www.markduenas.com/projects/visitingscheduler/
**Marketing URL:** https://www.markduenas.com/projects/visitingscheduler/
**Privacy Policy URL:** https://www.markduenas.com/privacy/

**What's New (v1.0.1):**
• Fixed app launch issue on first install
• Stability improvements

**Primary Category:** Medical
**Secondary Category:** Productivity

**Age Rating:** 4+
**Content:** No objectionable content

---

## GOOGLE PLAY

**App Name:** KindVisit
**Short Description** *(80 chars max):*
Schedule and manage care facility visits for patients and residents.

**Full Description** *(4000 chars max):*
KindVisit makes it easy to schedule and manage visits for loved ones in hospitals, care facilities, and assisted living communities — so caregivers can focus on what matters most.

Whether you're coordinating visits for a family member, managing a care facility, or visiting someone you love, KindVisit handles the scheduling so you don't have to.

FOR VISITORS
• Request visit time slots in seconds
• View upcoming and past visits at a glance
• Receive notifications when visits are approved or updated
• Check in and check out with a simple QR code scan
• Schedule video calls, in-person visits, and window visits

FOR COORDINATORS & CAREGIVERS
• Approve or deny visit requests with one tap
• Set and enforce visit restrictions — time windows, frequency limits, capacity caps, and medical clearances
• Manage visitor lists and approval levels
• View a full calendar of scheduled visits by day, week, or month
• Send messages directly to visitors from within the app

FOR FACILITIES
• Multi-role access: Admin, Primary Coordinator, Secondary Coordinator, Approved Visitor
• Customizable restriction engine for each resident or beneficiary
• QR code check-in for contactless visit tracking
• Automatic enforcement of capacity limits and rest periods
• Full visit history with notes and mood observations

FEATURES
• Secure login with biometric authentication (fingerprint / face unlock)
• Real-time notifications for visit updates
• Works offline — syncs automatically when back online
• Clean, accessible design built for all ages

KindVisit is built with care for the people who give it.

**Category:** Medical
**Content Rating:** Everyone
**Privacy Policy URL:** https://www.markduenas.com/privacy/

**What's New (1.0.1):**
• Fixed app launch issue on first install
• Stability improvements

---

## DATA SAFETY / PRIVACY NUTRITION LABELS

### Data Collected & Linked to Identity
| Data Type       | Purpose                          |
|-----------------|----------------------------------|
| Name, Email     | Account creation & login         |
| User ID         | Identify user across sessions    |
| Visit history   | Core app functionality           |
| Messages        | In-app coordinator communication |
| Device ID       | Crash reporting (Crashlytics)    |
| Analytics data  | Firebase Analytics (aggregated)  |

### Data NOT Collected
- Location (precise or coarse)
- Contacts
- Photos / Files
- Browsing history
- Financial info (ad removal purchase processed by Google/Apple)

### Third-Party SDKs That Access Data
- **Firebase Auth** — authentication
- **Firebase Firestore** — data storage
- **Firebase Crashlytics** — crash reporting
- **Firebase Analytics** — usage analytics
- **AdMob** — advertising (until ad removal purchased)
- **Google Play Billing / StoreKit** — in-app purchase

---

## FEATURE GRAPHIC (Google Play — 1024×500px)
Suggested copy: "Caring visits, made simple" on brand primary color background with app icon centered.

## DEPLOYMENT SCRIPTS

Both platforms have deploy scripts in `scripts/`. They require credentials outside the repo.

### Android — `scripts/deploy-android.sh`
```bash
# Internal testing (default)
./scripts/deploy-android.sh

# Other tracks
./scripts/deploy-android.sh --track alpha
./scripts/deploy-android.sh --track production

# Build only, no upload
./scripts/deploy-android.sh --skip-upload
```
**Requires:**
- `fastlane` installed (`gem install fastlane`)
- `../play-store-key.json` — Google Play service account JSON key
- Signing config in `VisiScheduler/local.properties`

**Credentials lookup order:** `--service-account` flag → `PLAY_SERVICE_ACCOUNT` env var → `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH` env var → `../play-store-key.json`

### iOS — `scripts/deploy-ios.sh`
```bash
# Build and upload to TestFlight
./scripts/deploy-ios.sh

# Build only
./scripts/deploy-ios.sh --skip-upload
```
**Requires:**
- Xcode + `xcodebuild`
- App Store Connect API key (`.p8` file)
- Default key path: `~/.config/AuthKey_TB52W6Z8MK.p8`
- Key ID: `TB52W6Z8MK` / Issuer ID: `69a6de8a-a43a-47e3-e053-5b8c7c11a4d1`

**Override via flags:** `--api-key-path`, `--api-key-id`, `--api-issuer-id`
**Or env vars:** `APP_STORE_CONNECT_API_KEY_PATH`, `APP_STORE_CONNECT_API_KEY_ID`, `APP_STORE_CONNECT_API_ISSUER_ID`

---

## SCREENSHOTS NEEDED
**Android (Phone):** 1080×1920 or 1080×2340 — at least 2, up to 8
**iOS (6.7" — required):** 1290×2796 (iPhone 16 Pro Max)
**iOS (6.5" — recommended):** 1242×2688 (iPhone 11 Pro Max)

Suggested screens to capture:
1. Splash / Launch screen
2. Login screen
3. Dashboard with upcoming visits
4. Calendar view
5. Schedule a visit flow
6. Visit detail / approval screen
7. QR check-in screen
8. Settings / profile
