package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WeightRecordRepositoryImpl(
    private val db: MyCatDatabase
) : WeightRecordRepository {

    override fun getWeightHistory(catId: Long): Flow<List<WeightRecord>> =
        db.weightRecordQueries.getWeightHistory(catId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getLatestWeight(catId: Long): WeightRecord? =
        withContext(Dispatchers.IO) {
            db.weightRecordQueries.getLatestWeight(catId).executeAsOneOrNull()?.toDomain()
        }

    override suspend fun insert(record: WeightRecord): Long =
        withContext(Dispatchers.IO) {
            db.weightRecordQueries.insert(
                catId = record.catId,
                weightG = record.weightG.toLong(),
                recordedAt = record.recordedAt,
                memo = record.memo
            )
            db.weightRecordQueries.lastInsertRowId().executeAsOne()
        }

    override suspend fun delete(id: Long) =
        withContext(Dispatchers.IO) {
            db.weightRecordQueries.delete(id)
            Unit
        }

    private fun com.lastaosi.mycat.db.Weight_record.toDomain() = WeightRecord(
        id = id,
        catId = catId,
        weightG = weightG.toInt(),
        recordedAt = recordedAt,
        memo = memo
    )
}
