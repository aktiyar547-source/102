package com.middleeastcontainer.ui.inventory

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.middleeastcontainer.ui.components.ContainerCode
import com.middleeastcontainer.ui.components.ContainerCodeSize
import com.middleeastcontainer.ui.components.FileImage
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.StencilFamily
import com.middleeastcontainer.ui.theme.VerifiedGreen

/**
 * A yard sweep: photograph a stack, confirm what was read, keep walking.
 *
 * The running count sits over the viewfinder deliberately. A sweep that has gone
 * wrong — wrong zone, camera not reading — should be obvious within a few frames
 * rather than at the end of the yard.
 */
@Composable
fun SweepScreen(
    onFinished: () -> Unit,
    viewModel: SweepViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sightings by viewModel.sightings.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    val zone by viewModel.zone.collectAsStateWithLifecycle()

    var showList by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var showUnread by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var askMissed by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        SweepCamera(
            newTarget = viewModel::newCaptureFile,
            onCaptured = viewModel::onPhotoTaken,
            onError = viewModel::onCameraError,
            enabled = state.pending == null && !state.scanning,
            modifier = Modifier.fillMaxSize(),
        )

        // Running count — the whole point of showing it here.
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                "${sightings.size} units",
                color = BrandGold,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                zone.ifBlank { "Yard" },
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
            )
            val unresolved = pending.count { !it.resolved }
            if (unresolved > 0) {
                Text(
                    "$unresolved need attention",
                    color = BrandGold,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (state.scanning) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = BrandGold)
                Text(
                    "Reading numbers…",
                    color = Color.White,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }

        state.message?.let { msg ->
            Text(
                msg,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(16.dp),
            )
        }

        // What the last frame counted — visible, and undoable, without stopping.
        state.lastShot?.let { last ->
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 96.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(10.dp))
                    .clickable { viewModel.reviewLastShot() }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(52.dp)) {
                    FileImage(
                        absolutePath = last.photoAbsolutePath,
                        contentDescription = "Last frame",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            if (last.added.isEmpty()) "All already counted"
                            else "Saved ${last.added.size}",
                            color = if (last.added.isEmpty()) Color.White else VerifiedGreen,
                            fontWeight = FontWeight.Medium,
                        )
                        if (last.needsAttention.isNotEmpty()) {
                            Text(
                                "· ${last.needsAttention.size} to check",
                                color = BrandGold,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    // Every number, not a sample: the inspector reads these off
                    // against the stack in front of them to work out which units
                    // are still missing.
                    if (last.added.isNotEmpty()) {
                        Text(
                            last.added.joinToString("  "),
                            color = Color(0xE6FFFFFF),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = StencilFamily,
                        )
                    }
                    if (last.duplicates.isNotEmpty()) {
                        Text(
                            "already had: " + last.duplicates.joinToString("  "),
                            color = Color(0x99FFFFFF),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                TextButton(onClick = { askMissed = true }) {
                    Text("Missed?", color = BrandGold)
                }
                TextButton(onClick = { viewModel.undoLastShot() }) {
                    Text("Undo", color = Color(0xCCFFFFFF))
                }
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = { showList = true }) { Text("List") }
            OutlinedButton(onClick = { showUnread = true }) {
                val n = pending.count { !it.resolved }
                Text(if (n > 0) "Gaps ($n)" else "Gaps")
            }
            OutlinedButton(onClick = { showManual = true }) { Text("Type") }
            Button(onClick = { viewModel.finish(onFinished) }) { Text("Finish") }
        }
    }

    // Only interrupts when the frame read nothing, or the inspector asks to look.
    state.pending?.let { shot ->
        ReviewSheet(
            shot = shot,
            onConfirm = { viewModel.confirm(it) },
            onDiscard = { viewModel.discardShot() },
        )
    }

    if (showList) {
        SweepListDialog(
            sightings = sightings,
            unread = unread,
            onRemove = { viewModel.remove(it) },
            onClose = { showList = false },
        )
    }

    if (askMissed) {
        MissedCountDialog(
            onPick = { n -> viewModel.noteMissed(n); askMissed = false },
            onCancel = { askMissed = false },
        )
    }

    if (showUnread) {
        UnreadDialog(
            unread = unread,
            onDismissOne = { viewModel.dismissUnread(it) },
            onResolve = { unit, number, onInvalid ->
                viewModel.resolveUnread(unit, number, onInvalid)
            },
            onClose = { showUnread = false },
        )
    }

    // Finishing with gaps is allowed, but never by accident.
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("${unread.size} still unread") },
            text = {
                Text(
                    "These containers were seen but their numbers could not be read. " +
                        "Finishing now records the sweep with those gaps."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    viewModel.finish(onFinished)
                }) { Text("Finish anyway") }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    showUnread = true
                }) { Text("Show them") }
            },
        )
    }

    if (showManual) {
        ManualEntryDialog(
            onAdd = { number, onInvalid -> viewModel.addManually(number, onInvalid) },
            onClose = { showManual = false },
        )
    }
}

@Composable
private fun ReviewSheet(
    shot: PendingShot,
    onConfirm: (List<String>) -> Unit,
    onDiscard: () -> Unit,
) {
    val chosen = remember(shot) {
        mutableStateListOf<String>().apply { addAll(shot.detected.map { it.number }) }
    }

    AlertDialog(
        onDismissRequest = onDiscard,
        title = {
            Text(
                if (shot.detected.isEmpty()) "Nothing readable"
                else "${shot.detected.size} in this frame"
            )
        },
        text = {
            Column {
                // Boxes make the gap obvious: five numbers on a stack of eight
                // is only visible as a miss when you can see where they were.
                DetectionOverlay(
                    photoAbsolutePath = shot.photoAbsolutePath,
                    detections = shot.detected,
                    unread = shot.unread,
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                )
                if (shot.detected.isEmpty()) {
                    Text(
                        "Move closer, or pinch to zoom and retake. You can also type " +
                            "the number from the sweep screen.",
                        Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else {
                    LazyColumn(Modifier.padding(top = 12.dp).height(200.dp)) {
                        if (shot.detected.isNotEmpty()) {
                            item {
                                Text(
                                    "SAVED — ${shot.detected.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = VerifiedGreen,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                        }
                        items(shot.detected) { d ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (chosen.contains(d.number)) chosen.remove(d.number)
                                        else chosen.add(d.number)
                                    }
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = chosen.contains(d.number),
                                    onCheckedChange = {
                                        if (it) chosen.add(d.number) else chosen.remove(d.number)
                                    },
                                )
                                ContainerCode(d.number, size = ContainerCodeSize.Small)
                            }
                        }
                        // Listed beneath, so the two groups read as one frame:
                        // these are what remains once the saved ones are matched.
                        if (shot.unread.isNotEmpty()) {
                            item {
                                Text(
                                    "NOT READABLE — ${shot.unread.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandGold,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(shot.unread) { i, u ->
                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        "A${i + 1}",
                                        color = BrandGold,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        if (u.partial.isNotBlank()) "${u.partial}…"
                                        else "not legible",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = StencilFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(chosen.toList()) }) {
                Text(if (chosen.isEmpty()) "Close" else "Keep ${chosen.size}")
            }
        },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Retake") } },
    )
}

@Composable
private fun SweepListDialog(
    sightings: List<com.middleeastcontainer.domain.model.Sighting>,
    unread: List<com.middleeastcontainer.domain.model.UnreadUnit>,
    onRemove: (com.middleeastcontainer.domain.model.Sighting) -> Unit,
    onClose: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                if (unread.isEmpty()) "${sightings.size} counted"
                else "${sightings.size} counted · ${unread.size} to check"
            )
        },
        text = {
            if (sightings.isEmpty() && unread.isEmpty()) {
                Text("Nothing counted yet.")
            } else {
                LazyColumn(Modifier.height(360.dp)) {
                    // Outstanding items first: they are the reason to open this.
                    if (unread.isNotEmpty()) {
                        item {
                            Text(
                                "STILL TO CHECK",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandGold,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                        }
                        items(unread) { u ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(u.tag, color = BrandGold, fontWeight = FontWeight.Bold)
                                Text(
                                    if (u.partial.isNotBlank()) "${u.partial}…" else "not legible",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = StencilFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        item {
                            Text(
                                "COUNTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = VerifiedGreen,
                                modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                            )
                        }
                    }
                    items(sightings) { s ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            ContainerCode(s.containerNumber, size = ContainerCodeSize.Small)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!s.fromOcr) {
                                    Text(
                                        "typed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { onRemove(s) }) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
    )
}

/**
 * How many containers were in shot but unreadable.
 *
 * A row of numbers rather than a text field: this is answered while standing in
 * front of a stack, and must cost one tap.
 */
@Composable
private fun MissedCountDialog(onPick: (Int) -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("How many could not be read?") },
        text = {
            Column {
                Text(
                    "They will be listed as gaps with this photo, so you can identify " +
                        "them later without walking the whole row again.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (1..6).forEach { n ->
                        OutlinedButton(
                            onClick = { onPick(n) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.weight(1f),
                        ) { Text("$n") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCancel) { Text("Cancel") } },
    )
}

/** The gaps: what is still unidentified, and the photo that will identify it. */
@Composable
private fun UnreadDialog(
    unread: List<com.middleeastcontainer.domain.model.UnreadUnit>,
    onDismissOne: (com.middleeastcontainer.domain.model.UnreadUnit) -> Unit,
    onResolve: (com.middleeastcontainer.domain.model.UnreadUnit, String, () -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    var typingFor by remember {
        mutableStateOf<com.middleeastcontainer.domain.model.UnreadUnit?>(null)
    }

    typingFor?.let { unit ->
        var value by remember(unit) { mutableStateOf(unit.partial) }
        var invalid by remember(unit) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { typingFor = null },
            title = { Text(unit.tag) },
            text = {
                Column {
                    Text(
                        "Read it off the container — the camera often cannot where a " +
                            "person can.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it.uppercase(); invalid = false },
                        label = { Text("Container number") },
                        isError = invalid,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                    if (invalid) {
                        Text(
                            "Not a valid ISO 6346 number — check the digits.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onResolve(unit, value) { invalid = true }
                    if (!invalid) typingFor = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { typingFor = null }) { Text("Cancel") }
            },
        )
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("${unread.size} to check") },
        text = {
            Column {
                Text(
                    "Seen but not readable. Walk closer and photograph each one; it " +
                        "clears itself once the number is captured.",
                    style = MaterialTheme.typography.bodySmall,
                )
                LazyColumn(Modifier.padding(top = 12.dp).height(300.dp)) {
                    items(unread) { u ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                // The photo, with this unit boxed — the fastest way
                                // to recognise which physical container it is.
                                u.photoPath?.let { _ ->
                                    Box(Modifier.size(56.dp)) {
                                        FileImage(
                                            absolutePath = u.photoPath,
                                            contentDescription = "Frame for ${u.tag}",
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(u.tag, fontWeight = FontWeight.Bold, color = BrandGold)
                                    Text(
                                        if (u.partial.isNotBlank()) "read: ${u.partial}…"
                                        else "number not legible",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { onDismissOne(u) }) { Text("Not there") }
                                TextButton(onClick = { typingFor = u }) { Text("Type") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } },
    )
}

@Composable
private fun ManualEntryDialog(
    onAdd: (String, () -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    var value by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Add a unit") },
        text = {
            Column {
                Text(
                    "For a container the camera cannot read — unreachable, or the number painted over.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.uppercase(); error = false },
                    label = { Text("Container number") },
                    isError = error,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (error) {
                    Text(
                        "That is not a valid ISO 6346 number — check the digits.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(value) { error = true }
                if (!error) { value = ""; onClose() }
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } },
    )
}
