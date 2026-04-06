package com.lastaosi.mycat.domain.usecase.vaccination

import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository

class GetUpcomingVaccinationsUseCase(
    private val vaccinationRecordRepository: VaccinationRecordRepository
) {
    suspend operator fun invoke(fromTimestamp: Long): List<VaccinationRecord> =
        vaccinationRecordRepository.getUpcomingVaccinations(fromTimestamp)
}