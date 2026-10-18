package com.elkrrai.feather.domain.rhythm

import kotlinx.coroutines.flow.Flow

/** Where "Your rhythm" gets its numbers: the record of every task that was finished. */
interface RhythmRepository {
    /** Every finish on record, newest first. */
    fun observeCompletions(): Flow<List<CompletionRecord>>
}
