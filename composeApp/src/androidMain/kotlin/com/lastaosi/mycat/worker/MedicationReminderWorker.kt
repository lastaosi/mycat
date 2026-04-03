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

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val catRepository: CatRepository by inject()
    private val medicationRepository: MedicationRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            val cat = catRepository.getRepresentativeCat() ?: return Result.success()

            // 활성 약 목록 가져오기
            val medications = medicationRepository
                .getActiveMedications(cat.id)
                .first()

            medications.forEach { medication ->
                // 해당 약의 알람 목록 가져오기
                val alarms = medicationRepository
                    .getAlarmsByMedication(medication.id)
                    .first()
                    .filter { it.isEnabled }

                // 현재 시각과 알람 시간 비교 (HH:mm 형식)
                val currentTime = getCurrentTimeString()
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

    // 현재 시각을 "HH:mm" 형식으로 반환
    private fun getCurrentTimeString(): String {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            .toString().padStart(2, '0')
        val minute = calendar.get(java.util.Calendar.MINUTE)
            .toString().padStart(2, '0')
        return "$hour:$minute"
    }
}