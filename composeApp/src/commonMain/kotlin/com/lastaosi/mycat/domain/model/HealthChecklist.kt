package com.lastaosi.mycat.domain.model

data class HealthChecklist(
    val id: Int,
    val month: Int,
    val itemType: HealthItemType,
    val title: String,
    val description: String,
    val isBreedSpecific: Boolean,
    val isRecommended: Boolean
)

enum class HealthItemType {
    VACCINE, CHECK, SURGERY
}