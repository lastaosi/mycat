package com.lastaosi.mycat.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자 입력 → "YYYY-MM-DD" 자동 포맷 [VisualTransformation].
 *
 * 예) 입력: "20260401" → 표시: "2026-04-01"
 *
 * ## OffsetMapping (커서 위치 변환)
 * "-" 구분자가 index 3 이후(YYYY 뒤)와 index 5 이후(MM 뒤)에 삽입된다.
 *
 * | original(숫자) 구간 | 삽입된 "-" 수 | transformed 보정 |
 * |---|---|---|
 * | 0~3 (연도)  | 0개 | +0 |
 * | 4~5 (월)    | 1개 | +1 |
 * | 6~8 (일)    | 2개 | +2 |
 *
 * transformedToOriginal 은 반대 방향으로 역보정한다.
 * offset 4 는 첫 번째 "-" 위치 → original 3(연도 끝)으로 흡수.
 */
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        // 숫자만 추출, 최대 8자리 (YYYYMMDD)
        val digits = text.text.filter { it.isDigit() }.take(8)
        // index 3 이후(연도 끝), index 5 이후(월 끝)에 "-" 삽입
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                append(c)
                if (index == 3 || index == 5) append("-")
            }
        }

        val offsetMapping = object : OffsetMapping {
            // original(숫자 인덱스) → transformed(포맷 인덱스)
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 3 -> offset          // 연도 구간: "-" 없음
                    offset <= 5 -> offset + 1      // 월 구간:   "-" 1개 보정
                    offset <= 8 -> offset + 2      // 일 구간:   "-" 2개 보정
                    else -> formatted.length
                }
            }

            // transformed(포맷 인덱스) → original(숫자 인덱스)
            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 4  -> offset         // 연도 + 첫 "-": 역보정 없음
                    offset <= 7  -> offset - 1     // 월 구간: "-" 1개 역보정
                    offset <= 10 -> offset - 2     // 일 구간: "-" 2개 역보정
                    else -> digits.length
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}