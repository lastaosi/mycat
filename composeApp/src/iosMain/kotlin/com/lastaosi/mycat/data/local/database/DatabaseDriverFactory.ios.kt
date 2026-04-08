package com.lastaosi.mycat.data.local.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.createDatabaseManager
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.util.L
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun createDriver(): SqlDriver {
        val appSupport = NSFileManager.defaultManager
            .URLForDirectory(
                NSApplicationSupportDirectory,
                NSUserDomainMask,
                null,
                true,
                null
            )!!.path!!

        val dbDir = "$appSupport/databases"
        val dbPath = "$dbDir/mycat.db"

        // databases 디렉토리 생성
        NSFileManager.defaultManager.createDirectoryAtPath(
            dbDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )

        if (!NSFileManager.defaultManager.fileExistsAtPath(dbPath)) {
            val bundlePath = NSBundle.mainBundle.pathForResource("mycat", ofType = "db")
            if (bundlePath != null) {
                NSFileManager.defaultManager.copyItemAtPath(
                    bundlePath, toPath = dbPath, error = null
                )
                L.d("DB 복사 완료: $dbPath")
            }
        }

        return NativeSqliteDriver(MyCatDatabase.Schema, "mycat.db")
    }
}