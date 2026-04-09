package com.lastaosi.mycat.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 생년월 입력 필드를 "YYYY-MM" 형식으로 자동 포맷하는 [VisualTransformation].
 *
 * ## 동작 방식
 * 사용자가 숫자만 입력하면 내부적으로 최대 6자리 digits 를 추출한 뒤,
 * index 4 위치(연도 4자리 이후)에 "-" 구분자를 삽입해 화면에 표시한다.
 * 실제 TextField 의 value 는 숫자만 유지되며, 표시(transformed)만 포맷된다.
 *
 * 예) 입력: "202504" → 표시: "2025-04"
 *
 * ## OffsetMapping 동작 원리
 * 텍스트 커서 위치를 original(숫자 인덱스) ↔ transformed(포맷 인덱스) 사이에서
 * 정확히 변환해야 커서가 올바른 위치에 표시된다.
 *
 * | original offset | transformed offset | 설명 |
 * |---|---|---|
 * | 0~4 | 0~4 | 연도 자리, "-" 삽입 전이므로 1:1 |
 * | 5~6 | 6~7 | 월 자리, "-"(1칸) 때문에 +1 보정 |
 *
 * transformedToOriginal 에서 offset==5 이면 "-" 위치이므로 4(연도 끝)로 보정한다.
 */
class BirthDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // 숫자만 추출, 최대 6자리 (YYYYMM)
        val digits = text.text.filter { it.isDigit() }.take(6)
        // index 4 에 "-" 삽입 → "YYYY-MM"
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                if (index == 4) append("-")
                append(c)
            }
        }
        val formattedLength = formatted.length
        val digitsLength = digits.length

        val offsetMapping = object : OffsetMapping {
            // original(숫자) 인덱스 → transformed(포맷) 인덱스
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, digitsLength)
                return when {
                    safeOffset <= 4 -> safeOffset          // 연도 구간: 1:1 매핑
                    else -> (safeOffset + 1).coerceIn(0, formattedLength)  // 월 구간: "-" 1칸 보정
                }
            }

            // transformed(포맷) 인덱스 → original(숫자) 인덱스
            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, formattedLength)
                return when {
                    safeOffset <= 4 -> safeOffset          // 연도 구간: 1:1 매핑
                    safeOffset == 5 -> 4                   // "-" 위치 → 연도 끝(4)으로 보정
                    else -> (safeOffset - 1).coerceIn(0, digitsLength)  // 월 구간: "-" 1칸 역보정
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}