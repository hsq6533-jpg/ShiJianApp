package com.shijian.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BrandDarkContainer = androidx.compose.ui.graphics.Color(0xFF0A2845)

private val LightColors = lightColorScheme(
    primary = Brand500,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand600,
    secondary = Teal500,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = Purple500,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = Danger500,
    onError = androidx.compose.ui.graphics.Color.White,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = CardLight,
    onSurface = TextPrimary,
    surfaceVariant = FillLight,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor
)

private val DarkColors = darkColorScheme(
    primary = Brand500,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = BrandDarkContainer,
    onPrimaryContainer = Brand100,
    secondary = Teal500,
    onSecondary = androidx.compose.ui.graphics.Color.Black,
    tertiary = Purple500,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    error = Danger500,
    onError = androidx.compose.ui.graphics.Color.White,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = FillDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = DividerColorDark,
    outlineVariant = DividerColorDark
)

@Composable
fun ShiJianTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
