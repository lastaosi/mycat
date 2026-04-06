package com.lastaosi.mycat.di

import com.lastaosi.mycat.data.remote.GeminiService
import com.lastaosi.mycat.data.remote.createHttpClient
import com.lastaosi.mycat.data.remote.getGeminiApiKey
import com.lastaosi.mycat.data.repository.BreedRepositoryImpl
import com.lastaosi.mycat.data.repository.CatDiaryRepositoryImpl
import com.lastaosi.mycat.data.repository.CatRepositoryImpl
import com.lastaosi.mycat.data.repository.CatTipRepositoryImpl
import com.lastaosi.mycat.data.repository.HealthChecklistRepositoryImpl
import com.lastaosi.mycat.data.repository.MedicationRepositoryImpl
import com.lastaosi.mycat.data.repository.WeightRecordRepositoryImpl
import com.lastaosi.mycat.data.repository.VaccinationRecordRepositoryImpl
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.domain.repository.CatDiaryRepository
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.CatTipRepository
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
import com.lastaosi.mycat.domain.usecase.cat.InsertCatUseCase
import com.lastaosi.mycat.domain.usecase.MainUseCase
import com.lastaosi.mycat.domain.usecase.RecognizeBreedUseCase
import com.lastaosi.mycat.domain.usecase.SearchBreedUseCase
import com.lastaosi.mycat.domain.usecase.breed.GetAllBreedGuidesUseCase
import com.lastaosi.mycat.domain.usecase.breed.GetBreedAverageDataUseCase
import com.lastaosi.mycat.domain.usecase.breed.GetBreedGuideUseCase
import com.lastaosi.mycat.domain.usecase.careguide.CareGuideUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetAllCatsUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetCatByIdUseCase
import com.lastaosi.mycat.domain.usecase.cat.GetRepresentativeCatUseCase
import com.lastaosi.mycat.domain.usecase.cat.SetRepresentativeCatUseCase
import com.lastaosi.mycat.domain.usecase.cat.UpdateCatUseCase
import com.lastaosi.mycat.domain.usecase.diary.DeleteDiaryUseCase
import com.lastaosi.mycat.domain.usecase.diary.DiaryUseCase
import com.lastaosi.mycat.domain.usecase.diary.GetDiariesUseCase
import com.lastaosi.mycat.domain.usecase.diary.SaveDiaryUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.GetHealthCheckSummaryUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.GetHealthChecklistUseCase
import com.lastaosi.mycat.domain.usecase.healthcheck.HealthCheckUseCase
import com.lastaosi.mycat.domain.usecase.medication.DeleteMedicationUseCase
import com.lastaosi.mycat.domain.usecase.medication.GetMedicationsUseCase
import com.lastaosi.mycat.domain.usecase.medication.MedicationUseCase
import com.lastaosi.mycat.domain.usecase.medication.SaveMedicationUseCase
import com.lastaosi.mycat.domain.usecase.tip.GetRandomTipUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.DeleteVaccinationUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.GetUpcomingVaccinationsUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.GetVaccinationsUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.SaveVaccinationUseCase
import com.lastaosi.mycat.domain.usecase.vaccination.VaccinationUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetLatestWeightUseCase
import com.lastaosi.mycat.domain.usecase.weight.GetWeightHistoryUseCase
import com.lastaosi.mycat.domain.usecase.weight.InsertWeightUseCase
import com.lastaosi.mycat.domain.usecase.weight.WeightUseCase
import org.koin.dsl.module

/**
 * Koin DI 모듈
 * - Repository: 인터페이스 → 구현체 바인딩 (single: 앱 수명과 동일한 싱글턴)
 * - Remote: HttpClient, GeminiService
 * - UseCase: factory (호출마다 새 인스턴스)
 */
val appModule = module {

    // Repository
    single<BreedRepository> { BreedRepositoryImpl(get()) }
    single<CatRepository> { CatRepositoryImpl(get()) }
    single<HealthChecklistRepository> { HealthChecklistRepositoryImpl(get()) }
    single<WeightRecordRepository> { WeightRecordRepositoryImpl(get()) }
    single<MedicationRepository> { MedicationRepositoryImpl(get()) }
    single<CatDiaryRepository> { CatDiaryRepositoryImpl(get()) }
    single<VaccinationRecordRepository> { VaccinationRecordRepositoryImpl(get()) }
    single<CatTipRepository> { CatTipRepositoryImpl(get()) }
    // Remote
    single { createHttpClient() }
    single {
        GeminiService(
            httpClient = get(),
            apiKey = getGeminiApiKey()
        )
    }

    // Cat UseCase
    factory { GetAllCatsUseCase(get()) }
    factory { GetRepresentativeCatUseCase(get()) }
    factory { SetRepresentativeCatUseCase(get()) }
    factory { UpdateCatUseCase(get()) }
    factory { InsertCatUseCase(get()) }
    factory { RecognizeBreedUseCase(get(), get()) }
    factory { SearchBreedUseCase(get()) }
    factory { CalculateAgeMonthUseCase() }
    factory { GetCatByIdUseCase(get()) }

    // Breed UseCase
    factory { GetBreedGuideUseCase(get()) }
    factory { GetBreedAverageDataUseCase(get()) }

    // Weight UseCase
    factory { GetWeightHistoryUseCase(get()) }
    factory { InsertWeightUseCase(get()) }
    factory { GetLatestWeightUseCase(get()) }

    // Vaccination UseCase
    factory { GetVaccinationsUseCase(get()) }
    factory { GetUpcomingVaccinationsUseCase(get()) }
    factory { SaveVaccinationUseCase(get()) }
    factory { DeleteVaccinationUseCase(get()) }

    // Medication UseCase
    factory { GetMedicationsUseCase(get()) }
    factory { SaveMedicationUseCase(get()) }
    factory { DeleteMedicationUseCase(get()) }

    // Diary UseCase
    factory { GetDiariesUseCase(get()) }
    factory { SaveDiaryUseCase(get()) }
    factory { DeleteDiaryUseCase(get()) }

    // Tip UseCase
    factory { GetRandomTipUseCase(get()) }
    // healthCare UseCase
    factory { GetHealthCheckSummaryUseCase(get()) }
    factory { GetHealthChecklistUseCase(get()) }
    factory { GetAllBreedGuidesUseCase(get()) }

    factory {
        WeightUseCase(
            getCatById = get(),
            getWeightHistory = get(),
            insertWeight = get(),
            getBreedAverageData = get()
        )
    }

    factory {
        VaccinationUseCase(
            getCatById = get(),
            getVaccinations = get(),
            saveVaccination = get(),
            deleteVaccination = get()
        )
    }

    factory {
        MedicationUseCase(
            getCatById = get(),
            getMedications = get(),
            saveMedication = get(),
            deleteMedication = get()
        )
    }

    factory {
        DiaryUseCase(
            getCatById = get(),
            getDiaries = get(),
            saveDiary = get(),
            deleteDiary = get()
        )
    }
    // MainUseCase 그룹
    factory {
        MainUseCase(
            getAllCats = get(),
            setRepresentative = get(),
            getBreedGuide = get(),
            getLatestWeight = get(),
            getUpcomingVaccinations = get(),
            getMedications = get(),
            getDiaries = get(),
            getRandomTip = get(),
            calculateAgeMonth = get(),
            getHealthCheckSummary = get()
        )
    }

    factory {
        HealthCheckUseCase(
            getRepresentativeCat = get(),
            calculateAgeMonth = get(),
            getHealthCheckList = get()
        )
    }

    factory {
        CareGuideUseCase(
            getRepresentativeCat = get(),
            calculateAgeMonth = get(),
            getAllBreedGuide = get()
        )
    }
}