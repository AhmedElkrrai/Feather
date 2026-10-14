package com.elkrrai.feather.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.elkrrai.feather.ui.DisplayPrefs

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = BrandOnPrimary,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = BrandOnSecondary,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = BrandOnTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    background = BrandBackground,
    onBackground = BrandOnBackground,
    surface = BrandBackground,
    onSurface = BrandOnBackground,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    surfaceContainerLowest = BrandContainerLowest,
    surfaceContainerLow = BrandContainerLow,
    surfaceContainer = BrandContainer,
    surfaceContainerHigh = BrandContainerHigh,
    surfaceContainerHighest = BrandContainerHighest,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = BrandOnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = BrandSecondaryDark,
    onSecondary = BrandOnSecondaryDark,
    secondaryContainer = BrandSecondaryContainerDark,
    onSecondaryContainer = BrandOnSecondaryContainerDark,
    tertiary = BrandTertiaryDark,
    onTertiary = BrandOnTertiaryDark,
    tertiaryContainer = BrandTertiaryContainerDark,
    onTertiaryContainer = BrandOnTertiaryContainerDark,
    background = BrandBackgroundDark,
    onBackground = BrandOnBackgroundDark,
    surface = BrandBackgroundDark,
    onSurface = BrandOnBackgroundDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark,
    surfaceContainerLowest = BrandContainerLowestDark,
    surfaceContainerLow = BrandContainerLowDark,
    surfaceContainer = BrandContainerDark,
    surfaceContainerHigh = BrandContainerHighDark,
    surfaceContainerHighest = BrandContainerHighestDark,
)

/** Pure black backgrounds for OLED screens; cards and sheets stay slightly lifted so they read as layers. */
private fun ColorScheme.toPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B),
    surfaceContainer = Color(0xFF111111),
)

/**
 * @param darkTheme follows the system by default. The app's own light/dark choice is applied
 * through `AppCompatDelegate.setDefaultNightMode`, so the system value already reflects it.
 * @param dynamicColor Material You colors, available on Android 12+; the default follows the
 * switch in Settings.
 * @param pureBlack true black backgrounds in dark mode; the default follows the switch in Settings.
 */
@Composable
fun FeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = DisplayPrefs.dynamicColor,
    pureBlack: Boolean = DisplayPrefs.pureBlack,
    content: @Composable () -> Unit
) {
    val base = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val colorScheme = if (darkTheme && pureBlack) base.toPureBlack() else base

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
