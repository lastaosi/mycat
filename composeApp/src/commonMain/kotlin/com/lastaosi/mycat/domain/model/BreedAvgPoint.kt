package com.lastaosi.mycat.domain.model
// 품종 평균 데이터 포인트
data class BreedAvgPoint(
    val month: Int,      // 1~240
    val weightMinG: Int,
    val weightMaxG: Int,
    val avgWeightG: Int  // (min + max) / 2
)