package com.middleeastcontainer.ui.activation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.Eyebrow
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.StencilFamily

/**
 * Shown once, before the app can be used.
 *
 * Installation itself cannot be gated — that is the operating system's business —
 * so this is what stops a copied APK from being any use to whoever has it.
 */
@Composable
fun ActivationScreen(
    onActivated: () -> Unit,
    viewModel: ActivationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Already activated on this device: never ask again.
    LaunchedEffect(Unit) { if (viewModel.alreadyActivated) onActivated() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Corrugation()

            Spacer(Modifier.height(26.dp))

            Text(
                "MECRC",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = StencilFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Eyebrow("Activation required", Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(28.dp))

            Text(
                "This device has not been activated. Enter the code supplied with the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state.code,
                onValueChange = viewModel::onCodeChange,
                label = { Text("Activation code") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = state.error != null,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { viewModel.submit(onActivated) },
                enabled = !state.checking,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.checking) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                } else {
                    Text(
                        "Activate",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        Text(
            "Developer · Akhtiyar Khan",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 22.dp),
        )
    }
}

@Composable
private fun Corrugation() {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(5) { i ->
            Box(
                Modifier
                    .size(width = 9.dp, height = if (i == 4) 34.dp else 26.dp)
                    .background(
                        if (i == 4) BrandGold else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(2.dp),
                    )
            )
        }
    }
}
