package com.lastaosi.mycat.data.repository

import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import com.lastaosi.mycat.domain.repository.BreedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class BreedRepositoryImpl(
    private val db: MyCatDatabase
) : BreedRepository {

    override fun getAllBreeds(): Flow<List<Breed>> =
        db.breedQueries.getAllBreeds()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getBreedById(breedId: Int): Breed? =
        db.breedQueries.getBreedById(breedId.toLong())
            .executeAsOneOrNull()
            ?.toDomain()

    override fun searchBreeds(keyword: String): Flow<List<Breed>> =
        db.breedQueries.searchBreeds(keyword)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getGuideForMonth(
        breedId: Int,
        currentMonth: Int
    ): BreedMonthlyGuide? =
        db.breedMonthlyGuideQueries
            .getGuideForMonth(breedId.toLong(), currentMonth.toLong())
            .executeAsOneOrNull()
            ?.toDomain()

    override suspend fun getAllGuidesByBreed(breedId: Int): List<BreedMonthlyGuide> =
        db.breedMonthlyGuideQueries
            .getAllGuidesByBreed(breedId.toLong())
            .executeAsList()
            .map { it.toDomain() }

    // Mapper
    private fun com.lastaosi.mycat.db.Breed.toDomain() = Breed(
        id = id.toInt(),
        nameKo = nameKo,
        nameEn = nameEn,
        origin = origin,
        sizeCategory = sizeCategory,
        coatType = coatType,
        adultWeightMinG = adultWeightMinG.toInt(),
        adultWeightMaxG = adultWeightMaxG.toInt(),
        adultAgeMonth = adultAgeMonth.toInt(),
        lifeExpectancyMin = lifeExpectancyMin.toInt(),
        lifeExpectancyMax = lifeExpectancyMax.toInt(),
        commonDisease = commonDisease,
        breedNote = breedNote
    )

    private fun com.lastaosi.mycat.db.Breed_monthly_guide.toDomain() = BreedMonthlyGuide(
        id = id,
        breedId = breedId.toInt(),
        month = month.toInt(),
        weightMinG = weightMinG.toInt(),
        weightMaxG = weightMaxG.toInt(),
        foodDryG = foodDryG.toInt(),
        foodWetG = foodWetG.toInt(),
        waterMl = waterMl.toInt(),
        treatMaxG = treatMaxG.toInt()
    )
}