package com.iptvcar.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists IPTV provider credentials (Xtream user/pass, or a remote M3U
 * URL that embeds auth) using Android Keystore-backed encryption.
 * Credentials never leave this class as plain SharedPreferences and are
 * never passed to any logging call.
 */
class SecureCredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "iptv_car_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveXtreamCredentials(providerId: String, username: String, password: String) {
        prefs.edit()
            .putString("$providerId.username", username)
            .putString("$providerId.password", password)
            .apply()
    }

    fun readXtreamCredentials(providerId: String): Pair<String, String>? {
        val username = prefs.getString("$providerId.username", null) ?: return null
        val password = prefs.getString("$providerId.password", null) ?: return null
        return username to password
    }

    fun saveM3uUrl(providerId: String, url: String) {
        prefs.edit().putString("$providerId.m3uUrl", url).apply()
    }

    fun readM3uUrl(providerId: String): String? = prefs.getString("$providerId.m3uUrl", null)

    /** Wipes all secrets for one provider. Used by "delete provider" / logout. */
    fun clear(providerId: String) {
        prefs.edit()
            .remove("$providerId.username")
            .remove("$providerId.password")
            .remove("$providerId.m3uUrl")
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
