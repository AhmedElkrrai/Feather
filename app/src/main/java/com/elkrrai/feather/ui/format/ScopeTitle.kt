package com.elkrrai.feather.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.view.TaskScope

/** The Inbox is a system list, so its name comes from a localized resource. */
@Composable
fun TaskList.displayName(): String = if (isInbox) stringResource(R.string.inbox) else name

@Composable
fun TaskScope.title(lists: List<TaskList>, tags: List<Tag>): String = when (this) {
    TaskScope.Today -> stringResource(R.string.scope_today)
    TaskScope.Upcoming -> stringResource(R.string.scope_upcoming)
    TaskScope.Overdue -> stringResource(R.string.scope_overdue)
    TaskScope.All -> stringResource(R.string.scope_all)
    TaskScope.Completed -> stringResource(R.string.scope_completed)
    TaskScope.NoDate -> stringResource(R.string.scope_no_date)
    is TaskScope.InList -> lists.firstOrNull { it.id == listId }?.displayName()
        ?: stringResource(R.string.inbox)
    is TaskScope.WithTag -> tags.firstOrNull { it.id == tagId }?.let { "#${it.name}" }.orEmpty()
}
