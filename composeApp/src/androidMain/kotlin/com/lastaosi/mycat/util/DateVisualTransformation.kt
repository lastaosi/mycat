package com.lastaosi.mycat.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자 입력 → YYYY-MM-DD 자동 포맷
 * 예: 20260401 → 2026-04-01
 */
class DateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(8)
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                append(c)
                if (index == 3 || index == 5) append("-")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 3 -> offset
                    offset <= 5 -> offset + 1
                    offset <= 8 -> offset + 2
                    else -> formatted.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 4 -> offset
                    offset <= 7 -> offset - 1
                    offset <= 10 -> offset - 2
                    else -> digits.length
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}