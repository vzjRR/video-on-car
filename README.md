# IPTV Car

A native IPTV app (Xtream Codes + M3U) built to expose Live TV, Movies,
and Series to CarPlay and Android Auto — no PC bridge, no proxy server,
no extra hardware. See `docs/RESEARCH.md` for why Movies/Series don't
show up in CarPlay today in apps like rPlayTV, and what this repo does
about it within official Apple/Google APIs.

## Start here

- `docs/RESEARCH.md` — platform research and the recommended architecture
- `docs/PLATFORM_LIMITATIONS.md` — exactly what's blocked and why
- `docs/RPLAYTV_BEHAVIOR.md` — hardware test protocol (pending execution)
- `docs/ARCHITECTURE.md` — how the pieces fit together
- `docs/BUILD.md` — build both apps
- `docs/USER_GUIDE.md` — set up a provider and use it in the car
- `docs/TEST_MATRIX.md` — what's verified vs. pending hardware

## Layout

```
/ios       Native Swift/SwiftUI + CarPlay (source written; needs a Mac to build — see docs/BUILD.md)
/android   Native Kotlin/Compose + Android for Cars App Library (builds and tests pass in this repo's sandbox)
/shared    Normalized model + Xtream/M3U parsing spec both platforms implement identically
/docs      All project documentation
/tests     Shared fixtures used by both platforms' parser tests
```

## Status

Android: parser/model unit tests pass, full debug APK builds successfully
(`docs/TEST_MATRIX.md`). iOS: source written and mirrors the verified
Android logic, but unbuilt (no macOS/Xcode in this repo's sandbox).
Hardware validation on the real 2021 Camry XSE is pending — see
`docs/TEST_MATRIX.md` for the exact steps to run.
