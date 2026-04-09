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

/**
 * 예방접종 D-1 알림 WorkManager Worker.
 *
 * [WorkManagerScheduler.scheduleVaccinationReminder]에 의해 매일 오전 9시에 실행된다.
 *
 * ## D-1 범위 계산 방식
 * - `from` = 지금 + 24h (내일 이 시각)
 * - `to`   = 지금 + 48h (모레 이 시각)
 * - nextDueAt 이 [from..to] 범위에 속하는 접종 = "내일 예정" = D-1
 *
 * 이 방식을 쓰는 이유: 오전 9시에 워커가 실행되므로, 그 시점으로부터
 * "24~48시간 내" 를 D-1로 간주하면 날짜 변경선 의존 없이 안전하게 계산 가능.
 *
 * ## 필터 조건
 * 1. nextDueAt 이 [from..to] 범위 내에 있어야 함
 * 2. isNotificationEnabled == true 인 접종 기록만 대상
 * 3. 대표 고양이에 한정
 */
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

            // D-1 범위: 현재 시각 기준 24~48시간 후 (= 내일 이 시각 ± 24h)
            val from = now + oneDayMillis
            val to = now + oneDayMillis * 2

            val cat = catRepository.getRepresentativeCat() ?: return Result.success()

            // getUpcomingVaccinations 는 fromTimestamp 이후 모든 예정 접종을 반환하므로
            // to 까지의 범위는 클라이언트 측에서 필터링한다.
            val upcoming = vaccinationRecordRepository
                .getUpcomingVaccinations(fromTimestamp = from)
                .filter { record ->
                    record.nextDueAt != null &&
                            record.nextDueAt in from..to &&
                            record.isNotificationEnabled
                }

            upcoming.forEach { record ->
                NotificationHelper.showVaccinationNotification(
                    context = applicationContext,
                    catName = cat.name,
                    vaccineName = record.title
                )
            }

            Result.success()
        } catch (e: Exception) {
            // 일시적 오류 시 WorkManager 재시도 요청
            Result.retry()
        }
    }
}