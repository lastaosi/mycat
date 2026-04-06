package com.lastaosi.mycat.di

import com.lastaosi.mycat.data.local.database.DatabaseDriverFactory
import com.lastaosi.mycat.data.local.database.createDatabase
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.presentation.diary.DiaryViewModel
import com.lastaosi.mycat.presentation.healthcheck.HealthCheckViewModel
import com.lastaosi.mycat.presentation.main.MainViewModel
import com.lastaosi.mycat.presentation.medication.MedicationViewModel
import com.lastaosi.mycat.presentation.nearbyvet.NearbyVetViewModel
import com.lastaosi.mycat.presentation.profile.ProfileRegisterViewModel
import com.lastaosi.mycat.presentation.splash.SplashViewModel
import com.lastaosi.mycat.presentation.vaccination.VaccinationViewModel
import com.lastaosi.mycat.presentation.weight.WeightViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single<DatabaseDriverFactory> {
        DatabaseDriverFactory(androidContext())
    }
    single<MyCatDatabase> {
        createDatabase(get())
    }

    // DAO 역할 — SQLDelight 쿼리 객체
    single { get<MyCatDatabase>().breedQueries }
    single { get<MyCatDatabase>().breedMonthlyGuideQueries }
    single { get<MyCatDatabase>().healthChecklistQueries }
    single { get<MyCatDatabase>().catQueries }
    single { get<MyCatDatabase>().weightRecordQueries }
    single { get<MyCatDatabase>().vaccinationRecordQueries }
    single { get<MyCatDatabase>().medicationQueries }
    single { get<MyCatDatabase>().medicationAlarmQueries }
    single { get<MyCatDatabase>().medicationLogQueries }
    single { get<MyCatDatabase>().catDiaryQueries }
    single { get<MyCatDatabase>().catTipQueries }

    viewModel { SplashViewModel(get()) }
    viewModel { ProfileRegisterViewModel(get(), get(), get(), get()) }
    viewModel { MainViewModel(get()) }
    viewModel { WeightViewModel(get()) }
    viewModel { VaccinationViewModel(get()) }
    viewModel { MedicationViewModel(androidApplication(),get(), get(), ) }
    viewModel { DiaryViewModel(get(), ) }
    viewModel { HealthCheckViewModel(get(), get(), get()) }
    viewModel { NearbyVetViewModel(androidApplication()) }

}