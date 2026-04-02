package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.WeightRecord
import kotlinx.coroutines.flow.Flow

interface WeightRecordRepository {
    fun getWeightHistory(catId: Long): Flow<List<WeightRecord>>
    suspend fun getLatestWeight(catId: Long): WeightRecord?
    suspend fun insert(record: WeightRecord): Long
    suspend fun delete(id: Long)
}