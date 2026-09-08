package com.iptvcar.app.data

import android.content.Context
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.ResumeState
import org.json.JSONObject

/**
 * Local Continue-Watching store. Plain (non-encrypted) SharedPreferences
 * is fine here: it holds playback positions, never credentials.
 */
class ResumeRepository(context: Context) {

    private val prefs = context.getSharedPreferences("iptv_car_resume", Context.MODE_PRIVATE)

    private fun key(providerId: String, contentType: ContentType, contentId: String) =
        "$providerId|$contentType|$contentId"

    fun save(state: ResumeState) {
        val json = JSONObject()
            .put("positionMs", state.positionMs)
            .put("durationMs", state.durationMs)
            .put("updatedAt", state.updatedAt)
        prefs.edit().putString(key(state.providerId, state.contentType, state.contentId), json.toString()).apply()
    }

    fun get(providerId: String, contentType: ContentType, contentId: String): ResumeState? {
        val raw = prefs.getString(key(providerId, contentType, contentId), null) ?: return null
        val json = JSONObject(raw)
        return ResumeState(
            providerId = providerId,
            contentType = contentType,
            contentId = contentId,
            positionMs = json.optLong("positionMs"),
            durationMs = json.optLong("durationMs"),
            updatedAt = json.optLong("updatedAt"),
        )
    }

    fun allContinueWatching(): List<ResumeState> {
        return prefs.all.mapNotNull { (key, value) ->
            if (value !is String) return@mapNotNull null
            val parts = key.split("|")
            if (parts.size != 3) return@mapNotNull null
            val contentType = runCatching { ContentType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            val json = JSONObject(value)
            ResumeState(
                providerId = parts[0],
                contentType = contentType,
                contentId = parts[2],
                positionMs = json.optLong("positionMs"),
                durationMs = json.optLong("durationMs"),
                updatedAt = json.optLong("updatedAt"),
            )
        }.sortedByDescending { it.updatedAt }
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
