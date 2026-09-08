package com.iptvcar.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.iptvcar.app.IptvCarApplication

private data class HomeItem(val label: String, val route: String)

private val homeItems = listOf(
    HomeItem("Live TV", "live"),
    HomeItem("Movies", "movies"),
    HomeItem("Series", "series"),
    HomeItem("Favorites", "favorites"),
    HomeItem("Continue Watching", "continue_watching"),
    HomeItem("Settings", "settings"),
)

@Composable
fun HomeScreen(app: IptvCarApplication, navController: NavHostController) {
    Scaffold(topBar = { TopAppBar(title = { Text("IPTV Car") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(homeItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { navController.navigate(item.route) },
                ) {
                    Text(item.label, modifier = Modifier.padding(20.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
