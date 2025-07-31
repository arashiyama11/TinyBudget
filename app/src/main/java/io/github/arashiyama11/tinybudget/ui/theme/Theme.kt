package io.github.arashiyama11.tinybudget.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview

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
    // Primary
    primary = Color(0xFF7790ED),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E0FE),
    onPrimaryContainer = Color(0xFF00154C),
    inversePrimary = Color(0xFF4E535E),

    // Secondary
    secondary = Color(0xFF5B6CED),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E4FF),
    onSecondaryContainer = Color(0xFF00124F),

    // Tertiary (Text/Icon)
    tertiary = Color(0xFF262E37),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFDCE1E6),
    onTertiaryContainer = Color(0xFF0E1318),

    // Background & Surface をニュートラルなほぼ白に
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF262E37),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF262E37),

    // Surface variants（コンテナやカード背景など）
    surfaceVariant = Color(0xFFF0F0F2),
    onSurfaceVariant = Color(0xFF494C50),
    surfaceTint = Color(0xFF7790ED),

    // Inverse surfaces
    inverseSurface = Color(0xFF353742),
    inverseOnSurface = Color(0xFFECEFF4),

    // Error（そのまま）
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    // その他
    outline = Color(0xFF7F8896),
    scrim = Color(0xFF000000),

    // カスタム拡張
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEEEEEE),

    surfaceContainer = Color(0xFFF7F7F7),
    surfaceContainerHigh = Color(0xFFF9F9F9),
    surfaceContainerHighest = Color(0xFFFBFBFB),
    surfaceContainerLow = Color(0xFFEFEFEF),
    surfaceContainerLowest = Color(0xFFECECEC),
)
private val FocusBlue = Color(0xFF7790ED)


val AppTextFieldColors: TextFieldColors
    @Composable
    get() = TextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        cursorColor = FocusBlue,
        focusedIndicatorColor = FocusBlue,
        focusedLabelColor = FocusBlue,
        focusedLeadingIconColor = MaterialTheme.colorScheme.onSurface,
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
        content = content
    )
}

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = TextStyle.Default,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        colors = AppTextFieldColors,
        textStyle = textStyle,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = TextStyle.Default,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        colors = AppTextFieldColors,
        textStyle = textStyle,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun PreviewOf(content: @Composable () -> Unit) {
    TinyBudgetTheme {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            content()
        }
    }
}

@Preview
@Composable
fun PreviewTinyBudgetTheme() {
    PreviewOf {
        AppTextField(
            value = "プレビュー",
            onValueChange = {},
            label = { Text("AppTextFieldラベル") },
            modifier = Modifier.fillMaxWidth()
        )

        AppOutlinedTextField(
            value = "プレビュー",
            onValueChange = {},
            label = { Text("AppOutlinedTextFieldラベル") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}