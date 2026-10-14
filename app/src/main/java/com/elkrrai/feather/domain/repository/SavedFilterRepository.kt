package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.model.DueWindow
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.SavedFilter
import kotlinx.coroutines.flow.Flow

interface SavedFilterRepository {
    /** Non-deleted filters in the order they were made. */
    fun observeFilters(): Flow<List<SavedFilter>>

    /** The name must not be blank. */
    suspend fun createFilter(
        name: String,
        listId: String?,
        tagId: String?,
        minPriority: Priority,
        due: DueWindow,
        hideCompleted: Boolean,
    ): SavedFilter

    /** Changes the name and conditions of [filter] (found by its id). */
    suspend fun updateFilter(filter: SavedFilter)

    suspend fun deleteFilter(id: String)
}
