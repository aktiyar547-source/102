package com.middleeastcontainer.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.theme.VerifiedGreen

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The permission is granted in system Settings, so re-check on return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPhotoFolderAccess() }

    MecrcScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Inspector", fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                label = { Text("Username") },
                isError = state.error != null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Device ID: ${state.deviceId}", style = MaterialTheme.typography.bodySmall)

            Text("Photo folder", fontWeight = FontWeight.Medium)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    if (state.hasPhotoFolderAccess) {
                        Text("Saving to ${state.photoFolderPath}", color = VerifiedGreen)
                        Text(
                            "Inspection and Inventory folders sit inside it, visible over USB.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Photos are hidden inside Android/data",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "Allow all-files access to save them to a visible " +
                                "${state.photoFolderPath} folder instead.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                        Uri.parse("package:" + context.packageName),
                                    )
                                    ContextCompat.startActivity(context, intent, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        ) { Text("Allow") }
                    }
                }
            }

            Text("Server", fontWeight = FontWeight.Medium)
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = viewModel::onServerUrlChange,
                label = { Text("Base URL") },
                supportingText = { Text("Photos post to <base>container/test") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedButton(
                onClick = { viewModel.testConnection() },
                enabled = !state.testing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.testing) {
                    CircularProgressIndicator(Modifier.size(18.dp))
                } else {
                    Text("Test connection")
                }
            }

            state.testResult?.let { result ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        result,
                        Modifier.padding(14.dp),
                        color = if (state.testOk) VerifiedGreen else MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedButton(
                onClick = { viewModel.resetServerUrl() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset to default server") }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(onClick = { viewModel.save(onBack) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }
}
