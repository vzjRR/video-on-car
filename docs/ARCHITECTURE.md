# Architecture

## Monorepo layout

```
/ios       Native Swift/SwiftUI app + CarPlay scene
/android   Native Kotlin/Compose app + Android for Cars App Library
/shared    Platform-agnostic specs: normalized model, parsing rules,
           sample fixtures. Not compiled code shared at build time
           (Swift and Kotlin can't share a binary artifact) — instead a
           single spec that both native implementations are written
           against, so behavior stays identical.
/docs      RESEARCH, ARCHITECTURE, PLATFORM_LIMITATIONS, RPLAYTV_BEHAVIOR,
           TEST_MATRIX, BUILD, USER_GUIDE
/tests     Fixture-driven parser tests (Xtream JSON + M3U samples) usable
           from both native test suites
```

## Data flow

```
IPTV Provider (Xtream Codes API or M3U playlist)
        │
        ▼
Provider Client (auth, HTTP, retry)      [shared/parsing-spec + per-platform impl]
        │
        ▼
Normalized Model (Provider/Category/LiveChannel/Movie/Series/Season/Episode/EPGEvent)
        │
        ├──► Phone UI (SwiftUI / Compose): Home, Live, Movies, Series,
        │    Favorites, Continue Watching, Settings
        │
        └──► Vehicle browsing UI:
                iOS:    CarPlay Audio-category templates (CPListTemplate /
                        CPGridTemplate / CPNowPlayingTemplate)
                Android: Android for Cars App Library media-browse
                        templates (MediaLibraryService) + optional
                        parked "video" app category once Play review
                        clears (see PLATFORM_LIMITATIONS.md)
        │
        ▼
Playback Engine (native decoder first: AVPlayer / Media3-ExoPlayer)
        │
        ▼
Video transport to vehicle screen:
    iOS:     AVPlayer external playback → AirPlay → Carlinkit/VehiConn
             adapter → Camry display  (hypothesis pending hardware
             confirmation, see RPLAYTV_BEHAVIOR.md)
    Android: Media3 surface rendered directly in the Android Auto/
             Automotive host template once the parked-video surface is
             available; falls back to phone-screen playback otherwise.
```

## Why not a shared compiled core

Swift and Kotlin cannot share a single compiled library without adding a
cross-compilation toolchain (KMP, C++ core, etc.), which is unnecessary
complexity for this project's size and violates the "don't add abstractions
beyond what the task requires" principle. Instead, `/shared` pins down the
exact normalized model and Xtream/M3U parsing rules once, in
implementation-neutral form (JSON Schema + prose + fixtures), and both the
Swift and Kotlin codebases implement it natively. `/tests` fixtures are
consumed by both test suites so behavior can't silently drift.

## Security boundary

Credentials (Xtream server/username/password, or a remote M3U URL that
embeds credentials) are:

- Written only to Keychain (iOS) / EncryptedSharedPreferences backed by
  Android Keystore (Android).
- Never logged. All logging goes through a redaction wrapper
  (`Core/Security/Redaction`) that masks query params matching
  `username`, `password`, `token` before any log line is emitted.
- Never embedded in crash reports, analytics, or CarPlay/Android Auto UI
  text (the vehicle UI shows only titles/artwork/duration, never the
  provider URL).
- Not required to be re-typed from the car: the vehicle UI only ever
  operates on an already-authenticated session created on the phone.

## Playback engine

Both platforms implement the same responsibilities:

1. Try native decode first (`AVPlayer` / Media3 `ExoPlayer` with platform
   codecs) for HLS, MPEG-TS, and MP4 — all three are natively supported by
   both AVFoundation and Media3/ExoPlayer, so no custom decoder is needed.
2. Stream directly from the provider URL; never pre-download a full file
   before starting playback.
3. Persist resume position keyed by `(providerId, contentType, contentId)`
   with `positionMs`, `durationMs`, `updatedAt` for Continue Watching.
4. Surface buffering/error state distinctly from "not authenticated" or
   "not found," per the Phase 14 failure taxonomy.
