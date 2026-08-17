package com.middleeastcontainer.ui.singleside

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.Eyebrow
import com.middleeastcontainer.ui.components.FileImage
import com.middleeastcontainer.ui.components.MecrcScaffold

/**
 * Remark editor for one side, reached by long-pressing a row in the grid.
 *
 * Capture lives in the camera screen now, so this only shows the photo and lets
 * a note be attached — the rare case, kept off the fast path.
 */
@Composable
fun SingleSideScreen(
    container: String,
    type: String,
    sideDbName: String,
    onDone: () -> Unit,
    viewModel: SingleSideViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MecrcScaffold(title = viewModel.side.label, onBack = onDone) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Eyebrow("$container · $type")

            if (state.previewPath != null) {
                FileImage(
                    absolutePath = state.previewPath,
                    contentDescription = "Side photo",
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                )
            } else {
                Card(Modifier.fillMaxWidth().height(140.dp)) {
                    Text(
                        "Not photographed yet. Go back and tap this side to capture it.",
                        Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            OutlinedTextField(
                value = state.remark,
                onValueChange = viewModel::onRemarkChange,
                label = { Text("Remark") },
                modifier = Modifier.fillMaxWidth(),
            )

            state.message?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.saving) "Saving..." else "Save remark")
            }
        }
    }
}
