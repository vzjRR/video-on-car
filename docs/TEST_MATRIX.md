# Phase 13 — Test Matrix

## Status legend
- **DONE (sandbox)**: executed by the agent in this development sandbox.
- **PENDING (hardware)**: cannot be executed without the physical iPhone,
  Carlinkit/VehiConn Mini SE, RedMagic phone, and 2021 Camry XSE — the
  agent has none of these. Marked pending until the user runs it.

## Automated / sandbox-verifiable tests (done)

| # | Test | Result |
|---|------|--------|
| 1 | `android/core` unit tests: Xtream JSON parsing (vod streams, series info, lenient field coercion) | PASS — 4/4 |
| 2 | `android/core` unit tests: M3U parsing (live/movie/series classification, catchup attrs, malformed-entry handling, unclassifiable-defaults-to-live) | PASS — 4/4 |
| 3 | `android/core` unit tests: credential redaction (query-string and path-embedded) | PASS — 2/2 |
| 4 | `android/app` full debug build (Compose UI, Media3/ExoPlayer, Android for Cars App Library `CarAppService`, `MediaLibraryService`, EncryptedSharedPreferences) | BUILD SUCCESSFUL — `app-debug.apk` produced |
| 5 | iOS Swift source compiles | **NOT POSSIBLE IN THIS SANDBOX** — Xcode/swiftc requires macOS, unavailable on this Linux host. Code is written to mirror the verified Android logic field-for-field; must be compiled and unit-tested on a Mac before first real run. |

## Hardware test matrix (PENDING — user must execute)

### iPhone → CarPlay → Camry

| Phone | OS version | App version | Vehicle | Adapter | Content type | Playback result | UI result | Video result | Audio result | Parked/driving | Error |
|---|---|---|---|---|---|---|---|---|---|---|---|
| iPhone (model TBD) | TBD | IPTVCar 0.1.0 | 2021 Camry XSE | Carlinkit/VehiConn Mini SE | LIVE | PENDING | PENDING | PENDING | PENDING | PENDING | |
| " | " | " | " | " | MOVIE | PENDING | PENDING | PENDING | PENDING | PENDING | |
| " | " | " | " | " | SERIES | PENDING | PENDING | PENDING | PENDING | PENDING | |
| " | " | " | " | " | EPISODE | PENDING | PENDING | PENDING | PENDING | PENDING | |

**Most important test**: MOVIE → select from the CarPlay list on the Camry
display → playback starts → actual video appears on the Camry screen.
Second most important: SERIES → season → episode → same check.

### Android → Android Auto → Camry

| Phone | OS version | App version | Vehicle | Adapter | Content type | Playback result | UI result | Video result | Audio result | Parked/driving | Error |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RedMagic (model TBD) | TBD | IPTV Car 0.1.0 | 2021 Camry XSE | Carlinkit/VehiConn Mini SE | LIVE | PENDING | PENDING | PENDING | PENDING | PENDING | |
| " | " | " | " | " | MOVIE | PENDING | PENDING | PENDING | PENDING | PENDING | |
| " | " | " | " | " | SERIES | PENDING | PENDING | PENDING | PENDING | PENDING | |
| " | " | " | " | " | EPISODE | PENDING | PENDING | PENDING | PENDING | PENDING | |

## How to fill this in

1. Install the built APK (`android/app/build/outputs/apk/debug/app-debug.apk`)
   on the RedMagic phone, add a real Xtream or M3U provider in Settings,
   connect to the Camry via the Carlinkit/VehiConn Mini SE, open Android
   Auto, and go through each content type.
2. Build the iOS app in Xcode on a Mac (see `BUILD.md`), request the
   CarPlay Audio entitlement from Apple, run on a real iPhone connected to
   the same adapter/vehicle, and go through each content type. Also run
   the black-box protocol in `RPLAYTV_BEHAVIOR.md` first — its result
   determines whether the AirPlay hand-off in `ExternalPlaybackRouter`
   is expected to succeed at all on this hardware.
3. For any PENDING row that fails, classify the failure using the Phase
   14 taxonomy in `PLATFORM_LIMITATIONS.md` before reporting it — not
   just "doesn't work."
