package com.elkrrai.feather.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.elkrrai.feather.ui.DisplayPrefs

/** Pure black for Material You colors, which have no black set of their own. */
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
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamic -> {
            val context = LocalContext.current
            val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && pureBlack) scheme.toPureBlack() else scheme
        }

        darkTheme -> if (pureBlack) NestBlackScheme else NestDarkScheme
        else -> NestLightScheme
    }
    val featherColors = when {
        !darkTheme -> LightFeatherColors
        pureBlack -> BlackFeatherColors
        else -> DarkFeatherColors
    }
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val typography = remember(arabic) { featherTypography(arabic) }

    CompositionLocalProvider(LocalFeatherColors provides featherColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = NestShapes,
            content = content,
        )
    }
}
