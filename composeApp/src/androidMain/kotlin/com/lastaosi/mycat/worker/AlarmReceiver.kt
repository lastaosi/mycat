package com.lastaosi.mycat.worker

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
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

/**
 * 약 복용 알람을 수신하는 BroadcastReceiver.
 *
 * ## 수신하는 액션
 * - [ACTION_MEDICATION]: AlarmManager 정확 알람 발동 시 수신.
 *   알림을 표시하고, 다음날 같은 시각에 알람을 재등록한다.
 * - [ACTION_BOOT]: 기기 재부팅 완료 시 수신.
 *   AlarmManager 알람은 재부팅 시 자동 소멸되므로, 활성 약 전체 알람을 재등록한다.
 *
 * ## 알람 3중 시스템 내에서의 역할
 * - WorkManagerScheduler(15분 폴링) → 정확도 낮음, 보조 역할
 * - MedicationAlarmScheduler(AlarmManager) → 정확 알람 등록
 * - AlarmReceiver(BroadcastReceiver) → 알람 수신 + 알림 표시 + **다음날 재등록**
 *
 * AndroidManifest.xml 에 receiver 등록 + RECEIVE_BOOT_COMPLETED 권한 선언 필요.
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {

    companion object {
        /** AlarmManager 정확 알람 액션 식별자 */
        const val ACTION_MEDICATION  = "com.lastaosi.mycat.ACTION_MEDICATION"
        /** 재부팅 완료 액션 (Intent.ACTION_BOOT_COMPLETED) */
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

                // Android 13(TIRAMISU)+ 에서는 POST_NOTIFICATIONS 권한을 런타임에 확인해야 한다.
                // 미만 버전은 권한 개념 없으므로 true 처리.
                val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else true

                L.d("약 복용 알람 권한 ${hasPermission}")

                if (hasPermission) {
                    NotificationHelper.showMedicationNotification(
                        context = context.applicationContext,
                        catName = catName,
                        medicationName = medicationName
                    )
                }
                L.d("약 복용 알람 발송 완료!")

                // AlarmManager 정확 알람은 1회성이므로 다음날 같은 시각으로 재등록해야 한다.
                // IO 코루틴에서 DB 조회 후 MedicationAlarmScheduler 재호출.
                // (catId=0L 로 getActiveMedications 를 호출해 medicationId 로 필터링)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val medication = medicationRepository
                            .getActiveMedications(0L)
                            .first()
                            .firstOrNull { it.id == medicationId } ?: return@launch

                        // 해당 alarmId + isEnabled 인 알람만 재등록 (비활성 알람 제외)
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

            // 재부팅 시 AlarmManager 알람 전체 재등록.
            // 기기가 꺼졌다 켜지면 등록된 AlarmManager 알람이 모두 소멸되므로
            // BOOT_COMPLETED 를 수신해서 대표 고양이의 활성 약 알람을 다시 등록한다.
            ACTION_BOOT -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val cat = catRepository.getRepresentativeCat() ?: return@launch
                        val medications = medicationRepository
                            .getActiveMedications(cat.id)
                            .first()

                        medications.forEach { medication ->
                            // isEnabled 인 알람만 재등록
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