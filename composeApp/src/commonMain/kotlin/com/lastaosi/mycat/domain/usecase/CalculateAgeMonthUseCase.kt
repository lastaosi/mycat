package com.lastaosi.mycat.domain.usecase

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CalculateAgeMonthUseCase {
    @OptIn(ExperimentalTime::class)
    operator fun invoke(birthDate: String): Int {
        return try{
            val parts = birthDate.split("-")
            val birthYear = parts[0].toInt()
            val birthMonth = parts[1].toInt()
            val now = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
            (now.year - birthYear) * 12 + (now.monthNumber - birthMonth)
        }catch (e: Exception){
            0
        }

    }
}