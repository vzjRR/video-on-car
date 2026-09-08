# Build Instructions

## Android

Verified in this repo's development sandbox with a headless Android SDK
(API 34 platform, build-tools 34.0.0, JDK 21, Gradle 8.14.3) — see
`docs/TEST_MATRIX.md` row 4.

```bash
cd android
# One-time: create local.properties pointing at your Android SDK
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Run the platform-independent parser/model unit tests (no SDK needed):
./gradlew :core:test

# Build the full app (Compose UI, Media3, Android for Cars App Library,
# MediaLibraryService, encrypted credential storage):
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

To test the Android Auto surfaces without a car: install the [Android
Auto Desktop Head Unit](https://developer.android.com/training/cars/testing)
or Android Automotive OS emulator image via Android Studio's AVD manager,
then run the app with the phone/emulator connected to it.

To test the real vehicle path: install `app-debug.apk` on the RedMagic
phone, add a provider in Settings, connect the Carlinkit/VehiConn Mini SE,
open Android Auto on the Camry head unit, and follow `TEST_MATRIX.md`.

## iOS

**Not buildable in this repo's development sandbox** — Xcode and `swiftc`
require macOS, and this environment is Linux-only. The `ios/IPTVCar`
source tree is written to mirror the Android `:core` logic field-for-field
(same Xtream/M3U parsing rules, same normalized model, same redaction
rules) but has not been compiled or unit-tested by the agent. Build it on
a Mac:

1. Open Xcode → File → New → Project → iOS App (SwiftUI, Swift).
2. Name it `IPTVCar`, then delete the generated placeholder files and add
   the existing groups from `ios/IPTVCar/` (`App`, `Core`, `CarPlay`) via
   File → Add Files to "IPTVCar"... — check "Copy items if needed" off
   since the files already live in this repo.
3. Add a CarPlay scene to the target: in the target's Signing & Capabilities
   tab, no capability toggle exists for CarPlay itself — instead you must
   have the CarPlay entitlement provisioned on your Apple Developer account
   (see `USER_GUIDE.md` "Requesting the CarPlay entitlement") and add a
   `UIApplicationSceneManifest` entry in Info.plist for
   `CPTemplateApplicationSceneSessionRoleApplication` pointing
   `CarPlaySceneDelegate` at it, per Apple's CarPlay Programming Guide.
4. Build and run on a real iPhone (the CarPlay Simulator in Xcode can
   exercise the browsing templates before the entitlement is granted, per
   Apple's own guidance that CarPlay development doesn't require the
   entitlement to be present yet for simulator testing).
5. Run on-device against the real Camry only after completing the
   Phase 6 hardware test protocol in `RPLAYTV_BEHAVIOR.md`.

## Shared fixtures

`tests/fixtures/` holds the Xtream JSON and M3U samples both platforms'
parsers are tested against; `android/core/src/test/resources/fixtures/`
is a copy consumed by the JVM test suite. If you add a new fixture, add
it in both places (or symlink) so iOS and Android tests stay aligned once
the iOS test target exists.
