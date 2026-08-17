package com.middleeastcontainer.ui.dimension

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.ContainerCode
import com.middleeastcontainer.ui.components.Eyebrow
import com.middleeastcontainer.ui.components.FileImage
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.components.StatefulScaffold
import com.middleeastcontainer.ui.theme.VerifiedGreen

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DimensionScreen(
    container: String,
    type: String,
    onBackToMenu: () -> Unit,
    onOpenSide: (sideDbName: String) -> Unit,
    onEditSide: (sideDbName: String) -> Unit,
    onAddExtra: () -> Unit,
    onViewExtra: () -> Unit,
    viewModel: DimensionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Coming back from the camera, the new thumbnail must be here already.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    MecrcScaffold(title = container, onBack = onBackToMenu) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp)) {
                ContainerCode(container)
                Eyebrow(type, Modifier.padding(top = 8.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Opens the camera and stays there — shoot as many as you like.
                Button(onClick = onAddExtra, modifier = Modifier.weight(1f)) { Text("Camera") }
                Button(onClick = onViewExtra, modifier = Modifier.weight(1f)) { Text("Photos") }
                Button(onClick = onBackToMenu, modifier = Modifier.weight(1f)) { Text("Done") }
            }

            StatefulScaffold(
                state = state,
                onRetry = { viewModel.load() },
                emptyMessage = "No sides to capture.",
            ) { rows ->
                LazyColumn(
                    Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(rows) { row ->
                        Card(
                            Modifier.fillMaxWidth().combinedClickable(
                                // Tap shoots straight away; the remark is a
                                // long-press so it never slows the common path.
                                onClick = { onOpenSide(row.side.dbName) },
                                onLongClick = { onEditSide(row.side.dbName) },
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (row.absolutePath != null) {
                                    FileImage(
                                        absolutePath = row.absolutePath,
                                        contentDescription = "${row.side.label} photo",
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(row.side.label, fontWeight = FontWeight.Medium)
                                    if (row.captured) {
                                        Text(
                                            "Captured",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VerifiedGreen,
                                        )
                                    } else {
                                        Text(
                                            "Tap to capture",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    row.remark?.takeIf { it.isNotBlank() }?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
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
