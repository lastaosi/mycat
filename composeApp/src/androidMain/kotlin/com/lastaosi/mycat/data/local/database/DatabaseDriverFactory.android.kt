package com.lastaosi.mycat.data.local.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lastaosi.mycat.db.MyCatDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            MyCatDatabase.Schema,
            context,
            "mycat.db"
        )
    }
}