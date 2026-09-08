package com.iptvcar.app.media

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.core.model.Category
import com.iptvcar.core.model.ContentType
import java.util.concurrent.Executors

private const val ROOT_ID = "root"
private const val LIVE_ID = "live"
private const val MOVIES_ID = "movies"
private const val SERIES_ID = "series"

/**
 * The primary, officially-documented Android Auto / Android Automotive OS
 * browsing surface (docs/RESEARCH.md fact 10): a MediaLibraryService whose
 * browse tree is rendered natively by the host's own media UI, so no
 * custom vehicle screen code is needed for Live TV / Movies / Series to
 * show up as browsable categories and playable items.
 */
class IptvMediaLibraryService : MediaLibraryService() {

    private lateinit var mediaSession: MediaLibrarySession

    // MediaLibrarySession.Callback methods are invoked on the main thread by
    // the host; the blocking Xtream/M3U network calls below must run off
    // of it (Android throws NetworkOnMainThreadException otherwise, same
    // failure mode fixed in ui/BrowseScreens.kt and carapp/CategoryListScreen.kt).
    private val ioExecutor: ListeningExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())

    override fun onCreate() {
        super.onCreate()
        val player = PlaybackController.get(this)
        mediaSession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = mediaSession

    override fun onDestroy() {
        mediaSession.release()
        PlaybackController.release()
        ioExecutor.shutdown()
        super.onDestroy()
    }

    private fun app() = application as IptvCarApplication

    private fun browsableItem(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    private fun categoryItem(category: Category): MediaItem =
        browsableItem("category:${category.contentType}:${category.id}", category.name)

    private fun playableItem(id: String, title: String, streamUrl: String, artworkUri: String?): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setIsBrowsable(false)
            .setIsPlayable(true)
        if (artworkUri != null) metadataBuilder.setArtworkUri(android.net.Uri.parse(artworkUri))
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(browsableItem(ROOT_ID, "IPTV Car"), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<com.google.common.collect.ImmutableList<MediaItem>>> {
            val provider = app().providerRepository.listProviders().firstOrNull()
                ?: return Futures.immediateFuture(LibraryResult.ofItemList(com.google.common.collect.ImmutableList.of(), params))

            val itemsFuture: ListenableFuture<List<MediaItem>> = ioExecutor.submit<List<MediaItem>> {
                when {
                    parentId == ROOT_ID -> listOf(
                        browsableItem(LIVE_ID, "Live TV"),
                        browsableItem(MOVIES_ID, "Movies"),
                        browsableItem(SERIES_ID, "Series"),
                    )
                    parentId == LIVE_ID -> app().providerRepository.loadLiveCategories(provider).map(::categoryItem)
                    parentId == MOVIES_ID -> app().providerRepository.loadMovieCategories(provider).map(::categoryItem)
                    parentId == SERIES_ID -> app().providerRepository.loadSeriesCategories(provider).map(::categoryItem)
                    parentId.startsWith("category:${ContentType.LIVE}:") -> {
                        val categoryId = parentId.substringAfterLast(":")
                        app().providerRepository.loadLiveChannels(provider, categoryId).map {
                            playableItem("live:${it.id}", it.name, it.streamUrl, it.logoUrl)
                        }
                    }
                    parentId.startsWith("category:${ContentType.MOVIE}:") -> {
                        val categoryId = parentId.substringAfterLast(":")
                        app().providerRepository.loadMovies(provider, categoryId).map {
                            playableItem("movie:${it.id}", it.title, it.streamUrl, it.posterUrl)
                        }
                    }
                    parentId.startsWith("category:${ContentType.SERIES}:") -> {
                        val categoryId = parentId.substringAfterLast(":")
                        app().providerRepository.loadSeries(provider, categoryId).map {
                            browsableItem("series:${it.id}", it.title)
                        }
                    }
                    parentId.startsWith("series:") -> {
                        val seriesId = parentId.substringAfter("series:")
                        val detail = app().providerRepository.findSeriesById(provider, seriesId)
                        detail?.seasons.orEmpty().flatMap { season ->
                            season.episodes.map { episode ->
                                playableItem("episode:${episode.id}", "S${season.seasonNumber}E${episode.episodeNumber} ${episode.title}", episode.streamUrl, episode.posterUrl)
                            }
                        }
                    }
                    else -> emptyList()
                }
            }

            return Futures.transform(
                itemsFuture,
                { items -> LibraryResult.ofItemList(com.google.common.collect.ImmutableList.copyOf(items), params) },
                MoreExecutors.directExecutor(),
            )
        }
    }
}
