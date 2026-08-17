package com.middleeastcontainer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SteelScheme = darkColorScheme(
    primary = BrandGold,
    onPrimary = NavyDeep,
    primaryContainer = BrandGoldDeep,
    onPrimaryContainer = NavyDeep,
    secondary = VerifiedGreen,
    background = NavyBase,
    onBackground = Silver,
    surface = NavyBase,
    onSurface = Silver,
    surfaceVariant = NavyRaised,
    onSurfaceVariant = SilverMuted,
    outline = NavyHair,
    error = AlertRed,
)

private val DaylightScheme = lightColorScheme(
    primary = BrandGoldDeep,
    onPrimary = PaperSurface,
    primaryContainer = BrandGold,
    onPrimaryContainer = Ink,
    secondary = VerifiedGreen,
    background = PaperCool,
    onBackground = Ink,
    surface = PaperSurface,
    onSurface = Ink,
    surfaceVariant = PaperCool,
    onSurfaceVariant = InkMuted,
    outline = PaperHair,
    error = AlertRed,
)

/** Defaults to the steel scheme — inspectors mostly work in low light or shade. */
@Composable
fun MecrcTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) SteelScheme else DaylightScheme,
        typography = MecrcTypography,
        content = content,
    )
}
