package com.iptvcar.app.carapp

import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.app.media.PlaybackController
import com.iptvcar.core.model.Category
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.Provider

/** Second level: categories for the chosen content type (Live/Movies/Series). */
class CategoryListScreen(carContext: CarContext, private val contentType: ContentType) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication
        val provider = app.providerRepository.listProviders().first()

        val categories: List<Category> = runCatching {
            when (contentType) {
                ContentType.LIVE -> app.providerRepository.loadLiveCategories(provider)
                ContentType.MOVIE -> app.providerRepository.loadMovieCategories(provider)
                ContentType.SERIES -> app.providerRepository.loadSeriesCategories(provider)
            }
        }.getOrElse {
            CarToast.makeText(carContext, "Failed to load categories: ${it.message}", CarToast.LENGTH_LONG).show()
            emptyList()
        }

        val itemList = ItemList.Builder()
        categories.forEach { category ->
            itemList.addItem(
                Row.Builder()
                    .setTitle(category.name)
                    .setOnClickListener { screenManager.push(ContentListScreen(carContext, provider, contentType, category)) }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(titleFor(contentType))
            .setHeaderAction(androidx.car.app.model.Action.BACK)
            .setSingleList(itemList.build())
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

    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication
        val itemList = ItemList.Builder()

        when (contentType) {
            ContentType.LIVE -> app.providerRepository.loadLiveChannels(provider, category.id).forEach { channel ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle(channel.name)
                        .setOnClickListener { playAndConfirm(channel.name, channel.streamUrl) }
                        .build()
                )
            }
            ContentType.MOVIE -> app.providerRepository.loadMovies(provider, category.id).forEach { movie ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle(movie.title)
                        .setOnClickListener { playAndConfirm(movie.title, movie.streamUrl) }
                        .build()
                )
            }
            ContentType.SERIES -> app.providerRepository.loadSeries(provider, category.id).forEach { series ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle(series.title)
                        .setOnClickListener { screenManager.push(EpisodeListScreen(carContext, provider, series)) }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle(category.name)
            .setHeaderAction(androidx.car.app.model.Action.BACK)
            .setSingleList(itemList.build())
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

    override fun onGetTemplate(): Template {
        val app = carContext.applicationContext as IptvCarApplication
        val detail = app.providerRepository.loadSeriesDetail(provider, series)

        val itemList = ItemList.Builder()
        detail.seasons.forEach { season ->
            season.episodes.forEach { episode ->
                itemList.addItem(
                    Row.Builder()
                        .setTitle("S${season.seasonNumber}E${episode.episodeNumber} ${episode.title}")
                        .setOnClickListener {
                            PlaybackController.play(carContext, episode.streamUrl)
                            CarToast.makeText(carContext, "Playing: ${episode.title}", CarToast.LENGTH_SHORT).show()
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setTitle(series.title)
            .setHeaderAction(androidx.car.app.model.Action.BACK)
            .setSingleList(itemList.build())
            .build()
    }
}
