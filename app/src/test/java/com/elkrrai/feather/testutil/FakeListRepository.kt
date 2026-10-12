package com.elkrrai.feather.testutil

import com.elkrrai.feather.domain.model.Folder
import com.elkrrai.feather.domain.model.TaskList
import com.elkrrai.feather.domain.repository.ListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [ListRepository]. The Inbox exists from the start. */
class FakeListRepository : ListRepository {

    private val lists = MutableStateFlow(
        listOf(TaskList(id = TaskList.INBOX_ID, name = "Inbox", isInbox = true, sortOrder = 0))
    )
    private val folders = MutableStateFlow<List<Folder>>(emptyList())
    private var counter = 0L

    override fun observeLists(): Flow<List<TaskList>> = lists

    override fun observeFolders(): Flow<List<Folder>> = folders

    override suspend fun ensureInbox() = Unit

    override suspend fun createList(name: String, folderId: String?, colorArgb: Int?): TaskList {
        require(name.isNotBlank())
        counter++
        val list = TaskList("list-$counter", name.trim(), false, counter, folderId, colorArgb)
        lists.update { it + list }
        return list
    }

    override suspend fun updateList(id: String, name: String, colorArgb: Int?, folderId: String?) {
        lists.update { all ->
            all.map {
                if (it.id != id) it
                else it.copy(
                    name = if (it.isInbox) it.name else name.trim().ifBlank { it.name },
                    colorArgb = colorArgb,
                    folderId = if (it.isInbox) null else folderId,
                )
            }
        }
    }

    override suspend fun deleteList(id: String) {
        if (id == TaskList.INBOX_ID) return
        lists.update { all -> all.filterNot { it.id == id } }
    }

    override suspend fun createFolder(name: String): Folder {
        require(name.isNotBlank())
        counter++
        val folder = Folder("folder-$counter", name.trim(), counter)
        folders.update { it + folder }
        return folder
    }

    override suspend fun renameFolder(id: String, name: String) {
        folders.update { all -> all.map { if (it.id == id) it.copy(name = name.trim()) else it } }
    }

    override suspend fun deleteFolder(id: String) {
        lists.update { all -> all.map { if (it.folderId == id) it.copy(folderId = null) else it } }
        folders.update { all -> all.filterNot { it.id == id } }
    }
}
