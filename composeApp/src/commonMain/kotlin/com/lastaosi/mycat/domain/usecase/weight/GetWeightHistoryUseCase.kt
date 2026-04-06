package com.lastaosi.mycat.domain.usecase.weight

import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
import kotlinx.coroutines.flow.Flow

class GetWeightHistoryUseCase(
    private val weightRecordRepository: WeightRecordRepository
) {
    operator fun invoke(catId: Long): Flow<List<WeightRecord>> =
        weightRecordRepository.getWeightHistory(catId)
}