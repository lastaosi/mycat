package com.lastaosi.mycat.domain.usecase.weight

import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.repository.WeightRecordRepository

class GetLatestWeightUseCase(
    private val weightRecordRepository: WeightRecordRepository
) {
    suspend operator fun invoke(catId: Long): WeightRecord? =
        weightRecordRepository.getLatestWeight(catId)
}