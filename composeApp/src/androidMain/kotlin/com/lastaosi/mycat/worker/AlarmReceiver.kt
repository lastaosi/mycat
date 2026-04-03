package com.lastaosi.mycat.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.util.L
import com.lastaosi.mycat.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        const val ACTION_MEDICATION  = "com.lastaosi.mycat.ACTION_MEDICATION"
        const val ACTION_BOOT        = Intent.ACTION_BOOT_COMPLETED
        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_ALARM_ID      = "alarm_id"
        const val EXTRA_CAT_NAME      = "cat_name"
        const val EXTRA_MEDICATION_NAME = "medication_name"
    }

    private val catRepository: CatRepository by inject()
    private val medicationRepository: MedicationRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        L.d("onReceive 호출 ${intent.action}")
        when (intent.action) {
            ACTION_MEDICATION -> {
                L.d("약 복용 알람 수신!")
                val catName = intent.getStringExtra(EXTRA_CAT_NAME) ?: return
                val medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME) ?: return
                val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)

                // 권한 확인
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                } else true
                L.d("약 복용 알람 권한 ${hasPermission}")
                // 알림 발송
                NotificationHelper.showMedicationNotification(
                    context = context.applicationContext,
                    catName = catName,
                    medicationName = medicationName
                )
                L.d("약 복용 알람 발송 완료!")
                // 다음날 같은 시각으로 재등록
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val medication = medicationRepository
                            .getActiveMedications(0L)
                            .first()
                            .firstOrNull { it.id == medicationId } ?: return@launch

                        val alarms = medicationRepository
                            .getAlarmsByMedication(medicationId)
                            .first()
                            .filter { it.id == alarmId && it.isEnabled }

                        alarms.forEach { alarm ->
                            MedicationAlarmScheduler.scheduleAlarm(
                                context = context.applicationContext,
                                medicationId = medicationId,
                                alarmId = alarm.id,
                                alarmTime = alarm.alarmTime,
                                catName = catName,
                                medicationName = medication.name
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // 재부팅 시 알람 재등록
            ACTION_BOOT -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val cat = catRepository.getRepresentativeCat() ?: return@launch
                        val medications = medicationRepository
                            .getActiveMedications(cat.id)
                            .first()

                        medications.forEach { medication ->
                            val alarms = medicationRepository
                                .getAlarmsByMedication(medication.id)
                                .first()
                                .filter { it.isEnabled }

                            alarms.forEach { alarm ->
                                MedicationAlarmScheduler.scheduleAlarm(
                                    context = context.applicationContext,
                                    medicationId = medication.id,
                                    alarmId = alarm.id,
                                    alarmTime = alarm.alarmTime,
                                    catName = cat.name,
                                    medicationName = medication.name
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}