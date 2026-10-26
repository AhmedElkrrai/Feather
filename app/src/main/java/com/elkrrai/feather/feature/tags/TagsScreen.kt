package com.elkrrai.feather.feature.tags

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TagRow
import com.elkrrai.feather.domain.model.possibleParentsOf
import com.elkrrai.feather.feature.tasklist.ConfirmDialog
import com.elkrrai.feather.feature.tasklist.NameDialog
import com.elkrrai.feather.ui.brand.scenes.SceneKind
import com.elkrrai.feather.ui.components.EmptyState
import com.elkrrai.feather.ui.components.MenuAction
import com.elkrrai.feather.ui.components.OverflowButton

/** What is being done to a tag, which decides the dialog on screen. */
private sealed interface TagDialog {
    data object New : TagDialog
    data class Rename(val tag: Tag) : TagDialog
    data class MoveUnder(val tag: Tag) : TagDialog
    data class Delete(val tag: Tag) : TagDialog
}

@Composable
fun TagsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val renameFailed = stringResource(R.string.tag_rename_failed)
    TagsContent(
        state = state,
        onBack = onBack,
        onCreate = viewModel::create,
        onRename = { id, name, done ->
            viewModel.rename(id, name) { ok ->
                if (!ok) Toast.makeText(context, renameFailed, Toast.LENGTH_SHORT).show()
                done(ok)
            }
        },
        onSetParent = viewModel::setParent,
        onDelete = viewModel::delete,
        modifier = modifier,
    )
}

/** Stateless Tags screen, so it can be previewed and tested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsContent(
    state: TagsUiState,
    onBack: () -> Unit,
    onCreate: (name: String, parentId: String?) -> Unit,
    onRename: (tagId: String, name: String, done: (Boolean) -> Unit) -> Unit,
    onSetParent: (tagId: String, parentId: String?) -> Unit,
    onDelete: (tagId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<TagDialog?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tags_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = TagDialog.New }) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = stringResource(R.string.new_tag),
                )
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(padding))

            state.rows.isEmpty() -> EmptyState(
                scene = SceneKind.EmptyList,
                title = stringResource(R.string.tags_empty_title),
                body = stringResource(R.string.tags_empty_body),
                modifier = Modifier.padding(padding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                items(state.rows, key = { it.tag.id }) { row ->
                    val tag = row.tag
                    val count = state.taskCounts[tag.id] ?: 0
                    val parentName = state.nameOf(tag.parentId)
                    val moveLabel = stringResource(R.string.tag_move_under)
                    ListItem(
                        // Each level sits a little further in, so the tree can be read at a glance. A long press
                        // moves the tag, the same as "Put under another tag" in its menu.
                        modifier = Modifier
                            .padding(start = 20.dp * row.depth)
                            .pointerInput(tag.id) {
                                detectTapGestures(onLongPress = { dialog = TagDialog.MoveUnder(tag) })
                            }
                            .semantics { onLongClick(label = moveLabel) { dialog = TagDialog.MoveUnder(tag); true } },
                        headlineContent = { Text(tag.name) },
                        supportingContent = {
                            Column {
                                if (parentName != null) {
                                    Text(
                                        text = stringResource(R.string.tag_extends, parentName),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = pluralStringResource(R.plurals.tag_task_count, count, count),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        trailingContent = {
                            OverflowButton(
                                actions = listOf(
                                    MenuAction(stringResource(R.string.action_rename)) { dialog = TagDialog.Rename(tag) },
                                    MenuAction(stringResource(R.string.tag_move_under)) { dialog = TagDialog.MoveUnder(tag) },
                                    MenuAction(stringResource(R.string.action_delete)) { dialog = TagDialog.Delete(tag) },
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    when (val open = dialog) {
        null -> Unit

        TagDialog.New -> NewTagDialog(
            rows = state.rows,
            onCreate = { name, parentId ->
                onCreate(name, parentId)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is TagDialog.Rename -> NameDialog(
            title = stringResource(R.string.rename_tag),
            label = stringResource(R.string.tag_name),
            initial = open.tag.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { name -> onRename(open.tag.id, name) { ok -> if (ok) dialog = null } },
            onDismiss = { dialog = null },
        )

        is TagDialog.MoveUnder -> ParentDialog(
            tag = open.tag,
            options = state.tags.possibleParentsOf(open.tag.id),
            onPick = { parentId ->
                onSetParent(open.tag.id, parentId)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is TagDialog.Delete -> {
            val hasChildren = state.tags.any { it.parentId == open.tag.id }
            ConfirmDialog(
                title = stringResource(R.string.delete_tag_title),
                message = stringResource(R.string.delete_tag_message, open.tag.name) +
                    if (hasChildren) " " + stringResource(R.string.delete_tag_children_note) else "",
                onConfirm = {
                    onDelete(open.tag.id)
                    dialog = null
                },
                onDismiss = { dialog = null },
            )
        }
    }
}

/**
 * Which tag [tag] goes under. The tag itself and every tag below it are not offered, which is what
 * keeps tags from ever looping: with life, work under life and bugfix under work, "life" can never
 * be put under "bugfix".
 */
@Composable
internal fun ParentDialog(
    tag: Tag,
    options: List<Tag>,
    onPick: (parentId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tag_parent_title, tag.name)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.tag_parent_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(Modifier.selectableGroup()) {
                    ParentChoice(
                        label = stringResource(R.string.tag_parent_none),
                        selected = tag.parentId == null,
                        onClick = { onPick(null) },
                    )
                    options.forEach { option ->
                        ParentChoice(
                            label = option.name,
                            selected = option.id == tag.parentId,
                            onClick = { onPick(option.id) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * A new tag: its name, and under which tag it goes (nowhere, by default). Writing a path in the name,
 * like "Work/Bugfix", makes the tags it names, each under the one before.
 */
@Composable
private fun NewTagDialog(
    rows: List<TagRow>,
    onCreate: (name: String, parentId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var parentId by rememberSaveable { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_tag)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                Text(
                    text = stringResource(R.string.tag_new_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (rows.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.tag_new_under),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Column(Modifier.selectableGroup()) {
                        ParentChoice(
                            label = stringResource(R.string.tag_parent_none),
                            selected = parentId == null,
                            onClick = { parentId = null },
                        )
                        rows.forEach { row ->
                            ParentChoice(
                                label = row.tag.name,
                                selected = parentId == row.tag.id,
                                onClick = { parentId = row.tag.id },
                                indent = 16.dp * row.depth,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, parentId) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ParentChoice(label: String, selected: Boolean, onClick: () -> Unit, indent: Dp = 0.dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(start = indent, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Box(Modifier.width(16.dp))
        Text(label)
    }
}
