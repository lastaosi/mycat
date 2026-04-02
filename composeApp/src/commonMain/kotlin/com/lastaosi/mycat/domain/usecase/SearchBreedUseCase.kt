package com.lastaosi.mycat.domain.usecase

import com.lastaosi.mycat.domain.model.Breed
import com.lastaosi.mycat.domain.repository.BreedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 품종 실시간 검색 UseCase.
 * query가 blank이면 빈 리스트를 즉시 반환하고,
 * 그 외에는 DB 검색 결과를 [maxResults]개로 제한해 Flow로 반환한다.
 */
class SearchBreedUseCase(
    private val breedRepository: BreedRepository
) {
    operator fun invoke(query: String, maxResults: Int = 5): Flow<List<Breed>> {
        if (query.isBlank()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return breedRepository.searchBreeds(query)
            .map { it.take(maxResults) }
    }
}