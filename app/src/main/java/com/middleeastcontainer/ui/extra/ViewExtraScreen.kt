package com.middleeastcontainer.ui.extra

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.FileImage
import com.middleeastcontainer.ui.components.MecrcScaffold
import com.middleeastcontainer.ui.components.StatefulScaffold

@Composable
fun ViewExtraScreen(
    container: String,
    onBack: () -> Unit,
    viewModel: ViewExtraViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MecrcScaffold(title = "Extra images", onBack = onBack) { padding ->
        StatefulScaffold(
            state = state,
            modifier = Modifier.padding(padding),
            onRetry = viewModel::load,
            emptyMessage = "No extra images for this project.",
        ) { items ->
            LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                items(items) { item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            FileImage(item.absolutePath, "Extra image", Modifier.fillMaxWidth().height(200.dp))
                            item.extra.category?.takeIf { it.isNotBlank() }?.let { Text("Category: $it") }
                            item.extra.remark?.takeIf { it.isNotBlank() }?.let { Text("Remark: $it") }
                            item.extra.time?.let { Text(it) }
                            Text("Status: ${item.extra.status}")
                        }
                    }
                }
            }
        }
    }
}
