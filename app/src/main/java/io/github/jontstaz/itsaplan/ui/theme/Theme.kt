package io.github.jontstaz.itsaplan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Indigo400,
    onPrimary = Indigo900,
    primaryContainer = Color(0xFF282560),
    onPrimaryContainer = Indigo100,
    secondary = Emerald500,
    onSecondary = Emerald900,
    secondaryContainer = Color(0xFF0E3A2F),
    onSecondaryContainer = Emerald100,
    tertiary = Amber500,
    onTertiary = Amber900,
    tertiaryContainer = Color(0xFF3E2909),
    onTertiaryContainer = Amber100,
    error = Coral500,
    onError = Color.White,
    errorContainer = Color(0xFF471519),
    onErrorContainer = Coral100,
    background = Slate950,
    onBackground = Color(0xFFF1F5F9),
    surface = Slate900,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate400,
    surfaceContainerLowest = Slate950,
    surfaceContainerLow = Slate900,
    surfaceContainer = Slate850,
    surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate700,
    // Control boundaries need 3:1 against the surface behind them. Slate700 measured
    // 1.48:1 on Slate900; SlateOutline measures 3.34:1.
    outline = SlateOutline,
    outlineVariant = Color(0xFF222B40)
)

private val LightColorScheme = lightColorScheme(
    primary = Indigo600,
    onPrimary = Color.White,
    primaryContainer = Indigo100,
    onPrimaryContainer = Indigo900,
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald900,
    tertiary = Amber500,
    onTertiary = Color.White,
    tertiaryContainer = Amber100,
    onTertiaryContainer = Amber900,
    error = Coral500,
    onError = Color.White,
    errorContainer = Coral100,
    onErrorContainer = Coral900,
    background = AlabasterBg,
    onBackground = InkPrimary,
    surface = AlabasterSurface,
    onSurface = InkPrimary,
    surfaceVariant = AlabasterContainer,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFAFBFC),
    surfaceContainer = AlabasterContainer,
    surfaceContainerHigh = Color(0xFFE9EEF5),
    surfaceContainerHighest = AlabasterContainerHigh,
    // Was #CBD5E1 (1.48:1 on white). InkOutline measures 3.43:1 and still reads as a
    // hairline rather than a rule.
    outline = InkOutline,
    outlineVariant = Color(0xFFE2E8F0)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun ItsAPlanTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
