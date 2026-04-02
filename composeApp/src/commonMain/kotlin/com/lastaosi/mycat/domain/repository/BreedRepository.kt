package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.model.BreedMonthlyGuide
import kotlinx.coroutines.flow.Flow

interface BreedRepository {
    fun getAllBreeds(): Flow<List<Breed>>
    suspend fun getBreedById(breedId: Int): Breed?
    fun searchBreeds(keyword: String): Flow<List<Breed>>
    suspend fun getGuideForMonth(breedId: Int, currentMonth: Int): BreedMonthlyGuide?
    suspend fun getAllGuidesByBreed(breedId: Int): List<BreedMonthlyGuide>
}