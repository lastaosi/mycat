package com.lastaosi.mycat.domain.usecase

import com.lastaosi.mycat.domain.usecase.cat.GetAllCatsUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetRepresentativeCatUseCase
import com.lastaosi.mycat.domain.usecase.cat.SetRepresentativeCatUseCase
import com.lastaosi.mycat.domain.usecase.breed.GetBreedGuideUseCase
import com.lastaosi.mycat.domain.usecase.diary.GetDiariesUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.GetHealthCheckSummaryUseCase
import com.lastaosi.mycat.domain.usecase.medication.GetMedicationsUseCase
import com.lastaosi.mycat.domain.usecase.tip.GetRandomTipUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.GetUpcomingVaccinationsUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetLatestWeightUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetWeightHistoryUseCase

/**
 * 메인 화면 / iOS HomeScreen 에서 필요한 UseCase 를 하나로 묶은 Facade.
 *
 * Koin 에서 `single { MainUseCase(...) }` 로 등록되며, ViewModel/KotlinViewModel 에
 * 단일 의존성으로 주입된다. 개별 UseCase 를 직접 주입하면 생성자 파라미터가 늘어나므로
 * 관련 UseCase 를 이 클래스로 묶어 주입 부담을 줄인다.
 *
 * ## 포함 UseCase
 * | 프로퍼티 | 역할 |
 * |---------|------|
 * | [getAllCats] | 전체 고양이 Flow 구독 (대표 고양이 포함) |
 * | [setRepresentative] | 대표 고양이 전환 |
 * | [getBreedGuide] | 월령별 품종 급여 가이드 조회 |
 * | [getLatestWeight] | 체중 히스토리 Flow (최신값 추출용) |
 * | [getUpcomingVaccinations] | 예정된 예방접종 목록 (D-Day 계산용) |
 * | [getMedications] | 활성/전체 약 목록 Flow |
 * | [getDiaries] | 일기 Flow (최근 미리보기용) |
 * | [getRandomTip] | 랜덤 고양이 팁 |
 * | [calculateAgeMonth] | 생년월 → 현재 월령 계산 |
 * | [getHealthCheckSummary] | 월령별 건강 체크리스트 항목 |
 */
data class MainUseCase(
    val getAllCats: GetAllCatsUseCase,
    val setRepresentative: SetRepresentativeCatUseCase,
    val getBreedGuide: GetBreedGuideUseCase,
    val getLatestWeight: GetWeightHistoryUseCase,
    val getUpcomingVaccinations: GetUpcomingVaccinationsUseCase,
    val getMedications: GetMedicationsUseCase,
    val getDiaries: GetDiariesUseCase,
    val getRandomTip: GetRandomTipUseCase,
    val calculateAgeMonth: CalculateAgeMonthUseCase,
    val getHealthCheckSummary: GetHealthCheckSummaryUseCase
)