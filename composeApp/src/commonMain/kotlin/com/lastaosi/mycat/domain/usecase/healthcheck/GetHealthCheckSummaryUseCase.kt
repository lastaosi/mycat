package com.lastaosi.mycat.domain.usecase.healthcheck

import com.lastaosi.mycat.domain.model.HealthChecklist
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetHealthCheckSummaryUseCase(
    private val healthChecklistRepository: HealthChecklistRepository
) {
    operator fun invoke(ageMonth: Int, maxItems: Int = 3): Flow<List<HealthChecklist>> =
        healthChecklistRepository.getChecklistUpToMonth(ageMonth).map { items ->
            val recommended = items.filter { it.isRecommended }
            val others = items.filter { !it.isRecommended }
            (recommended + others).take(maxItems)
        }
}