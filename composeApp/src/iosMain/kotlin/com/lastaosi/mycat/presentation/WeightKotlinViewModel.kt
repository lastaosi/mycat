package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.BreedAvgPoint
import com.lastaosi.mycat.domain.model.WeightRecord
import com.lastaosi.mycat.domain.usecase.weight.WeightUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * iOS 전용 체중 기록 KotlinViewModel.
 *
 * Android WeightViewModel과 동일한 비즈니스 로직을 수행하며,
 * suspend / Flow → callback 패턴으로 Swift에 데이터를 전달한다.
 *
 * 사용 패턴 (HomeKotlinViewModel, ProfileKotlinViewModel과 동일):
 *  1. loadData() 로 catId를 받아 데이터 로드 시작
 *  2. insertWeight() 로 체중 저장
 *  3. deinit 시 dispose() 호출 → 코루틴 스코프 취소
 */
class WeightKotlinViewModel : KoinComponent {
    private val scope = MainScope()
    private val useCase: WeightUseCase by inject()

    /**
     * catId에 해당하는 데이터를 로드한다.
     *
     * @param catId             로드할 고양이의 ID
     * @param onCatLoaded       고양이 이름, 생년월 로드 완료 콜백
     * @param onWeightHistoryLoaded 체중 히스토리 Flow 업데이트 콜백 (List<WeightRecord>)
     * @param onBreedAverageLoaded  품종 평균 성장 데이터 로드 완료 콜백 (List<BreedAvgPoint>)
     */
    fun loadData(
        catId: Long,
        onCatLoaded: (catName: String, birthDate: String) -> Unit,
        onWeightHistoryLoaded: (records: List<WeightRecord>) -> Unit,
        onBreedAverageLoaded: (points: List<BreedAvgPoint>) -> Unit
    ) {
        scope.launch {
            // 1. 고양이 정보 로드
            val cat = useCase.getCatById(catId)
            if (cat == null) {
                L.d("WeightKotlinViewModel: cat not found for id=$catId")
                return@launch
            }
            onCatLoaded(cat.name, cat.birthDate)

            // 2. 체중 기록 Flow 구독 (DB 변경 시 자동 업데이트)
            launch {
                useCase.getWeightHistory(catId).collect { records ->
                    val sorted = records.sortedBy { it.recordedAt }
                    L.d("WeightKotlinViewModel: weightHistory size=${sorted.size}")
                    onWeightHistoryLoaded(sorted)
                }
            }

            // 3. 품종 평균 성장 데이터 (breedId 있을 때만)
            cat.breedId?.let { breedId ->
                launch {
                    val avgPoints = useCase.getBreedAverageData(breedId)
                    L.d("WeightKotlinViewModel: breedAvgPoints size=${avgPoints.size}")
                    onBreedAverageLoaded(avgPoints)
                }
            }
        }
    }

    /**
     * 체중 기록을 DB에 저장한다.
     *
     * @param catId     고양이 ID
     * @param weightKg  체중 (kg 단위 문자열, 예: "3.5")
     * @param memo      메모 (선택)
     * @param onComplete 저장 완료 콜백
     */
    @OptIn(ExperimentalTime::class)
    fun insertWeight(
        catId: Long,
        weightKg: String,
        memo: String,
        onComplete: () -> Unit
    ) {
        val weightG = weightKg.toDoubleOrNull()?.times(1000)?.toInt()
        if (weightG == null) {
            L.d("WeightKotlinViewModel: 잘못된 체중 입력 weightKg=$weightKg")
            return
        }
        scope.launch {
            useCase.insertWeight(
                WeightRecord(
                    catId = catId,
                    weightG = weightG,
                    recordedAt = Clock.System.now().toEpochMilliseconds(),
                    memo = memo.ifBlank { null }
                )
            )
            L.d("WeightKotlinViewModel: insertWeight 완료 catId=$catId weightG=$weightG")
            onComplete()
        }
    }

    /** 코루틴 스코프 취소. Swift deinit에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
        L.d("WeightKotlinViewModel: disposed")
    }
}
