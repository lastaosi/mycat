package com.lastaosi.mycat.di

import com.lastaosi.mycat.data.local.database.DatabaseDriverFactory
import com.lastaosi.mycat.data.local.database.createDatabase
import com.lastaosi.mycat.db.MyCatDatabase
import org.koin.core.context.startKoin
import org.koin.dsl.module

val iosDatabaseModule = module {
    single<DatabaseDriverFactory> { DatabaseDriverFactory() }
    single<MyCatDatabase> { createDatabase(get()) }
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
}

fun initKoin() {
    startKoin {
        modules(iosDatabaseModule, appModule)
    }
}
