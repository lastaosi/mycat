package com.lastaosi.mycat.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

/**
 * WorkManager 기반 백그라운드 알림 스케줄러.
 *
 * ## 등록하는 작업
 * - [scheduleVaccinationReminder]: [VaccinationReminderWorker] 매일 오전 9시 실행
 * - [scheduleMedicationReminder]: [MedicationReminderWorker] 매 15분마다 실행 (보조 알람)
 *
 * ## ExistingPeriodicWorkPolicy.UPDATE
 * 앱 재시작 시 동일 이름으로 enqueue 해도 기존 작업을 덮어쓰지 않고
 * 초기 딜레이만 갱신해 중복 실행을 방지한다.
 *
 * ## calculateDelayUntil
 * Calendar 를 이용해 "오늘 target 시각까지 남은 밀리초"를 계산한다.
 * target 이 이미 지난 시각이면 다음날로 자동 이월한다.
 */
object WorkManagerScheduler {

    private const val VACCINATION_WORK_NAME = "vaccination_reminder"
    private const val MEDICATION_WORK_NAME  = "medication_reminder"

    /**
     * 예방접종 알림 — 매일 오전 9시 실행
     */
    fun scheduleVaccinationReminder(context: Context) {
        val delay = calculateDelayUntil(hour = 9, minute = 0)

        val request = PeriodicWorkRequestBuilder<VaccinationReminderWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            VACCINATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * 약 복용 알림 — 매 15분마다 실행 (MedicationAlarm 시간과 비교)
     * WorkManager 최소 주기는 15분
     */
    fun scheduleMedicationReminder(context: Context) {
        val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            MEDICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
    }

    // 특정 시각까지 남은 밀리초 계산
    private fun calculateDelayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 이미 지난 시각이면 다음날로
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}