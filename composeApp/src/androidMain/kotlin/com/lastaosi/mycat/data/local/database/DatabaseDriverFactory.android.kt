package com.lastaosi.mycat.data.local.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.lastaosi.mycat.db.MyCatDatabase
import java.io.File

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val dbFile = context.getDatabasePath("mycat.db")

        // assets에서 DB 복사 (최초 1회)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            context.assets.open("mycat.db").use { input ->
                dbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        // schema 파라미터 없이 생성 → 테이블 자동 생성 안 함
        return AndroidSqliteDriver(
            schema = MyCatDatabase.Schema,
            context = context,
            name = "mycat.db",
            callback = object : AndroidSqliteDriver.Callback(MyCatDatabase.Schema) {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // assets에서 복사했으므로 onCreate 무시
                }
            }
        )
    }
}