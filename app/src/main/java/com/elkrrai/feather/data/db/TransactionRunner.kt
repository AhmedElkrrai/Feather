package com.elkrrai.feather.data.db

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs several database writes atomically. A seam so repositories do not depend on Room directly. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner @Inject constructor(
    private val db: FeatherDatabase,
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}
