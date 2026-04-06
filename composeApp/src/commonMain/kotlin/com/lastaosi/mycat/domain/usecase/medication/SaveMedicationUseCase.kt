package com.lastaosi.mycat.domain.usecase.medication

import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.repository.MedicationRepository

class SaveMedicationUseCase(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(medication: Medication): Long {
        return if (medication.id == 0L) {
            medicationRepository.insert(medication)
        } else {
            medicationRepository.update(medication)
            medication.id
        }
    }
}