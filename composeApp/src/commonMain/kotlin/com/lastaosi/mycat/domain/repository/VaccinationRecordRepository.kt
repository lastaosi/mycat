package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.VaccinationRecord
import kotlinx.coroutines.flow.Flow

interface VaccinationRecordRepository {
    fun getVaccinationsByCat(catId: Long): Flow<List<VaccinationRecord>>
    suspend fun getUpcomingVaccinations(fromTimestamp: Long): List<VaccinationRecord>
    suspend fun insert(record: VaccinationRecord)
    suspend fun update(record: VaccinationRecord)
    suspend fun delete(id: Long)
}