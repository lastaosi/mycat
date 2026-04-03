package com.lastaosi.mycat.presentation.healthcheck

import com.lastaosi.mycat.domain.model.HealthChecklist
import com.lastaosi.mycat.domain.model.HealthItemType

data class HealthCheckUiState(
    val catName: String = "",
    val ageMonth: Int = 0,
    val selectedTab: HealthCheckTab = HealthCheckTab.ALL,
    val allItems: List<HealthChecklist> = emptyList(),
    val isLoading: Boolean = false
) {
    // 탭별 필터링
    val filteredItems: List<HealthChecklist>
        get() = when (selectedTab) {
            HealthCheckTab.ALL     -> allItems
            HealthCheckTab.VACCINE -> allItems.filter { it.itemType == HealthItemType.VACCINE }
            HealthCheckTab.CHECK   -> allItems.filter { it.itemType == HealthItemType.CHECK }
            HealthCheckTab.SURGERY -> allItems.filter { it.itemType == HealthItemType.SURGERY }
        }
}

enum class HealthCheckTab(val label: String, val emoji: String) {
    ALL("전체", "📋"),
    VACCINE("예방접종", "💉"),
    CHECK("건강검진", "🏥"),
    SURGERY("수술/처치", "✂️")
}