package com.iptvcar.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.iptvcar.app.IptvCarApplication
import com.iptvcar.core.model.Provider

/**
 * The only screen that ever handles raw credentials. Nothing entered here
 * is logged (see core/security/Redaction) and the raw M3U URL/password is
 * never re-displayed once a provider is saved (Phase 3 requirement).
 */
@Composable
fun SettingsScreen(app: IptvCarApplication, navController: NavHostController) {
    var providers by remember { mutableStateOf(app.providerRepository.listProviders()) }
    var showXtreamForm by remember { mutableStateOf(false) }
    var showM3uForm by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() { providers = app.providerRepository.listProviders() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(providers) { provider ->
                ProviderRow(provider) {
                    app.providerRepository.deleteProvider(provider.id)
                    refresh()
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Row {
                    Button(onClick = { showXtreamForm = true }) { Text("Add Xtream Provider") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { showM3uForm = true }) { Text("Add M3U Provider") }
                }
                statusMessage?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
    }

    if (showXtreamForm) {
        XtreamForm(
            onCancel = { showXtreamForm = false },
            onSubmit = { name, url, user, pass ->
                app.providerRepository.addXtreamProvider(name, url, user, pass)
                showXtreamForm = false
                statusMessage = "Xtream provider added."
                refresh()
            },
        )
    }

    if (showM3uForm) {
        M3uForm(
            onCancel = { showM3uForm = false },
            onSubmit = { name, url ->
                app.providerRepository.addM3uProvider(name, url)
                showM3uForm = false
                statusMessage = "M3U provider added."
                refresh()
            },
        )
    }
}

@Composable
private fun ProviderRow(provider: Provider, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(provider.displayName, style = MaterialTheme.typography.titleMedium)
                Text(provider.kind.name, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDelete) { Text("Remove") }
        }
    }
}

@Composable
private fun XtreamForm(onCancel: () -> Unit, onSubmit: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add Xtream Provider") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Server URL") })
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") })
                OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(name.ifBlank { url }, url, user, pass) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

@Composable
private fun M3uForm(onCancel: () -> Unit, onSubmit: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add M3U Provider") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") })
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Playlist URL or file path") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(name.ifBlank { url }, url) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
