package com.lastaosi.mycat.domain.usecase.medication

import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.repository.MedicationRepository
import kotlinx.coroutines.flow.Flow

class GetMedicationsUseCase(
    private val medicationRepository: MedicationRepository
) {
    fun active(catId: Long): Flow<List<Medication>> =
        medicationRepository.getActiveMedications(catId)

    fun all(catId: Long): Flow<List<Medication>> =
        medicationRepository.getAllMedications(catId)
}