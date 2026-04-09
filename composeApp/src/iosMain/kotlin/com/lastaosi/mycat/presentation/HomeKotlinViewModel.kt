package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.usecase.MainUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS 홈 화면 전용 KotlinViewModel.
 *
 * Android MainViewModel 과 동일한 비즈니스 로직을 수행하며,
 * suspend / Flow → callback 패턴으로 Swift 에 데이터를 전달한다.
 *
 * 사용 패턴:
 *  1. loadData() 로 대표 고양이 기준 데이터 로드 시작 (Flow 구독 포함)
 *  2. 화면이 사라질 때 dispose() 호출 → 코루틴 스코프 취소
 */
class HomeKotlinViewModel : KoinComponent {
    private val scope = MainScope()
    private val useCase: MainUseCase by inject()

    /**
     * 대표 고양이를 기준으로 홈 화면에 필요한 모든 데이터를 로드한다.
     *
     * @param onCatLoaded          고양이 기본 정보 (id, 이름, 품종명) 콜백
     * @param onGuideLoaded        오늘의 케어 가이드 (건식·습식·물 급여량, 적정 체중 범위) 콜백
     * @param onWeightLoaded       최근 체중 (g 단위) 콜백
     * @param onTipLoaded          랜덤 고양이 팁 콜백
     * @param onHealthCheckLoaded  현재 월령 건강 체크리스트 제목 목록 콜백
     */
    fun loadData(
        onCatLoaded: (catId: Long, name: String, breedName: String) -> Unit,
        onGuideLoaded: (dryG: Int, wetG: Int, waterMl: Int, minG: Int, maxG: Int) -> Unit,
        onWeightLoaded: (weightG: Int) -> Unit,
        onTipLoaded: (tip: String) -> Unit,
        onHealthCheckLoaded: (titles: List<String>) -> Unit
    ) {
        scope.launch {
            useCase.getAllCats().collect { cats ->
                val cat = cats.firstOrNull { it.isRepresentative } ?: cats.firstOrNull() ?: return@collect

                onCatLoaded(cat.id, cat.name, cat.breedNameCustom ?: "품종 미등록")

                val ageMonth = useCase.calculateAgeMonth(cat.birthDate)
                L.d("birthDate: ${cat.birthDate}, ageMonth: $ageMonth")
                cat.breedId?.let { breedId ->
                    L.d("breedId: $breedId, ageMonth: $ageMonth")
                    val guide = useCase.getBreedGuide(breedId, ageMonth)
                    L.d("guide: $guide")
                    guide?.let {
                        L.d("onGuideLoaded 호출: dry=${it.foodDryG}, wet=${it.foodWetG}, water=${it.waterMl}")
                        onGuideLoaded(it.foodDryG, it.foodWetG, it.waterMl, it.weightMinG, it.weightMaxG)
                    } ?: L.d("guide가 null이에요!")
                }  ?: L.d("breedId가 null이에요!")

                launch {
                    useCase.getLatestWeight(cat.id).collect { records ->
                        val latest = records.maxByOrNull { it.recordedAt }
                        latest?.let { onWeightLoaded(it.weightG) }
                    }
                }

                launch {
                    useCase.getHealthCheckSummary(ageMonth).collect { items ->
                        onHealthCheckLoaded(items.map { it.title })
                    }
                }

                val tip = useCase.getRandomTip()
                tip?.let { onTipLoaded(it.content) }
            }
        }
    }

    /** 코루틴 스코프 취소. Swift deinit 에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
    }
}