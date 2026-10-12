package com.elkrrai.feather.domain.repository

import com.elkrrai.feather.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun observeLists(): Flow<List<TaskList>>

    /** Creates the Inbox if it does not exist yet. Safe to call any number of times. */
    suspend fun ensureInbox()
}
