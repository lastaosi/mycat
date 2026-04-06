package com.lastaosi.mycat.util

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * DatePicker 다이얼로그를 띄우는 OutlinedTextField 래퍼
 * - value: 밀리초 (null이면 미선택)
 * - onDateSelected: 선택된 날짜 밀리초 반환
 * - label: 필드 레이블
 * - isRequired: 필수 여부
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    value: Long?,
    onDateSelected: (Long?) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val displayText = value?.let { millis ->
        val local = Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.year}-${local.monthNumber.toString().padStart(2,'0')}-${local.dayOfMonth.toString().padStart(2,'0')}"
    } ?: ""

    // Box로 감싸서 클릭 처리
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "날짜 선택",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            readOnly = true,
            isError = isError,
            supportingText = if (isError && errorMessage != null) {
                { Text(errorMessage) }
            } else null,
            enabled = false,  // 추가 — 포인터 이벤트를 Box로 넘김
            modifier = Modifier
                .fillMaxWidth()
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateSelected(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }
                ) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}