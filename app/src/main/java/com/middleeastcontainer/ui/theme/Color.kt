package com.middleeastcontainer.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Drawn from the MECRC company logo: navy grounds, a warm gold accent, and
 * brushed-silver secondaries.
 *
 * The gold is set brighter than the logo's metallic mid-tone (#BB9254). Two
 * reasons: a flat UI fill cannot reproduce a metallic gradient, and inspectors
 * read these screens in direct sunlight, where the darker tone loses contrast.
 * Same hue family, tuned for the yard.
 */

// Navy — grounds and surfaces
val NavyDeep    = Color(0xFF081324)   // deepest ground
val NavyBase    = Color(0xFF0C1B33)   // app surface
val NavyRaised  = Color(0xFF16294A)   // cards, raised panels
val NavyHair    = Color(0xFF243C63)   // hairlines, dividers

// Gold — the single accent
val BrandGold      = Color(0xFFF2A33C)
val BrandGoldDeep  = Color(0xFFBB9254)   // the logo's metallic mid-tone

// Status
val VerifiedGreen = Color(0xFF3FB27F)  // valid code, uploaded
val AlertRed      = Color(0xFFE5533D)  // rejected, failed

// Type on navy
val Silver      = Color(0xFFE8EBF0)    // primary
val SilverMuted = Color(0xFF93A6C2)    // secondary

// Daylight scheme — for bright yards and desk review
val PaperCool    = Color(0xFFF2F5F9)
val PaperSurface = Color(0xFFFFFFFF)
val PaperHair    = Color(0xFFD8E0EB)
val Ink          = Color(0xFF081324)
val InkMuted     = Color(0xFF56688A)
