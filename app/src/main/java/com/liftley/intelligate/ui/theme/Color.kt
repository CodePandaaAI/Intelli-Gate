package com.liftley.intelligate.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Material color roles with a violet primary and contrasting warm tertiary.
internal val LightColors = lightColorScheme(
    primary = Color(0xFF6240C5), onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF), onPrimaryContainer = Color(0xFF24005B),
    secondary = Color(0xFF605876), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7DEF8), onSecondaryContainer = Color(0xFF1D1730),
    tertiary = Color(0xFF855300), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB0), onTertiaryContainer = Color(0xFF2A1700),
    background = Color(0xFFFCF8FF), onBackground = Color(0xFF1D1A24),
    surface = Color(0xFFFCF8FF), onSurface = Color(0xFF1D1A24),
    surfaceVariant = Color(0xFFE7E0EE), onSurfaceVariant = Color(0xFF494453),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF6F1FB),
    surfaceContainer = Color(0xFFF0EBF5), surfaceContainerHigh = Color(0xFFEAE5EF),
    surfaceContainerHighest = Color(0xFFE4DFE9),
    outline = Color(0xFF7B7485), outlineVariant = Color(0xFFCCC4D5),
)
internal val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381475),
    primaryContainer = Color(0xFF4C289F), onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFCBC2DC), onSecondary = Color(0xFF322C46),
    secondaryContainer = Color(0xFF49425D), onSecondaryContainer = Color(0xFFE7DEF8),
    tertiary = Color(0xFFFFB95C), onTertiary = Color(0xFF462A00),
    tertiaryContainer = Color(0xFF653E00), onTertiaryContainer = Color(0xFFFFDDB0),
    background = Color(0xFF15121B), onBackground = Color(0xFFE8E0EE),
    surface = Color(0xFF15121B), onSurface = Color(0xFFE8E0EE),
    surfaceVariant = Color(0xFF494453), onSurfaceVariant = Color(0xFFCCC4D5),
    surfaceContainerLowest = Color(0xFF100D15), surfaceContainerLow = Color(0xFF1D1A24),
    surfaceContainer = Color(0xFF211E28), surfaceContainerHigh = Color(0xFF2C2833),
    surfaceContainerHighest = Color(0xFF37333E),
    outline = Color(0xFF958DA0), outlineVariant = Color(0xFF494453),
)

