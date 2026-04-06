package com.lastaosi.mycat.domain.usecase.vaccination

import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import kotlinx.coroutines.flow.Flow

class GetVaccinationsUseCase(
    private val vaccinationRecordRepository: VaccinationRecordRepository
) {
    operator fun invoke(catId: Long): Flow<List<VaccinationRecord>> =
        vaccinationRecordRepository.getVaccinationsByCat(catId)
}