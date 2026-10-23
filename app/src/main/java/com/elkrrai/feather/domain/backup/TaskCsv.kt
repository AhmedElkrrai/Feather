package com.elkrrai.feather.domain.backup

import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.Task
import com.elkrrai.feather.domain.recurrence.RepeatMode

/**
 * Tasks as CSV text for a spreadsheet: one row per top-level task. The columns are English and
 * fixed (this is data, not display), the text is UTF-8 with a byte order mark so spreadsheets
 * show Arabic correctly, and cells are quoted as RFC 4180 asks.
 */
object TaskCsv {

    private const val BYTE_ORDER_MARK = "﻿"
    private const val LINE_BREAK = "\r\n"

    val HEADER: List<String> = listOf(
        "Title", "Notes", "Due date", "Due time", "Priority", "Status",
        "Completed at", "Tags", "Repeat", "Subtasks",
    )

    fun build(tasks: List<Task>): String {
        val rows = tasks.map { task ->
            listOf(
                task.title,
                task.notes,
                task.dueDate?.toString().orEmpty(),
                task.dueTime?.toString().orEmpty(),
                priorityName(task.priority),
                if (task.isCompleted) "done" else "open",
                task.completedAt?.toString().orEmpty(),
                task.tags.joinToString("; ") { it.name },
                repeatText(task),
                task.subtasks.joinToString(" | ") { (if (it.isCompleted) "[x] " else "[ ] ") + it.title },
            )
        }
        return BYTE_ORDER_MARK + (listOf(HEADER) + rows).joinToString(LINE_BREAK, postfix = LINE_BREAK) { row ->
            row.joinToString(",") { escape(it) }
        }
    }

    private fun priorityName(priority: Priority): String = when (priority) {
        Priority.NONE -> "none"
        Priority.LOW -> "low"
        Priority.MEDIUM -> "medium"
        Priority.HIGH -> "high"
    }

    private fun repeatText(task: Task): String {
        val rule = task.recurrence ?: return ""
        return if (rule.mode == RepeatMode.AFTER_COMPLETION) rule.toRule() + " (after completion)" else rule.toRule()
    }

    private fun escape(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
}
