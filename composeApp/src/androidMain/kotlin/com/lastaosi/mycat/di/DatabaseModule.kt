package com.lastaosi.mycat.di

import com.lastaosi.mycat.data.local.database.DatabaseDriverFactory
import com.lastaosi.mycat.data.local.database.createDatabase
import com.lastaosi.mycat.db.MyCatDatabase
import org.koin.android.ext.koin.androidContext
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
}