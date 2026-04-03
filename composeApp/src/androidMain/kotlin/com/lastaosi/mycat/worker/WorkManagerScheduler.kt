package com.lastaosi.mycat.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import java.util.Calendar

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