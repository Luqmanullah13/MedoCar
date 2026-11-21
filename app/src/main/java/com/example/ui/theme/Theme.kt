package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DocMedsLightColors = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = CrispWhite,
    primaryContainer = EmeraldMint,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldDeep,
    onSecondary = CrispWhite,
    secondaryContainer = EmeraldSoftSurface,
    onSecondaryContainer = EmeraldDark,
    tertiary = AmberRx,
    onTertiary = CrispWhite,
    tertiaryContainer = AmberRxBg,
    onTertiaryContainer = InkPrimary,
    background = SoftBackground,
    onBackground = InkPrimary,
    surface = CrispWhite,
    onSurface = InkPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = InkSecondary,
    outline = BorderSubtle,
    error = CoralAlert,
    onError = CrispWhite,
    errorContainer = CoralAlertBg,
    onErrorContainer = CoralAlert
)

val DocMedsShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DocMedsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DocMedsLightColors,
        typography = DocMedsTypography,
        shapes = DocMedsShapes,
        content = content
    )
}
