package com.elkrrai.feather.testutil

import com.elkrrai.feather.domain.model.DueWindow
import com.elkrrai.feather.domain.model.Priority
import com.elkrrai.feather.domain.model.SavedFilter
import com.elkrrai.feather.domain.repository.SavedFilterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** In-memory [SavedFilterRepository]. */
class FakeSavedFilterRepository : SavedFilterRepository {

    private val filters = MutableStateFlow<List<SavedFilter>>(emptyList())
    private var counter = 0L

    override fun observeFilters(): Flow<List<SavedFilter>> = filters

    override suspend fun createFilter(
        name: String,
        listId: String?,
        tagId: String?,
        minPriority: Priority,
        due: DueWindow,
        hideCompleted: Boolean,
    ): SavedFilter {
        require(name.isNotBlank())
        counter++
        val filter = SavedFilter("filter-$counter", name.trim(), listId, tagId, minPriority, due, hideCompleted, counter)
        filters.update { it + filter }
        return filter
    }

    override suspend fun updateFilter(filter: SavedFilter) {
        filters.update { all -> all.map { if (it.id == filter.id) filter.copy(name = filter.name.trim()) else it } }
    }

    override suspend fun deleteFilter(id: String) {
        filters.update { all -> all.filterNot { it.id == id } }
    }
}
