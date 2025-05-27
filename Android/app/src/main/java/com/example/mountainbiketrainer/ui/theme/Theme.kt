package com.example.mountainbiketrainer.ui.theme // Ensure this package is correct

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightTrailblazerColorScheme = lightColorScheme(
    primary = Color(0xFF38761D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9F099),
    onPrimaryContainer = Color(0xFF002203),
    secondary = Color(0xFFFF8A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDDBE),
    onSecondaryContainer = Color(0xFF2C1300),
    tertiary = Color(0xFF795548),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7CCC8),
    onTertiaryContainer = Color(0xFF261915),
    error = Color(0xFFB00020), // Standard Error Colors
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF141213),
    background = Color(0xFFF7FDF3),
    onBackground = Color(0xFF1A1C19),
    surface = Color(0xFFF7FDF3),
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFDEE5D9),
    onSurfaceVariant = Color(0xFF424940),
    outline = Color(0xFF727970)
)

private val DarkTrailblazerColorScheme = darkColorScheme(
    primary = Color(0xFF6AAA64),
    onPrimary = Color(0xFF00390A),
    primaryContainer = Color(0xFF2A5D2F),
    onPrimaryContainer = Color(0xFFD4EABE),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF452A00),
    secondaryContainer = Color(0xFF8F4F00), // Adjusted for better contrast
    onSecondaryContainer = Color(0xFFFFDDBE),
    tertiary = Color(0xFFBCAAA4),
    onTertiary = Color(0xFF3E2723),
    tertiaryContainer = Color(0xFF6D4C41), // Adjusted
    onTertiaryContainer = Color(0xFFF4E9E6),
    error = Color(0xFFCF6679), // Standard Error Colors
    onError = Color(0xFF000000),
    errorContainer = Color(0xFFB1384E),
    onErrorContainer = Color(0xFFFBE8EC),
    background = Color(0xFF1A1C19),
    onBackground = Color(0xFFE2E3DD),
    surface = Color(0xFF1A1C19), // Can be slightly different if desired, e.g., Color(0xFF232622)
    onSurface = Color(0xFFE2E3DD),
    surfaceVariant = Color(0xFF424940),
    onSurfaceVariant = Color(0xFFC2C9BE),
    outline = Color(0xFF8B9389)
)

@Composable
fun MountainBikeTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set to false to use Trailblazer theme by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkTrailblazerColorScheme
        else -> LightTrailblazerColorScheme
    }

    // You can also customize Typography and Shapes here if needed
    // val typography = AppTypography
    // val shapes = AppShapes

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming you have Typography.kt defined
        content = content
    )
}