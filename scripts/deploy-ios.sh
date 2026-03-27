#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODE_ROOT="$(cd "$REPO_ROOT/.." && pwd)"
APP_ROOT="$REPO_ROOT/VisiScheduler"
IOS_ROOT="$APP_ROOT/iosApp"
WORKSPACE="$IOS_ROOT/iosApp.xcworkspace"
SCHEME="${IOS_SCHEME:-iosApp}"
CONFIGURATION="Release"
SKIP_UPLOAD=false

API_KEY_PATH="${API_KEY_PATH:-}"
API_KEY_ID="${API_KEY_ID:-}"
API_ISSUER_ID="${API_ISSUER_ID:-}"

SHARED_ENV_FILE="$CODE_ROOT/.deploy-config/deploy.env"
if [[ -f "$SHARED_ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$SHARED_ENV_FILE"
fi

usage() {
  cat <<'EOF'
Usage:
  ./scripts/deploy-ios.sh [options]

Options:
  --scheme <name>              Xcode scheme (default: iosApp)
  --skip-upload                Build only, do not upload to TestFlight
  --api-key-path <path>        App Store Connect private key (.p8)
  --api-key-id <id>            App Store Connect API key id
  --api-issuer-id <id>         App Store Connect issuer id
  -h, --help                   Show help

Environment:
  APP_STORE_CONNECT_API_KEY_PATH
  APP_STORE_CONNECT_API_KEY_ID
  APP_STORE_CONNECT_API_ISSUER_ID
  ASC_KEY_PATH
  ASC_KEY_ID
  ASC_ISSUER_ID
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --scheme)
      SCHEME="$2"
      shift 2
      ;;
    --skip-upload)
      SKIP_UPLOAD=true
      shift
      ;;
    --api-key-path)
      API_KEY_PATH="$2"
      shift 2
      ;;
    --api-key-id)
      API_KEY_ID="$2"
      shift 2
      ;;
    --api-issuer-id)
      API_ISSUER_ID="$2"
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

if [[ -z "$API_KEY_PATH" ]]; then
  API_KEY_PATH="${APP_STORE_CONNECT_API_KEY_PATH:-${ASC_KEY_PATH:-$HOME/.config/AuthKey_TB52W6Z8MK.p8}}"
fi
if [[ -z "$API_KEY_ID" ]]; then
  API_KEY_ID="${APP_STORE_CONNECT_API_KEY_ID:-${ASC_KEY_ID:-TB52W6Z8MK}}"
fi
if [[ -z "$API_ISSUER_ID" ]]; then
  API_ISSUER_ID="${APP_STORE_CONNECT_API_ISSUER_ID:-${ASC_ISSUER_ID:-69a6de8a-a43a-47e3-e053-5b8c7c11a4d1}}"
fi

if [[ ! -d "$APP_ROOT" ]]; then
  echo "Missing app directory: $APP_ROOT" >&2
  exit 1
fi

if ! command -v xcodebuild >/dev/null 2>&1; then
  echo "xcodebuild is required for iOS deployment." >&2
  exit 1
fi

pushd "$APP_ROOT" >/dev/null

if [[ ! -x "./gradlew" ]]; then
  chmod +x ./gradlew
fi

echo "Generating shared podspec..."
./gradlew :shared:podspec --no-daemon

echo "Installing CocoaPods dependencies..."
pushd "$IOS_ROOT" >/dev/null
pod install --repo-update
popd >/dev/null

STAMP="$(date +%Y%m%d-%H%M%S)"
ARCHIVE_PATH="${TMPDIR:-/tmp}/visischeduler-${STAMP}.xcarchive"
EXPORT_PATH="${TMPDIR:-/tmp}/visischeduler-${STAMP}-export"
EXPORT_OPTIONS="${TMPDIR:-/tmp}/visischeduler-${STAMP}-ExportOptions.plist"

cat > "$EXPORT_OPTIONS" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>method</key>
  <string>app-store</string>
  <key>signingStyle</key>
  <string>automatic</string>
  <key>uploadBitcode</key>
  <false/>
</dict>
</plist>
PLIST

AUTH_ARGS=()
if [[ -n "$API_KEY_PATH" && -n "$API_KEY_ID" && -n "$API_ISSUER_ID" ]]; then
  AUTH_ARGS+=(
    -authenticationKeyPath "$API_KEY_PATH"
    -authenticationKeyID "$API_KEY_ID"
    -authenticationKeyIssuerID "$API_ISSUER_ID"
  )
fi

echo "Archiving iOS app..."
xcodebuild archive \
  -workspace "$WORKSPACE" \
  -scheme "$SCHEME" \
  -configuration "$CONFIGURATION" \
  -destination "generic/platform=iOS" \
  -archivePath "$ARCHIVE_PATH" \
  -allowProvisioningUpdates \
  "${AUTH_ARGS[@]}"

echo "Exporting IPA..."
xcodebuild -exportArchive \
  -archivePath "$ARCHIVE_PATH" \
  -exportPath "$EXPORT_PATH" \
  -exportOptionsPlist "$EXPORT_OPTIONS" \
  -allowProvisioningUpdates \
  "${AUTH_ARGS[@]}"

IPA_PATH="$(find "$EXPORT_PATH" -maxdepth 1 -name '*.ipa' | head -1)"
if [[ -z "$IPA_PATH" ]]; then
  echo "Could not find exported IPA." >&2
  exit 1
fi

echo "Built IPA: $IPA_PATH"

if [[ "$SKIP_UPLOAD" == "true" ]]; then
  echo "Upload skipped (--skip-upload)."
  popd >/dev/null
  exit 0
fi

if [[ -z "$API_KEY_ID" || -z "$API_ISSUER_ID" ]]; then
  echo "Missing App Store Connect credentials. Set --api-key-id and --api-issuer-id." >&2
  popd >/dev/null
  exit 1
fi

UPLOAD_CMD=(xcrun altool --upload-app --type ios --file "$IPA_PATH" --apiKey "$API_KEY_ID" --apiIssuer "$API_ISSUER_ID")
if [[ -n "$API_KEY_PATH" ]]; then
  UPLOAD_CMD+=(--apiKeyPath "$API_KEY_PATH")
fi

echo "Uploading to TestFlight..."
"${UPLOAD_CMD[@]}"

popd >/dev/null
echo "iOS deployment complete."
