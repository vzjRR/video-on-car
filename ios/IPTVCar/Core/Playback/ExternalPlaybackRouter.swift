import AVFoundation
import AVKit

/// Owns the app's single AVPlayer and configures it for external
/// playback (AirPlay). This is the code path referenced by
/// docs/RESEARCH.md §5 and docs/RPLAYTV_BEHAVIOR.md: since no vehicle yet
/// supports Apple's native CarPlay video-app template (docs/RESEARCH.md
/// §2 facts 4-6), video actually reaching the Camry screen is expected to
/// happen via AirPlay through whatever the Carlinkit/VehiConn adapter
/// exposes — the same mechanism rPlayTV's Live TV appears to already use.
///
/// Every API used here is public AVFoundation/AVKit: no private API, no
/// jailbreak, nothing that depends on the Carlinkit adapter specifically.
/// If Phase 6 hardware testing (RPLAYTV_BEHAVIOR.md) shows the Camry does
/// NOT surface as an AirPlay route through the adapter, this router still
/// works for driving a real AirPlay-certified target (e.g. a genuine
/// CarPlay video-app vehicle once one exists) — only the "does it reach
/// this specific Carlinkit dongle" question is unresolved without hardware.
final class ExternalPlaybackRouter: NSObject {

    let player = AVPlayer()

    override init() {
        super.init()
        // Let the system route video to an external screen/AirPlay
        // receiver automatically when one is connected/selected, instead
        // of confining playback to the phone's own display.
        player.allowsExternalPlayback = true
        player.usesExternalPlaybackWhileExternalScreenIsActive = true
    }

    func play(streamUrl: String, resumeAt seconds: Double = 0) {
        guard let url = URL(string: streamUrl) else { return }
        let item = AVPlayerItem(url: url)
        player.replaceCurrentItem(with: item)
        if seconds > 0 {
            player.seek(to: CMTime(seconds: seconds, preferredTimescale: 1))
        }
        player.play()
    }

    func pause() { player.pause() }

    func currentPosition() -> Double { player.currentTime().seconds.isFinite ? player.currentTime().seconds : 0 }

    func duration() -> Double {
        guard let duration = player.currentItem?.duration.seconds, duration.isFinite else { return 0 }
        return duration
    }

    /// A visible, standard AirPlay route picker for the user to explicitly
    /// choose the Carlinkit/VehiConn adapter as an AirPlay target if the
    /// system doesn't route to it automatically. Attach this to a SwiftUI
    /// view via `AVRoutePickerViewRepresentable` (see PlayerView.swift).
    func makeRoutePickerView() -> AVRoutePickerView {
        let picker = AVRoutePickerView()
        picker.prioritizesVideoDevices = true
        return picker
    }
}
