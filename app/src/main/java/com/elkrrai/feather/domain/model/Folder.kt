package com.elkrrai.feather.domain.model

/** A one-level container for lists. */
data class Folder(
    val id: String,
    val name: String,
    val sortOrder: Long,
)
