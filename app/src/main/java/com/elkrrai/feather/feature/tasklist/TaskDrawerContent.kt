package com.elkrrai.feather.feature.tasklist

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.SavedFilter
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.view.TaskScope
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton
import com.elkrrai.feather.ui.format.displayName

/** What the drawer can ask the screen to do with lists, folders and tags. */
class DrawerActions(
    val onSelect: (TaskScope) -> Unit,
    val onNewList: () -> Unit,
    val onNewFolder: () -> Unit,
    val onEditList: (TaskList) -> Unit,
    val onDeleteList: (TaskList) -> Unit,
    val onRenameFolder: (Folder) -> Unit,
    val onDeleteFolder: (Folder) -> Unit,
    val onRenameTag: (Tag) -> Unit,
    val onDeleteTag: (Tag) -> Unit,
    val onOpenTrash: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onClose: () -> Unit = {},
    val onNewFilter: () -> Unit = {},
    val onEditFilter: (SavedFilter) -> Unit = {},
    val onDeleteFilter: (SavedFilter) -> Unit = {},
)

private val smartViews: List<Pair<Int, TaskScope>> = listOf(
    R.string.scope_today to TaskScope.Today,
    R.string.scope_upcoming to TaskScope.Upcoming,
    R.string.scope_overdue to TaskScope.Overdue,
    R.string.scope_all to TaskScope.All,
    R.string.scope_no_date to TaskScope.NoDate,
    R.string.scope_completed to TaskScope.Completed,
)

@Composable
fun TaskDrawerContent(
    state: TaskListUiState,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    val itemPadding = NavigationDrawerItemDefaults.ItemPadding

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

        SectionHeader(R.string.drawer_section_views)
        smartViews.forEach { (labelRes, scope) ->
            DrawerItem(
                label = stringResource(labelRes),
                selected = state.scope == scope,
                count = state.counts[scope],
                onClick = { actions.onSelect(scope) },
                modifier = Modifier.padding(itemPadding),
            )
        }

        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
        SectionHeader(R.string.drawer_section_filters)
        state.filters.forEach { filter ->
            val scope = TaskScope.Filtered(filter)
            DrawerItem(
                label = filter.name,
                selected = state.scope == scope,
                count = state.counts[scope],
                onClick = { actions.onSelect(scope) },
                menu = listOf(
                    MenuAction(stringResource(R.string.action_edit)) { actions.onEditFilter(filter) },
                    MenuAction(stringResource(R.string.action_delete)) { actions.onDeleteFilter(filter) },
                ),
                modifier = Modifier.padding(itemPadding),
            )
        }
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.new_filter)) },
            selected = false,
            onClick = actions.onNewFilter,
            modifier = Modifier.padding(itemPadding),
        )

        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
        SectionHeader(R.string.drawer_section_lists)

        // The Inbox and lists outside any folder come first, then each folder with its lists.
        val folderIds = state.folders.map { it.id }.toSet()
        val looseLists = state.lists.filter { it.folderId == null || it.folderId !in folderIds }
        looseLists.forEach { list ->
            ListItemRow(list, state, actions, Modifier.padding(itemPadding))
        }
        state.folders.forEach { folder ->
            FolderHeader(folder, actions)
            state.lists.filter { it.folderId == folder.id }.forEach { list ->
                ListItemRow(
                    list = list,
                    state = state,
                    actions = actions,
                    modifier = Modifier.padding(start = 28.dp, end = 12.dp),
                )
            }
        }
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.new_list)) },
            selected = false,
            onClick = actions.onNewList,
            modifier = Modifier.padding(itemPadding),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.new_folder)) },
            selected = false,
            onClick = actions.onNewFolder,
            modifier = Modifier.padding(itemPadding),
        )

        if (state.tags.isNotEmpty()) {
            HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
            SectionHeader(R.string.drawer_section_tags)
            state.tags.forEach { tag ->
                val scope = TaskScope.WithTag(tag.id)
                DrawerItem(
                    label = "#${tag.name}",
                    selected = state.scope == scope,
                    count = state.counts[scope],
                    onClick = { actions.onSelect(scope) },
                    menu = listOf(
                        MenuAction(stringResource(R.string.action_rename)) { actions.onRenameTag(tag) },
                        MenuAction(stringResource(R.string.action_delete)) { actions.onDeleteTag(tag) },
                    ),
                    modifier = Modifier.padding(itemPadding),
                )
            }
        }

        HorizontalDivider(Modifier.padding(horizontal = 28.dp, vertical = 8.dp))
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
    }
}

@Composable
private fun SectionHeader(@StringRes textRes: Int) {
    // Section titles are set apart from the items under them: the display face, in the primary color.
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 4.dp).semantics { heading() },
    )
}

@Composable
private fun FolderHeader(folder: Folder, actions: DrawerActions) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OverflowButton(
            actions = listOf(
                MenuAction(stringResource(R.string.action_rename)) { actions.onRenameFolder(folder) },
                MenuAction(stringResource(R.string.action_delete)) { actions.onDeleteFolder(folder) },
            ),
        )
    }
}

@Composable
private fun ListItemRow(
    list: TaskList,
    state: TaskListUiState,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    val scope = TaskScope.InList(list.id)
    DrawerItem(
        label = list.displayName(),
        selected = state.scope == scope,
        count = state.counts[scope],
        onClick = { actions.onSelect(scope) },
        colorArgb = list.colorArgb,
        showColorDot = !list.isInbox,
        // The Inbox can be neither edited nor deleted.
        menu = if (list.isInbox) {
            emptyList()
        } else {
            listOf(
                MenuAction(stringResource(R.string.action_edit)) { actions.onEditList(list) },
                MenuAction(stringResource(R.string.action_delete)) { actions.onDeleteList(list) },
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun DrawerItem(
    label: String,
    selected: Boolean,
    count: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorArgb: Int? = null,
    showColorDot: Boolean = false,
    menu: List<MenuAction> = emptyList(),
) {
    NavigationDrawerItem(
        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.bodyLarge) },
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        icon = if (showColorDot) {
            { ColorDot(colorArgb) }
        } else {
            null
        },
        badge = if ((count != null && count > 0) || menu.isNotEmpty()) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (count != null && count > 0) Text(count.toString())
                    if (menu.isNotEmpty()) OverflowButton(menu)
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun ColorDot(colorArgb: Int?) {
    val color = colorArgb?.let { Color(it) }
        ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        Modifier
            .size(14.dp)
            .background(color, CircleShape)
    )
}
