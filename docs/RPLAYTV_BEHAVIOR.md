# Phase 6 — rPlayTV Black-Box Behavior Investigation

## Status: TEST PROTOCOL ONLY — not yet executed

This agent's sandbox has no iPhone, no Carlinkit/VehiConn Mini SE, and no
physical 2021 Camry XSE, so none of the hands-on tests below have been run
by the agent. What follows is (a) the reasoned hypothesis from documented
platform facts, and (b) the exact, minimal test protocol the user should
run on the real hardware to confirm or refute it. **Do not treat the
hypothesis in this document as confirmed until the user has run these
tests and recorded results.**

We do not reverse-engineer or decompile rPlayTV's binary, and nothing here
relies on inspecting its proprietary source or bypassing any license —
every test is a black-box observation of behavior that's visible to any
end user (what shows on screen, what iOS system UI shows, whether an
AirPlay icon lights up, etc.).

## Working hypothesis

Given `RESEARCH.md` §2 facts 4–6 (no vehicle in the world officially
supports Apple's CarPlay video-app entitlement pathway yet) and fact 13
(wireless CarPlay adapters are documented as already bypassing Apple's
official video gating), the most likely explanation is:

> The Carlinkit/VehiConn Mini SE runs its own AirPlay receiver (or an
> equivalent screen/video sink) alongside its CarPlay emulation. rPlayTV's
> Live TV video is not rendered inside an Apple-sanctioned CarPlay video
> template at all — it is delivered as an AirPlay video stream (or a
> full-screen AirPlay mirror of the phone) that the dongle displays on the
> Camry's screen. The CarPlay "browsing" the user sees (categories,
> channel lists) is a real, standard **CarPlay Audio-category** template;
> selecting a channel then triggers AirPlay output to the dongle rather
> than an in-template video frame.

This is testable without any special tools — it only requires watching
iOS's own system indicators.

## Test protocol (run on the real iPhone + Camry + Carlinkit)

Record every field in the table under "What to log" for each test.

1. **iPhone iOS version** — Settings → General → About. Record exact
   version/build.
2. **CarPlay connection** — connect Carlinkit, confirm CarPlay icon
   appears on Camry display, confirm phone shows "Connected to CarPlay"
   banner or icon in Control Center.
3. **rPlayTV launch on CarPlay** — open rPlayTV's icon from the Camry's
   CarPlay home screen (not from the phone). Note: does the phone's own
   screen also change (e.g., go to a CarPlay-lock/"Now on CarPlay" screen)
   or stay independent?
4. **Live channel browsing** — browse categories/channels on the Camry
   screen. Confirm whether this looks like a standard CarPlay list
   template (rows with chevrons, standard CarPlay fonts/spacing) — this
   would confirm the Audio-category-template part of the hypothesis.
5. **Channel playback** — select a channel. **Critical observation:**
   open iOS Control Center on the phone *while video is playing on the
   Camry* and check the AirPlay/route picker icon. If it shows an active
   AirPlay video route (usually a small screen/rectangle icon, sometimes
   named after the car or "CarPlay"), that confirms AirPlay is the
   transport. If Control Center shows no active AirPlay route and the
   phone screen itself looks like a CarPlay "Now Playing" audio screen
   with no visible video, the mechanism is different (worth capturing a
   screenshot of Control Center for the record).
6. **Pause** — pause playback from the Camry's on-screen controls; note
   whether the phone's own screen also reflects the pause state instantly
   (implies a shared player session) or lags/doesn't (implies mirroring).
7. **Resume** — resume; note any re-buffering delay.
8. **Channel switching** — switch channels rapidly; note stream-switch
   latency and whether the video ever briefly reverts to the phone's app
   UI chrome (a mirroring artifact) versus a clean video-only transition
   (an AirPlay-video-only artifact).
9. **Disconnect/reconnect** — unplug/reconnect the Carlinkit mid-playback;
   note whether playback auto-resumes on reconnect or requires
   re-selecting the channel.
10. **Parked state** — confirm playback works with the vehicle in Park.
11. **Moving state** — confirm whether playback is blocked, blacked out,
    or continues while the vehicle is moving (this determines whether
    Carlinkit enforces any parked-only restriction on its own, independent
    of Apple's official gate).
12. **VOD availability on iPhone** — confirm Movies/Series play normally
    in the rPlayTV iPhone app itself (outside CarPlay).
13. **VOD availability on CarPlay** — confirm Movies/Series do **not**
    appear anywhere in rPlayTV's CarPlay home screen or category list
    (this is the reported bug that motivates this whole project — just
    re-confirm it explicitly and note the exact CarPlay app structure
    rPlayTV shows, e.g. "Live TV" only vs. a grayed-out "Movies" entry).
14. **Series availability on iPhone** — same as #12 for Series.
15. **Series availability on CarPlay** — same as #13 for Series.

## What to log (table template)

| # | Test | Observed result | Inference |
|---|------|------------------|-----------|
| 1 | iOS version | | |
| 2 | CarPlay connect | | |
| 3 | Launch on CarPlay | | |
| 4 | Browse channels | | |
| 5 | Play channel + AirPlay icon check | | |
| 6 | Pause | | |
| 7 | Resume | | |
| 8 | Channel switch | | |
| 9 | Disconnect/reconnect | | |
| 10 | Parked | | |
| 11 | Moving | | |
| 12 | VOD on iPhone | | |
| 13 | VOD on CarPlay | | |
| 14 | Series on iPhone | | |
| 15 | Series on CarPlay | | |

## How the result changes the architecture

- **If AirPlay is confirmed** (test 5 shows an active AirPlay video
  route): `ios/IPTVCar/Core/Playback/ExternalPlaybackRouter.swift` as
  already written in this repo is the correct approach — no changes
  needed, just wire it up and test.
- **If no AirPlay route is shown and video still appears** (i.e. the
  dongle is doing something below the OS, such as intercepting the video
  layer's rendering surface directly): the AirPlay-based architecture in
  this repo will likely **not** reproduce the effect, and the project
  should pivot to filing an actual support request with Carlinkit/
  VehiConn asking how third-party apps can target their video sink, since
  that would be an undocumented/proprietary integration outside public
  Apple APIs — which Phase 17 explicitly says not to reverse-engineer or
  fake. Document this outcome here and re-open `RESEARCH.md` §5.

## Explicit non-goals

- No decompiling, no proprietary-protocol sniffing of rPlayTV's binary or
  network traffic beyond what's visible through iOS's own system UI.
- No jailbreak, no private API usage, regardless of what this test
  reveals.
