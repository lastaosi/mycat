package com.lastaosi.mycat.domain.usecase.medication

import com.lastaosi.mycat.domain.repository.MedicationRepository

class DeleteMedicationUseCase(
    private val medicationRepository: MedicationRepository
) {
    suspend operator fun invoke(id: Long) = medicationRepository.delete(id)
}