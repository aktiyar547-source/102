package com.middleeastcontainer.ui.preview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.ui.components.ContainerCode
import com.middleeastcontainer.ui.components.ContainerCodeSize
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.components.StatefulScaffold

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreviewScreen(
    onBack: () -> Unit,
    onOpen: (container: String, type: String) -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var typeDialogFor by remember { mutableStateOf<Container?>(null) }

    MecrcScaffold(title = "Preview", onBack = onBack) { padding ->
        StatefulScaffold(
            state = state,
            modifier = Modifier.padding(padding),
            emptyMessage = "No projects yet. Create one from New Project.",
        ) { containers ->
            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                items(containers) { c ->
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).combinedClickable(
                            onClick = { onOpen(c.name, c.type) },
                            onLongClick = { typeDialogFor = c },
                        )
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            ContainerCode(c.name, size = ContainerCodeSize.Small)
                            Text("${c.type} • ${c.date}")
                            Text("Status: ${c.status} • Extra images: ${c.extraImageCount}")
                        }
                    }
                }
            }
        }
    }

    typeDialogFor?.let { c ->
        var expanded by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { typeDialogFor = null },
            title = { Text("Update type") },
            text = {
                Column {
                    TextButton(onClick = { expanded = true }) { Text("Choose type") }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        viewModel.types.forEach { t ->
                            DropdownMenuItem(text = { Text(t) }, onClick = {
                                viewModel.updateType(c.name, t); expanded = false; typeDialogFor = null
                            })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { typeDialogFor = null }) { Text("Close") } },
        )
    }
}
