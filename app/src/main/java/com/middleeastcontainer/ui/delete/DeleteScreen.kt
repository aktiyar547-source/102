package com.middleeastcontainer.ui.delete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.components.StatefulScaffold

@Composable
fun DeleteScreen(onBack: () -> Unit, viewModel: DeleteViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selected = remember { mutableStateMapOf<String, Boolean>() }

    MecrcScaffold(title = "Delete", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Button(
                onClick = { viewModel.delete(selected.filterValues { it }.keys); selected.clear() },
                modifier = Modifier.fillMaxWidth().padding(12.dp),
            ) { Text("Delete selected") }

            StatefulScaffold(
                state = state,
                emptyMessage = "Nothing to delete. Only uploaded projects appear here.",
            ) { containers ->
                LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                    items(containers) { c ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clickable { selected[c.name] = !(selected[c.name] ?: false) }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected[c.name] ?: false,
                                    onCheckedChange = { selected[c.name] = it },
                                )
                                Column {
                                    Text(c.name, fontWeight = FontWeight.Medium)
                                    Text("${c.type} • ${c.status}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
