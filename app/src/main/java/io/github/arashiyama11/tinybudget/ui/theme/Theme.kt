package io.github.arashiyama11.tinybudget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4E535E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2B2F36),
    onPrimaryContainer = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFBFC4CB),

    secondary = Color(0xFF7F8896),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2A2F37),
    onSecondaryContainer = Color(0xFFFFFFFF),

    tertiary = Color(0xFFAFBAC1),
    onTertiary = Color(0xFF202528),
    tertiaryContainer = Color(0xFFE1E5E8),
    onTertiaryContainer = Color(0xFF000000),

    background = Color(0xFF353742),
    onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF353742),
    onSurface = Color(0xFFECEFF4),

    surfaceVariant = Color(0xFF4E535E),
    onSurfaceVariant = Color(0xFFFFFFFF),
    surfaceTint = Color(0xFF4E535E),

    inverseSurface = Color(0xFFECEFF4),
    inverseOnSurface = Color(0xFF353742),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    outline = Color(0xFF7F8896),
    outlineVariant = Color(0xFFAFBAC1),
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFFECEFF4),
    surfaceDim = Color(0xFF2A2C30),

    surfaceContainer = Color(0xFF3F4146),
    surfaceContainerHigh = Color(0xFF4A4D52),
    surfaceContainerHighest = Color(0xFF55585E),
    surfaceContainerLow = Color(0xFF25262A),
    surfaceContainerLowest = Color(0xFF1A1B1D),
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun TinyBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}