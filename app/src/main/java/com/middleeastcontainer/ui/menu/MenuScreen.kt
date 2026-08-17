package com.middleeastcontainer.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.middleeastcontainer.ui.components.Eyebrow
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.NavyDeep
import com.middleeastcontainer.ui.theme.StencilFamily

/**
 * The yard hub. Capture is the primary act and gets the accent; everything else
 * is a quiet secondary row, so the screen has one obvious thing to press while
 * wearing gloves in poor light.
 */
@Composable
fun MenuScreen(
    onNewProject: () -> Unit,
    onPreview: () -> Unit,
    onUpload: () -> Unit,
    onDelete: () -> Unit,
    onInventory: () -> Unit,
    onSettings: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel(),
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(38.dp))
        Text(
            "MECRC",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = StencilFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Eyebrow("Container Inspection", Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(30.dp))

        PrimaryAction(
            title = "New inspection",
            detail = "Scan a container number and capture all sides",
            onClick = onNewProject,
        )

        Spacer(Modifier.height(10.dp))

        PrimaryAction(
            title = "Yard inventory",
            detail = "Photograph stacks and count what is here",
            onClick = onInventory,
        )

        Spacer(Modifier.height(26.dp))
        Eyebrow("Records")
        Spacer(Modifier.height(10.dp))

        SecondaryAction("Inspections", "Review and continue captured work", onPreview)
        SecondaryAction("Upload", "Send completed inspections to the server", onUpload)
        SecondaryAction("Delete", "Remove inspections already uploaded", onDelete)
        SecondaryAction("Settings", "Inspector name and server address", onSettings)

        Spacer(Modifier.weight(1f))
        Text(
            "Developer · Akhtiyar Khan",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun PrimaryAction(title: String, detail: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(BrandGold, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = NavyDeep.copy(alpha = 0.80f),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun SecondaryAction(title: String, detail: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 26.dp)
                .background(BrandGold, RoundedCornerShape(2.dp))
        )
        Column(Modifier.padding(start = 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
