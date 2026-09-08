# User Guide

## Setting up an IPTV provider

Open Settings in the phone app (iOS or Android) and add either:

- **Xtream Codes**: server URL, username, password.
- **M3U**: a playlist URL or local file.

Credentials are stored in the Keychain (iOS) or Android Keystore-backed
encrypted storage (Android) and never shown again in plain text after
saving — see `docs/PLATFORM_LIMITATIONS.md`/`ARCHITECTURE.md` "Security
boundary."

## Using Live TV / Movies / Series on the phone

Home → Live / Movies / Series → pick a category → pick an item → it
plays natively (AVPlayer on iOS, Media3/ExoPlayer on Android), streaming
directly from your provider.

## Using it in the car

- **iOS + CarPlay**: connect the Carlinkit/VehiConn Mini SE, open the
  IPTV Car icon on the Camry's CarPlay screen, browse Live TV / Movies /
  Series the same way as the phone. Selecting an item starts playback;
  video reaches the Camry screen via AirPlay hand-off (pending the
  hardware confirmation described in `RPLAYTV_BEHAVIOR.md` — if it
  doesn't appear automatically, use the small AirPlay route-picker icon
  shown in the corner of the phone's own player screen to explicitly pick
  the car as the target).
- **Android + Android Auto**: connect the same adapter, open Android
  Auto on the Camry head unit, and the app appears as a media app with
  Live TV / Movies / Series as browsable categories (via the Android for
  Cars App Library / MediaLibraryService — see `ARCHITECTURE.md`).

You never need to type your IPTV credentials from the car — the vehicle
UI only ever operates on a session already authenticated on the phone.

## Requesting the CarPlay entitlement (Apple)

1. Go to `developer.apple.com/carplay` and submit a request, choosing the
   **Audio** category (this repo's CarPlay integration targets Audio, not
   the currently-nonfunctional Video category — see `RESEARCH.md` §2).
2. Wait for Apple's review/approval (this is a manual process; timeline
   is not published by Apple).
3. Once granted, add the entitlement to your app's provisioning profile in
   Xcode and rebuild — no code changes are needed in this repo for that
   step.

## Requesting Android Automotive OS video-app listing (Google) — optional

Only needed if you want the dedicated parked-video surface (§2 fact 7-8 in
`RESEARCH.md`) instead of (or in addition to) the already-working
MediaLibraryService browsing surface:

1. Meet Google's [car-app quality guidelines for video apps](https://developer.android.com/docs/quality-guidelines/car-app-quality?category=video).
2. Submit the app for the "specific-criteria approval" step Google
   requires before an Android Automotive OS video app can be listed on
   Google Play.
3. This is a Play Console process outside of this codebase; the manifest
   flag (`android:appCategory="video"`, `automotive_app_desc.xml`) is
   already in place and ready for that review.

## If Movies/Series still don't show up in the car

Work through `docs/PLATFORM_LIMITATIONS.md` and classify the failure using
the taxonomy in that document (auth failure vs. metadata failure vs.
entitlement limitation vs. vehicle capability, etc.) before assuming it's
"CarPlay doesn't support this" — most failure modes are fixable in this
app; only the items explicitly marked "BLOCKED BY PLATFORM" are not.
