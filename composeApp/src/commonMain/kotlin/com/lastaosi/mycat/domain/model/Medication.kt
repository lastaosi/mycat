package com.lastaosi.mycat.domain.model

/**
 * 약 복용 정보 도메인 모델.
 *
 * @property medicationType  복용 주기 타입. [MedicationType] 참고.
 * @property intervalDays    [MedicationType.INTERVAL] 일 때만 사용. 몇 일마다 복용하는지.
 * @property startDate       복용 시작일 (epoch ms)
 * @property endDate         복용 종료일 (epoch ms). null 이면 종료일 미지정 (계속 복용).
 * @property isActive        복용 중 여부. false 면 "복용 완료" 상태로 목록에서 구분 표시.
 */
data class Medication(
    val id: Long = 0,
    val catId: Long,
    val name: String,
    val dosage: String? = null,
    val medicationType: MedicationType,
    val intervalDays: Int? = null,
    val startDate: Long,
    val endDate: Long? = null,
    val memo: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = 0
)

/**
 * 약 복용 주기 타입.
 *
 * - [ONCE]     : 1회성 복용 (예: 항생제 단기 처방)
 * - [DAILY]    : 매일 복용 (예: 심장사상충 예방약)
 * - [INTERVAL] : N일마다 복용. [Medication.intervalDays] 에 간격 저장. (예: 3일마다)
 * - [PERIOD]   : startDate ~ endDate 기간 동안 매일 복용 (예: 처방 기간 내)
 */
enum class MedicationType {
    ONCE, DAILY, INTERVAL, PERIOD
}

/**
 * 약 복용 알람 정보.
 *
 * 하나의 [Medication] 에 알람 시각을 여러 개 등록할 수 있다 (예: 아침 8시 + 저녁 9시).
 *
 * @property alarmTime  알람 시각. "HH:mm" 형식 문자열 (예: "08:00", "21:30").
 * @property isEnabled  false 이면 AlarmManager 에 등록하지 않는다.
 */
data class MedicationAlarm(
    val id: Long = 0,
    val medicationId: Long,
    val alarmTime: String,  // "HH:mm"
    val isEnabled: Boolean = true
)

/**
 * 약 복용 기록 (복약/건너뜀).
 *
 * @property scheduledAt  예정된 복용 시각 (epoch ms)
 * @property takenAt      실제 복용 시각 (epoch ms). null 이면 미복약.
 * @property isSkipped    사용자가 명시적으로 "건너뜀" 처리한 경우 true.
 */
data class MedicationLog(
    val id: Long = 0,
    val medicationId: Long,
    val scheduledAt: Long,
    val takenAt: Long? = null,
    val isSkipped: Boolean = false
)