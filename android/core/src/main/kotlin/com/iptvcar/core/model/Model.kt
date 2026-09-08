package com.iptvcar.core.model

enum class ContentType { LIVE, MOVIE, SERIES }

enum class ProviderKind { XTREAM, M3U }

data class Provider(
    val id: String,
    val displayName: String,
    val kind: ProviderKind,
    val baseUrl: String,
    val createdAt: Long,
)

data class Category(
    val id: String,
    val providerId: String,
    val name: String,
    val contentType: ContentType,
    val parentId: String? = null,
)

data class LiveChannel(
    val id: String,
    val providerId: String,
    val categoryId: String,
    val name: String,
    val logoUrl: String? = null,
    val streamUrl: String,
    val epgChannelId: String? = null,
    val catchupAvailable: Boolean = false,
    val catchupDays: Int? = null,
)

data class Movie(
    val id: String,
    val providerId: String,
    val categoryId: String,
    val title: String,
    val year: Int? = null,
    val posterUrl: String? = null,
    val durationSeconds: Int? = null,
    val description: String? = null,
    val genre: String? = null,
    val rating: Double? = null,
    val streamUrl: String,
    val containerExtension: String? = null,
)

data class Episode(
    val id: String,
    val seasonId: String,
    val episodeNumber: Int,
    val title: String,
    val posterUrl: String? = null,
    val durationSeconds: Int? = null,
    val description: String? = null,
    val streamUrl: String,
    val containerExtension: String? = null,
)

data class Season(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodes: List<Episode> = emptyList(),
)

data class Series(
    val id: String,
    val providerId: String,
    val categoryId: String,
    val title: String,
    val posterUrl: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val rating: Double? = null,
    val seasons: List<Season> = emptyList(),
)

data class EPGEvent(
    val id: String,
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startTime: Long,
    val endTime: Long,
)

data class ResumeState(
    val providerId: String,
    val contentType: ContentType,
    val contentId: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

data class Favorite(
    val providerId: String,
    val contentType: ContentType,
    val contentId: String,
    val addedAt: Long,
)
