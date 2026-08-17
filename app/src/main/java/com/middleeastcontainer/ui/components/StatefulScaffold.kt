package com.middleeastcontainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Renders the shared states uniformly. [emptyMessage]/[content] are supplied per
 * screen. Error and Offline expose a Retry affordance. Messages are written as
 * directive interface copy (frontend-design writing guidance), not apologies.
 */
@Composable
fun <T> StatefulScaffold(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Nothing here yet.",
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is UiState.Loading -> CircularProgressIndicator(
                Modifier.semantics { contentDescription = "Loading" }
            )
            is UiState.Empty -> Text(emptyMessage, Modifier.padding(24.dp))
            is UiState.Offline -> RetryPanel(
                title = "You're offline",
                body = "Connect to a network to continue. Your captured data is saved.",
                onRetry = onRetry,
            )
            is UiState.Error -> RetryPanel(
                title = "Something went wrong",
                body = state.message,
                onRetry = onRetry,
            )
            is UiState.Content -> content(state.data)
        }
    }
}

@Composable
private fun RetryPanel(title: String, body: String, onRetry: (() -> Unit)?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(24.dp),
    ) {
        Text(title)
        Text(body)
        if (onRetry != null) Button(onClick = onRetry) { Text("Retry") }
    }
}
