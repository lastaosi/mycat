package com.lastaosi.mycat.domain.repository

import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationAlarm
import com.lastaosi.mycat.domain.model.MedicationLog
import kotlinx.coroutines.flow.Flow

interface MedicationRepository {
    fun getActiveMedications(catId: Long): Flow<List<Medication>>
    fun getAllMedications(catId: Long): Flow<List<Medication>>
    suspend fun insert(medication: Medication): Long
    suspend fun update(medication: Medication)
    suspend fun delete(id: Long)

    suspend fun insertAlarm(alarm: MedicationAlarm): Long
    suspend fun deleteAlarm(id: Long)
    suspend fun deleteAlarmsByMedication(medicationId: Long)
    fun getAlarmsByMedication(medicationId: Long): Flow<List<MedicationAlarm>>

    suspend fun insertLog(log: MedicationLog): Long
    suspend fun markAsTaken(id: Long, takenAt: Long)
    suspend fun markAsSkipped(id: Long)
    fun getLogsByMedication(medicationId: Long): Flow<List<MedicationLog>>
}