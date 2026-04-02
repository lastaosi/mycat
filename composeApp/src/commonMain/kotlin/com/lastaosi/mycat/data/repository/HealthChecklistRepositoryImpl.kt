package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.HealthChecklist
import com.lastaosi.mycat.domain.model.HealthItemType
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HealthChecklistRepositoryImpl(
    private val db: MyCatDatabase
) : HealthChecklistRepository {

    override fun getChecklistUpToMonth(currentMonth: Int): Flow<List<HealthChecklist>> =
        db.healthChecklistQueries.getChecklistUpToMonth(currentMonth.toLong())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getChecklistByMonth(month: Int): List<HealthChecklist> =
        withContext(Dispatchers.IO) {
            db.healthChecklistQueries.getChecklistByMonth(month.toLong())
                .executeAsList()
                .map { it.toDomain() }
        }

    override fun getChecklistByType(type: String): Flow<List<HealthChecklist>> =
        db.healthChecklistQueries.getChecklistByType(type)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    private fun com.lastaosi.mycat.db.Health_checklist.toDomain() = HealthChecklist(
        id = id.toInt(),
        month = month.toInt(),
        itemType = runCatching { HealthItemType.valueOf(itemType) }.getOrDefault(HealthItemType.CHECK),
        title = title,
        description = description,
        isBreedSpecific = isBreedSpecific == 1L,
        isRecommended = isRecommended == 1L
    )
}
