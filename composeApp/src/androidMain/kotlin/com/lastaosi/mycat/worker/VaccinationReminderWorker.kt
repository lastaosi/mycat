package com.lastaosi.mycat.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import com.lastaosi.mycat.util.NotificationHelper
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class VaccinationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val catRepository: CatRepository by inject()
    private val vaccinationRecordRepository: VaccinationRecordRepository by inject()

    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result {
        return try {
            val now = Clock.System.now().toEpochMilliseconds()
            val oneDayMillis = 24 * 60 * 60 * 1000L

            // D-1 범위: 지금부터 24~48시간 후
            val from = now + oneDayMillis
            val to = now + oneDayMillis * 2

            // 대표 고양이 가져오기
            val cat = catRepository.getRepresentativeCat() ?: return Result.success()

            // 예정된 예방접종 조회
            val upcoming = vaccinationRecordRepository
                .getUpcomingVaccinations(fromTimestamp = from)
                .filter { record ->
                    record.nextDueAt != null &&
                            record.nextDueAt in from..to &&
                            record.isNotificationEnabled
                }

            // 알림 발송
            upcoming.forEach { record ->
                NotificationHelper.showVaccinationNotification(
                    context = applicationContext,
                    catName = cat.name,
                    vaccineName = record.title
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}