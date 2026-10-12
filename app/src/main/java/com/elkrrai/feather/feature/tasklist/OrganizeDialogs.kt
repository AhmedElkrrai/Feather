package com.elkrrai.feather.feature.tasklist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.elkrrai.feather.R
import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.Tag
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.ui.theme.ListColorPalette

/** The dialogs the drawer can open. */
sealed interface OrganizeDialog {
    data object NewList : OrganizeDialog
    data class EditList(val list: TaskList) : OrganizeDialog
    data class DeleteList(val list: TaskList) : OrganizeDialog
    data object NewFolder : OrganizeDialog
    data class RenameFolder(val folder: Folder) : OrganizeDialog
    data class DeleteFolder(val folder: Folder) : OrganizeDialog
    data class RenameTag(val tag: Tag) : OrganizeDialog
    data class DeleteTag(val tag: Tag) : OrganizeDialog
}

/** What the dialogs ask the ViewModel to do once confirmed. */
class OrganizeActions(
    val createList: (name: String, folderId: String?, colorArgb: Int?) -> Unit,
    val updateList: (list: TaskList, name: String, colorArgb: Int?, folderId: String?) -> Unit,
    val deleteList: (TaskList) -> Unit,
    val createFolder: (String) -> Unit,
    val renameFolder: (Folder, String) -> Unit,
    val deleteFolder: (Folder) -> Unit,
    val renameTag: (Tag, String) -> Unit,
    val deleteTag: (Tag) -> Unit,
)

/** Shows the dialog for [dialog], if any, and closes it through [onDismiss] after any action. */
@Composable
fun OrganizeDialogHost(
    dialog: OrganizeDialog?,
    folders: List<Folder>,
    actions: OrganizeActions,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        null -> Unit

        OrganizeDialog.NewList -> ListDialog(
            initial = null,
            folders = folders,
            onConfirm = { name, color, folderId ->
                actions.createList(name, folderId, color)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is OrganizeDialog.EditList -> ListDialog(
            initial = dialog.list,
            folders = folders,
            onConfirm = { name, color, folderId ->
                actions.updateList(dialog.list, name, color, folderId)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is OrganizeDialog.DeleteList -> ConfirmDialog(
            title = stringResource(R.string.delete_list_title),
            message = stringResource(R.string.delete_list_message, dialog.list.name),
            onConfirm = {
                actions.deleteList(dialog.list)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        OrganizeDialog.NewFolder -> NameDialog(
            title = stringResource(R.string.new_folder),
            label = stringResource(R.string.folder_name),
            initial = "",
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = {
                actions.createFolder(it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is OrganizeDialog.RenameFolder -> NameDialog(
            title = stringResource(R.string.rename_folder),
            label = stringResource(R.string.folder_name),
            initial = dialog.folder.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                actions.renameFolder(dialog.folder, it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is OrganizeDialog.DeleteFolder -> ConfirmDialog(
            title = stringResource(R.string.delete_folder_title),
            message = stringResource(R.string.delete_folder_message, dialog.folder.name),
            onConfirm = {
                actions.deleteFolder(dialog.folder)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is OrganizeDialog.RenameTag -> NameDialog(
            title = stringResource(R.string.rename_tag),
            label = stringResource(R.string.tag_name),
            initial = dialog.tag.name,
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                actions.renameTag(dialog.tag, it)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        is OrganizeDialog.DeleteTag -> ConfirmDialog(
            title = stringResource(R.string.delete_tag_title),
            message = stringResource(R.string.delete_tag_message, dialog.tag.name),
            onConfirm = {
                actions.deleteTag(dialog.tag)
                onDismiss()
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Create or edit a list: name, color and (when folders exist) folder. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListDialog(
    initial: TaskList?,
    folders: List<Folder>,
    onConfirm: (name: String, colorArgb: Int?, folderId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var color by rememberSaveable { mutableStateOf(initial?.colorArgb) }
    var folderId by rememberSaveable { mutableStateOf(initial?.folderId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.new_list else R.string.edit_list))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )

                Text(
                    text = stringResource(R.string.list_color),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ColorChoice(argb = null, selected = color == null) { color = null }
                    ListColorPalette.forEach { argb ->
                        ColorChoice(argb = argb, selected = color == argb) { color = argb }
                    }
                }

                if (folders.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.list_folder),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Column(Modifier.selectableGroup()) {
                        FolderChoice(
                            label = stringResource(R.string.no_folder),
                            selected = folderId == null,
                            onClick = { folderId = null },
                        )
                        folders.forEach { folder ->
                            FolderChoice(
                                label = folder.name,
                                selected = folderId == folder.id,
                                onClick = { folderId = folder.id },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name, color, folderId) }, enabled = name.isNotBlank()) {
                Text(
                    stringResource(if (initial == null) R.string.action_create else R.string.action_save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** A color swatch; a null [argb] is the "no color" choice, drawn as an empty ring. */
@Composable
private fun ColorChoice(argb: Int?, selected: Boolean, onClick: () -> Unit) {
    val description = if (argb == null) stringResource(R.string.color_none) else ""
    val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(argb?.let { Color(it) } ?: Color.Transparent)
            .border(width = if (selected) 3.dp else 1.dp, color = ring, shape = CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { if (description.isNotEmpty()) contentDescription = description },
    ) {}
}

@Composable
private fun FolderChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(16.dp))
        Text(label)
    }
}
