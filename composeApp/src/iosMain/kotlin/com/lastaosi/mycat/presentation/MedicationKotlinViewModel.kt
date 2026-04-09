package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationAlarm
import com.lastaosi.mycat.domain.model.MedicationType
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.domain.usecase.medication.MedicationUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS 전용 약 복용 관리 KotlinViewModel.
 *
 * Android MedicationViewModel 과 동일한 비즈니스 로직을 수행하며,
 * suspend / Flow → callback 패턴으로 Swift 에 데이터를 전달한다.
 *
 * 알람 스케줄링은 iOS 의 UNUserNotificationCenter 를 사용하므로
 * Swift 쪽(MedicationNotificationManager)에서 처리한다.
 * Kotlin 레이어는 DB 에 알람 시간을 저장/삭제하는 역할만 담당하고,
 * 실제 알람 등록/취소는 onScheduleAlarms / onCancelAlarms 콜백을 통해
 * Swift 로 위임한다.
 *
 * 사용 패턴:
 *  1. loadData() 로 catId 를 받아 고양이 정보 + 복용 목록 Flow 구독 시작
 *  2. saveMedication() 으로 신규 저장 또는 수정
 *  3. deleteMedication() 으로 삭제
 *  4. toggleActive() 로 복용 중 / 완료 전환
 *  5. deinit 시 dispose() 호출 → 코루틴 스코프 취소
 */
class MedicationKotlinViewModel : KoinComponent {

    private val scope = MainScope()
    private val useCase: MedicationUseCase by inject()
    private val medicationRepository: MedicationRepository by inject()

    // ── 현재 catId ──────────────────────────────────────────────────────────
    private var currentCatId: Long = 0L
    private var currentCatName: String = ""

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Load
    // ────────────────────────────────────────────────────────────────────────

    /**
     * catId 에 해당하는 고양이 정보와 약 목록을 로드한다.
     *
     * @param catId                로드할 고양이 ID
     * @param onCatLoaded          고양이 이름 로드 완료 콜백
     * @param onActiveMedications  복용 중인 약 목록 업데이트 콜백
     * @param onAllMedications     전체 약 목록 업데이트 콜백 (inactive 필터링은 Swift 에서)
     */
    fun loadData(
        catId: Long,
        onCatLoaded: (catName: String) -> Unit,
        onActiveMedications: (medications: List<Medication>) -> Unit,
        onAllMedications: (medications: List<Medication>) -> Unit
    ) {
        currentCatId = catId
        scope.launch {
            val cat = useCase.getCatById(catId)
            if (cat == null) {
                L.d("MedicationKotlinViewModel: cat not found for id=$catId")
                return@launch
            }
            currentCatName = cat.name
            onCatLoaded(cat.name)
            L.d("MedicationKotlinViewModel: cat loaded name=${cat.name}")

            // 복용 중인 약 Flow 구독
            launch {
                useCase.getMedications.active(catId).collect { list ->
                    L.d("MedicationKotlinViewModel: active medications size=${list.size}")
                    onActiveMedications(list)
                }
            }

            // 전체 약 Flow 구독 (inactive 포함)
            launch {
                useCase.getMedications.all(catId).collect { list ->
                    L.d("MedicationKotlinViewModel: all medications size=${list.size}")
                    onAllMedications(list)
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Save (신규 / 수정)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 약 복용 기록을 저장(신규 또는 수정)한다.
     *
     * 알람 스케줄링은 Swift 쪽 콜백으로 위임한다.
     *
     * @param medicationId      수정 시 기존 ID (신규면 0L)
     * @param name              약 이름
     * @param medicationType    복용 타입 문자열 ("ONCE" | "DAILY" | "INTERVAL" | "PERIOD")
     * @param dosage            투약량 (없으면 빈 문자열)
     * @param startDate         시작일 epoch ms
     * @param endDate           종료일 epoch ms (없으면 null)
     * @param intervalDays      복용 간격 일수 (INTERVAL 타입만, 없으면 null)
     * @param memo              메모 (없으면 빈 문자열)
     * @param alarmTimes        알람 시간 목록 (예: ["08:00", "21:00"])
     * @param onScheduleAlarms  신규/수정 완료 후 알람 등록 요청 콜백 (medicationId, catName, medName, alarmTimes)
     * @param onCancelAlarms    수정 시 기존 알람 취소 요청 콜백 (alarmIds)
     * @param onComplete        저장 완료 콜백
     */
    @OptIn(ExperimentalTime::class)
    fun saveMedication(
        medicationId: Long,
        name: String,
        medicationType: String,
        dosage: String,
        startDate: Long,
        endDate: Long?,
        intervalDays: Int?,
        memo: String,
        alarmTimes: List<String>,
        onScheduleAlarms: (medicationId: Long, catName: String, medName: String, alarmTimes: List<String>) -> Unit,
        onCancelAlarms: (alarmIds: List<Long>) -> Unit,
        onComplete: () -> Unit
    ) {
        val type = parseMedicationType(medicationType)

        scope.launch {
            if (medicationId != 0L) {
                // ── 수정 모드 ──────────────────────────────────────────────
                val existing = medicationRepository.getAllMedications(currentCatId)
                    .first()
                    .find { it.id == medicationId } ?: run {
                    L.d("MedicationKotlinViewModel: medication not found for id=$medicationId")
                    onComplete()
                    return@launch
                }

                medicationRepository.update(
                    existing.copy(
                        name = name,
                        medicationType = type,
                        dosage = dosage.ifBlank { null },
                        startDate = startDate,
                        endDate = endDate,
                        intervalDays = intervalDays,
                        memo = memo.ifBlank { null }
                    )
                )

                // 기존 알람 id 수집 → Swift 에서 UNNotification 취소
                val existingAlarms = medicationRepository.getAlarmsByMedication(medicationId).first()
                val alarmIds = existingAlarms.map { it.id }
                onCancelAlarms(alarmIds)

                // DB 알람 삭제 후 새로 삽입
                medicationRepository.deleteAlarmsByMedication(medicationId)
                alarmTimes.forEach { time ->
                    medicationRepository.insertAlarm(
                        MedicationAlarm(
                            medicationId = medicationId,
                            alarmTime = time,
                            isEnabled = true
                        )
                    )
                }

                // Swift 에 알람 등록 요청
                if (alarmTimes.isNotEmpty()) {
                    onScheduleAlarms(medicationId, currentCatName, name, alarmTimes)
                }

                L.d("MedicationKotlinViewModel: updated medication id=$medicationId name=$name")

            } else {
                // ── 신규 모드 ──────────────────────────────────────────────
                val newId = medicationRepository.insert(
                    Medication(
                        catId = currentCatId,
                        name = name,
                        medicationType = type,
                        dosage = dosage.ifBlank { null },
                        startDate = startDate,
                        endDate = endDate,
                        intervalDays = intervalDays,
                        memo = memo.ifBlank { null },
                        isActive = true,
                        createdAt = Clock.System.now().toEpochMilliseconds()
                    )
                )

                // 알람 DB 저장 후 Swift 에 등록 요청
                alarmTimes.forEach { time ->
                    medicationRepository.insertAlarm(
                        MedicationAlarm(
                            medicationId = newId,
                            alarmTime = time,
                            isEnabled = true
                        )
                    )
                }

                if (alarmTimes.isNotEmpty()) {
                    onScheduleAlarms(newId, currentCatName, name, alarmTimes)
                }

                L.d("MedicationKotlinViewModel: inserted medication id=$newId name=$name")
            }

            onComplete()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Delete
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 약 복용 기록을 삭제한다.
     *
     * @param medicationId   삭제할 약 ID
     * @param onCancelAlarms 삭제 전 알람 취소 요청 콜백 (alarmIds)
     * @param onComplete     삭제 완료 콜백
     */
    fun deleteMedication(
        medicationId: Long,
        onCancelAlarms: (alarmIds: List<Long>) -> Unit,
        onComplete: () -> Unit
    ) {
        scope.launch {
            val alarms = medicationRepository.getAlarmsByMedication(medicationId).first()
            onCancelAlarms(alarms.map { it.id })

            medicationRepository.deleteAlarmsByMedication(medicationId)
            medicationRepository.delete(medicationId)

            L.d("MedicationKotlinViewModel: deleted medication id=$medicationId")
            onComplete()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Toggle Active
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 복용 중 / 완료 상태를 토글한다.
     *
     * @param medicationId      대상 약 ID
     * @param currentIsActive   현재 isActive 값 (true → 완료로, false → 복용 중으로)
     * @param onCancelAlarms    비활성화 시 알람 취소 요청 콜백 (alarmIds)
     * @param onScheduleAlarms  재활성화 시 알람 등록 요청 콜백
     * @param onComplete        토글 완료 콜백
     */
    fun toggleActive(
        medicationId: Long,
        currentIsActive: Boolean,
        onCancelAlarms: (alarmIds: List<Long>) -> Unit,
        onScheduleAlarms: (medicationId: Long, catName: String, medName: String, alarmTimes: List<String>) -> Unit,
        onComplete: () -> Unit
    ) {
        scope.launch {
            val medication = medicationRepository.getAllMedications(currentCatId)
                .first()
                .find { it.id == medicationId } ?: run {
                L.d("MedicationKotlinViewModel: toggleActive - medication not found id=$medicationId")
                onComplete()
                return@launch
            }

            medicationRepository.update(medication.copy(isActive = !currentIsActive))

            val alarms = medicationRepository.getAlarmsByMedication(medicationId).first()

            if (currentIsActive) {
                // 활성 → 비활성: 알람 취소
                onCancelAlarms(alarms.map { it.id })
                L.d("MedicationKotlinViewModel: deactivated id=$medicationId, canceled ${alarms.size} alarms")
            } else {
                // 비활성 → 활성: 알람 재등록
                val times = alarms.filter { it.isEnabled }.map { it.alarmTime }
                if (times.isNotEmpty()) {
                    onScheduleAlarms(medicationId, currentCatName, medication.name, times)
                }
                L.d("MedicationKotlinViewModel: reactivated id=$medicationId, rescheduled ${times.size} alarms")
            }

            onComplete()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // MARK: - Helpers
    // ────────────────────────────────────────────────────────────────────────

    private fun parseMedicationType(raw: String): MedicationType =
        when (raw.uppercase()) {
            "ONCE"     -> MedicationType.ONCE
            "INTERVAL" -> MedicationType.INTERVAL
            "PERIOD"   -> MedicationType.PERIOD
            else       -> MedicationType.DAILY   // 기본값
        }

    /** 코루틴 스코프 취소. Swift deinit 에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
        L.d("MedicationKotlinViewModel: disposed")
    }
}
