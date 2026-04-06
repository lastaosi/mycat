package com.lastaosi.mycat.domain.usecase.weight

import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.repository.WeightRecordRepository

class InsertWeightUseCase(
    private val weightRecordRepository: WeightRecordRepository
) {
    suspend operator fun invoke(record: WeightRecord): Long =
        weightRecordRepository.insert(record)
}