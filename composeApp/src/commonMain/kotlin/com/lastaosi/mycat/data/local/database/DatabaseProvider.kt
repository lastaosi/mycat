package com.lastaosi.mycat.data.local.database

import com.lastaosi.mycat.db.MyCatDatabase

fun createDatabase(driverFactory: DatabaseDriverFactory): MyCatDatabase {
    return MyCatDatabase(driverFactory.createDriver())
}