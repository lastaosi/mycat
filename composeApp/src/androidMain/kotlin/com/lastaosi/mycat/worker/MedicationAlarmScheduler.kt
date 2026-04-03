package com.lastaosi.mycat.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lastaosi.mycat.util.L
import java.util.Calendar

object MedicationAlarmScheduler {

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

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            // 고유 requestCode — medicationId + alarmId 조합
            (medicationId * 1000 + alarmId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // alarmTime 파싱 → 오늘 또는 내일 해당 시각
        val triggerTime = calculateTriggerTime(alarmTime)

        val triggerCalendar = java.util.Calendar.getInstance().apply {
            timeInMillis = triggerTime
        }
        L.d("=== 알람 등록 ===")
        L.d("약 이름: $medicationName")
        L.d("알람 시간: $alarmTime")
        L.d("발동 시각: ${triggerCalendar.time}")


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ — 정확한 알람 권한 확인
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

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

    // "HH:mm" → 오늘 해당 시각 (이미 지났으면 내일)
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