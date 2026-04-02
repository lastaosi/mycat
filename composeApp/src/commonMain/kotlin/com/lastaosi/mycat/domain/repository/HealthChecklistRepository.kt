package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.HealthChecklist
import kotlinx.coroutines.flow.Flow

interface HealthChecklistRepository {
    fun getChecklistUpToMonth(currentMonth: Int): Flow<List<HealthChecklist>>
    suspend fun getChecklistByMonth(month: Int): List<HealthChecklist>
    fun getChecklistByType(type: String): Flow<List<HealthChecklist>>
}