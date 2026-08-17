package com.middleeastcontainer.ui.upload

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.VerifiedGreen
import com.middleeastcontainer.ui.components.StatefulScaffold

@Composable
fun UploadScreen(onBack: () -> Unit, viewModel: UploadViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    MecrcScaffold(title = "Upload", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatefulScaffold(
                state = state,
                emptyMessage = "No projects to upload yet.",
            ) { summary ->
                Column(Modifier.fillMaxSize()) {

                    Button(
                        onClick = {
                            val names = selected.filterValues { it }.keys
                            if (names.isNotEmpty()) {
                                viewModel.upload(names)
                                selected.clear()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    ) { Text("Upload selected") }

                    // Live summary while a batch is in flight.
                    if (summary.activeTotal > 0 || summary.done > 0 || summary.failed > 0) {
                        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    "Uploading ${summary.running} • queued ${summary.queued} • " +
                                        "done ${summary.done} • failed ${summary.failed}",
                                    fontWeight = FontWeight.Medium,
                                )
                                if (summary.activeTotal > 0) {
                                    Text(
                                        "Uploads run one at a time and continue in the background.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(summary.rows) { row ->
                            val c = row.container
                            Card(
                                Modifier.fillMaxWidth().clickable {
                                    selected[c.name] = !(selected[c.name] ?: false)
                                }
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = selected[c.name] ?: false,
                                        onCheckedChange = { selected[c.name] = it },
                                        enabled = row.status != UploadStatus.RUNNING,
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, fontWeight = FontWeight.Medium)
                                        Text(
                                            "${c.type} • extra: ${c.extraImageCount}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        StatusLine(row)
                                    }
                                    if (row.status == UploadStatus.RUNNING) {
                                        CircularProgressIndicator(Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(row: UploadRow) {
    val (label, color) = when (row.status) {
        UploadStatus.NONE -> (row.container.uploadStatus) to Color.Unspecified
        UploadStatus.QUEUED -> "Queued — waiting for network" to Color.Unspecified
        UploadStatus.RUNNING -> (row.step ?: "Uploading…") to BrandGold
        UploadStatus.DONE -> "Uploaded" to VerifiedGreen
        UploadStatus.FAILED -> "Failed — will retry" to MaterialTheme.colorScheme.error
    }
    Text(label, style = MaterialTheme.typography.bodySmall, color = color)
}
