package com.elkrrai.feather.domain.model

/** A checklist item of a task: one level only, with just a title and a done state. */
data class Subtask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
)
