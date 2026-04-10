package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import com.lastaosi.mycat.domain.usecase.careguide.CareGuideUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS 케어 가이드 화면 전용 KotlinViewModel.
 *
 * Android CareGuideViewModel 과 동일한 비즈니스 로직을 수행하며,
 * suspend → callback 패턴으로 Swift 에 데이터를 전달한다.
 *
 * 흐름:
 *  1. 대표 고양이 조회
 *  2. breedId 없으면 onNoBreed() 호출 후 종료
 *  3. 월령 계산 + 전체 가이드 목록 조회 → onLoaded() 콜백
 */
class CareGuideKotlinViewModel : KoinComponent {

    private val scope = MainScope()
    private val useCase: CareGuideUseCase by inject()

    /**
     * 대표 고양이 기준으로 케어 가이드 데이터를 로드한다.
     *
     * @param onLoaded   로드 성공 콜백 (고양이 이름, 품종명, 현재 월령, 가이드 목록)
     * @param onNoBreed  breedId 미등록 콜백 (품종 없음 안내 표시용)
     */
    fun loadData(
        onLoaded: (catName: String, breedName: String, ageMonth: Int, guides: List<BreedMonthlyGuide>) -> Unit,
        onNoBreed: () -> Unit
    ) {
        scope.launch {
            val cat = useCase.getRepresentativeCat() ?: run {
                L.d("CareGuideKotlinViewModel: 대표 고양이 없음")
                onNoBreed()
                return@launch
            }

            if (cat.breedId == null) {
                L.d("CareGuideKotlinViewModel: breedId 없음")
                onNoBreed()
                return@launch
            }

            val ageMonth = useCase.calculateAgeMonth(cat.birthDate)
            val guides = useCase.getAllBreedGuide(cat.breedId!!)
            val breedName = cat.breedNameCustom ?: ""

            L.d("CareGuideKotlinViewModel: loaded name=${cat.name}, ageMonth=$ageMonth, guides=${guides.size}")
            onLoaded(cat.name, breedName, ageMonth, guides)
        }
    }

    /** 코루틴 스코프 취소. Swift deinit 에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
        L.d("CareGuideKotlinViewModel: disposed")
    }
}
