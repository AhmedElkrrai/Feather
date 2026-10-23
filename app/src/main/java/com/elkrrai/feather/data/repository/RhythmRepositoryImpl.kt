package com.elkrrai.feather.data.repository

import com.elkrrai.feather.data.db.CompletionDao
import com.elkrrai.feather.domain.rhythm.CompletionRecord
import com.elkrrai.feather.domain.rhythm.RhythmRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RhythmRepositoryImpl @Inject constructor(
    private val completionDao: CompletionDao,
) : RhythmRepository {
    override fun observeCompletions(): Flow<List<CompletionRecord>> =
        completionDao.observeRows().map { rows ->
            rows.map { CompletionRecord(it.taskId, it.title, Instant.ofEpochMilli(it.completedAt)) }
        }
}
