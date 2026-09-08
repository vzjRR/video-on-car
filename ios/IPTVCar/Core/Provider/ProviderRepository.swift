import Foundation

/// Bridges Core/Networking + Core/Parsing to a single app-facing
/// repository, mirroring android/app/.../data/ProviderRepository.kt so
/// both platforms behave identically against the same provider.
final class ProviderRepository {

    static let shared = ProviderRepository()
    private let defaults = UserDefaults.standard
    private let providersKey = "iptv_car_providers"
    private var m3uCache: [String: M3UParseResult] = [:]

    private init() {}

    func listProviders() -> [Provider] {
        guard let data = defaults.data(forKey: providersKey) else { return [] }
        return (try? JSONDecoder().decode([Provider].self, from: data)) ?? []
    }

    private func persist(_ providers: [Provider]) {
        guard let data = try? JSONEncoder().encode(providers) else { return }
        defaults.set(data, forKey: providersKey)
    }

    @discardableResult
    func addXtreamProvider(displayName: String, baseUrl: String, username: String, password: String) -> Provider {
        let provider = Provider(id: UUID().uuidString, displayName: displayName, kind: .xtream, baseUrl: baseUrl, createdAt: Date())
        KeychainStore.shared.set(username, providerId: provider.id, field: "username")
        KeychainStore.shared.set(password, providerId: provider.id, field: "password")
        persist(listProviders() + [provider])
        return provider
    }

    @discardableResult
    func addM3uProvider(displayName: String, url: String) -> Provider {
        let provider = Provider(id: UUID().uuidString, displayName: displayName, kind: .m3u, baseUrl: url, createdAt: Date())
        KeychainStore.shared.set(url, providerId: provider.id, field: "m3uUrl")
        persist(listProviders() + [provider])
        return provider
    }

    func deleteProvider(_ providerId: String) {
        KeychainStore.shared.clearAll(providerId: providerId)
        persist(listProviders().filter { $0.id != providerId })
    }

    private func xtreamCredentials(for provider: Provider) -> XtreamCredentials? {
        guard let username = KeychainStore.shared.get(providerId: provider.id, field: "username"),
              let password = KeychainStore.shared.get(providerId: provider.id, field: "password") else { return nil }
        return XtreamCredentials(baseUrl: provider.baseUrl, username: username, password: password)
    }

    private func xtreamClient(for provider: Provider) -> XtreamClient? {
        xtreamCredentials(for: provider).map { XtreamClient(credentials: $0) }
    }

    func loadLiveCategories(_ provider: Provider) async throws -> [Category] {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider) else { return [] }
            return try XtreamParser.parseCategories(providerId: provider.id, contentType: .live, data: try await client.getLiveCategoriesRaw())
        case .m3u:
            return try await loadM3u(provider).categories.filter { $0.contentType == .live }
        }
    }

    func loadLiveChannels(_ provider: Provider, categoryId: String) async throws -> [LiveChannel] {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider), let creds = xtreamCredentials(for: provider) else { return [] }
            return try XtreamParser.parseLiveStreams(providerId: provider.id, credentials: creds, data: try await client.getLiveStreamsRaw(categoryId: categoryId))
        case .m3u:
            return try await loadM3u(provider).liveChannels.filter { $0.categoryId == categoryId }
        }
    }

    func loadMovieCategories(_ provider: Provider) async throws -> [Category] {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider) else { return [] }
            return try XtreamParser.parseCategories(providerId: provider.id, contentType: .movie, data: try await client.getVodCategoriesRaw())
        case .m3u:
            return try await loadM3u(provider).categories.filter { $0.contentType == .movie }
        }
    }

    func loadMovies(_ provider: Provider, categoryId: String) async throws -> [Movie] {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider), let creds = xtreamCredentials(for: provider) else { return [] }
            return try XtreamParser.parseVodStreams(providerId: provider.id, credentials: creds, data: try await client.getVodStreamsRaw(categoryId: categoryId))
        case .m3u:
            return try await loadM3u(provider).movies.filter { $0.categoryId == categoryId }
        }
    }

    func loadMovieDetail(_ provider: Provider, movie: Movie) async throws -> Movie {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider) else { return movie }
            return XtreamParser.parseVodInfo(movie: movie, data: try await client.getVodInfoRaw(vodId: movie.id))
        case .m3u:
            return movie
        }
    }

    func loadSeriesCategories(_ provider: Provider) async throws -> [Category] {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider) else { return [] }
            return try XtreamParser.parseCategories(providerId: provider.id, contentType: .series, data: try await client.getSeriesCategoriesRaw())
        case .m3u:
            return try await loadM3u(provider).categories.filter { $0.contentType == .series }
        }
    }

    func loadSeries(_ provider: Provider, categoryId: String) async throws -> [Series] {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider) else { return [] }
            return try XtreamParser.parseSeriesList(providerId: provider.id, data: try await client.getSeriesRaw(categoryId: categoryId))
        case .m3u:
            return try await loadM3u(provider).series.filter { $0.categoryId == categoryId }
        }
    }

    func loadSeriesDetail(_ provider: Provider, series: Series) async throws -> Series {
        switch provider.kind {
        case .xtream:
            guard let client = xtreamClient(for: provider), let creds = xtreamCredentials(for: provider) else { return series }
            return XtreamParser.parseSeriesInfo(series: series, credentials: creds, data: try await client.getSeriesInfoRaw(seriesId: series.id))
        case .m3u:
            let all = try await loadM3u(provider).series
            return all.first { $0.id == series.id } ?? series
        }
    }

    private func loadM3u(_ provider: Provider) async throws -> M3UParseResult {
        if let cached = m3uCache[provider.id] { return cached }
        let content: String
        if provider.baseUrl.hasPrefix("http") {
            let url = URL(string: provider.baseUrl)!
            let (data, _) = try await URLSession.shared.data(from: url)
            content = String(data: data, encoding: .utf8) ?? ""
        } else {
            content = try String(contentsOfFile: provider.baseUrl, encoding: .utf8)
        }
        let result = M3UParser.parse(providerId: provider.id, content: content)
        m3uCache[provider.id] = result
        return result
    }
}
