package com.iptvcar.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.app.PlaybackLauncher
import com.iptvcar.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class BrowseLevel { CATEGORIES, ITEMS, SERIES_DETAIL }

/**
 * Loading/Loaded/Error are tracked explicitly and rendered as distinct
 * UI states. Previously "still fetching" and "fetched zero results" both
 * rendered as an empty LazyColumn with no text — indistinguishable from
 * each other and from a silent failure, which is exactly what made the
 * earlier "shows nothing" report impossible to diagnose from a screenshot.
 */
private sealed class LoadState<out T> {
    object Loading : LoadState<Nothing>()
    data class Loaded<T>(val value: T) : LoadState<T>()
    data class Failed(val message: String) : LoadState<Nothing>()
}

@Composable
fun LiveScreen(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val provider = app.providerRepository.listProviders().firstOrNull()
    if (provider == null) { NoProviderMessage(); return }

    val scope = rememberCoroutineScope()
    var level by remember { mutableStateOf(BrowseLevel.CATEGORIES) }
    var categoriesState by remember { mutableStateOf<LoadState<List<Category>>>(LoadState.Loading) }
    var channelsState by remember { mutableStateOf<LoadState<List<LiveChannel>>>(LoadState.Loading) }

    // All network calls below run on Dispatchers.IO: Android throws
    // NetworkOnMainThreadException (with no message, hence the confusing
    // "...: null" errors) if a blocking OkHttp call executes on the UI
    // thread — Compose's LaunchedEffect/onClick run there by default.
    LaunchedEffect(provider.id) {
        categoriesState = LoadState.Loading
        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadLiveCategories(provider) } }
        categoriesState = result.fold(
            onSuccess = { LoadState.Loaded(it) },
            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Live TV") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                level == BrowseLevel.CATEGORIES -> LoadStateList(
                    state = categoriesState,
                    emptyMessage = "Provider returned zero live categories.",
                    itemLabel = { it.name },
                ) { category ->
                    channelsState = LoadState.Loading
                    level = BrowseLevel.ITEMS
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadLiveChannels(provider, category.id) } }
                        channelsState = result.fold(
                            onSuccess = { LoadState.Loaded(it) },
                            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
                        )
                    }
                }
                else -> LoadStateList(
                    state = channelsState,
                    emptyMessage = "This category has zero channels.",
                    itemLabel = { it.name },
                ) { channel -> onPlay(provider, ContentType.LIVE, channel.id, channel.name, channel.streamUrl) }
            }
        }
    }
}

@Composable
fun MoviesScreen(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val provider = app.providerRepository.listProviders().firstOrNull()
    if (provider == null) { NoProviderMessage(); return }

    val scope = rememberCoroutineScope()
    var level by remember { mutableStateOf(BrowseLevel.CATEGORIES) }
    var categoriesState by remember { mutableStateOf<LoadState<List<Category>>>(LoadState.Loading) }
    var moviesState by remember { mutableStateOf<LoadState<List<Movie>>>(LoadState.Loading) }

    LaunchedEffect(provider.id) {
        categoriesState = LoadState.Loading
        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadMovieCategories(provider) } }
        categoriesState = result.fold(
            onSuccess = { LoadState.Loaded(it) },
            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Movies") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                level == BrowseLevel.CATEGORIES -> LoadStateList(
                    state = categoriesState,
                    emptyMessage = "Provider returned zero movie categories.",
                    itemLabel = { it.name },
                ) { category ->
                    moviesState = LoadState.Loading
                    level = BrowseLevel.ITEMS
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadMovies(provider, category.id) } }
                        moviesState = result.fold(
                            onSuccess = { LoadState.Loaded(it) },
                            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
                        )
                    }
                }
                else -> LoadStateList(
                    state = moviesState,
                    emptyMessage = "This category has zero movies.",
                    itemLabel = { if (it.year != null) "${it.title} (${it.year})" else it.title },
                ) { movie -> onPlay(provider, ContentType.MOVIE, movie.id, movie.title, movie.streamUrl) }
            }
        }
    }
}

@Composable
fun SeriesScreen(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val provider = app.providerRepository.listProviders().firstOrNull()
    if (provider == null) { NoProviderMessage(); return }

    val scope = rememberCoroutineScope()
    var level by remember { mutableStateOf(BrowseLevel.CATEGORIES) }
    var categoriesState by remember { mutableStateOf<LoadState<List<Category>>>(LoadState.Loading) }
    var seriesListState by remember { mutableStateOf<LoadState<List<Series>>>(LoadState.Loading) }
    var selectedSeriesState by remember { mutableStateOf<LoadState<Series>?>(null) }

    LaunchedEffect(provider.id) {
        categoriesState = LoadState.Loading
        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadSeriesCategories(provider) } }
        categoriesState = result.fold(
            onSuccess = { LoadState.Loaded(it) },
            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
        )
    }

    val titleText = (selectedSeriesState as? LoadState.Loaded)?.value?.title ?: "Series"

    Scaffold(topBar = { TopAppBar(title = { Text(titleText) }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                selectedSeriesState is LoadState.Loaded -> {
                    val series = (selectedSeriesState as LoadState.Loaded<Series>).value
                    if (series.seasons.isEmpty()) {
                        CenteredMessage("This series has zero seasons/episodes returned.")
                    } else {
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
                }
                selectedSeriesState is LoadState.Failed -> CenteredMessage((selectedSeriesState as LoadState.Failed).message, isError = true)
                selectedSeriesState is LoadState.Loading -> CenteredMessage("Loading…")
                level == BrowseLevel.CATEGORIES -> LoadStateList(
                    state = categoriesState,
                    emptyMessage = "Provider returned zero series categories.",
                    itemLabel = { it.name },
                ) { category ->
                    seriesListState = LoadState.Loading
                    level = BrowseLevel.ITEMS
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadSeries(provider, category.id) } }
                        seriesListState = result.fold(
                            onSuccess = { LoadState.Loaded(it) },
                            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
                        )
                    }
                }
                else -> LoadStateList(
                    state = seriesListState,
                    emptyMessage = "This category has zero series.",
                    itemLabel = { it.title },
                ) { series ->
                    selectedSeriesState = LoadState.Loading
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { runCatching { app.providerRepository.loadSeriesDetail(provider, series) } }
                        selectedSeriesState = result.fold(
                            onSuccess = { LoadState.Loaded(it) },
                            onFailure = { LoadState.Failed(it.message ?: it.toString()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> LoadStateList(
    state: LoadState<List<T>>,
    emptyMessage: String,
    itemLabel: (T) -> String,
    onClick: (T) -> Unit,
) {
    when (state) {
        is LoadState.Loading -> CenteredMessage("Loading…")
        is LoadState.Failed -> CenteredMessage(state.message, isError = true)
        is LoadState.Loaded -> {
            if (state.value.isEmpty()) {
                CenteredMessage(emptyMessage)
            } else {
                LazyColumn {
                    items(state.value) { item ->
                        ListRow(itemLabel(item)) { onClick(item) }
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
private fun CenteredMessage(message: String, isError: Boolean = false) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            modifier = Modifier.padding(24.dp),
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun NoProviderMessage() {
    CenteredMessage("No IPTV provider configured yet. Go to Settings to add one.")
}
