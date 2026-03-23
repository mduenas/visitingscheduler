package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.CheckIn
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import com.markduenas.visischeduler.domain.entities.ExpectedVisitor
import com.markduenas.visischeduler.domain.entities.ExpectedVisitorStatus
import com.markduenas.visischeduler.domain.entities.QrCodeData
import com.markduenas.visischeduler.domain.entities.QrValidationResult
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.domain.entities.VisitorBadge
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.CheckInStatistics
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Cross-platform Firestore implementation of CheckInRepository.
 */
class CommonFirestoreCheckInRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth
) : CheckInRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override suspend fun checkIn(visitId: String, method: CheckInMethod): Result<CheckIn> = runCatching {
        val now = Clock.System.now()

        val checkInData = mapOf<String, Any?>(
            "visitId" to visitId,
            "checkInTime" to firestore.serverTimestamp(),
            "method" to method.name,
            "createdAt" to firestore.serverTimestamp()
        )

        val checkInId = firestore.createCheckIn(checkInData)

        CheckIn(
            id = checkInId,
            visitId = visitId,
            checkInTime = now,
            checkOutTime = null,
            method = method,
            notes = null,
            rating = null
        )
    }

    override suspend fun checkOut(checkInId: String, notes: String?, rating: Int?, moodLevel: Int?, energyLevel: Int?): Result<CheckIn> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "checkOutTime" to firestore.serverTimestamp()
        )
        notes?.let { updates["notes"] = it }
        rating?.let { updates["rating"] = it }

        firestore.updateCheckIn(checkInId, updates)

        firestore.getCheckIn(checkInId)?.toCheckIn()
            ?: throw Exception("CheckIn not found after checkout")
    }

    override suspend fun generateQrCode(visitId: String): Result<QrCodeData> = runCatching {
        val visit = firestore.getVisit(visitId)
            ?: throw Exception("Visit not found")

        val now = Clock.System.now()
        val validUntil = Instant.fromEpochMilliseconds(
            now.toEpochMilliseconds() + 24 * 60 * 60 * 1000 // 24 hours
        )

        QrCodeData(
            visitId = visitId,
            visitorId = visit.get("visitorId") ?: "",
            validFrom = now,
            validUntil = validUntil,
            signature = generateSignature(visitId, now)
        )
    }

    override suspend fun validateQrCode(qrData: String): Result<QrValidationResult> = runCatching {
        // Parse QR data and validate
        // In a real implementation, this would decrypt/verify the token
        val visit = firestore.getVisit(qrData)?.toVisit()
        if (visit != null) {
            QrValidationResult.Valid(visit)
        } else {
            QrValidationResult.VisitNotFound(qrData)
        }
    }

    override fun getActiveCheckIn(visitId: String): Flow<CheckIn?> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_CHECK_INS,
            "visitId",
            visitId
        ).map { docs ->
            docs.mapNotNull { it.toCheckIn() }
                .firstOrNull { it.checkOutTime == null }
        }
    }

    override suspend fun getCheckInById(checkInId: String): Result<CheckIn> = runCatching {
        firestore.getCheckIn(checkInId)?.toCheckIn()
            ?: throw Exception("CheckIn not found")
    }

    override fun getCheckInsForVisit(visitId: String): Flow<List<CheckIn>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_CHECK_INS,
            "visitId",
            visitId
        ).map { docs -> docs.mapNotNull { it.toCheckIn() } }
    }

    override fun getTodayExpectedVisitors(): Flow<List<ExpectedVisitor>> {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        return getExpectedVisitorsForDate(today)
    }

    override fun getExpectedVisitorsForDate(date: LocalDate): Flow<List<ExpectedVisitor>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_VISITS,
            "scheduledDate",
            date.toString()
        ).map { docs ->
            docs.mapNotNull { doc ->
                try {
                    val visit = doc.toVisit() ?: return@mapNotNull null
                    ExpectedVisitor(
                        visit = visit,
                        visitorName = "", // Would need to fetch from users
                        visitorPhotoUrl = null,
                        beneficiaryName = "", // Would need to fetch from beneficiaries
                        beneficiaryRoom = null,
                        checkInStatus = ExpectedVisitorStatus.NOT_ARRIVED
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    override suspend fun generateVisitorBadge(checkInId: String): Result<VisitorBadge> = runCatching {
        val checkIn = firestore.getCheckIn(checkInId)?.toCheckIn()
            ?: throw Exception("CheckIn not found")

        val visit = firestore.getVisit(checkIn.visitId)?.toVisit()
            ?: throw Exception("Visit not found")

        val now = Clock.System.now()
        val validUntil = Instant.fromEpochMilliseconds(
            now.toEpochMilliseconds() + 8 * 60 * 60 * 1000 // 8 hours
        )

        val qrCodeData = QrCodeData(
            visitId = checkIn.visitId,
            visitorId = visit.visitorId,
            validFrom = checkIn.checkInTime,
            validUntil = validUntil,
            signature = generateSignature(checkIn.visitId, checkIn.checkInTime)
        )

        VisitorBadge(
            visit = visit,
            visitorName = "", // Would fetch from user
            visitorPhotoUrl = null,
            beneficiaryName = "", // Would fetch from beneficiary
            beneficiaryRoom = null,
            checkInTime = checkIn.checkInTime,
            validUntil = validUntil,
            qrCodeData = qrCodeData,
            badgeNumber = generateBadgeNumber()
        )
    }

    override suspend fun verifyBadge(badgeQrData: String): Result<VisitorBadge> = runCatching {
        val parts = badgeQrData.split(":")
        val checkInId = parts.getOrNull(0) ?: throw Exception("Invalid badge")
        generateVisitorBadge(checkInId).getOrThrow()
    }

    override suspend fun getCheckInStatistics(startDate: LocalDate, endDate: LocalDate): Result<CheckInStatistics> = runCatching {
        val allCheckIns = firestore.getAll(FirestoreDatabase.COLLECTION_CHECK_INS)
            .mapNotNull { it.toCheckIn() }

        val filtered = allCheckIns.filter { checkIn ->
            val date = checkIn.checkInTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
            date in startDate..endDate
        }

        CheckInStatistics(
            totalCheckIns = filtered.size,
            qrCodeCheckIns = filtered.count { it.method == CheckInMethod.QR_CODE },
            manualCheckIns = filtered.count { it.method == CheckInMethod.MANUAL },
            automaticCheckIns = filtered.count { it.method == CheckInMethod.AUTOMATIC },
            averageVisitDurationMinutes = 60, // Simplified
            averageRating = filtered.mapNotNull { it.rating }.average().toFloat().takeIf { !it.isNaN() },
            onTimePercentage = 0.85f, // Simplified
            latePercentage = 0.10f,
            noShowCount = 0
        )
    }

    override suspend fun syncCheckIns(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    private fun generateSignature(visitId: String, timestamp: Instant): String {
        // Simplified signature generation
        return "${visitId}_${timestamp.toEpochMilliseconds()}_${generateToken()}"
    }

    private fun generateToken(): String {
        return (1..32).map { ('a'..'z').random() }.joinToString("")
    }

    private fun generateBadgeNumber(): String {
        return "B${(10000..99999).random()}"
    }

    private fun DocumentSnapshot.toCheckIn(): CheckIn? {
        return try {
            CheckIn(
                id = id,
                visitId = get("visitId") ?: return null,
                checkInTime = get<Long?>("checkInTime")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                checkOutTime = get<Long?>("checkOutTime")?.let { Instant.fromEpochMilliseconds(it) },
                method = try {
                    CheckInMethod.valueOf(get("method") ?: "MANUAL")
                } catch (e: Exception) {
                    CheckInMethod.MANUAL
                },
                notes = get("notes"),
                rating = get<Long?>("rating")?.toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toVisit(): Visit? {
        return try {
            val now = Clock.System.now()
            Visit(
                id = id,
                beneficiaryId = get("beneficiaryId") ?: return null,
                visitorId = get("visitorId") ?: return null,
                scheduledDate = LocalDate.parse(get("scheduledDate") ?: return null),
                startTime = LocalTime.parse(get("startTime") ?: return null),
                endTime = LocalTime.parse(get("endTime") ?: return null),
                status = try {
                    VisitStatus.valueOf(get("status") ?: "PENDING")
                } catch (e: Exception) {
                    VisitStatus.PENDING
                },
                visitType = try {
                    VisitType.valueOf(get("visitType") ?: "IN_PERSON")
                } catch (e: Exception) {
                    VisitType.IN_PERSON
                },
                purpose = get("purpose"),
                notes = get("notes"),
                additionalVisitors = emptyList(), // Simplified - would parse from Firestore
                checkInTime = get<Long?>("checkInTime")?.let { Instant.fromEpochMilliseconds(it) },
                checkOutTime = get<Long?>("checkOutTime")?.let { Instant.fromEpochMilliseconds(it) },
                approvedBy = get("approvedBy"),
                approvedAt = get<Long?>("approvedAt")?.let { Instant.fromEpochMilliseconds(it) },
                denialReason = get("denialReason"),
                cancellationReason = get("cancellationReason"),
                cancelledBy = get("cancelledBy"),
                cancelledAt = get<Long?>("cancelledAt")?.let { Instant.fromEpochMilliseconds(it) },
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) } ?: now,
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) } ?: now
            )
        } catch (e: Exception) {
            null
        }
    }
}
