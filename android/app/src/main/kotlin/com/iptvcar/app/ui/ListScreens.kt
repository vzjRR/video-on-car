package com.iptvcar.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iptvcar.app.IptvCarApplication
import java.text.DateFormat
import java.util.Date

@Composable
fun FavoritesScreen(app: IptvCarApplication) {
    val favorites = app.favoritesRepository.all()
    Scaffold(topBar = { TopAppBar(title = { Text("Favorites") }) }) { padding ->
        if (favorites.isEmpty()) {
            Text("No favorites yet.", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(favorites) { favorite ->
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Text(
                        "${favorite.contentType} · ${favorite.contentId}",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingScreen(app: IptvCarApplication) {
    val items = app.resumeRepository.allContinueWatching()
    Scaffold(topBar = { TopAppBar(title = { Text("Continue Watching") }) }) { padding ->
        if (items.isEmpty()) {
            Text("Nothing in progress.", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(items) { resume ->
                val percent = if (resume.durationMs > 0) (resume.positionMs * 100 / resume.durationMs) else 0
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${resume.contentType} · ${resume.contentId}")
                        Text(
                            "$percent% watched · ${DateFormat.getDateTimeInstance().format(Date(resume.updatedAt))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
