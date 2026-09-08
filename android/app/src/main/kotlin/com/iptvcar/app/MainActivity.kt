package com.iptvcar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iptvcar.app.player.PlayerActivity
import com.iptvcar.app.ui.*
import com.iptvcar.core.model.ContentType
import com.iptvcar.core.model.Movie
import com.iptvcar.core.model.Provider
import com.iptvcar.core.model.Series

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as IptvCarApplication

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IptvCarNavHost(app, ::startPlayback)
                }
            }
        }
    }

    private fun startPlayback(provider: Provider, contentType: ContentType, contentId: String, title: String, streamUrl: String) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_PROVIDER_ID, provider.id)
            putExtra(PlayerActivity.EXTRA_CONTENT_TYPE, contentType.name)
            putExtra(PlayerActivity.EXTRA_CONTENT_ID, contentId)
            putExtra(PlayerActivity.EXTRA_TITLE, title)
            putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
        }
        startActivity(intent)
    }
}

typealias PlaybackLauncher = (Provider, ContentType, String, String, String) -> Unit

@Composable
fun IptvCarNavHost(app: IptvCarApplication, onPlay: PlaybackLauncher) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(app, navController) }
        composable("live") { LiveScreen(app, onPlay) }
        composable("movies") { MoviesScreen(app, onPlay) }
        composable("series") { SeriesScreen(app, onPlay) }
        composable("favorites") { FavoritesScreen(app) }
        composable("continue_watching") { ContinueWatchingScreen(app) }
        composable("settings") { SettingsScreen(app, navController) }
    }
}
