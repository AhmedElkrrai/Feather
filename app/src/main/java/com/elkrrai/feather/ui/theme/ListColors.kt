package com.elkrrai.feather.ui.theme

/**
 * The eight list colors offered (ARGB), the light-theme values of `FeatherColors.listColors` in the
 * same order. Each color has its own shape too ([ListShape]), so color is never the only signal. A
 * list with no color shows a neutral dot. What is drawn comes from the theme (see [listColorIndex]),
 * so the same list is lighter in the dark theme.
 */
val ListColorPalette: List<Int> = listOf(
    0xFF2A7F9E.toInt(),
    0xFFB5700A.toInt(),
    0xFFD9604A.toInt(),
    0xFF5E7F3E.toInt(),
    0xFF9A4F73.toInt(),
    0xFFA6613B.toInt(),
    0xFF5A6EA3.toInt(),
    0xFF17485E.toInt(),
)

/** The colors saved by earlier versions, and the Nest color each one becomes (by position in [ListColorPalette]). */
private val LegacyListColors: Map<Int, Int> = mapOf(
    0xFFE57373.toInt() to 2, // red -> coral
    0xFFFFB74D.toInt() to 1, // orange -> amber
    0xFFFFD54F.toInt() to 1, // yellow -> amber
    0xFF81C784.toInt() to 3, // green -> moss
    0xFF4DB6AC.toInt() to 0, // teal -> plumage
    0xFF64B5F6.toInt() to 6, // blue -> slate blue
    0xFFBA68C8.toInt() to 4, // purple -> plum
    0xFFF06292.toInt() to 2, // pink -> coral
)

/**
 * Which of the eight Nest colors a list's saved color stands for, or null for no color. A color
 * saved by an earlier version keeps its meaning, so no list loses its color.
 */
fun listColorIndex(argb: Int?): Int? {
    if (argb == null) return null
    val current = ListColorPalette.indexOf(argb)
    return if (current >= 0) current else LegacyListColors[argb]
}

/** The shapes that go with the list colors, by position. */
enum class ListShape { Circle, Sun, Drop, Leaf, Diamond, Square, Triangle, Moon }

fun listShapeFor(colorIndex: Int?): ListShape =
    if (colorIndex == null) ListShape.Circle else ListShape.entries[colorIndex.mod(ListShape.entries.size)]
