package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.VaccinationRecord
import com.lastaosi.mycat.domain.usecase.vaccination.VaccinationUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS 전용 예방접종 관리 KotlinViewModel.
 *
 * Android VaccinationViewModel과 동일한 비즈니스 로직을 수행하며,
 * suspend / Flow → callback 패턴으로 Swift에 데이터를 전달한다.
 *
 * 사용 패턴 (WeightKotlinViewModel과 동일):
 *  1. loadData() 로 catId를 받아 고양이 정보 + 접종 기록 Flow 구독 시작
 *  2. saveVaccination() 으로 신규 저장 또는 수정
 *  3. deleteVaccination() 으로 삭제
 *  4. deinit 시 dispose() 호출 → 코루틴 스코프 취소
 */
class VaccinationKotlinViewModel : KoinComponent {

    private val scope = MainScope()
    private val useCase: VaccinationUseCase by inject()

    // ── 현재 catId (saveVaccination 신규 저장 시 사용) ──────────────────────
    private var currentCatId: Long = 0L

    /**
     * catId에 해당하는 고양이 정보와 예방접종 기록을 로드한다.
     *
     * @param catId              로드할 고양이의 ID
     * @param onCatLoaded        고양이 이름 로드 완료 콜백
     * @param onRecordsLoaded    접종 기록 Flow 업데이트 콜백 (List<VaccinationRecord>, 최신순 정렬)
     */
    fun loadData(
        catId: Long,
        onCatLoaded: (catName: String) -> Unit,
        onRecordsLoaded: (records: List<VaccinationRecord>) -> Unit
    ) {
        currentCatId = catId
        scope.launch {
            // 1. 고양이 정보 로드
            val cat = useCase.getCatById(catId)
            if (cat == null) {
                L.d("VaccinationKotlinViewModel: cat not found for id=$catId")
                return@launch
            }
            onCatLoaded(cat.name)
            L.d("VaccinationKotlinViewModel: cat loaded name=${cat.name}")

            // 2. 접종 기록 Flow 구독 (DB 변경 시 자동 업데이트)
            launch {
                useCase.getVaccinations(catId).collect { records ->
                    val sorted = records.sortedByDescending { it.vaccinatedAt }
                    L.d("VaccinationKotlinViewModel: records size=${sorted.size}")
                    onRecordsLoaded(sorted)
                }
            }
        }
    }

    /**
     * 접종 기록을 저장(신규 또는 수정)한다.
     *
     * @param recordId              수정 시 기존 레코드 ID (신규면 0L)
     * @param title                 접종명
     * @param vaccinatedAt          접종일 (epoch ms)
     * @param nextDueAt             다음 예정일 (epoch ms, 없으면 null)
     * @param memo                  메모 (없으면 빈 문자열)
     * @param isNotificationEnabled 알림 여부
     * @param onComplete            저장 완료 콜백
     */
    fun saveVaccination(
        recordId: Long,
        title: String,
        vaccinatedAt: Long,
        nextDueAt: Long?,
        memo: String,
        isNotificationEnabled: Boolean,
        onComplete: () -> Unit
    ) {
        scope.launch {
            val record = if (recordId != 0L) {
                // 수정: 기존 id 유지
                VaccinationRecord(
                    id = recordId,
                    catId = currentCatId,
                    title = title,
                    vaccinatedAt = vaccinatedAt,
                    nextDueAt = nextDueAt,
                    memo = memo.ifBlank { null },
                    isNotificationEnabled = isNotificationEnabled
                )
            } else {
                // 신규
                VaccinationRecord(
                    catId = currentCatId,
                    title = title,
                    vaccinatedAt = vaccinatedAt,
                    nextDueAt = nextDueAt,
                    memo = memo.ifBlank { null },
                    isNotificationEnabled = isNotificationEnabled
                )
            }
            useCase.saveVaccination(record)
            L.d("VaccinationKotlinViewModel: saveVaccination 완료 id=$recordId title=$title")
            onComplete()
        }
    }

    /**
     * 접종 기록을 삭제한다.
     *
     * @param recordId   삭제할 레코드의 ID
     * @param onComplete 삭제 완료 콜백
     */
    fun deleteVaccination(
        recordId: Long,
        onComplete: () -> Unit
    ) {
        scope.launch {
            useCase.deleteVaccination(recordId)
            L.d("VaccinationKotlinViewModel: deleteVaccination 완료 id=$recordId")
            onComplete()
        }
    }

    /** 코루틴 스코프 취소. Swift deinit에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
        L.d("VaccinationKotlinViewModel: disposed")
    }
}
