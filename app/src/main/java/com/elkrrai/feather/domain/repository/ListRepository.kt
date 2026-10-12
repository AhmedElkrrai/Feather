package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun observeLists(): Flow<List<TaskList>>

    fun observeFolders(): Flow<List<Folder>>

    /** Creates the Inbox if it does not exist yet. Safe to call any number of times. */
    suspend fun ensureInbox()

    /** The name must not be blank. */
    suspend fun createList(name: String, folderId: String? = null, colorArgb: Int? = null): TaskList

    /**
     * Updates a list. The Inbox keeps its name and cannot be put in a folder; a blank [name]
     * keeps the old one.
     */
    suspend fun updateList(id: String, name: String, colorArgb: Int?, folderId: String?)

    /** Moves the list's tasks to the Inbox, then deletes the list. The Inbox cannot be deleted. */
    suspend fun deleteList(id: String)

    /** The name must not be blank. */
    suspend fun createFolder(name: String): Folder

    suspend fun renameFolder(id: String, name: String)

    /** Takes the folder's lists out of it (they are kept), then deletes the folder. */
    suspend fun deleteFolder(id: String)
}
