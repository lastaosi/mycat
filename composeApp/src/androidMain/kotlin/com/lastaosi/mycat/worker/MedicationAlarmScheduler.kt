package com.lastaosi.mycat.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lastaosi.mycat.util.L
import java.util.Calendar

/**
 * AlarmManager 기반 약 복용 정확 알람 스케줄러.
 *
 * ## 역할
 * - [scheduleAlarm]: 특정 약/알람 ID 에 대해 AlarmManager 정확 알람 등록
 * - [cancelAlarm]: 등록된 알람 취소
 *
 * ## PendingIntent requestCode 고유성
 * `requestCode = (medicationId * 1000 + alarmId).toInt()`
 * 동일한 requestCode 를 가진 PendingIntent 는 시스템에서 같은 알람으로 취급하므로,
 * 약 ID 와 알람 ID 를 조합해 고유값을 만든다.
 * (약 1개당 알람이 최대 수십 개 수준이므로 1000 배수 조합으로 충분)
 *
 * ## Android 버전별 분기
 * - Android 12(S)+ : `canScheduleExactAlarms()` 권한 확인 후 등록
 * - Android 11 이하: 권한 확인 없이 바로 등록 (USE_EXACT_ALARM 미필요)
 *
 * ## 반복 알람이 아닌 이유
 * AlarmManager 반복 알람(`setRepeating`)은 Doze 모드에서 부정확하다.
 * 대신 1회성 정확 알람 발동 후 [AlarmReceiver] 에서 다음날 같은 시각으로 재등록한다.
 */
object MedicationAlarmScheduler {

    /**
     * 약 복용 알람을 등록한다.
     *
     * @param medicationId  약 ID (PendingIntent 고유 코드에 사용)
     * @param alarmId       알람 ID (PendingIntent 고유 코드에 사용)
     * @param alarmTime     알람 시각 "HH:mm" 형식
     * @param catName       알림 표시용 고양이 이름
     * @param medicationName 알림 표시용 약 이름
     */
    fun scheduleAlarm(
        context: Context,
        medicationId: Long,
        alarmId: Long,
        alarmTime: String,  // "HH:mm"
        catName: String,
        medicationName: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MEDICATION
            putExtra(AlarmReceiver.EXTRA_MEDICATION_ID, medicationId)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_CAT_NAME, catName)
            putExtra(AlarmReceiver.EXTRA_MEDICATION_NAME, medicationName)
        }

        // requestCode 를 medicationId * 1000 + alarmId 로 만들어 PendingIntent 고유성 보장.
        // FLAG_IMMUTABLE: Android 12+ 필수. FLAG_UPDATE_CURRENT: 동일 코드 재등록 시 업데이트.
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (medicationId * 1000 + alarmId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = calculateTriggerTime(alarmTime)

        val triggerCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = triggerTime
        }
        L.d("=== 알람 등록 ===")
        L.d("약 이름: $medicationName")
        L.d("알람 시간: $alarmTime")
        L.d("발동 시각: ${triggerCalendar.time}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12(S)+ : SCHEDULE_EXACT_ALARM 권한을 사용자가 허용한 경우에만 등록
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            // Android 11 이하: 권한 없이 정확 알람 등록 가능
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * 등록된 약 복용 알람을 취소한다.
     * scheduleAlarm 과 동일한 requestCode 로 PendingIntent 를 조회해 취소한다.
     */
    fun cancelAlarm(context: Context, medicationId: Long, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_MEDICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (medicationId * 1000 + alarmId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * "HH:mm" 문자열을 파싱해 오늘 해당 시각의 epoch ms 를 반환한다.
     * 이미 지난 시각이면 내일로 설정한다.
     */
    private fun calculateTriggerTime(alarmTime: String): Long {
        val parts = alarmTime.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 이미 지난 시각이면 내일로
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }
}