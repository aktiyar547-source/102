package com.middleeastcontainer.ui.login

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.Eyebrow
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.StencilFamily

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.alreadyLoggedIn) { if (state.alreadyLoggedIn) onLoggedIn() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            // Corrugation mark — the vertical ribbing of a container wall.
            Corrugation()

            Spacer(Modifier.height(26.dp))

            Text(
                "MECRC",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = StencilFamily,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Eyebrow("Container Inspection", Modifier.padding(top = 6.dp))

            Spacer(Modifier.height(40.dp))

            Eyebrow("Inspector")
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsernameChange,
                placeholder = { Text("Your name") },
                singleLine = true,
                isError = state.usernameError != null,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandGold,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            state.usernameError?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Eyebrow("Device")
            Text(
                state.deviceId.take(18),
                fontFamily = StencilFamily,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = { viewModel.save(onLoggedIn) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(
                    "Start inspecting",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            "Developer · Akhtiyar Khan",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 22.dp),
        )
    }
}

/** Five bars echoing container wall corrugation; the last one carries the accent. */
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
