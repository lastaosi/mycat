package com.lastaosi.mycat.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lastaosi.mycat.db.MyCatDatabase
import com.lastaosi.mycat.domain.model.Medication
import com.lastaosi.mycat.domain.model.MedicationAlarm
import com.lastaosi.mycat.domain.model.MedicationLog
import com.lastaosi.mycat.domain.model.MedicationType
import com.lastaosi.mycat.domain.repository.MedicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * 투약 관련 3개 테이블(medication, medication_alarm, medication_log)을 통합 관리하는 Repository.
 * - medication: 투약 정보 (ONCE/DAILY/INTERVAL/PERIOD 타입)
 * - medication_alarm: 투약 시각 알람 (1개 투약에 N개 알람 가능)
 * - medication_log: 실제 복약/건너뜀 기록
 */
class MedicationRepositoryImpl(
    private val db: MyCatDatabase
) : MedicationRepository {

    override fun getActiveMedications(catId: Long): Flow<List<Medication>> =
        db.medicationQueries.getActiveMedications(catId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override fun getAllMedications(catId: Long): Flow<List<Medication>> =
        db.medicationQueries.getAllMedications(catId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun insert(medication: Medication): Long =
        withContext(Dispatchers.IO) {
            db.medicationQueries.insert(
                catId = medication.catId,
                name = medication.name,
                dosage = medication.dosage,
                medicationType = medication.medicationType.name,
                intervalDays = medication.intervalDays?.toLong(),
                startDate = medication.startDate,
                endDate = medication.endDate,
                memo = medication.memo,
                isActive = if (medication.isActive) 1L else 0L,
                createdAt = medication.createdAt
            )
            db.medicationQueries.lastInsertRowId().executeAsOne()
        }

    override suspend fun update(medication: Medication) =
        withContext(Dispatchers.IO) {
            db.medicationQueries.update(
                id = medication.id,
                name = medication.name,
                dosage = medication.dosage,
                medicationType = medication.medicationType.name,
                intervalDays = medication.intervalDays?.toLong(),
                startDate = medication.startDate,
                endDate = medication.endDate,
                memo = medication.memo,
                isActive = if (medication.isActive) 1L else 0L
            )
            Unit
        }

    override suspend fun delete(id: Long) =
        withContext(Dispatchers.IO) {
            db.medicationQueries.delete(id)
            Unit
        }

    override suspend fun insertAlarm(alarm: MedicationAlarm): Long =
        withContext(Dispatchers.IO) {
            db.medicationAlarmQueries.insert(
                medicationId = alarm.medicationId,
                alarmTime = alarm.alarmTime,
                isEnabled = if (alarm.isEnabled) 1L else 0L
            )
            db.medicationAlarmQueries.lastInsertRowId().executeAsOne()
        }

    override suspend fun deleteAlarm(id: Long) =
        withContext(Dispatchers.IO) {
            db.medicationAlarmQueries.delete(id)
            Unit
        }

    override suspend fun deleteAlarmsByMedication(medicationId: Long) =
        withContext(Dispatchers.IO) {
            db.medicationAlarmQueries.deleteByMedication(medicationId)
            Unit
        }

    override fun getAlarmsByMedication(medicationId: Long): Flow<List<MedicationAlarm>> =
        db.medicationAlarmQueries.getAlarmsByMedication(medicationId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun insertLog(log: MedicationLog): Long =
        withContext(Dispatchers.IO) {
            db.medicationLogQueries.insert(
                medicationId = log.medicationId,
                scheduledAt = log.scheduledAt,
                takenAt = log.takenAt,
                isSkipped = if (log.isSkipped) 1L else 0L
            )
            db.medicationLogQueries.lastInsertRowId().executeAsOne()
        }

    override suspend fun markAsTaken(id: Long, takenAt: Long) =
        withContext(Dispatchers.IO) {
            db.medicationLogQueries.markAsTaken(takenAt = takenAt, id = id)
            Unit
        }

    override suspend fun markAsSkipped(id: Long) =
        withContext(Dispatchers.IO) {
            db.medicationLogQueries.markAsSkipped(id)
            Unit
        }

    override fun getLogsByMedication(medicationId: Long): Flow<List<MedicationLog>> =
        db.medicationLogQueries.getLogsByMedication(medicationId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    // ──────────────────────────────────────────────────────────────────
    // DB → Domain 매퍼
    // SQLDelight 가 생성한 DB 타입(Long 기반)을 도메인 모델로 변환한다.
    // ──────────────────────────────────────────────────────────────────

    /**
     * SQLDelight Medication DB 타입 → [Medication] 도메인 모델 변환.
     *
     * - medicationType: DB 에 String 으로 저장된 enum 이름 → [MedicationType] 변환.
     *   runCatching 으로 알 수 없는 값이 들어와도 [MedicationType.ONCE] 로 폴백.
     * - isActive: SQLite 에는 Boolean 없음 → 1L=true, 0L=false 로 저장.
     * - intervalDays: Long? → Int? 변환 (SQLite 는 INTEGER, 도메인은 Int).
     */
    private fun com.lastaosi.mycat.db.Medication.toDomain() = Medication(
        id = id,
        catId = catId,
        name = name,
        dosage = dosage,
        medicationType = runCatching { MedicationType.valueOf(medicationType) }.getOrDefault(MedicationType.ONCE),
        intervalDays = intervalDays?.toInt(),
        startDate = startDate,
        endDate = endDate,
        memo = memo,
        isActive = isActive == 1L,
        createdAt = createdAt
    )

    /** SQLDelight Medication_alarm DB 타입 → [MedicationAlarm] 도메인 모델 변환. */
    private fun com.lastaosi.mycat.db.Medication_alarm.toDomain() = MedicationAlarm(
        id = id,
        medicationId = medicationId,
        alarmTime = alarmTime,
        isEnabled = isEnabled == 1L
    )

    /** SQLDelight Medication_log DB 타입 → [MedicationLog] 도메인 모델 변환. */
    private fun com.lastaosi.mycat.db.Medication_log.toDomain() = MedicationLog(
        id = id,
        medicationId = medicationId,
        scheduledAt = scheduledAt,
        takenAt = takenAt,
        isSkipped = isSkipped == 1L
    )
}
