package com.middleeastcontainer.ui.inventory

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.domain.model.Sweep
import androidx.core.content.ContextCompat
import com.middleeastcontainer.ui.components.Eyebrow
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.components.StatefulScaffold
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.VerifiedGreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    onOpenSweep: (Long) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exported by viewModel.lastExport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var askZone by remember { mutableStateOf(false) }

    MecrcScaffold(title = "Inventory", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Button(
                onClick = { askZone = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) { Text("Start a sweep") }

            Eyebrow("Previous sweeps", Modifier.padding(start = 16.dp, bottom = 8.dp))

            StatefulScaffold(
                state = state,
                emptyMessage = "No sweeps yet. Start one to count the yard.",
            ) { sweeps ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sweeps) { sweep ->
                        SweepRow(
                            sweep = sweep,
                            onOpen = { onOpenSweep(sweep.id) },
                            onExport = { viewModel.exportToPhone(sweep) },
                            onDelete = { viewModel.delete(sweep) },
                        )
                    }
                }
            }
        }
    }

    // Written straight to the phone, so it exists before any server is reachable.
    exported?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissExport() },
            title = { Text(if (result.error == null) "Saved to Downloads" else "Could not save") },
            text = {
                if (result.error != null) {
                    Text(result.error)
                } else {
                    Column {
                        Text("${result.rows} units")
                        Text(
                            result.fileName.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                if (result.uri != null) {
                    TextButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = XLSX_MIME
                            putExtra(Intent.EXTRA_STREAM, result.uri)
                            putExtra(Intent.EXTRA_SUBJECT, result.fileName)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        ContextCompat.startActivity(
                            context,
                            Intent.createChooser(send, "Send inventory"),
                            null,
                        )
                        viewModel.dismissExport()
                    }) { Text("Share") }
                } else {
                    TextButton(onClick = { viewModel.dismissExport() }) { Text("Close") }
                }
            },
            dismissButton = {
                if (result.uri != null) {
                    TextButton(onClick = { viewModel.dismissExport() }) { Text("Done") }
                }
            },
        )
    }

    if (askZone) {
        ZoneDialog(
            onStart = { zone ->
                askZone = false
                viewModel.startSweep(zone) { onOpenSweep(it) }
            },
            onCancel = { askZone = false },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SweepRow(
    sweep: Sweep,
    onOpen: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = onOpen,
            onLongClick = { confirmDelete = true },
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(sweep.zone, fontWeight = FontWeight.Medium)
                Text(
                    sweep.startedAt + "  ·  " + sweep.startedBy,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              TextButton(onClick = onExport) { Text("Excel") }
              Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${sweep.unitCount}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = BrandGold,
                )
                Text(
                    if (sweep.finishedAt != null) "finished" else "in progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sweep.finishedAt != null) VerifiedGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this sweep?") },
            text = { Text("${sweep.unitCount} counted units will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Keep") }
            },
        )
    }
}

private const val XLSX_MIME =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@Composable
private fun ZoneDialog(onStart: (String) -> Unit, onCancel: () -> Unit) {
    var zone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Which part of the yard?") },
        text = {
            Column {
                Text(
                    "Naming the area makes the count useful for finding a unit later, not just counting it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = zone,
                    onValueChange = { zone = it },
                    label = { Text("Zone, row or block") },
                    placeholder = { Text("e.g. Row C") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onStart(zone) }) { Text("Start") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}
