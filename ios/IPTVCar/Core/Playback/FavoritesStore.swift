import Foundation

/// Mirrors android/app/.../data/FavoritesRepository.kt.
final class FavoritesStore {
    static let shared = FavoritesStore()
    private let defaults = UserDefaults.standard
    private let key = "iptv_car_favorites"

    private init() {}

    private func all() -> [String: Favorite] {
        guard let data = defaults.data(forKey: key),
              let decoded = try? JSONDecoder().decode([String: Favorite].self, from: data) else { return [:] }
        return decoded
    }

    private func compoundKey(_ f: Favorite) -> String { "\(f.providerId)|\(f.contentType.rawValue)|\(f.contentId)" }

    func add(_ favorite: Favorite) {
        var current = all()
        current[compoundKey(favorite)] = favorite
        if let data = try? JSONEncoder().encode(current) { defaults.set(data, forKey: key) }
    }

    func remove(providerId: String, contentType: ContentType, contentId: String) {
        var current = all()
        current.removeValue(forKey: "\(providerId)|\(contentType.rawValue)|\(contentId)")
        if let data = try? JSONEncoder().encode(current) { defaults.set(data, forKey: key) }
    }

    func isFavorite(providerId: String, contentType: ContentType, contentId: String) -> Bool {
        all()["\(providerId)|\(contentType.rawValue)|\(contentId)"] != nil
    }

    func list() -> [Favorite] { all().values.sorted { $0.addedAt > $1.addedAt } }
}
