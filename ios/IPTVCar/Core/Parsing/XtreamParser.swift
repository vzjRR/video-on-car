import Foundation

/// Parses raw Xtream Codes `player_api.php` JSON per
/// shared/parsing-spec/xtream-and-m3u.md. Mirrors
/// android/core/.../xtream/XtreamParser.kt field-for-field. Every accessor
/// is lenient: a missing or mistyped field degrades to nil rather than
/// throwing, because Xtream server implementations vary widely.
enum XtreamParser {

    private static func string(_ any: Any?) -> String? {
        if let s = any as? String { return s }
        if let n = any as? NSNumber { return n.stringValue }
        return nil
    }

    private static func int(_ any: Any?) -> Int? {
        if let n = any as? NSNumber { return n.intValue }
        if let s = any as? String { return Int(s) }
        return nil
    }

    private static func double(_ any: Any?) -> Double? {
        if let n = any as? NSNumber { return n.doubleValue }
        if let s = any as? String { return Double(s) }
        return nil
    }

    static func isAuthenticated(_ data: Data) -> Bool {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let userInfo = root["user_info"] as? [String: Any] else { return false }
        return string(userInfo["auth"]) == "1"
    }

    static func parseCategories(providerId: String, contentType: ContentType, data: Data) throws -> [Category] {
        guard let array = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            throw XtreamError.malformedResponse("Expected a JSON array of categories")
        }
        return array.map { o in
            Category(
                id: string(o["category_id"]) ?? UUID().uuidString,
                providerId: providerId,
                name: string(o["category_name"]) ?? "Unnamed",
                contentType: contentType,
                parentId: nil
            )
        }
    }

    static func liveStreamUrl(_ c: XtreamCredentials, streamId: String, ext: String = "m3u8") -> String {
        "\(c.baseUrl)/live/\(c.username)/\(c.password)/\(streamId).\(ext)"
    }

    static func vodStreamUrl(_ c: XtreamCredentials, streamId: String, ext: String) -> String {
        "\(c.baseUrl)/movie/\(c.username)/\(c.password)/\(streamId).\(ext)"
    }

    static func episodeStreamUrl(_ c: XtreamCredentials, episodeId: String, ext: String) -> String {
        "\(c.baseUrl)/series/\(c.username)/\(c.password)/\(episodeId).\(ext)"
    }

    static func parseLiveStreams(providerId: String, credentials: XtreamCredentials, data: Data) throws -> [LiveChannel] {
        guard let array = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            throw XtreamError.malformedResponse("Expected a JSON array of live streams")
        }
        return array.map { o in
            let streamId = string(o["stream_id"]) ?? UUID().uuidString
            return LiveChannel(
                id: streamId,
                providerId: providerId,
                categoryId: string(o["category_id"]) ?? "uncategorized",
                name: string(o["name"]) ?? "Unnamed channel",
                logoUrl: string(o["stream_icon"]),
                streamUrl: liveStreamUrl(credentials, streamId: streamId),
                epgChannelId: string(o["epg_channel_id"]),
                catchupAvailable: int(o["tv_archive"]) == 1,
                catchupDays: int(o["tv_archive_duration"])
            )
        }
    }

    static func parseVodStreams(providerId: String, credentials: XtreamCredentials, data: Data) throws -> [Movie] {
        guard let array = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            throw XtreamError.malformedResponse("Expected a JSON array of VOD streams")
        }
        return array.map { o in
            let streamId = string(o["stream_id"]) ?? UUID().uuidString
            let ext = string(o["container_extension"]) ?? "mp4"
            return Movie(
                id: streamId,
                providerId: providerId,
                categoryId: string(o["category_id"]) ?? "uncategorized",
                title: string(o["name"]) ?? "Untitled",
                year: nil,
                posterUrl: string(o["stream_icon"]),
                durationSeconds: nil,
                description: nil,
                genre: nil,
                rating: double(o["rating"]),
                streamUrl: vodStreamUrl(credentials, streamId: streamId, ext: ext),
                containerExtension: ext
            )
        }
    }

    static func parseVodInfo(movie: Movie, data: Data) -> Movie {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let info = root["info"] as? [String: Any] else { return movie }
        var updated = movie
        updated.description = string(info["plot"]) ?? movie.description
        updated.genre = string(info["genre"]) ?? movie.genre
        updated.durationSeconds = int(info["duration_secs"]) ?? movie.durationSeconds
        if let release = string(info["releasedate"]), release.count >= 4 {
            updated.year = Int(release.prefix(4)) ?? movie.year
        }
        return updated
    }

    static func parseSeriesList(providerId: String, data: Data) throws -> [Series] {
        guard let array = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            throw XtreamError.malformedResponse("Expected a JSON array of series")
        }
        return array.map { o in
            Series(
                id: string(o["series_id"]) ?? UUID().uuidString,
                providerId: providerId,
                categoryId: string(o["category_id"]) ?? "uncategorized",
                title: string(o["name"]) ?? "Untitled",
                posterUrl: string(o["cover"]),
                description: nil,
                genre: nil,
                rating: double(o["rating"]),
                seasons: []
            )
        }
    }

    static func parseSeriesInfo(series: Series, credentials: XtreamCredentials, data: Data) -> Series {
        guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return series }
        var updated = series
        if let info = root["info"] as? [String: Any] {
            updated.description = string(info["plot"]) ?? series.description
            updated.genre = string(info["genre"]) ?? series.genre
            updated.posterUrl = string(info["cover"]) ?? series.posterUrl
        }

        guard let episodesBySeason = root["episodes"] as? [String: [[String: Any]]] else { return updated }

        let seasons: [Season] = episodesBySeason.keys.compactMap { seasonKey in
            let seasonNumber = Int(seasonKey) ?? 0
            let episodesArray = episodesBySeason[seasonKey] ?? []
            let episodes: [Episode] = episodesArray.map { eo in
                let epInfo = eo["info"] as? [String: Any]
                let ext = string(eo["container_extension"]) ?? "mp4"
                let episodeId = string(eo["id"]) ?? UUID().uuidString
                return Episode(
                    id: episodeId,
                    seasonId: "\(series.id)-s\(seasonNumber)",
                    episodeNumber: int(eo["episode_num"]) ?? 0,
                    title: string(eo["title"]) ?? "Episode",
                    posterUrl: nil,
                    durationSeconds: epInfo.flatMap { int($0["duration_secs"]) },
                    description: epInfo.flatMap { string($0["plot"]) },
                    streamUrl: episodeStreamUrl(credentials, episodeId: episodeId, ext: ext),
                    containerExtension: ext
                )
            }
            return Season(id: "\(series.id)-s\(seasonNumber)", seriesId: series.id, seasonNumber: seasonNumber, episodes: episodes.sorted { $0.episodeNumber < $1.episodeNumber })
        }.sorted { $0.seasonNumber < $1.seasonNumber }

        updated.seasons = seasons
        return updated
    }
}
