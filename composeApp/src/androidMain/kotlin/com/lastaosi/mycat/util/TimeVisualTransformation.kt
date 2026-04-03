package com.lastaosi.mycat.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * 숫자 입력 → HH:mm 자동 포맷
 * 예: 0800 → 08:00
 */
class TimeVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(4)
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                append(c)
                if (index == 1 && digits.length > 2) append(":")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 4 -> offset + 1
                    else -> formatted.length
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                return when {
                    offset <= 2 -> offset
                    offset <= 5 -> offset - 1
                    else -> digits.length
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}