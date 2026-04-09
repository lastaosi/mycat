package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.usecase.MainUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * iOS 사이드 드로어용 간단한 Cat 요약 모델.
 * Swift 에서 CatSummaryIos 로 접근 가능 (ComposeApp 모듈).
 */
data class CatSummaryIos(
    val id: Long,
    val name: String,
    val breedName: String,
    val photoPath: String?,
    val isRepresentative: Boolean
)

/**
 * iOS MainDrawerView 전용 KotlinViewModel.
 * - 모든 고양이 목록을 로드하여 드로어 헤더에 표시
 * - 대표 고양이 전환 처리
 */
class MainKotlinViewModel : KoinComponent {

    private val scope = MainScope()
    private val useCase: MainUseCase by inject()

    /**
     * 고양이 목록 로드.
     * Flow 업데이트 시마다 콜백 호출 (멀티캣 전환 시 자동 갱신).
     */
    fun loadCats(
        onLoaded: (List<CatSummaryIos>) -> Unit
    ) {
        scope.launch {
            useCase.getAllCats().collect { cats ->
                val summaries = cats.map { cat ->
                    CatSummaryIos(
                        id = cat.id,
                        name = cat.name,
                        breedName = cat.breedNameCustom ?: "품종 미등록",
                        photoPath = cat.photoPath,
                        isRepresentative = cat.isRepresentative
                    )
                }
                L.d("MainKotlinViewModel: cats loaded count=${summaries.size}")
                onLoaded(summaries)
            }
        }
    }

    /** 대표 고양이 전환 */
    fun setRepresentative(catId: Long) {
        scope.launch {
            L.d("MainKotlinViewModel: setRepresentative catId=$catId")
            useCase.setRepresentative(catId)
        }
    }

    fun dispose() {
        scope.cancel()
    }
}
