package com.lastaosi.mycat.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자 입력 → "HH:mm" 자동 포맷 [VisualTransformation].
 *
 * 예) 입력: "0800" → 표시: "08:00"
 *
 * ## OffsetMapping (커서 위치 변환)
 * ":" 구분자가 index 1 이후(시간 2자리 뒤)에 삽입된다.
 * 단, 숫자가 3자리 이상일 때만 ":" 를 삽입한다 (입력 도중 깜빡임 방지).
 *
 * | original(숫자) 구간 | 보정 |
 * |---|---|
 * | 0~2 (HH) | +0 |
 * | 3~4 (mm) | +1 (":" 1개) |
 */
class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // 숫자만 추출, 최대 4자리 (HHmm)
        val digits = text.text.filter { it.isDigit() }.take(4)
        // 3자리 이상 입력됐을 때만 index 1 뒤에 ":" 삽입 (입력 중 깜빡임 방지)
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                append(c)
                if (index == 1 && digits.length > 2) append(":")
            }
        }

        val offsetMapping = object : OffsetMapping {
            // original(숫자 인덱스) → transformed(포맷 인덱스)
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset          // HH 구간: ":" 없음
                    offset <= 4 -> offset + 1      // mm 구간: ":" 1개 보정
                    else -> formatted.length
                }
            }

            // transformed(포맷 인덱스) → original(숫자 인덱스)
            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset          // HH 구간
                    offset <= 5 -> offset - 1      // ":" 포함 구간: 1개 역보정
                    else -> digits.length
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}