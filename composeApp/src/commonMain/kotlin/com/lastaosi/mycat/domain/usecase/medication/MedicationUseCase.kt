package com.lastaosi.mycat.domain.usecase.medication

import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase

data class MedicationUseCase(
    val getCatById: GetCatByIdUseCase,
    val getMedications: GetMedicationsUseCase,
    val saveMedication: SaveMedicationUseCase,
    val deleteMedication: DeleteMedicationUseCase
)