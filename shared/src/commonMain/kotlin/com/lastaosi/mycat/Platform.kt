package com.lastaosi.mycat

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform