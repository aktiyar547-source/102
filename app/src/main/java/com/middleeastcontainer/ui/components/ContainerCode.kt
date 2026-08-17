package com.middleeastcontainer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.StencilFamily

/**
 * The signature treatment of this app: an ISO 6346 code shown the way it is
 * actually stencilled on a container's steel — monospaced, letter-spaced, and
 * split into its three real parts.
 *
 *     CSQU  305438  3
 *     owner  serial  check
 *
 * The check digit is set in signal amber because it is the part that decides
 * whether the code is valid at all.
 */
@Composable
fun ContainerCode(
    code: String,
    modifier: Modifier = Modifier,
    size: ContainerCodeSize = ContainerCodeSize.Medium,
) {
    val owner = code.take(4)
    val serial = code.drop(4).take(6)
    val check = code.drop(10).take(1)

    Row(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(4.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .padding(horizontal = size.padH, vertical = size.padV),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = owner,
            fontFamily = StencilFamily,
            fontWeight = FontWeight.Bold,
            fontSize = size.font,
            letterSpacing = size.tracking,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "  $serial",
            fontFamily = StencilFamily,
            fontWeight = FontWeight.Medium,
            fontSize = size.font,
            letterSpacing = size.tracking,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (check.isNotEmpty()) {
            Text(
                text = "  $check",
                fontFamily = StencilFamily,
                fontWeight = FontWeight.Bold,
                fontSize = size.font,
                letterSpacing = size.tracking,
                color = BrandGold,
            )
        }
    }
}

enum class ContainerCodeSize(
    val font: androidx.compose.ui.unit.TextUnit,
    val tracking: androidx.compose.ui.unit.TextUnit,
    val padH: androidx.compose.ui.unit.Dp,
    val padV: androidx.compose.ui.unit.Dp,
) {
    Small(13.sp, 1.2.sp, 8.dp, 4.dp),
    Medium(16.sp, 1.8.sp, 11.dp, 6.dp),
    Large(21.sp, 2.4.sp, 14.dp, 9.dp),
}

/** Small uppercase label used above sections and beside data. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color ?: MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
