package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RoyaleDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianDeep,
    primaryContainer = ElectricCyanDim,
    onPrimaryContainer = ElectricCyan,
    secondary = RoyalGold,
    onSecondary = ObsidianDeep,
    secondaryContainer = RoyalGoldDim,
    onSecondaryContainer = RoyalGoldSoft,
    tertiary = ElixirMagenta,
    onTertiary = TextPrimary,
    tertiaryContainer = ElixirMagentaDim,
    onTertiaryContainer = ElixirMagenta,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianDeep,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder,
    error = CrimsonThreat,
    onError = TextPrimary,
    errorContainer = CrimsonDim,
    onErrorContainer = CrimsonThreat
)

val RoyaleShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RoyaleAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RoyaleDarkColorScheme,
        typography = RoyaleTypography,
        shapes = RoyaleShapes,
        content = content
    )
}
