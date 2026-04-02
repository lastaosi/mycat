package com.lastaosi.mycat.di

import com.lastaosi.mycat.data.remote.GeminiService
import com.lastaosi.mycat.data.remote.createHttpClient
import com.lastaosi.mycat.data.remote.getGeminiApiKey
import com.lastaosi.mycat.data.repository.BreedRepositoryImpl
import com.lastaosi.mycat.data.repository.CatDiaryRepositoryImpl
import com.lastaosi.mycat.data.repository.CatRepositoryImpl
import com.lastaosi.mycat.data.repository.HealthChecklistRepositoryImpl
import com.lastaosi.mycat.data.repository.MedicationRepositoryImpl
import com.lastaosi.mycat.data.repository.WeightRecordRepositoryImpl
import com.lastaosi.mycat.data.repository.VaccinationRecordRepositoryImpl
import com.lastaosi.mycat.domain.repository.BreedRepository
import com.lastaosi.mycat.domain.repository.CatDiaryRepository
import com.lastaosi.mycat.domain.repository.CatRepository
import com.lastaosi.mycat.domain.repository.HealthChecklistRepository
import com.lastaosi.mycat.domain.repository.MedicationRepository
import com.lastaosi.mycat.domain.repository.WeightRecordRepository
import com.lastaosi.mycat.domain.repository.VaccinationRecordRepository
import com.lastaosi.mycat.domain.usecase.CalculateAgeMonthUseCase
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

    // Remote
    single { createHttpClient() }
    single {
        GeminiService(
            httpClient = get(),
            apiKey = getGeminiApiKey()
        )
    }

    // UseCase
    factory { CalculateAgeMonthUseCase() }
}