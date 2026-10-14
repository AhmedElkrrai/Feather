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

private val DarkColorScheme = darkColorScheme(
    primary = Sky80,
    secondary = SkyGrey80,
    tertiary = Sage80
)

private val LightColorScheme = lightColorScheme(
    primary = Sky40,
    secondary = SkyGrey40,
    tertiary = Sage40
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
