import Foundation

// Normalized model shared in spirit with android/core/.../model/Model.kt
// and documented in shared/models/normalized-model.md. Keep both in sync
// when adding fields.

enum ContentType: String, Codable {
    case live, movie, series
}

enum ProviderKind: String, Codable {
    case xtream, m3u
}

struct Provider: Identifiable, Codable {
    let id: String
    var displayName: String
    let kind: ProviderKind
    let baseUrl: String
    let createdAt: Date
}

struct Category: Identifiable, Codable {
    let id: String
    let providerId: String
    let name: String
    let contentType: ContentType
    let parentId: String?
}

struct LiveChannel: Identifiable, Codable {
    let id: String
    let providerId: String
    let categoryId: String
    let name: String
    let logoUrl: String?
    let streamUrl: String
    let epgChannelId: String?
    let catchupAvailable: Bool
    let catchupDays: Int?
}

struct Movie: Identifiable, Codable {
    let id: String
    let providerId: String
    let categoryId: String
    var title: String
    var year: Int?
    var posterUrl: String?
    var durationSeconds: Int?
    var description: String?
    var genre: String?
    var rating: Double?
    let streamUrl: String
    let containerExtension: String?
}

struct Episode: Identifiable, Codable {
    let id: String
    let seasonId: String
    let episodeNumber: Int
    let title: String
    let posterUrl: String?
    let durationSeconds: Int?
    let description: String?
    let streamUrl: String
    let containerExtension: String?
}

struct Season: Identifiable, Codable {
    let id: String
    let seriesId: String
    let seasonNumber: Int
    let episodes: [Episode]
}

struct Series: Identifiable, Codable {
    let id: String
    let providerId: String
    let categoryId: String
    var title: String
    var posterUrl: String?
    var description: String?
    var genre: String?
    var rating: Double?
    var seasons: [Season]
}

struct EPGEvent: Identifiable, Codable {
    let id: String
    let channelId: String
    let title: String
    let description: String?
    let startTime: Date
    let endTime: Date
}

struct ResumeState: Codable {
    let providerId: String
    let contentType: ContentType
    let contentId: String
    var positionSeconds: Double
    var durationSeconds: Double
    var updatedAt: Date
}

struct Favorite: Codable {
    let providerId: String
    let contentType: ContentType
    let contentId: String
    let addedAt: Date
}
