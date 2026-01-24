package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID
import kotlin.time.Duration.Companion.hours

/**
 * Firestore implementation of CheckInRepository.
 */
class FirestoreCheckInRepository(
    private val firestore: FirestoreDatabase
) : CheckInRepository {

    override suspend fun checkIn(visitId: String, method: CheckInMethod): Result<CheckIn> = runCatching {
        // Verify no active check-in exists
        val activeCheckIn = firestore.getActiveCheckIn(visitId)
        if (activeCheckIn != null) {
            throw Exception("Already checked in for this visit")
        }

        val data = mapOf(
            "visitId" to visitId,
            "checkInTime" to Timestamp.now(),
            "checkOutTime" to null,
            "method" to method.name,
            "notes" to null,
            "rating" to null
        )

        val id = firestore.createCheckIn(data)

        // Update visit status
        firestore.updateVisit(visitId, mapOf(
            "status" to VisitStatus.CHECKED_IN.name,
            "checkInTime" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        ))

        CheckIn(
            id = id,
            visitId = visitId,
            checkInTime = Instant.fromEpochMilliseconds(System.currentTimeMillis()),
            checkOutTime = null,
            method = method,
            notes = null,
            rating = null
        )
    }

    override suspend fun checkOut(
        checkInId: String,
        notes: String?,
        rating: Int?
    ): Result<CheckIn> = runCatching {
        val updates = mutableMapOf<String, Any?>(
            "checkOutTime" to Timestamp.now(),
            "notes" to notes,
            "rating" to rating
        )

        firestore.updateCheckIn(checkInId, updates.filterValues { it != null }.mapValues { it.value!! })

        val checkIn = firestore.getById(FirestoreDatabase.COLLECTION_CHECK_INS, checkInId)?.toCheckIn()
            ?: throw Exception("Check-in not found")

        // Update visit status
        firestore.updateVisit(checkIn.visitId, mapOf(
            "status" to VisitStatus.COMPLETED.name,
            "checkOutTime" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        ))

        checkIn
    }

    override suspend fun generateQrCode(visitId: String): Result<QrCodeData> = runCatching {
        val visit = firestore.getVisit(visitId)?.let { doc ->
            doc.getString("visitorId") ?: throw Exception("Visit not found")
        } ?: throw Exception("Visit not found")

        val now = Clock.System.now()
        val validFrom = now
        val validUntil = now.plus(24.hours)

        val dataToSign = "$visitId|$visit|${validFrom.toEpochMilliseconds()}|${validUntil.toEpochMilliseconds()}"
        val signature = generateSignature(dataToSign)

        QrCodeData(
            visitId = visitId,
            visitorId = visit,
            validFrom = validFrom,
            validUntil = validUntil,
            signature = signature
        )
    }

    override suspend fun validateQrCode(qrData: String): Result<QrValidationResult> = runCatching {
        try {
            val qrCodeData = Json.decodeFromString<QrCodeData>(qrData)
            val now = Clock.System.now()

            // Check if expired
            if (qrCodeData.isExpired(now)) {
                return@runCatching QrValidationResult.Expired(qrCodeData.validUntil)
            }

            // Check if not yet valid
            if (qrCodeData.isNotYetValid(now)) {
                return@runCatching QrValidationResult.NotYetValid(qrCodeData.validFrom)
            }

            // Verify signature
            val dataToSign = "${qrCodeData.visitId}|${qrCodeData.visitorId}|${qrCodeData.validFrom.toEpochMilliseconds()}|${qrCodeData.validUntil.toEpochMilliseconds()}"
            val expectedSignature = generateSignature(dataToSign)
            if (qrCodeData.signature != expectedSignature) {
                return@runCatching QrValidationResult.InvalidSignature("Invalid QR code signature")
            }

            // Check if already checked in
            val activeCheckIn = firestore.getActiveCheckIn(qrCodeData.visitId)
            if (activeCheckIn != null) {
                val checkIn = activeCheckIn.toCheckIn()
                    ?: return@runCatching QrValidationResult.InvalidSignature("Failed to parse check-in")
                return@runCatching QrValidationResult.AlreadyCheckedIn(checkIn)
            }

            // Get visit
            val visitDoc = firestore.getVisit(qrCodeData.visitId)
                ?: return@runCatching QrValidationResult.VisitNotFound(qrCodeData.visitId)

            val visit = visitDoc.toVisit()
                ?: return@runCatching QrValidationResult.VisitNotFound(qrCodeData.visitId)

            // Check if cancelled
            if (visit.status == VisitStatus.CANCELLED) {
                return@runCatching QrValidationResult.VisitCancelled(visit)
            }

            QrValidationResult.Valid(visit)
        } catch (e: Exception) {
            QrValidationResult.InvalidSignature("Failed to parse QR code: ${e.message}")
        }
    }

    override fun getActiveCheckIn(visitId: String): Flow<CheckIn?> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_CHECK_INS,
            "visitId",
            visitId
        ).map { docs ->
            docs.mapNotNull { it.toCheckIn() }
                .find { it.isActive }
        }
    }

    override suspend fun getCheckInById(checkInId: String): Result<CheckIn> = runCatching {
        firestore.getById(FirestoreDatabase.COLLECTION_CHECK_INS, checkInId)?.toCheckIn()
            ?: throw Exception("Check-in not found")
    }

    override fun getCheckInsForVisit(visitId: String): Flow<List<CheckIn>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_CHECK_INS,
            "visitId",
            visitId
        ).map { docs -> docs.mapNotNull { it.toCheckIn() } }
    }

    override fun getTodayExpectedVisitors(): Flow<List<ExpectedVisitor>> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return getExpectedVisitorsForDate(today)
    }

    override fun getExpectedVisitorsForDate(date: LocalDate): Flow<List<ExpectedVisitor>> {
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_VISITS,
            "scheduledDate",
            date.toString()
        ).map { docs ->
            docs.mapNotNull { doc ->
                val visit = doc.toVisit() ?: return@mapNotNull null

                // Determine status based on visit and check-in state
                val status = when (visit.status) {
                    VisitStatus.CHECKED_IN -> ExpectedVisitorStatus.CHECKED_IN
                    VisitStatus.COMPLETED -> ExpectedVisitorStatus.CHECKED_OUT
                    VisitStatus.NO_SHOW -> ExpectedVisitorStatus.NO_SHOW
                    else -> {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        if (now.time > visit.endTime) {
                            ExpectedVisitorStatus.LATE
                        } else {
                            ExpectedVisitorStatus.NOT_ARRIVED
                        }
                    }
                }

                ExpectedVisitor(
                    visit = visit,
                    visitorName = "Visitor", // Would need to fetch from users
                    visitorPhotoUrl = null,
                    beneficiaryName = "Beneficiary", // Would need to fetch
                    beneficiaryRoom = null,
                    checkInStatus = status
                )
            }
        }
    }

    override suspend fun generateVisitorBadge(checkInId: String): Result<VisitorBadge> = runCatching {
        val checkIn = getCheckInById(checkInId).getOrThrow()
        val visitDoc = firestore.getVisit(checkIn.visitId)
            ?: throw Exception("Visit not found")
        val visit = visitDoc.toVisit()
            ?: throw Exception("Failed to parse visit")

        val validUntil = checkIn.checkInTime.plus(4.hours) // 4 hour badge validity
        val qrCodeData = generateQrCode(visit.id).getOrThrow()

        VisitorBadge(
            visit = visit,
            visitorName = "Visitor", // Would fetch from users collection
            visitorPhotoUrl = null,
            beneficiaryName = "Beneficiary", // Would fetch
            beneficiaryRoom = null,
            checkInTime = checkIn.checkInTime,
            validUntil = validUntil,
            qrCodeData = qrCodeData,
            badgeNumber = UUID.randomUUID().toString().take(8).uppercase()
        )
    }

    override suspend fun verifyBadge(badgeQrData: String): Result<VisitorBadge> = runCatching {
        val qrCodeData = Json.decodeFromString<QrCodeData>(badgeQrData)
        val now = Clock.System.now()

        if (!qrCodeData.isValid(now)) {
            throw Exception("Badge has expired")
        }

        // Get active check-in
        val activeCheckIn = firestore.getActiveCheckIn(qrCodeData.visitId)?.toCheckIn()
            ?: throw Exception("No active check-in found")

        generateVisitorBadge(activeCheckIn.id).getOrThrow()
    }

    override suspend fun getCheckInStatistics(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<CheckInStatistics> = runCatching {
        val checkIns = firestore.getAll(FirestoreDatabase.COLLECTION_CHECK_INS)
            .mapNotNull { it.toCheckIn() }
            .filter { checkIn ->
                val checkInDate = checkIn.checkInTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
                checkInDate in startDate..endDate
            }

        val totalCheckIns = checkIns.size
        val qrCodeCheckIns = checkIns.count { it.method == CheckInMethod.QR_CODE }
        val manualCheckIns = checkIns.count { it.method == CheckInMethod.MANUAL }
        val automaticCheckIns = checkIns.count { it.method == CheckInMethod.AUTOMATIC }

        val completedCheckIns = checkIns.filter { it.isCheckedOut }
        val averageDuration = if (completedCheckIns.isNotEmpty()) {
            completedCheckIns.mapNotNull { it.durationMillis }
                .average()
                .toLong() / 60000 // Convert to minutes
        } else 0

        val ratings = checkIns.mapNotNull { it.rating }
        val averageRating = if (ratings.isNotEmpty()) ratings.average().toFloat() else null

        CheckInStatistics(
            totalCheckIns = totalCheckIns,
            qrCodeCheckIns = qrCodeCheckIns,
            manualCheckIns = manualCheckIns,
            automaticCheckIns = automaticCheckIns,
            averageVisitDurationMinutes = averageDuration.toInt(),
            averageRating = averageRating,
            onTimePercentage = 0.85f, // Would calculate based on scheduled vs actual times
            latePercentage = 0.10f,
            noShowCount = 0 // Would need to track
        )
    }

    override suspend fun syncCheckIns(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    // ==================== Helper Functions ====================

    private fun generateSignature(data: String): String {
        val secretKey = "VisiScheduler-SecretKey" // In production, use secure key management
        val combined = "$data|$secretKey"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(combined.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    // ==================== Mapping Functions ====================

    private fun DocumentSnapshot.toCheckIn(): CheckIn? {
        return try {
            CheckIn(
                id = id,
                visitId = getString("visitId") ?: return null,
                checkInTime = getTimestamp("checkInTime")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: return null,
                checkOutTime = getTimestamp("checkOutTime")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                method = CheckInMethod.valueOf(getString("method") ?: "MANUAL"),
                notes = getString("notes"),
                rating = getLong("rating")?.toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toVisit(): Visit? {
        return try {
            Visit(
                id = id,
                beneficiaryId = getString("beneficiaryId") ?: return null,
                visitorId = getString("visitorId") ?: return null,
                scheduledDate = LocalDate.parse(getString("scheduledDate") ?: return null),
                startTime = LocalTime.parse(getString("startTime") ?: return null),
                endTime = LocalTime.parse(getString("endTime") ?: return null),
                status = VisitStatus.valueOf(getString("status") ?: "PENDING"),
                visitType = VisitType.valueOf(getString("visitType") ?: "IN_PERSON"),
                purpose = getString("purpose"),
                notes = getString("notes"),
                additionalVisitors = (get("additionalVisitors") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                checkInTime = getString("checkInTime")?.let { Instant.parse(it) },
                checkOutTime = getString("checkOutTime")?.let { Instant.parse(it) },
                approvedBy = getString("approvedBy"),
                approvedAt = getTimestamp("approvedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                denialReason = getString("denialReason"),
                cancellationReason = getString("cancellationReason"),
                cancelledBy = getString("cancelledBy"),
                cancelledAt = getTimestamp("cancelledAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                },
                createdAt = getTimestamp("createdAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                updatedAt = getTimestamp("updatedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
