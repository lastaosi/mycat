package com.lastaosi.mycat.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lastaosi.mycat.R

object NotificationHelper {

    const val CHANNEL_VACCINATION = "channel_vaccination"
    const val CHANNEL_MEDICATION  = "channel_medication"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // 예방접종 채널
            NotificationChannel(
                CHANNEL_VACCINATION,
                "예방접종 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "예방접종 예정일 알림"
                manager.createNotificationChannel(this)
            }

            // 약 복용 채널
            NotificationChannel(
                CHANNEL_MEDICATION,
                "약 복용 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "약 복용 시간 알림"
                manager.createNotificationChannel(this)
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showVaccinationNotification(
        context: Context,
        catName: String,
        vaccineName: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_VACCINATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💉 예방접종 D-1")
            .setContentText("${catName}의 ${vaccineName} 예정일이 내일이에요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify(vaccineName.hashCode(), notification)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showMedicationNotification(
        context: Context,
        catName: String,
        medicationName: String
    ) {
        Log.d("NotificationHelper", "showMedicationNotification 호출")

        val notificationManager = NotificationManagerCompat.from(context)

        // 채널 존재 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_MEDICATION)
            Log.d("NotificationHelper", "채널 존재: ${channel != null}")
            Log.d("NotificationHelper", "채널 중요도: ${channel?.importance}")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MEDICATION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💊 약 복용 시간")
            .setContentText("${catName}의 ${medicationName} 복용 시간이에요!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)  // 추가
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // 추가
            .setAutoCancel(true)
            .build()

        try {
            notificationManager.notify(medicationName.hashCode(), notification)
            Log.d("NotificationHelper", "notify 호출 완료")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "notify 실패: ${e.message}")
        }
    }
}