package com.lastaosi.mycat.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.util.NotificationHelper
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 약 복용 알림 WorkManager Worker (보조 알람).
 *
 * [WorkManagerScheduler.scheduleMedicationReminder]에 의해 매 15분마다 실행된다.
 * (WorkManager 최소 반복 주기는 15분)
 *
 * ## 역할
 * AlarmManager 정확 알람([MedicationAlarmScheduler])이 주(主) 알람이고,
 * 이 Worker 는 AlarmManager 알람이 누락됐을 때의 보조 역할을 한다.
 *
 * ## 시각 비교 방식
 * MedicationAlarm.alarmTime 은 DB에 "HH:mm" 문자열로 저장된다.
 * 워커 실행 시점의 현재 시각을 동일 형식으로 변환해 문자열 비교한다.
 * 초(second) 단위는 무시하므로 1분 이내 오차 허용.
 *
 * ## 주의
 * - WorkManager 는 배터리 최적화, Doze 모드 등에 의해 실제 실행 시각이 밀릴 수 있다.
 * - 알람 정확도가 중요한 경우 AlarmManager 정확 알람 경로를 사용한다.
 */
class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val catRepository: CatRepository by inject()
    private val medicationRepository: MedicationRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val cat = catRepository.getRepresentativeCat() ?: return Result.success()

            val medications = medicationRepository
                .getActiveMedications(cat.id)
                .first()

            // 현재 시각("HH:mm")을 미리 계산해 반복마다 재계산하지 않는다
            val currentTime = getCurrentTimeString()

            medications.forEach { medication ->
                val alarms = medicationRepository
                    .getAlarmsByMedication(medication.id)
                    .first()
                    .filter { it.isEnabled }

                // alarmTime("HH:mm") 과 현재 시각이 일치하는 알람만 알림 발송
                alarms.forEach { alarm ->
                    if (alarm.alarmTime == currentTime) {
                        NotificationHelper.showMedicationNotification(
                            context = applicationContext,
                            catName = cat.name,
                            medicationName = medication.name
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /**
     * 현재 시각을 "HH:mm" 형식으로 반환한다.
     * Device 기본 TimeZone 을 그대로 사용하여 사용자의 로컬 시간을 기준으로 한다.
     */
    private fun getCurrentTimeString(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            .toString().padStart(2, '0')
        val minute = calendar.get(java.util.Calendar.MINUTE)
            .toString().padStart(2, '0')
        return "$hour:$minute"
    }
}