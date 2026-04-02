package com.lastaosi.mycat.domain.model

data class CatDiary(
    val id: Long = 0,
    val catId: Long,
    val title: String? = null,
    val content: String,
    val mood: DiaryMood? = null,
    val photoPath: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)

enum class DiaryMood {
    HAPPY, NORMAL, SAD, SICK, PLAYFUL
}