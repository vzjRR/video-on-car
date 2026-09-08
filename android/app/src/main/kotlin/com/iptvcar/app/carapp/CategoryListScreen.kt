package com.iptvcar.app.carapp

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.app.media.PlaybackController
import com.iptvcar.core.model.Category
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Second level: categories for the chosen content type (Live/Movies/Series).
 *
 * androidx.car.app's Screen.onGetTemplate() is invoked on the main thread
 * by the host, same as an Activity lifecycle callback — a blocking network
 * call here throws NetworkOnMainThreadException exactly like it does in
 * the Compose phone UI (see ui/BrowseScreens.kt). This loads data via
 * lifecycleScope on Dispatchers.IO and uses ListTemplate's built-in
 * loading state + invalidate() to refresh once the data arrives.
 */
class CategoryListScreen(carContext: CarContext, private val contentType: ContentType) : Screen(carContext) {

    private var categories: List<Category>? = null
    private var errorMessage: String? = null
    private var loadStarted = false

    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication
        val provider = app.providerRepository.listProviders().first()

        if (!loadStarted) {
            loadStarted = true
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        when (contentType) {
                            ContentType.LIVE -> app.providerRepository.loadLiveCategories(provider)
                            ContentType.MOVIE -> app.providerRepository.loadMovieCategories(provider)
                            ContentType.SERIES -> app.providerRepository.loadSeriesCategories(provider)
                        }
                    }
                }
                result.onSuccess { categories = it }.onFailure { errorMessage = it.message ?: "Unknown error" }
                invalidate()
            }
        }

        errorMessage?.let {
            CarToast.makeText(carContext, "Failed to load categories: $it", CarToast.LENGTH_LONG).show()
        }

        val loadedCategories = categories
        val itemList = ItemList.Builder()
        if (loadedCategories != null) {
            loadedCategories.forEach { category ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle(category.name)
                        .setOnClickListener { screenManager.push(ContentListScreen(carContext, provider, contentType, category)) }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle(titleFor(contentType))
            .setHeaderAction(androidx.car.app.model.Action.BACK)
            .apply {
                if (loadedCategories == null && errorMessage == null) setLoading(true) else setSingleList(itemList.build())
            }
            .build()
    }

    private fun titleFor(type: ContentType) = when (type) {
        ContentType.LIVE -> "Live TV"
        ContentType.MOVIE -> "Movies"
        ContentType.SERIES -> "Series"
    }
}

/** Third level: playable items in a category (channels, movies, or a series list). */
class ContentListScreen(
    carContext: CarContext,
    private val provider: Provider,
    private val contentType: ContentType,
    private val category: Category,
) : Screen(carContext) {

    private var rows: List<Row>? = null
    private var errorMessage: String? = null
    private var loadStarted = false

    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication

        if (!loadStarted) {
            loadStarted = true
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        when (contentType) {
                            ContentType.LIVE -> app.providerRepository.loadLiveChannels(provider, category.id).map { channel ->
                                Row.Builder()
                                    .setTitle(channel.name)
                                    .setOnClickListener { playAndConfirm(channel.name, channel.streamUrl) }
                                    .build()
                            }
                            ContentType.MOVIE -> app.providerRepository.loadMovies(provider, category.id).map { movie ->
                                Row.Builder()
                                    .setTitle(movie.title)
                                    .addText(movie.year?.toString() ?: "")
                                    .setOnClickListener { playAndConfirm(movie.title, movie.streamUrl) }
                                    .build()
                            }
                            ContentType.SERIES -> app.providerRepository.loadSeries(provider, category.id).map { series ->
                                Row.Builder()
                                    .setTitle(series.title)
                                    .setOnClickListener { screenManager.push(EpisodeListScreen(carContext, provider, series)) }
                                    .build()
                            }
                        }
                    }
                }
                result.onSuccess { rows = it }.onFailure { errorMessage = it.message ?: "Unknown error" }
                invalidate()
            }
        }

        errorMessage?.let {
            CarToast.makeText(carContext, "Failed to load ${category.name}: $it", CarToast.LENGTH_LONG).show()
        }

        val loadedRows = rows
        return ListTemplate.Builder()
            .setTitle(category.name)
            .setHeaderAction(androidx.car.app.model.Action.BACK)
            .apply {
                if (loadedRows == null && errorMessage == null) {
                    setLoading(true)
                } else {
                    val itemList = ItemList.Builder()
                    loadedRows?.forEach { itemList.addItem(it) }
                    setSingleList(itemList.build())
                }
            }
            .build()
    }

    private fun playAndConfirm(title: String, streamUrl: String) {
        PlaybackController.play(carContext, streamUrl)
        CarToast.makeText(carContext, "Playing: $title", CarToast.LENGTH_SHORT).show()
    }
}

/** Fourth level: seasons/episodes for one series. */
class EpisodeListScreen(
    carContext: CarContext,
    private val provider: Provider,
    private val series: com.iptvcar.core.model.Series,
) : Screen(carContext) {

    private var sections: List<androidx.car.app.model.ItemList>? = null
    private var sectionHeaders: List<String>? = null
    private var errorMessage: String? = null
    private var loadStarted = false

    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication

        if (!loadStarted) {
            loadStarted = true
            lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { app.providerRepository.loadSeriesDetail(provider, series) }
                }
                result.onSuccess { detail ->
                    sectionHeaders = detail.seasons.map { "Season ${it.seasonNumber}" }
                    sections = detail.seasons.map { season ->
                        val itemList = ItemList.Builder()
                        season.episodes.forEach { episode ->
                            itemList.addItem(
                                Row.Builder()
                                    .setTitle("${episode.episodeNumber}. ${episode.title}")
                                    .setOnClickListener {
                                        PlaybackController.play(carContext, episode.streamUrl)
                                        CarToast.makeText(carContext, "Playing: ${episode.title}", CarToast.LENGTH_SHORT).show()
                                    }
                                    .build()
                            )
                        }
                        itemList.build()
                    }
                }.onFailure { errorMessage = it.message ?: "Unknown error" }
                invalidate()
            }
        }

        errorMessage?.let {
            CarToast.makeText(carContext, "Failed to load ${series.title}: $it", CarToast.LENGTH_LONG).show()
        }

        val loadedSections = sections
        return ListTemplate.Builder()
            .setTitle(series.title)
            .setHeaderAction(androidx.car.app.model.Action.BACK)
            .apply {
                if (loadedSections == null && errorMessage == null) {
                    setLoading(true)
                } else if (loadedSections != null && loadedSections.size == 1) {
                    setSingleList(loadedSections.first())
                } else {
                    loadedSections?.forEachIndexed { index, list ->
                        val header = sectionHeaders?.getOrNull(index) ?: ""
                        addSectionedList(androidx.car.app.model.SectionedItemList.create(list, header))
                    }
                }
            }
            .build()
    }
}
