package com.lastaosi.mycat.presentation.main

import com.lastaosi.mycat.domain.model.Cat

/** 사이드 드로어 메뉴 항목 */
enum class DrawerItem {
    HOME, CARE_GUIDE, WEIGHT, HEALTH_CHECK,
    VACCINATION, DIARY, MEDICATION, VET_MAP, SETTINGS,  PROFILE_EDIT
}

/**
 * 메인 화면 UI 상태.
 * - 대표 고양이 정보, 오늘의 급여량·수분량, 체중 범위
 * - 다가오는 예방접종/투약 알람, 최근 일기 미리보기 포함
 */
data class MainUiState(
    val cat: Cat? = null,
    val allCats: List<Cat> = emptyList(),  // 추가
    val selectedDrawerItem: DrawerItem = DrawerItem.HOME,
    val todayFoodDryG: Int = 0,
    val todayFoodWetG: Int = 0,
    val todayWaterMl: Int = 0,
    val weightMinG: Int = 0,
    val weightMaxG: Int = 0,
    val latestWeightG: Int? = null,
    val randomTip: String? = null,
    val isLoading: Boolean = false,
    // 추가
    val upcomingVaccinations: List<UpcomingAlarm> = emptyList(),
    val upcomingMedications: List<UpcomingAlarm> = emptyList(),
    val recentDiaries: List<DiaryPreview> = emptyList()
)

// 알림 미리보기 모델
data class UpcomingAlarm(
    val label: String,       // "3차 종합백신" / "항생제"
    val dateLabel: String,   // "D-3" / "오늘 18:00"
    val isUrgent: Boolean    // D-3 이내면 true
)

// 다이어리 미리보기 모델
data class DiaryPreview(
    val id: Long,
    val title: String,
    val content: String,     // 2줄 미리보기
    val mood: String?,       // 이모지
    val dateLabel: String    // "2026.04.01"
)