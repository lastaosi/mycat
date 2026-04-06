package com.lastaosi.mycat.domain.usecase.tip

import com.lastaosi.mycat.domain.model.CatTip
import com.lastaosi.mycat.domain.repository.CatTipRepository

class GetRandomTipUseCase(
    private val catTipRepository: CatTipRepository
) {
    suspend operator fun invoke(): CatTip? = catTipRepository.getRandomTip()
}