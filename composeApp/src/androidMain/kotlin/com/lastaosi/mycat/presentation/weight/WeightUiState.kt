package com.lastaosi.mycat.presentation.weight

import com.lastaosi.mycat.domain.model.WeightRecord

/** 체중 기록 화면 UI 상태 */
data class WeightUiState(
    val catId: Long = 0L,
    val catName: String = "",
    val birthDate: String = "",  // 추가
    val weightHistory: List<WeightRecord> = emptyList(),
    val latestWeightG: Int? = null,
    val breedAverageData: List<BreedAvgPoint> = emptyList(), // 품종 평균
    val selectedTab: WeightTab = WeightTab.MY_CAT,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showInputDialog: Boolean = false
)

enum class WeightTab(val label: String) {
    MY_CAT("내 고양이 추이"),
    BREED_AVERAGE("품종 평균 성장")
}

// 품종 평균 데이터 포인트
data class BreedAvgPoint(
    val month: Int,      // 1~240
    val weightMinG: Int,
    val weightMaxG: Int,
    val avgWeightG: Int  // (min + max) / 2
)