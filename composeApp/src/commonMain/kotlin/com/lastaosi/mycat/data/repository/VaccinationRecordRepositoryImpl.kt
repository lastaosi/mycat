package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VaccinationRecordRepositoryImpl(
    private val db: MyCatDatabase
) : VaccinationRecordRepository {

    override fun getVaccinationsByCat(catId: Long): Flow<List<VaccinationRecord>> =
        db.vaccinationRecordQueries.getVaccinationsByCat(catId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getUpcomingVaccinations(fromTimestamp: Long): List<VaccinationRecord> =
        withContext(Dispatchers.IO) {
            db.vaccinationRecordQueries
                .getUpcomingVaccinations(fromTimestamp)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun insert(record: VaccinationRecord): Unit =
        withContext(Dispatchers.IO) {
            db.vaccinationRecordQueries.insert(
                catId = record.catId,
                checklistId = record.checklistId?.toLong(),
                title = record.title,
                vaccinatedAt = record.vaccinatedAt,
                nextDueAt = record.nextDueAt,
                memo = record.memo,
                isNotificationEnabled = if (record.isNotificationEnabled) 1L else 0L
            )
            Unit
        }

    override suspend fun update(record: VaccinationRecord): Unit =
        withContext(Dispatchers.IO) {
            db.vaccinationRecordQueries.update(
                id = record.id,
                title = record.title,
                vaccinatedAt = record.vaccinatedAt,
                nextDueAt = record.nextDueAt,
                memo = record.memo,
                isNotificationEnabled = if (record.isNotificationEnabled) 1L else 0L
            )
            Unit
        }

    override suspend fun delete(id: Long): Unit =
        withContext(Dispatchers.IO) {
            db.vaccinationRecordQueries.delete(id)
            Unit
        }

    private fun com.lastaosi.mycat.db.Vaccination_record.toDomain() = VaccinationRecord(
        id = id,
        catId = catId,
        checklistId = checklistId?.toInt(),
        title = title,
        vaccinatedAt = vaccinatedAt,
        nextDueAt = nextDueAt,
        memo = memo,
        isNotificationEnabled = isNotificationEnabled == 1L
    )
}