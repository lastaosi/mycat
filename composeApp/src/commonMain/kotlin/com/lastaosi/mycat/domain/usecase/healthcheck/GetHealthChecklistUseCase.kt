package com.lastaosi.mycat.domain.usecase.healthcheck

import com.lastaosi.mycat.domain.model.HealthChecklist
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetHealthChecklistUseCase(
    private val healthChecklistRepository: HealthChecklistRepository
) {
    operator fun invoke(): Flow<List<HealthChecklist>> =
        healthChecklistRepository.getAllChecklist()
}