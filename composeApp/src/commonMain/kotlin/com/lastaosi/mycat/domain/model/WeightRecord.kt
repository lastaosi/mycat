package com.lastaosi.mycat.domain.model

data class WeightRecord(
    val id: Long = 0,
    val catId: Long,
    val weightG: Int,
    val recordedAt: Long,
    val memo: String? = null
)