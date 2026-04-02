package com.lastaosi.mycat.domain.model

data class Breed(
    val id: Int,
    val nameKo: String,
    val nameEn: String,
    val origin: String,
    val sizeCategory: String,
    val coatType: String,
    val adultWeightMinG: Int,
    val adultWeightMaxG: Int,
    val adultAgeMonth: Int,
    val lifeExpectancyMin: Int,
    val lifeExpectancyMax: Int,
    val commonDisease: String,
    val breedNote: String
)

data class BreedMonthlyGuide(
    val id: Long,
    val breedId: Int,
    val month: Int,
    val weightMinG: Int,
    val weightMaxG: Int,
    val foodDryG: Int,
    val foodWetG: Int,
    val waterMl: Int,
    val treatMaxG: Int
)