package com.lastaosi.mycat.presentation.medication

import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationType

data class MedicationUiState(
    val catId: Long = 0L,
    val catName: String = "",
    val activeMedications: List<Medication> = emptyList(),
    val inactiveMedications: List<Medication> = emptyList(),
    val showInputDialog: Boolean = false,
    val editingMedication: Medication? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)