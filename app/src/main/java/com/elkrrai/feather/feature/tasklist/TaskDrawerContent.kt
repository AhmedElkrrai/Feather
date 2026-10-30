package com.elkrrai.feather.feature.tasklist

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.ui.DisplayPrefs
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription

/** What the drawer can ask the screen to do. */
class DrawerActions(
    val onSelect: (TaskScope) -> Unit,
    val onOpenTrash: () -> Unit,
    val onOpenRhythm: () -> Unit = {},
    val onOpenHabits: () -> Unit = {},
    val onOpenNest: () -> Unit = {},
    val onOpenSettings: () -> Unit,
    val onOpenTips: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/** Each view of the tasks: its name, the line under it in the menu, and the view itself. */
private class DrawerView(@StringRes val label: Int, @StringRes val hint: Int, val scope: TaskScope)

private val smartViews: List<DrawerView> = listOf(
    DrawerView(R.string.scope_today, R.string.drawer_hint_today, TaskScope.Today),
    DrawerView(R.string.scope_upcoming, R.string.drawer_hint_upcoming, TaskScope.Upcoming),
    DrawerView(R.string.scope_overdue, R.string.drawer_hint_overdue, TaskScope.Overdue),
    DrawerView(R.string.scope_no_date, R.string.drawer_hint_goals, TaskScope.NoDate),
    DrawerView(R.string.scope_completed, R.string.drawer_hint_completed, TaskScope.Completed),
)

@Composable
fun TaskDrawerContent(
    state: TaskListUiState,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
    /** A quiet dot on Your nest: something new has come to it. */
    nestDot: Boolean = false,
    /** Whether tips are possible (the amounts loaded from Google Play); the item is hidden when not. */
    tipsVisible: Boolean = false,
    /** False on a screen that is not one of the task views: no view is marked as the current one. */
    highlightScope: Boolean = true,
    /** True on the Habits screen. */
    habitsSelected: Boolean = false,
) {
    val itemPadding = NavigationDrawerItemDefaults.ItemPadding
    val nestNewDescription = stringResource(R.string.nest_new_dot)

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 28.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            // A round, quiet close button: the drawer can be dismissed without reaching for the scrim.
            FilledTonalIconButton(
                onClick = actions.onClose,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.action_close_menu),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        smartViews.forEach { view ->
            DrawerItem(
                label = stringResource(view.label),
                hint = stringResource(view.hint),
                selected = highlightScope && state.scope == view.scope,
                count = state.counts[view.scope],
                onClick = { actions.onSelect(view.scope) },
                modifier = Modifier.padding(itemPadding),
            )
        }

        // Habits are a screen of their own, not a view of the tasks.
        NavigationDrawerItem(
            label = { DrawerLabel(stringResource(R.string.habits_title), stringResource(R.string.drawer_hint_habits)) },
            selected = habitsSelected,
            onClick = actions.onOpenHabits,
            modifier = Modifier.padding(itemPadding),
        )

        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
        NavigationDrawerItem(
            label = { DrawerLabel(stringResource(R.string.rhythm_title), stringResource(R.string.drawer_hint_progress)) },
            selected = false,
            onClick = actions.onOpenRhythm,
            modifier = Modifier.padding(itemPadding),
        )
        // A quiet dot says there is something new in the nest; it goes when the nest has been looked at.
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.nest_title)) },
            selected = false,
            onClick = actions.onOpenNest,
            badge = if (nestDot) {
                {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .semantics { contentDescription = nestNewDescription },
                    )
                }
            } else {
                null
            },
            modifier = Modifier.padding(itemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.trash)) },
            selected = false,
            onClick = actions.onOpenTrash,
            modifier = Modifier.padding(itemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.settings)) },
            selected = false,
            onClick = actions.onOpenSettings,
            modifier = Modifier.padding(itemPadding),
        )
        if (tipsVisible) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings_support)) },
                selected = false,
                onClick = actions.onOpenTips,
                modifier = Modifier.padding(itemPadding),
            )
        }
    }
}

/** A menu line: the name, and in smaller grey letters what is in it. */
@Composable
private fun DrawerLabel(label: String, hint: String) {
    Column {
        Text(label, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
        if (DisplayPrefs.menuHints) {
            // dimmer than the name, and the same whether or not the item is the selected one
            Text(
                text = hint,
                maxLines = 1,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    hint: String,
    selected: Boolean,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        label = { DrawerLabel(label, hint) },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        badge = if (count != null && count > 0) {
            { Text(count.toString()) }
        } else {
            null
        },
    )
}
