# Phase 0 Research — CarPlay / Android Auto VOD Feasibility

Researched: 2026-09-07. Sources are cited inline; anything not sourced is
explicitly marked as an assumption, inference, or unknown. This document
governs the architecture used in the rest of the repo — read it before
changing CarPlay/Android Auto integration code.

## 1. Executive summary

- **Native CarPlay "video app" templates (the official way to browse and
  play Movies/Series with a real video frame in the car's own UI) exist in
  the API surface since iOS 26, but are inert in the real world today.**
  Apple requires the **vehicle head unit** (not just the app) to support a
  "video in car" feature, and the **automaker must be enrolled in Apple's
  MFi Program and explicitly enable it**. As of this research, **zero
  vehicles support it**, including every current Toyota. This is a hardware
  ecosystem gap, not something fixable by better app code.
  [Apple — CarPlay entitlements](https://developer.apple.com/documentation/carplay/requesting-carplay-entitlements),
  [xda-developers](https://www.xda-developers.com/carplay-might-finally-get-video-apps-soon-but-there-will-be-limitations/)
- **Android Auto/Automotive gained an equivalent capability in 2026**: a
  first-party "parked video app" surface (`android:appCategory="video"`,
  parked-only by default). Unlike Apple's version, this does **not**
  require OEM/MFi-style vehicle hardware enablement — it is a phone/host
  software capability — **but it does require Google Play approval against
  car-app quality guidelines before the app can be listed as a
  video app for Android Automotive OS**, and the general rollout is still
  described as "rolling out to compatible vehicles later in 2026."
  [Android Developers — Build video apps for Android Automotive OS](https://developer.android.com/training/cars/parked/video),
  [Android Developers Blog, May 2026](https://android-developers.googleblog.com/2026/05/android-for-cars-unifying-platforms-premium-experiences.html)
- **The Carlinkit/VehiConn Mini SE is not a certified MFi head-unit
  integration.** It is a third-party wireless CarPlay adapter that
  emulates a head unit to the phone and feeds video to the factory Toyota
  display through its own (undocumented) firmware. Because it sits outside
  Apple's official OEM gate, it is the most plausible explanation for how
  **rPlayTV already gets live video onto the Camry screen** despite no
  vehicle in the world officially supporting CarPlay video yet — see
  §5 and `RPLAYTV_BEHAVIOR.md`. This is an **inference**, not a confirmed
  fact, and is the top priority to validate empirically in Phase 6.
- **Bottom line architecture**: build the CarPlay **audio-app** entitlement
  (self-service today, broadly granted) for Movies/Series browsing
  (`CPListTemplate`/`CPGridTemplate` metadata, artwork, "Now Playing"), and
  drive the actual video pixels to the Camry via **AirPlay** (standard
  `AVPlayer` external playback / `AVRoutePickerView`), which is the same
  mechanism the Carlinkit dongle already appears to use for rPlayTV's Live
  TV video. On Android, use the officially supported **Android for Cars
  App Library** media-browsing templates plus the new 2026 parked-video
  surface, and treat Google Play's car-app review as the (documented,
  non-hackable) gate. Both paths are fully within public, documented APIs.

## 2. Confirmed facts

### Apple / CarPlay

| # | Fact | Source |
|---|------|--------|
| 1 | CarPlay entitlements are **not self-service** in the normal Apple Developer Portal sense: you submit a request at `developer.apple.com/carplay` naming the **category** you want (Audio, Communication, Navigation, Parking, EV Charging, Quick Food Ordering), and Apple reviews/grants it. | [Requesting CarPlay Entitlements](https://developer.apple.com/documentation/carplay/requesting-carplay-entitlements) |
| 2 | An app can be granted **exactly one category's templates**, chosen at request time — you cannot mix Navigation templates into an Audio-category app, etc. | Same |
| 3 | A distinct **CarPlay video app entitlement** exists and can be combined with a CarPlay audio entitlement in the same app. | Same |
| 4 | CarPlay video apps **only appear on the CarPlay home screen if the car itself supports the "video in car" feature**. AirPlay video (which is what CarPlay video apps use under the hood) **only works while parked**, and critically: **"automakers will need to enable the feature and be part of Apple's MFi Program."** | [Apple Developer](https://developer.apple.com/), corroborated by [xda-developers](https://www.xda-developers.com/carplay-might-finally-get-video-apps-soon-but-there-will-be-limitations/) |
| 5 | As of this research, **no vehicle from any automaker has enabled CarPlay video**, despite the capability shipping in iOS 26 (~Sept 2025). No automaker has publicly committed a date. | [xda-developers](https://www.xda-developers.com/carplay-might-finally-get-video-apps-soon-but-there-will-be-limitations/), [9to5Mac](https://9to5mac.com/2026/05/06/carplay-video-ios-26/) |
| 6 | The 2021 Toyota Camry XSE's factory head unit predates this entire feature generation (its CarPlay stack shipped years before iOS 26) and has received no public announcement of a retrofit update path for MFi video-in-car support. | Vehicle age / absence of any Toyota announcement (inference from #5, not a Toyota-specific document) |

### Google / Android Auto

| # | Fact | Source |
|---|------|--------|
| 7 | Android Auto/Automotive OS added a genuine **parked video app** surface in 2026: declare `android:appCategory="video"` in the manifest; the host enforces **parked-only** playback via the DD-2 quality guideline (pauses automatically when driving-restrictions are active). | [Build video apps for Android Automotive OS](https://developer.android.com/training/cars/parked/video) |
| 8 | Shipping a video app for Android Automotive OS **requires meeting Google's car-app quality guidelines and passing a specific-criteria approval step before Google Play listing** — this is a real review gate, not merely a manifest flag. | Same |
| 9 | Widevine L3 DRM is supported in this surface; background "audio while driving" is a separate, early-access-only beta feature and not required for our use case. | Same |
| 10 | The **Android for Cars App Library** (`androidx.car.app`) already supports first-class, driving-safe **media browsing templates** (categories → lists → playback) via `MediaLibraryService`/`MediaSession`, independent of the 2026 parked-video surface — this is the mechanism for Live TV/Movies/Series *browsing* regardless of whether full-frame parked video is enabled on a given device yet. | [Extend your media app to Android for Cars](https://developer.android.com/media/implement/surfaces/cars), [ParkedOnlyOnClickListener](https://developer.android.com/reference/androidx/car/app/model/ParkedOnlyOnClickListener) |
| 11 | The May 2026 Android Auto announcement is explicit that "for the first time... users will be able to sit back, relax, and watch videos while parked," rolling out to **compatible vehicles later in 2026**, on phones running **Android 17+**. No OEM/head-unit MFi-equivalent gate is mentioned for this — the gating described is Google Play review + phone/host software version, not vehicle hardware certification. | [Android Developers Blog](https://android-developers.googleblog.com/2026/05/android-for-cars-unifying-platforms-premium-experiences.html) |

### Toyota / Carlinkit / observed behavior

| # | Fact | Source |
|---|------|--------|
| 12 | rPlayTV already renders live IPTV video on the physical 2021 Camry XSE screen through the Carlinkit/VehiConn Mini SE, per the user's direct hands-on test. | User-reported, first-hand |
| 13 | Independent wireless CarPlay adapters (the category Carlinkit/VehiConn belongs to) are documented in the press as **already bypassing Apple's official video-in-car gate** to deliver streaming video, because they are not MFi-certified head units and don't enforce Apple's OEM-side restrictions the way a real car's infotainment system would. | [xda-developers](https://www.xda-developers.com/carplay-might-finally-get-video-apps-soon-but-there-will-be-limitations/) ("independent wireless CarPlay adapters with custom OS overlays already bypass official channels to deliver streaming video") |

## 3. Assumptions (not directly sourced, used for planning)

- Carlinkit Mini SE most likely presents itself to iOS as **both** a
  CarPlay receiver and a general **AirPlay/mirroring sink**, and rPlayTV's
  "Live TV" video is arriving on the Camry screen via that AirPlay/mirror
  path rather than via Apple's (currently nonexistent-in-the-wild)
  CarPlay-video-entitlement template pipeline. This must be validated in
  Phase 6 (see `RPLAYTV_BEHAVIOR.md`) — it is the single most important
  unknown in this project, because it determines whether our own app can
  reuse the same path.
- The Camry's factory display, being driven entirely through Carlinkit's
  emulated head unit, has no independent opinion about "video in car" —
  whatever Carlinkit feeds it, it shows. This is favorable for us: the
  bottleneck is what the **phone + adapter** will do, not the car itself.
- A CarPlay **Audio** category entitlement is realistically obtainable for
  an IPTV app (many indie audio-streaming apps hold one) and imposes no
  content-type restriction on what plays once AirPlay/external playback
  takes over — the entitlement only gates which *templates* render in the
  CarPlay UI, not what the phone is allowed to do with its own screen or
  AirPlay session.

## 4. Unknowns requiring hands-on testing (cannot be resolved from docs)

1. Does the Carlinkit Mini SE actually run a parallel AirPlay receiver
   session alongside its CarPlay emulation? (Primary unknown — see
   `RPLAYTV_BEHAVIOR.md` test protocol.)
2. Does `AVPlayer.allowsExternalPlayback` / `AVRoutePickerView` on iOS
   surface the Camry/Carlinkit as a target the same way it would surface
   an Apple TV?
3. Whether Apple's CarPlay **Audio** entitlement request for this specific
   app (an IPTV client) will be granted in a reasonable time — Apple's
   review criteria for what counts as "audio content" are not fully public.
4. Whether the 2026 Android Auto parked-video Play Store review gate is
   reachable for a sideloaded/internal-testing build, or only for public
   listings (needs a real Play Console test).
5. Exact device/vehicle allowlist for the Android parked-video rollout
   ("compatible vehicles" is undefined in the public post).

## 5. Recommended architecture (this repo implements this)

```
Browsing (categories, posters, titles, metadata)
    → CarPlay Audio-category templates (iOS) / Android for Cars
      App Library media-browse templates (Android)
      [Officially supported today, no vehicle hardware dependency]

Video pixels on the car screen
    → iOS: AVPlayer with external playback enabled, routed via
      AirPlay (AVRoutePickerView / automatic external-screen routing)
      to whatever the Carlinkit adapter exposes.
    → Android: Media3/ExoPlayer inside the phone-side Activity/Compose UI
      today; migrate to the native Android-Automotive parked "video"
      app category once (a) Google Play car-app review is completed and
      (b) the target vehicle is confirmed on the 2026 rollout allowlist.

Everything else (auth, metadata, provider parsing, secure storage,
favorites, resume, search) is identical to a normal mobile IPTV app and
has zero platform-specific blockers.
```

This keeps every blocked piece isolated (see `PLATFORM_LIMITATIONS.md`)
while shipping a fully working phone app plus real, standards-compliant
CarPlay/Android Auto browsing UIs immediately.

## 6. What is BLOCKED BY PLATFORM vs. buildable now

- BLOCKED BY PLATFORM (needs external approval and/or ecosystem rollout,
  not fixable in code): native CarPlay video-app template rendering on
  the Camry (needs Toyota+Apple MFi enablement, §2 fact 4–6); Android
  Automotive OS public "video app" Play Store listing (needs Google
  review, §2 fact 8); CarPlay/Google Play entitlement grants themselves
  (need Apple/Google approval, not code).
- Buildable and testable now, no external approval required: IPTV
  provider engine (Xtream + M3U), all VOD/Series/Live metadata browsing,
  secure credential storage, phone-side native playback (AVPlayer /
  Media3), CarPlay Audio-category browsing templates, Android for Cars
  App Library media-browse templates, resume/favorites/continue-watching,
  and the AirPlay-based video hand-off hypothesis for iOS (pending the
  Phase 6 hardware validation in `RPLAYTV_BEHAVIOR.md`).

## Sources

- [Requesting CarPlay Entitlements — Apple Developer](https://developer.apple.com/documentation/carplay/requesting-carplay-entitlements)
- [CarPlay — Apple Developer](https://developer.apple.com/carplay/)
- [CarPlay might finally get video apps soon — but there will be limitations — xda-developers](https://www.xda-developers.com/carplay-might-finally-get-video-apps-soon-but-there-will-be-limitations/)
- [CarPlay in iOS 26 promises video playback feature — 9to5Mac](https://9to5mac.com/2026/05/06/carplay-video-ios-26/)
- [Build video apps for Android Automotive OS — Android Developers](https://developer.android.com/training/cars/parked/video)
- [Build parked apps for cars — Android Developers](https://developer.android.com/training/cars/parked)
- [ParkedOnlyOnClickListener — Android Developers](https://developer.android.com/reference/androidx/car/app/model/ParkedOnlyOnClickListener)
- [Extend your media app to Android for Cars — Android Developers](https://developer.android.com/media/implement/surfaces/cars)
- [Android for Cars: Unifying platforms and unlocking premium experiences — Android Developers Blog, May 2026](https://android-developers.googleblog.com/2026/05/android-for-cars-unifying-platforms-premium-experiences.html)
