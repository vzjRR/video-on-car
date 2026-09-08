import Foundation
import Security

/// Persists IPTV provider credentials in the iOS Keychain. Never written
/// to UserDefaults, never logged, never included in crash reports (the
/// Keychain APIs used here are synchronous and hold no debug description
/// that would leak a secret through NSLog/os_log if this object were ever
/// accidentally printed — callers must not printf the returned strings).
final class KeychainStore {

    static let shared = KeychainStore()
    private let service = "com.iptvcar.credentials"

    private init() {}

    private func account(providerId: String, field: String) -> String { "\(providerId).\(field)" }

    func set(_ value: String, providerId: String, field: String) {
        let account = account(providerId: providerId, field: field)
        let data = Data(value.utf8)

        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)

        var attributes = query
        attributes[kSecValueData as String] = data
        attributes[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        SecItemAdd(attributes as CFDictionary, nil)
    }

    func get(providerId: String, field: String) -> String? {
        let account = account(providerId: providerId, field: field)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }

    func clear(providerId: String, field: String) {
        let account = account(providerId: providerId, field: field)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)
    }

    func clearAll(providerId: String) {
        clear(providerId: providerId, field: "username")
        clear(providerId: providerId, field: "password")
        clear(providerId: providerId, field: "m3uUrl")
    }
}
