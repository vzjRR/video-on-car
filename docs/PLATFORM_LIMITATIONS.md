# Platform Limitations (authoritative list)

Each item below is classified using the Phase 14 failure taxonomy from the
project brief. "Fixable in this repo" = false means no amount of app code
changes it; it needs an external approval, an OEM/Google rollout, or a
physical test that only the user can run.

## BLOCKED BY PLATFORM

### 1. Native CarPlay video-app template rendering (Movies/Series with a
real in-template video frame, browsable the way Live TV browses today)

- Classification: **#7 CarPlay entitlement limitation** + **#8 vehicle
  capability limitation**.
- Why: requires (a) Apple's CarPlay video entitlement, granted per-app by
  Apple, and (b) the vehicle's head unit to support "video in car" AND the
  automaker to be enrolled in Apple's MFi Program with the feature turned
  on. As of this research, zero vehicles support (b), including all
  Toyotas. See `RESEARCH.md` §2 facts 4–6.
- Fixable in this repo: **No.** This needs Toyota + Apple to ship it.
- What we do instead: browse with the CarPlay **Audio** category
  (`CPListTemplate`/`CPGridTemplate`, artwork + metadata, no in-template
  video frame) and hand off actual playback to AirPlay/external-display
  routing — see `RESEARCH.md` §5 and `RPLAYTV_BEHAVIOR.md`.
- Re-check trigger: watch Apple's CarPlay entitlement page and Toyota's
  software-update notes for "video in car" support; re-evaluate the moment
  either ships.

### 2. Android Automotive OS public "video app" Google Play listing

- Classification: **#7 (Android equivalent) entitlement/review limitation**.
- Why: Google requires meeting car-app quality guidelines and a
  "specific-criteria approval" step before a video app can be listed for
  Android Automotive OS. See `RESEARCH.md` §2 fact 8.
- Fixable in this repo: **No**, this is a Google Play Console review
  process the user must complete outside of code changes. The app code
  itself (manifest `android:appCategory="video"`, parked-only gating) is
  fully implemented and ready in `/android`.
- What we do instead: ship Android for Cars App Library **media-browsing**
  templates now (fully supported, no special review gate for a media
  browse/playback app — this is the same class of integration Spotify/
  Google Podcasts use), and treat the dedicated parked-video surface as an
  upgrade path once the Play Console review is complete.

### 3. Both CarPlay and Android Auto/Automotive entitlement grants

- Classification: **#7 / Android equivalent entitlement limitation**.
- Why: Apple's CarPlay entitlement request (`developer.apple.com/carplay`)
  and Google's car-app review are manual approval processes performed by
  Apple/Google staff, not something an agent or script can grant.
- Fixable in this repo: **No.** Requires the user (as the Apple
  Developer Program / Google Play Console account holder) to submit the
  requests. The exact next steps are in `USER_GUIDE.md`.

## NOT BLOCKED — testable only with real hardware the agent doesn't have

### 4. Whether AirPlay video hand-off actually reaches the Camry screen
through the Carlinkit/VehiConn Mini SE

- Classification: pending — could resolve as **#8 vehicle/adapter
  capability** (confirmed working, in which case this is our shipping
  path) or **#9 adapter limitation** (Carlinkit blocks/ignores AirPlay
  video while in CarPlay mode, in which case we need a fallback).
- Why untested here: this sandbox has no iPhone, no Carlinkit/VehiConn
  Mini SE, and no physical Camry. It cannot be resolved by writing more
  code — see `RPLAYTV_BEHAVIOR.md` for the exact test protocol the user
  must run.
- Fixable in this repo: the **code path** is fully implemented
  (`ios/IPTVCar/Core/Playback/ExternalPlaybackRouter.swift`) and uses only
  public `AVFoundation`/`AVKit` APIs. Whether it *works on this specific
  adapter* is an empirical question, not a code question.

### 5. Exact device/vehicle allowlist for Android's 2026 parked-video rollout

- Classification: **#8 vehicle capability limitation**, unknown scope.
- Why: Google's announcement says "rolling out to compatible vehicles
  later in 2026" without publishing the list. Whether the Camry running
  Android Auto through a RedMagic phone + Carlinkit/VehiConn is in scope
  cannot be determined from documentation.
- Fixable in this repo: No — needs live testing on the actual hardware
  once the rollout reaches the user's phone/vehicle combination.

## Explicitly NOT blocked (do not stop work on these)

- IPTV provider engine (Xtream Codes + M3U), on both platforms: fully
  implementable and testable today, zero platform gate.
- Phone-side native VOD/Series/Live playback (AVPlayer, Media3/ExoPlayer):
  fully implementable and testable today.
- CarPlay Audio-category browsing UI for Movies/Series/Live: fully
  implementable today; entitlement approval is the only external
  dependency, and development/testing can proceed against the CarPlay
  Simulator (Xcode) without waiting for approval, per Apple's docs.
- Android for Cars App Library media-browsing UI for Movies/Series/Live:
  fully implementable and testable today (Android Auto Desktop Head
  Unit / emulator), no special review needed for the browse+play surface.
- Secure credential storage, favorites, resume/continue-watching, search,
  EPG: no platform dependency at all.
