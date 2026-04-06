package com.lastaosi.mycat.presentation.vaccination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.DeleteVaccinationUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.GetVaccinationsUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.SaveVaccinationUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.VaccinationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

class VaccinationViewModel(
    private val useCase: VaccinationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaccinationUiState())
    val uiState: StateFlow<VaccinationUiState> = _uiState

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = useCase.getCatById(catId) ?: return@launch
            _uiState.update { it.copy(catId = cat.id, catName = cat.name) }

            useCase.getVaccinations(catId)
                .collect { records ->
                    _uiState.update {
                        it.copy(
                            records = records.sortedByDescending { r -> r.vaccinatedAt }
                        )
                    }
                }
        }
    }

    private fun onFabClick() {
        _uiState.update { it.copy(showInputDialog = true, editingRecord = null) }
    }

    private fun onEditClick(record: VaccinationRecord) {
        _uiState.update { it.copy(showInputDialog = true, editingRecord = record) }
    }

    private fun onDialogDismiss() {
        _uiState.update { it.copy(showInputDialog = false, editingRecord = null) }
    }

    fun onAction(action: VaccinationAction){
        when(action){
            is VaccinationAction.FabClick -> onFabClick()
            is VaccinationAction.EditClick -> onEditClick(action.record)
            is VaccinationAction.DialogDismiss -> onDialogDismiss()
            is VaccinationAction.DeleteClick -> onDelete(action.recordId)
            is VaccinationAction.VaccinationSave -> onSave(action.title,action.vaccinatedAt,
                action.nextDueAt,action.memo,action.isNotificationEnabled)
        }

    }

    private fun onSave(
        title: String,
        vaccinatedAt: Long,
        nextDueAt: Long?,
        memo: String,
        isNotificationEnabled: Boolean
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingRecord
            if (editing != null) {
                // 수정
                useCase.saveVaccination(
                    editing.copy(
                        title = title,
                        vaccinatedAt = vaccinatedAt,
                        nextDueAt = nextDueAt,
                        memo = memo.ifBlank { null },
                        isNotificationEnabled = isNotificationEnabled
                    )
                )
            } else {
                // 신규
                useCase.saveVaccination(
                    VaccinationRecord(
                        catId = _uiState.value.catId,
                        title = title,
                        vaccinatedAt = vaccinatedAt,
                        nextDueAt = nextDueAt,
                        memo = memo.ifBlank { null },
                        isNotificationEnabled = isNotificationEnabled
                    )
                )
            }
            _uiState.update { it.copy(showInputDialog = false, editingRecord = null) }
        }
    }

    private fun onDelete(id: Long) {
        viewModelScope.launch {
            useCase.deleteVaccination(id)
        }
    }
}