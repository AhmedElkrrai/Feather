package com.elkrrai.feather.domain.model

/** A tag's name with a capital first letter ("work" is "Work"); scripts without capitals are left alone. */
fun String.asTagName(): String =
    trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

/**
 * A tag written as a path, "work/bugfix", shown as "Work / Bugfix": each name is capitalised and the
 * names under one another are separated by a slash.
 */
fun String.asTagPath(): String =
    split('/').map { it.trim().removePrefix("#").trim() }.filter { it.isNotEmpty() }.joinToString(" / ") { it.asTagName() }
