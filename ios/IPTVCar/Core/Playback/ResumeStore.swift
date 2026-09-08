import Foundation

/// Local Continue-Watching store. Plain UserDefaults is fine here: it
/// holds playback positions, never credentials. Mirrors
/// android/app/.../data/ResumeRepository.kt.
final class ResumeStore {
    static let shared = ResumeStore()
    private let defaults = UserDefaults.standard
    private let key = "iptv_car_resume"

    private init() {}

    private func all() -> [String: ResumeState] {
        guard let data = defaults.data(forKey: key),
              let decoded = try? JSONDecoder().decode([String: ResumeState].self, from: data) else { return [:] }
        return decoded
    }

    private func compoundKey(_ providerId: String, _ contentType: ContentType, _ contentId: String) -> String {
        "\(providerId)|\(contentType.rawValue)|\(contentId)"
    }

    func save(_ state: ResumeState) {
        var current = all()
        current[compoundKey(state.providerId, state.contentType, state.contentId)] = state
        if let data = try? JSONEncoder().encode(current) { defaults.set(data, forKey: key) }
    }

    func get(providerId: String, contentType: ContentType, contentId: String) -> ResumeState? {
        all()[compoundKey(providerId, contentType, contentId)]
    }

    func allContinueWatching() -> [ResumeState] {
        all().values.sorted { $0.updatedAt > $1.updatedAt }
    }
}
