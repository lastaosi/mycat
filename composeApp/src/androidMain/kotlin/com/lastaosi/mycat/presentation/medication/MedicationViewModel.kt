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

/**
 * 약 복용 관리 화면 ViewModel.
 *
 * ## 알람 3중 시스템과의 연동
 * 약을 저장(추가/수정)/삭제/토글할 때 DB 변경과 함께 AlarmManager 알람도 함께 관리한다.
 *
 * | 작업 | DB | AlarmManager |
 * |------|-----|--------------|
 * | 추가  | insert | scheduleAlarm (알람 시간별) |
 * | 수정  | update | cancelAlarm(기존) → scheduleAlarm(신규) |
 * | 삭제  | delete | cancelAlarm |
 * | 비활성 토글 | update(isActive=false) | cancelAlarm |
 * | 활성 토글   | update(isActive=true)  | scheduleAlarm (재등록) |
 *
 * ## 의존성
 * - [MedicationUseCase]: 고양이/약 조회 통합 UseCaes
 * - [MedicationRepository]: 알람 CRUD (insertAlarm, deleteAlarmsByMedication 등) 직접 접근
 * - [MedicationAlarmScheduler]: AlarmManager 알람 등록/취소
 */
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

    /**
     * 약 저장 (추가 / 수정 통합).
     *
     * ## 수정 모드 흐름
     * 1. DB 약 정보 update
     * 2. 기존 알람 전부 AlarmManager 취소 + DB 삭제 (기존 alarmTimes 완전 교체)
     * 3. 새 alarmTimes 를 DB 에 insert 후 AlarmManager 등록
     *
     * ## 추가 모드 흐름
     * 1. DB 에 Medication insert → 생성된 medicationId 수령
     * 2. alarmTimes 를 DB 에 insert → 생성된 alarmId 수령
     * 3. (medicationId, alarmId) 쌍으로 AlarmManager 등록
     */
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
            // 알람 알림 표시용 고양이 이름
            val cat = useCase.getCatById(_uiState.value.catId)

            val editing = _uiState.value.editingMedication
            if (editing != null) {
                // 수정 모드: DB 업데이트 후 알람 전체 교체
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

                // 기존 알람 전부 취소 + DB 삭제 (알람 시간이 변경됐을 수 있으므로 전체 교체)
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

                // 새 알람 시간별로 DB 삽입 → 생성된 alarmId 로 AlarmManager 등록
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
                // 추가 모드: DB insert 후 반환된 medicationId 로 알람 등록
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

    /**
     * 약 삭제.
     * DB 삭제 전에 먼저 AlarmManager 알람을 취소해야 한다.
     * 순서: AlarmManager 취소 → DB 알람 삭제 → DB 약 삭제
     */
    private fun onDelete(id: Long) {
        viewModelScope.launch {
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

    /**
     * 복용 중 / 완료 상태 토글.
     *
     * isActive 를 반전시키면서 AlarmManager 알람도 함께 제어한다.
     * - 활성 → 비활성 : 모든 알람 취소 (복용 완료 처리)
     * - 비활성 → 활성 : isEnabled 인 알람만 재등록 (복용 재시작)
     *
     * 주의: 파라미터 [medication] 은 토글 *전* 상태 객체다.
     * `medication.isActive == true` 이면 "현재 활성" → 비활성으로 바꾸는 의도.
     */
    fun onToggleActive(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.update(medication.copy(isActive = !medication.isActive))

            if (medication.isActive) {
                // 현재 활성 → 비활성으로 변경: 알람 전체 취소
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
                // 현재 비활성 → 활성으로 변경: isEnabled 인 알람만 재등록
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