package com.lastaosi.mycat.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.lastaosi.mycat.db.MyCatDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(MyCatDatabase.Schema, "mycat.db")
    }
}