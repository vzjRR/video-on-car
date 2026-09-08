import SwiftUI
import AVKit

/// Native playback via AVPlayer, wired for external playback/AirPlay
/// hand-off (see Core/Playback/ExternalPlaybackRouter.swift and
/// docs/RPLAYTV_BEHAVIOR.md). Streams directly from the provider URL —
/// nothing is downloaded to disk first.
struct PlayerView: View {
    let streamUrl: String
    let title: String
    var providerId: String? = nil
    var contentType: ContentType? = nil
    var contentId: String? = nil

    @StateObject private var holder = PlayerHolder()

    var body: some View {
        ZStack(alignment: .topTrailing) {
            VideoPlayer(player: holder.router.player)
            RoutePickerRepresentable(router: holder.router)
                .frame(width: 44, height: 44)
                .padding()
        }
        .onAppear {
            holder.configure(providerId: providerId, contentType: contentType, contentId: contentId)
            holder.start(streamUrl: streamUrl)
        }
        .onDisappear { holder.stop() }
        .navigationTitle(title)
    }
}

@MainActor
final class PlayerHolder: ObservableObject {
    let router = ExternalPlaybackRouter()
    private var providerId: String?
    private var contentType: ContentType?
    private var contentId: String?

    func configure(providerId: String?, contentType: ContentType?, contentId: String?) {
        self.providerId = providerId
        self.contentType = contentType
        self.contentId = contentId
    }

    func start(streamUrl: String) {
        var resumeSeconds: Double = 0
        if let providerId, let contentType, let contentId,
           let resume = ResumeStore.shared.get(providerId: providerId, contentType: contentType, contentId: contentId) {
            resumeSeconds = resume.positionSeconds
        }
        router.play(streamUrl: streamUrl, resumeAt: resumeSeconds)
    }

    func stop() {
        router.pause()
        if let providerId, let contentType, let contentId {
            ResumeStore.shared.save(ResumeState(
                providerId: providerId, contentType: contentType, contentId: contentId,
                positionSeconds: router.currentPosition(), durationSeconds: router.duration(), updatedAt: Date()
            ))
        }
    }
}

/// UIKit bridge for AVRoutePickerView (SwiftUI has no native wrapper).
struct RoutePickerRepresentable: UIViewRepresentable {
    let router: ExternalPlaybackRouter

    func makeUIView(context: Context) -> AVRoutePickerView {
        router.makeRoutePickerView()
    }

    func updateUIView(_ uiView: AVRoutePickerView, context: Context) {}
}
