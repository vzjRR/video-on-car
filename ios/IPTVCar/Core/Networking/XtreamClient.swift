import Foundation

struct XtreamCredentials {
    let baseUrl: String
    let username: String
    let password: String
}

enum XtreamError: Error {
    case authenticationFailed(String)
    case networkFailure(String)
    case malformedResponse(String)
}

/// Thin HTTP layer over Xtream Codes' `player_api.php`. Mirrors
/// android/core/.../xtream/XtreamClient.kt. Never logs a full request URL
/// (it contains the password) — see Core/Security/Redaction.
final class XtreamClient {

    private let credentials: XtreamCredentials
    private let session: URLSession

    init(credentials: XtreamCredentials, session: URLSession = .shared) {
        self.credentials = credentials
        self.session = session
    }

    private func apiUrl(action: String?, extraParams: [String: String] = [:]) -> URL {
        var components = URLComponents(string: "\(credentials.baseUrl)/player_api.php")!
        var items = [
            URLQueryItem(name: "username", value: credentials.username),
            URLQueryItem(name: "password", value: credentials.password),
        ]
        if let action = action {
            items.append(URLQueryItem(name: "action", value: action))
        }
        for (key, value) in extraParams {
            items.append(URLQueryItem(name: key, value: value))
        }
        components.queryItems = items
        return components.url!
    }

    private func get(_ url: URL) async throws -> Data {
        let (data, response) = try await session.data(from: url)
        guard let http = response as? HTTPURLResponse else {
            throw XtreamError.networkFailure("No HTTP response")
        }
        if http.statusCode == 401 || http.statusCode == 403 {
            throw XtreamError.authenticationFailed("HTTP \(http.statusCode) from provider")
        }
        guard (200...299).contains(http.statusCode) else {
            throw XtreamError.networkFailure("HTTP \(http.statusCode)")
        }
        return data
    }

    func authenticate() async throws -> Bool {
        let data = try await get(apiUrl(action: nil))
        guard XtreamParser.isAuthenticated(data) else {
            throw XtreamError.authenticationFailed("Invalid Xtream credentials")
        }
        return true
    }

    func getLiveCategoriesRaw() async throws -> Data { try await get(apiUrl(action: "get_live_categories")) }
    func getLiveStreamsRaw(categoryId: String? = nil) async throws -> Data {
        try await get(apiUrl(action: "get_live_streams", extraParams: categoryId.map { ["category_id": $0] } ?? [:]))
    }

    func getVodCategoriesRaw() async throws -> Data { try await get(apiUrl(action: "get_vod_categories")) }
    func getVodStreamsRaw(categoryId: String? = nil) async throws -> Data {
        try await get(apiUrl(action: "get_vod_streams", extraParams: categoryId.map { ["category_id": $0] } ?? [:]))
    }
    func getVodInfoRaw(vodId: String) async throws -> Data {
        try await get(apiUrl(action: "get_vod_info", extraParams: ["vod_id": vodId]))
    }

    func getSeriesCategoriesRaw() async throws -> Data { try await get(apiUrl(action: "get_series_categories")) }
    func getSeriesRaw(categoryId: String? = nil) async throws -> Data {
        try await get(apiUrl(action: "get_series", extraParams: categoryId.map { ["category_id": $0] } ?? [:]))
    }
    func getSeriesInfoRaw(seriesId: String) async throws -> Data {
        try await get(apiUrl(action: "get_series_info", extraParams: ["series_id": seriesId]))
    }
}
