package com.lastaosi.mycat.domain.model

data class CatTip(
    val id: Long,
    val content: String,
    val category: CatTipCategory
)

enum class CatTipCategory {
    HEALTH, FOOD, GROOMING, BEHAVIOR, SAFETY
}