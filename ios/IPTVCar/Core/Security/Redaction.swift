import Foundation

/// Masks credential-bearing parts of a URL before it is ever logged.
/// Mirrors android/core/.../security/Redaction.kt — keep both in sync.
enum Redaction {

    private static let sensitiveQueryKeys: Set<String> = ["username", "password", "token", "pass", "user"]

    static func redactQuery(_ urlString: String) -> String {
        guard var components = URLComponents(string: urlString), let items = components.queryItems else {
            return urlString
        }
        components.queryItems = items.map { item in
            if sensitiveQueryKeys.contains(item.name.lowercased()) {
                return URLQueryItem(name: item.name, value: "***")
            }
            return item
        }
        return components.string ?? urlString
    }

    /// Strips Xtream's path-embedded credentials: /live/USER/PASS/id.ext
    static func redactPathCredentials(_ urlString: String) -> String {
        guard let regex = try? NSRegularExpression(pattern: "/(live|movie|series)/[^/]+/[^/]+/") else {
            return urlString
        }
        let range = NSRange(urlString.startIndex..., in: urlString)
        return regex.stringByReplacingMatches(in: urlString, range: range, withTemplate: "/$1/***/***/")
    }

    static func redact(_ urlString: String) -> String {
        redactPathCredentials(redactQuery(urlString))
    }
}
