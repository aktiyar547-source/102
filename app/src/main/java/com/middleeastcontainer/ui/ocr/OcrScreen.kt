package com.middleeastcontainer.ui.ocr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.MecrcScaffold

/**
 * "New Project" — scan or type a container number, pick a type, create the
 * inspection.
 *
 * Unlike a side photo, the scan cannot be fire-and-forget: OCR can misread a
 * weathered stencil, so the number is shown for confirmation before anything is
 * created. The camera itself is still the in-app one, so there is no
 * keep/discard prompt.
 */
@Composable
fun OcrScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    onCreated: (container: String, type: String) -> Unit,
    viewModel: OcrViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var typeExpanded by remember { mutableStateOf(false) }

    // Returning from the camera, read whatever scan it left behind.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.collectPendingScan() }

    MecrcScaffold(title = "New Project", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                Text("Scan container number")
            }

            if (state.recognizing) {
                CircularProgressIndicator()
            }

            OutlinedTextField(
                value = state.containerNumber,
                onValueChange = { viewModel.onNumberChange(it) },
                label = { Text("Container No") },
                isError = state.error != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            TextButton(onClick = { typeExpanded = true }) {
                Text("Type: " + state.selectedType)
            }
            DropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false },
            ) {
                state.types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = {
                            viewModel.onTypeChange(type)
                            typeExpanded = false
                        },
                    )
                }
            }

            Button(
                onClick = { viewModel.save(onCreated) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
