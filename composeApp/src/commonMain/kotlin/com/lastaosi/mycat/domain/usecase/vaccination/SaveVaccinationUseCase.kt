package com.lastaosi.mycat.domain.usecase.vaccination

import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository

class SaveVaccinationUseCase(
    private val vaccinationRecordRepository: VaccinationRecordRepository
) {
    suspend operator fun invoke(record: VaccinationRecord) {
        if (record.id == 0L) {
            vaccinationRecordRepository.insert(record)
        } else {
            vaccinationRecordRepository.update(record)
        }
    }
}