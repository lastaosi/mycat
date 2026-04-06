package com.lastaosi.mycat.presentation.medication

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationAlarm
import com.lastaosi.mycat.domain.model.MedicationType
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.domain.usecase.medication.MedicationUseCase
import com.lastaosi.mycat.worker.MedicationAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class MedicationViewModel(
    application: Application,
    private val useCase: MedicationUseCase,
    private val medicationRepository: MedicationRepository,
) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val _uiState = MutableStateFlow(MedicationUiState())
    val uiState: StateFlow<MedicationUiState> = _uiState

    fun loadData(catId: Long) {
        viewModelScope.launch {
            val cat = useCase.getCatById(catId) ?: return@launch
            _uiState.update { it.copy(catId = cat.id, catName = cat.name) }

            launch {
                useCase.getMedications.active(catId)
                    .collect { list ->
                        _uiState.update { it.copy(activeMedications = list) }
                    }
            }

            launch {
                useCase.getMedications.all(catId)
                    .collect { list ->
                        _uiState.update {
                            it.copy(inactiveMedications = list.filter { m -> !m.isActive })
                        }
                    }
            }
        }
    }

    private fun onFabClick() {
        _uiState.update { it.copy(showInputDialog = true, editingMedication = null) }
    }

    private fun onEditClick(medication: Medication) {
        _uiState.update { it.copy(showInputDialog = true, editingMedication = medication) }
    }

    private fun onDialogDismiss() {
        _uiState.update { it.copy(showInputDialog = false, editingMedication = null) }
    }

    fun onAction(action: MedicationAction){
        when(action){
            is MedicationAction.FabClick -> onFabClick()
            is MedicationAction.EditClick -> onEditClick(action.medication)
            is MedicationAction.DialogDismiss -> onDialogDismiss()
            is MedicationAction.DeleteClick -> onDelete(action.medicationId)
            is MedicationAction.ToggleActive -> onToggleActive(action.medication )
            is MedicationAction.MedicationSave -> onSave(action.name,action.medicationType,action.dosage,action.startDate,action.endDate,action.intervalDays,action.memo,action.alarmTimes)
        }


    }

    @OptIn(ExperimentalTime::class)
    private fun onSave(
        name: String,
        medicationType: MedicationType,
        dosage: String,
        startDate: Long,
        endDate: Long?,
        intervalDays: Int?,
        memo: String,
        alarmTimes: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            // 대표 고양이 이름 (알람 알림용)
            val cat = useCase.getCatById(_uiState.value.catId)

            val editing = _uiState.value.editingMedication
            if (editing != null) {
                // 수정 모드
                medicationRepository.update(
                    editing.copy(
                        name = name,
                        medicationType = medicationType,
                        dosage = dosage.ifBlank { null },
                        startDate = startDate,
                        endDate = endDate,
                        intervalDays = intervalDays,
                        memo = memo.ifBlank { null }
                    )
                )

                // 기존 알람 전부 취소 + 삭제
                medicationRepository.getAlarmsByMedication(editing.id)
                    .first()
                    .forEach { alarm ->
                        MedicationAlarmScheduler.cancelAlarm(
                            context = context,
                            medicationId = editing.id,
                            alarmId = alarm.id
                        )
                    }
                medicationRepository.deleteAlarmsByMedication(editing.id)

                // 새 알람 등록
                alarmTimes.forEach { alarmTime ->
                    val alarmId = medicationRepository.insertAlarm(
                        MedicationAlarm(
                            medicationId = editing.id,
                            alarmTime = alarmTime,
                            isEnabled = true
                        )
                    )
                    MedicationAlarmScheduler.scheduleAlarm(
                        context = context,
                        medicationId = editing.id,
                        alarmId = alarmId,
                        alarmTime = alarmTime,
                        catName = cat?.name ?: "",
                        medicationName = name
                    )
                }
            } else {
                // 추가 모드
                val medicationId = medicationRepository.insert(
                    Medication(
                        catId = _uiState.value.catId,
                        name = name,
                        medicationType = medicationType,
                        dosage = dosage.ifBlank { null },
                        startDate = startDate,
                        endDate = endDate,
                        intervalDays = intervalDays,
                        memo = memo.ifBlank { null },
                        isActive = true,
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                )

                // 알람 등록
                alarmTimes.forEach { alarmTime ->
                    val alarmId = medicationRepository.insertAlarm(
                        MedicationAlarm(
                            medicationId = medicationId,
                            alarmTime = alarmTime,
                            isEnabled = true
                        )
                    )
                    MedicationAlarmScheduler.scheduleAlarm(
                        context = context,
                        medicationId = medicationId,
                        alarmId = alarmId,
                        alarmTime = alarmTime,
                        catName = cat?.name ?: "",
                        medicationName = name
                    )
                }
            }

            _uiState.update { it.copy(showInputDialog = false, editingMedication = null) }
        }
    }

    private fun onDelete(id: Long) {
        viewModelScope.launch {
            // 알람 취소 후 삭제
            medicationRepository.getAlarmsByMedication(id)
                .first()
                .forEach { alarm ->
                    MedicationAlarmScheduler.cancelAlarm(
                        context = context,
                        medicationId = id,
                        alarmId = alarm.id
                    )
                }
            medicationRepository.deleteAlarmsByMedication(id)
            medicationRepository.delete(id)
        }
    }

    fun onToggleActive(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.update(medication.copy(isActive = !medication.isActive))

            // 비활성화 시 알람 취소, 활성화 시 알람 재등록
            if (medication.isActive) {
                // 현재 활성 → 비활성으로 변경 → 알람 취소
                medicationRepository.getAlarmsByMedication(medication.id)
                    .first()
                    .forEach { alarm ->
                        MedicationAlarmScheduler.cancelAlarm(
                            context = context,
                            medicationId = medication.id,
                            alarmId = alarm.id
                        )
                    }
            } else {
                // 현재 비활성 → 활성으로 변경 → 알람 재등록
                val cat = useCase.getCatById(_uiState.value.catId)
                medicationRepository.getAlarmsByMedication(medication.id)
                    .first()
                    .filter { it.isEnabled }
                    .forEach { alarm ->
                        MedicationAlarmScheduler.scheduleAlarm(
                            context = context,
                            medicationId = medication.id,
                            alarmId = alarm.id,
                            alarmTime = alarm.alarmTime,
                            catName = cat?.name ?: "",
                            medicationName = medication.name
                        )
                    }
            }
        }
    }
}