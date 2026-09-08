package com.iptvcar.app.data

import android.content.Context
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.Favorite

class FavoritesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("iptv_car_favorites", Context.MODE_PRIVATE)

    private fun key(f: Favorite) = "${f.providerId}|${f.contentType}|${f.contentId}"

    fun add(favorite: Favorite) {
        prefs.edit().putLong(key(favorite), favorite.addedAt).apply()
    }

    fun remove(providerId: String, contentType: ContentType, contentId: String) {
        prefs.edit().remove("$providerId|$contentType|$contentId").apply()
    }

    fun isFavorite(providerId: String, contentType: ContentType, contentId: String): Boolean =
        prefs.contains("$providerId|$contentType|$contentId")

    fun all(): List<Favorite> = prefs.all.mapNotNull { (key, value) ->
        if (value !is Long) return@mapNotNull null
        val parts = key.split("|")
        if (parts.size != 3) return@mapNotNull null
        val contentType = runCatching { ContentType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
        Favorite(providerId = parts[0], contentType = contentType, contentId = parts[2], addedAt = value)
    }.sortedByDescending { it.addedAt }
}
