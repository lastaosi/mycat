package com.lastaosi.mycat.domain.model

data class Cat(
    val id: Long = 0,
    val name: String,
    val birthDate: String,
    val gender: Gender,
    val breedId: Int? = null,
    val breedNameCustom: String? = null,
    val geminiBreedRaw: String? = null,
    val geminiConfidence: Double? = null,
    val weightG: Int? = null,
    val heightCm: Double? = null,
    val photoPath: String? = null,
    val isNeutered: Boolean = false,
    val isRepresentative: Boolean = false,
    val memo: String? = null,
    val createdAt: Long = 0
)

enum class Gender {
    MALE, FEMALE, UNKNOWN
}