package com.elkrrai.feather.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.elkrrai.feather.R

// Latin: Young Serif for the big, friendly lines and Nunito for everything else.
// Arabic: Noto Naskh for the big lines and Readex Pro for everything else.
// All four are SIL OFL (third_party/fonts). Nunito, Readex Pro and Noto Naskh are variable fonts,
// so one file serves every weight.

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val YoungSerif = FontFamily(Font(R.font.young_serif, FontWeight.Normal))

private val Nunito = FontFamily(
    variable(R.font.nunito, 400),
    variable(R.font.nunito, 700),
    variable(R.font.nunito, 800),
)

private val ReadexPro = FontFamily(
    variable(R.font.readex_pro, 400),
    variable(R.font.readex_pro, 500),
    variable(R.font.readex_pro, 600),
)

private val NotoNaskh = FontFamily(
    variable(R.font.noto_naskh_arabic, 400),
    variable(R.font.noto_naskh_arabic, 700),
)

private fun style(
    family: FontFamily,
    weight: Int,
    size: Int,
    line: Int,
    tracking: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = family,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
)

/**
 * The type scale for the language in use. Arabic gets taller lines (1.5 to 1.65 times the size),
 * no letter spacing and its own families.
 */
fun featherTypography(arabic: Boolean): Typography = if (arabic) {
    Typography(
        displayLarge = style(NotoNaskh, 700, 48, 72),
        displayMedium = style(NotoNaskh, 700, 40, 62),
        displaySmall = style(NotoNaskh, 700, 36, 56),
        headlineLarge = style(NotoNaskh, 700, 32, 50),
        headlineMedium = style(NotoNaskh, 700, 26, 39),
        headlineSmall = style(NotoNaskh, 700, 22, 34),
        titleLarge = style(ReadexPro, 600, 20, 30),
        titleMedium = style(ReadexPro, 500, 16, 26),
        titleSmall = style(ReadexPro, 500, 14, 22),
        bodyLarge = style(ReadexPro, 400, 16, 26),
        bodyMedium = style(ReadexPro, 400, 14, 23),
        bodySmall = style(ReadexPro, 400, 12, 19),
        labelLarge = style(ReadexPro, 500, 14, 22),
        labelMedium = style(ReadexPro, 500, 12, 18),
        labelSmall = style(ReadexPro, 500, 11, 18),
    )
} else {
    Typography(
        displayLarge = style(YoungSerif, 400, 48, 56),
        displayMedium = style(YoungSerif, 400, 40, 48),
        displaySmall = style(YoungSerif, 400, 36, 42),
        headlineLarge = style(YoungSerif, 400, 32, 40),
        headlineMedium = style(YoungSerif, 400, 26, 32),
        headlineSmall = style(YoungSerif, 400, 22, 28),
        titleLarge = style(Nunito, 800, 20, 26),
        titleMedium = style(Nunito, 700, 16, 22),
        titleSmall = style(Nunito, 700, 14, 20),
        bodyLarge = style(Nunito, 400, 16, 24),
        bodyMedium = style(Nunito, 400, 14, 20),
        bodySmall = style(Nunito, 400, 12, 16),
        labelLarge = style(Nunito, 800, 14, 20, 0.1.sp),
        labelMedium = style(Nunito, 800, 12, 16, 0.2.sp),
        labelSmall = style(Nunito, 800, 11, 16, 0.2.sp),
    )
}

/**
 * The large number on the progress screen: tabular, lining figures so digits do not jump as they
 * count up.
 */
fun statNumeralStyle(arabic: Boolean): TextStyle = if (arabic) {
    style(NotoNaskh, 700, 52, 52).copy(fontFeatureSettings = "tnum, lnum")
} else {
    style(YoungSerif, 400, 48, 48).copy(fontFeatureSettings = "tnum, lnum")
}
