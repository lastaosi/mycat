package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.model.HealthChecklist
import com.lastaosi.mycat.domain.usecase.healthcheck.HealthCheckUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS 건강 체크리스트 화면 전용 KotlinViewModel.
 *
 * Android HealthCheckViewModel 과 동일한 비즈니스 로직을 수행하며,
 * suspend / Flow → callback 패턴으로 Swift 에 데이터를 전달한다.
 *
 * 대표 고양이를 자동으로 조회하므로 catId 파라미터가 필요하지 않다.
 *
 * 사용 패턴:
 *  1. loadData() 로 대표 고양이 정보 + 전체 체크리스트 Flow 구독 시작
 *  2. deinit 시 dispose() 호출 → 코루틴 스코프 취소
 */
class HealthCheckKotlinViewModel : KoinComponent {

    private val scope = MainScope()
    private val useCase: HealthCheckUseCase by inject()

    /**
     * 대표 고양이 기준으로 건강 체크리스트 데이터를 로드한다.
     *
     * @param onCatLoaded    고양이 이름 + 현재 월령 로드 완료 콜백
     * @param onItemsLoaded  전체 체크리스트 항목 Flow 업데이트 콜백
     *                       (DB 변경 시 자동 재전달)
     */
    fun loadData(
        onCatLoaded: (catName: String, ageMonth: Int) -> Unit,
        onItemsLoaded: (items: List<HealthChecklist>) -> Unit
    ) {
        scope.launch {
            val cat = useCase.getRepresentativeCat() ?: run {
                L.d("HealthCheckKotlinViewModel: 대표 고양이 없음")
                return@launch
            }
            val ageMonth = useCase.calculateAgeMonth(cat.birthDate)
            onCatLoaded(cat.name, ageMonth)
            L.d("HealthCheckKotlinViewModel: cat loaded name=${cat.name}, ageMonth=$ageMonth")

            // getAllChecklist: 전체 체크리스트 Flow (월령 필터 없음 — Swift 에서 탭 필터링)
            useCase.getHealthCheckList().collect { items ->
                L.d("HealthCheckKotlinViewModel: items loaded count=${items.size}")
                onItemsLoaded(items)
            }
        }
    }

    /** 코루틴 스코프 취소. Swift deinit 에서 반드시 호출해야 한다. */
    fun dispose() {
        scope.cancel()
        L.d("HealthCheckKotlinViewModel: disposed")
    }
}
