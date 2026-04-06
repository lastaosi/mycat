package com.lastaosi.mycat.domain.usecase.vaccination

import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository

class DeleteVaccinationUseCase(
    private val vaccinationRecordRepository: VaccinationRecordRepository
) {
    suspend operator fun invoke(id: Long) = vaccinationRecordRepository.delete(id)
}