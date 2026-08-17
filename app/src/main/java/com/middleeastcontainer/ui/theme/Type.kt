package com.middleeastcontainer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Two voices: a tight sans for interface text, and monospace reserved strictly
 * for container numbers and machine data. Nothing else gets mono — that is what
 * makes a stencilled code read as a code rather than as decoration.
 */
val MecrcTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal, fontSize = 12.5.sp, letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.9.sp,
    ),
)

/** Reserved for ISO 6346 codes, device ids and timestamps. */
val StencilFamily = FontFamily.Monospace
