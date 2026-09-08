import Foundation

struct M3UParseResult {
    let categories: [Category]
    let liveChannels: [LiveChannel]
    let movies: [Movie]
    let series: [Series]
    let warnings: [String]
}

/// Parses M3U / M3U Plus playlists per shared/parsing-spec/xtream-and-m3u.md.
/// Mirrors android/core/.../m3u/M3UParser.kt. Never assumes attribute order
/// or completeness; unclassifiable entries default to Live rather than
/// being dropped.
enum M3UParser {

    private struct RawEntry {
        let attrs: [String: String]
        let displayName: String
        let url: String
    }

    private static let attrRegex = try! NSRegularExpression(pattern: #"([a-zA-Z0-9_-]+)="([^"]*)""#)
    private static let episodeTagRegex = try! NSRegularExpression(pattern: #"(?i)\bS(\d{1,2})E(\d{1,3})\b"#)
    private static let yearRegex = try! NSRegularExpression(pattern: #"\((\d{4})\)"#)

    private static func attributes(from text: String) -> [String: String] {
        var result: [String: String] = [:]
        let range = NSRange(text.startIndex..., in: text)
        attrRegex.enumerateMatches(in: text, range: range) { match, _, _ in
            guard let match = match, let keyRange = Range(match.range(at: 1), in: text), let valueRange = Range(match.range(at: 2), in: text) else { return }
            result[String(text[keyRange]).lowercased()] = String(text[valueRange])
        }
        return result
    }

    private static func firstMatch(_ regex: NSRegularExpression, in text: String) -> NSTextCheckingResult? {
        regex.firstMatch(in: text, range: NSRange(text.startIndex..., in: text))
    }

    private static func extractYear(_ name: String) -> Int? {
        guard let match = firstMatch(yearRegex, in: name), let range = Range(match.range(at: 1), in: name) else { return nil }
        return Int(name[range])
    }

    private static func stripYearNoise(_ name: String) -> String {
        let range = NSRange(name.startIndex..., in: name)
        return yearRegex.stringByReplacingMatches(in: name, range: range, withTemplate: "").trimmingCharacters(in: .whitespaces)
    }

    private static func classify(url: String, groupTitle: String?, looksLikeEpisode: Bool) -> ContentType {
        let group = groupTitle?.lowercased() ?? ""
        if group.contains("series") && looksLikeEpisode { return .series }
        if group.contains("movie") || group.contains("vod") { return .movie }
        if url.contains("/movie/") { return .movie }
        if url.contains("/series/") { return .series }
        return .live
    }

    static func parse(providerId: String, content: String) -> M3UParseResult {
        let lines = content.split(separator: "\n").map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
        var warnings: [String] = []
        var entries: [RawEntry] = []

        var i = 0
        while i < lines.count {
            let line = lines[i]
            if line.hasPrefix("#EXTINF") {
                guard let commaIndex = line.lastIndex(of: ",") else {
                    warnings.append("Skipped malformed #EXTINF at line \(i + 1): no display name")
                    i += 1
                    continue
                }
                let prefixEnd = line.index(line.startIndex, offsetBy: "#EXTINF:".count)
                let attrsPart = String(line[prefixEnd..<commaIndex])
                let displayName = String(line[line.index(after: commaIndex)...]).trimmingCharacters(in: .whitespaces)
                let attrs = attributes(from: attrsPart)

                guard i + 1 < lines.count, !lines[i + 1].hasPrefix("#") else {
                    warnings.append("Skipped '\(displayName)': no stream URL followed #EXTINF")
                    i += 1
                    continue
                }
                entries.append(RawEntry(attrs: attrs, displayName: displayName, url: lines[i + 1]))
                i += 2
            } else {
                i += 1
            }
        }

        var categoriesByKey: [String: Category] = [:]
        var categoryOrder: [String] = []
        var liveChannels: [LiveChannel] = []
        var movies: [Movie] = []
        var seriesEpisodes: [String: [Int: [Episode]]] = [:]
        var seriesMeta: [String: RawEntry] = [:]
        var seriesOrder: [String] = []

        func categoryFor(_ name: String?, _ contentType: ContentType) -> Category {
            let resolvedName = (name?.isEmpty == false) ? name! : "Uncategorized"
            let key = "\(contentType)::\(resolvedName)"
            if let existing = categoriesByKey[key] { return existing }
            let category = Category(id: key, providerId: providerId, name: resolvedName, contentType: contentType, parentId: nil)
            categoriesByKey[key] = category
            categoryOrder.append(key)
            return category
        }

        for entry in entries {
            let groupTitle = entry.attrs["group-title"]
            let episodeMatch = firstMatch(episodeTagRegex, in: entry.displayName)
            let classification = classify(url: entry.url, groupTitle: groupTitle, looksLikeEpisode: episodeMatch != nil)

            switch classification {
            case .movie:
                let category = categoryFor(groupTitle, .movie)
                movies.append(Movie(
                    id: entry.url, providerId: providerId, categoryId: category.id,
                    title: stripYearNoise(entry.displayName), year: extractYear(entry.displayName),
                    posterUrl: entry.attrs["tvg-logo"], durationSeconds: nil, description: nil, genre: nil,
                    rating: nil, streamUrl: entry.url, containerExtension: nil
                ))
            case .series:
                var seasonNum = 1
                var episodeNum = 0
                var seriesTitle = entry.displayName
                if let match = episodeMatch, let seasonRange = Range(match.range(at: 1), in: entry.displayName), let episodeRange = Range(match.range(at: 2), in: entry.displayName) {
                    seasonNum = Int(entry.displayName[seasonRange]) ?? 1
                    episodeNum = Int(entry.displayName[episodeRange]) ?? 0
                    if let fullRange = Range(match.range, in: entry.displayName) {
                        let prefix = String(entry.displayName[entry.displayName.startIndex..<fullRange.lowerBound]).trimmingCharacters(in: .whitespaces)
                        if !prefix.isEmpty { seriesTitle = prefix }
                    }
                }
                if seriesMeta[seriesTitle] == nil {
                    seriesMeta[seriesTitle] = entry
                    seriesOrder.append(seriesTitle)
                }
                seriesEpisodes[seriesTitle, default: [:]][seasonNum, default: []].append(
                    Episode(id: entry.url, seasonId: "\(seriesTitle)-s\(seasonNum)", episodeNumber: episodeNum, title: entry.displayName, posterUrl: nil, durationSeconds: nil, description: nil, streamUrl: entry.url, containerExtension: nil)
                )
                _ = categoryFor(groupTitle, .series)
            case .live:
                let category = categoryFor(groupTitle, .live)
                liveChannels.append(LiveChannel(
                    id: entry.url, providerId: providerId, categoryId: category.id, name: entry.displayName,
                    logoUrl: entry.attrs["tvg-logo"], streamUrl: entry.url, epgChannelId: entry.attrs["tvg-id"],
                    catchupAvailable: entry.attrs["catchup"] != nil, catchupDays: entry.attrs["catchup-days"].flatMap { Int($0) }
                ))
            }
        }

        let seriesList: [Series] = seriesOrder.map { title in
            let metaEntry = seriesMeta[title]
            let category = categoryFor(metaEntry?.attrs["group-title"], .series)
            let seasons = (seriesEpisodes[title] ?? [:]).map { seasonNum, episodes in
                Season(id: "\(title)-s\(seasonNum)", seriesId: title, seasonNumber: seasonNum, episodes: episodes.sorted { $0.episodeNumber < $1.episodeNumber })
            }.sorted { $0.seasonNumber < $1.seasonNumber }
            return Series(id: title, providerId: providerId, categoryId: category.id, title: title, posterUrl: metaEntry?.attrs["tvg-logo"], description: nil, genre: nil, rating: nil, seasons: seasons)
        }

        return M3UParseResult(
            categories: categoryOrder.compactMap { categoriesByKey[$0] },
            liveChannels: liveChannels, movies: movies, series: seriesList, warnings: warnings
        )
    }
}
