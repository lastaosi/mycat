package com.lastaosi.mycat.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class BirthDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(6)
        val formatted = buildString {
            digits.forEachIndexed { index, c ->
                if (index == 4) append("-")
                append(c)
            }
        }
        val formattedLength = formatted.length
        val digitsLength = digits.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, digitsLength)
                return when {
                    safeOffset <= 4 -> safeOffset
                    else -> (safeOffset + 1).coerceIn(0, formattedLength)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, formattedLength)
                return when {
                    safeOffset <= 4 -> safeOffset
                    safeOffset == 5 -> 4  // "-" 자리는 4번째로
                    else -> (safeOffset - 1).coerceIn(0, digitsLength)
                }
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}