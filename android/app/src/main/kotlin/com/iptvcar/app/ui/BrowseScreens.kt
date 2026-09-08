package com.iptvcar.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.app.PlaybackLauncher
import com.iptvcar.core.model.*

private enum class BrowseLevel { CATEGORIES, ITEMS, SERIES_DETAIL }

@Composable
fun LiveScreen(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val provider = app.providerRepository.listProviders().firstOrNull()
    if (provider == null) { NoProviderMessage(); return }

    var level by remember { mutableStateOf(BrowseLevel.CATEGORIES) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var channels by remember { mutableStateOf<List<LiveChannel>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(provider.id) {
        runCatching { categories = app.providerRepository.loadLiveCategories(provider) }
            .onFailure { error = "Failed to load live categories: ${it.message}" }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Live TV") }) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                error != null -> ErrorMessage(error!!)
                level == BrowseLevel.CATEGORIES -> LazyColumn {
                    items(categories) { category ->
                        ListRow(category.name) {
                            runCatching { channels = app.providerRepository.loadLiveChannels(provider, category.id) }
                                .onFailure { error = "Failed to load channels: ${it.message}" }
                            level = BrowseLevel.ITEMS
                        }
                    }
                }
                else -> LazyColumn {
                    items(channels) { channel ->
                        ListRow(channel.name) {
                            onPlay(provider, ContentType.LIVE, channel.id, channel.name, channel.streamUrl)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoviesScreen(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val provider = app.providerRepository.listProviders().firstOrNull()
    if (provider == null) { NoProviderMessage(); return }

    var level by remember { mutableStateOf(BrowseLevel.CATEGORIES) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(provider.id) {
        runCatching { categories = app.providerRepository.loadMovieCategories(provider) }
            .onFailure { error = "Failed to load movie categories: ${it.message}" }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Movies") }) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                error != null -> ErrorMessage(error!!)
                level == BrowseLevel.CATEGORIES -> LazyColumn {
                    items(categories) { category ->
                        ListRow(category.name) {
                            runCatching { movies = app.providerRepository.loadMovies(provider, category.id) }
                                .onFailure { error = "Failed to load movies: ${it.message}" }
                            level = BrowseLevel.ITEMS
                        }
                    }
                }
                else -> LazyColumn {
                    items(movies) { movie ->
                        val label = if (movie.year != null) "${movie.title} (${movie.year})" else movie.title
                        ListRow(label) {
                            onPlay(provider, ContentType.MOVIE, movie.id, movie.title, movie.streamUrl)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeriesScreen(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val provider = app.providerRepository.listProviders().firstOrNull()
    if (provider == null) { NoProviderMessage(); return }

    var level by remember { mutableStateOf(BrowseLevel.CATEGORIES) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var seriesList by remember { mutableStateOf<List<Series>>(emptyList()) }
    var selectedSeries by remember { mutableStateOf<Series?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(provider.id) {
        runCatching { categories = app.providerRepository.loadSeriesCategories(provider) }
            .onFailure { error = "Failed to load series categories: ${it.message}" }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(selectedSeries?.title ?: "Series") }) }) { padding ->
        Box(Modifier.padding(padding)) {
            when {
                error != null -> ErrorMessage(error!!)
                selectedSeries != null -> {
                    val series = selectedSeries!!
                    LazyColumn {
                        series.seasons.forEach { season ->
                            item { Text("Season ${season.seasonNumber}", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleSmall) }
                            items(season.episodes) { episode ->
                                ListRow("${episode.episodeNumber}. ${episode.title}") {
                                    onPlay(provider, ContentType.SERIES, episode.id, episode.title, episode.streamUrl)
                                }
                            }
                        }
                    }
                }
                level == BrowseLevel.CATEGORIES -> LazyColumn {
                    items(categories) { category ->
                        ListRow(category.name) {
                            runCatching { seriesList = app.providerRepository.loadSeries(provider, category.id) }
                                .onFailure { error = "Failed to load series: ${it.message}" }
                            level = BrowseLevel.ITEMS
                        }
                    }
                }
                else -> LazyColumn {
                    items(seriesList) { series ->
                        ListRow(series.title) {
                            runCatching { selectedSeries = app.providerRepository.loadSeriesDetail(provider, series) }
                                .onFailure { error = "Failed to load series detail: ${it.message}" }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListRow(title: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), onClick = onClick) {
        Text(title, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(message, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun NoProviderMessage() {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        Text("No IPTV provider configured yet. Go to Settings to add one.")
    }
}
