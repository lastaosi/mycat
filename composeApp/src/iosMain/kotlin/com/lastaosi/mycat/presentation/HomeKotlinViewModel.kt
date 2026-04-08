package com.lastaosi.mycat.presentation

import com.lastaosi.mycat.domain.usecase.MainUseCase
import com.lastaosi.mycat.util.L
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class HomeKotlinViewModel : KoinComponent {
    private val scope = MainScope()
    private val useCase: MainUseCase by inject()

    fun loadData(
        onCatLoaded: (name: String, breedName: String) -> Unit,
        onGuideLoaded: (dryG: Int, wetG: Int, waterMl: Int, minG: Int, maxG: Int) -> Unit,
        onWeightLoaded: (weightG: Int) -> Unit,
        onTipLoaded: (tip: String) -> Unit,
        onHealthCheckLoaded: (titles: List<String>) -> Unit
    ) {
        scope.launch {
            useCase.getAllCats().collect { cats ->
                val cat = cats.firstOrNull { it.isRepresentative } ?: cats.firstOrNull() ?: return@collect

                onCatLoaded(cat.name, cat.breedNameCustom ?: "품종 미등록")

                val ageMonth = useCase.calculateAgeMonth(cat.birthDate)
                L.d("birthDate: ${cat.birthDate}, ageMonth: $ageMonth")
                cat.breedId?.let { breedId ->
                    L.d("breedId: $breedId, ageMonth: $ageMonth")
                    val guide = useCase.getBreedGuide(breedId, ageMonth)
                    L.d("guide: $guide")
                    guide?.let {
                        L.d("onGuideLoaded 호출: dry=${it.foodDryG}, wet=${it.foodWetG}, water=${it.waterMl}")
                        onGuideLoaded(it.foodDryG, it.foodWetG, it.waterMl, it.weightMinG, it.weightMaxG)
                    } ?: L.d("guide가 null이에요!")
                }  ?: L.d("breedId가 null이에요!")

                launch {
                    useCase.getLatestWeight(cat.id).collect { records ->
                        val latest = records.maxByOrNull { it.recordedAt }
                        latest?.let { onWeightLoaded(it.weightG) }
                    }
                }

                launch {
                    useCase.getHealthCheckSummary(ageMonth).collect { items ->
                        onHealthCheckLoaded(items.map { it.title })
                    }
                }

                val tip = useCase.getRandomTip()
                tip?.let { onTipLoaded(it.content) }
            }
        }
    }

    fun dispose() {
        scope.cancel()
    }
}