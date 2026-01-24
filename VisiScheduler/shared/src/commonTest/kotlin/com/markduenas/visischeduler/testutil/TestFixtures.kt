package com.markduenas.visischeduler.testutil

import kotlinx.datetime.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Test fixtures factory for creating consistent test data.
 * All factories use sensible defaults that can be overridden.
 */
object TestFixtures {

    // ============================================================
    // USER FIXTURES
    // ============================================================

    fun createUser(
        id: String = generateId("user"),
        email: String = "test@example.com",
        name: String = "Test User",
        role: UserRole = UserRole.VISITOR,
        permissions: Set<Permission> = role.defaultPermissions,
        isActive: Boolean = true,
        mfaEnabled: Boolean = false,
        createdAt: Instant = Clock.System.now(),
    ) = User(
        id = id,
        email = email,
        name = name,
        role = role,
        permissions = permissions,
        isActive = isActive,
        mfaEnabled = mfaEnabled,
        createdAt = createdAt,
    )

    fun createAdministrator(
        id: String = generateId("admin"),
        email: String = "admin@visischeduler.com",
        name: String = "Admin User",
    ) = createUser(
        id = id,
        email = email,
        name = name,
        role = UserRole.ADMINISTRATOR,
        mfaEnabled = true,
    )

    fun createPrimaryCoordinator(
        id: String = generateId("coord"),
        email: String = "coordinator@example.com",
        name: String = "Primary Coordinator",
        beneficiaryIds: List<String> = emptyList(),
    ) = createUser(
        id = id,
        email = email,
        name = name,
        role = UserRole.PRIMARY_COORDINATOR,
    ).copy(assignedBeneficiaryIds = beneficiaryIds)

    fun createSecondaryCoordinator(
        id: String = generateId("coord2"),
        email: String = "secondary@example.com",
        name: String = "Secondary Coordinator",
    ) = createUser(
        id = id,
        email = email,
        name = name,
        role = UserRole.SECONDARY_COORDINATOR,
    )

    fun createApprovedVisitor(
        id: String = generateId("visitor"),
        email: String = "visitor@example.com",
        name: String = "Approved Visitor",
    ) = createUser(
        id = id,
        email = email,
        name = name,
        role = UserRole.APPROVED_VISITOR,
    )

    fun createPendingVisitor(
        id: String = generateId("pending"),
        email: String = "pending@example.com",
        name: String = "Pending Visitor",
    ) = createUser(
        id = id,
        email = email,
        name = name,
        role = UserRole.PENDING_VISITOR,
    )

    // ============================================================
    // BENEFICIARY FIXTURES
    // ============================================================

    fun createBeneficiary(
        id: String = generateId("beneficiary"),
        name: String = "Test Patient",
        roomNumber: String = "101A",
        unitType: UnitType = UnitType.GENERAL,
        coordinatorIds: List<String> = emptyList(),
        isActive: Boolean = true,
    ) = Beneficiary(
        id = id,
        name = name,
        roomNumber = roomNumber,
        unitType = unitType,
        coordinatorIds = coordinatorIds,
        isActive = isActive,
    )

    // ============================================================
    // VISIT FIXTURES
    // ============================================================

    fun createVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        visitorName: String = "Test Visitor",
        status: VisitStatus = VisitStatus.PENDING,
        startTime: Instant = Clock.System.now().plus(1.hours),
        duration: Duration = 1.hours,
        visitType: VisitType = VisitType.IN_PERSON,
        numberOfGuests: Int = 0,
        reason: String? = null,
        notes: String? = null,
        createdAt: Instant = Clock.System.now(),
        approvedBy: String? = null,
        approvedAt: Instant? = null,
        denialReason: String? = null,
    ) = Visit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        visitorName = visitorName,
        status = status,
        startTime = startTime,
        endTime = startTime.plus(duration),
        visitType = visitType,
        numberOfGuests = numberOfGuests,
        reason = reason,
        notes = notes,
        createdAt = createdAt,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        denialReason = denialReason,
    )

    fun createApprovedVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        visitorName: String = "Approved Visitor",
        startTime: Instant = Clock.System.now().plus(1.hours),
        duration: Duration = 1.hours,
        approvedBy: String = generateId("coordinator"),
    ) = createVisit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        visitorName = visitorName,
        status = VisitStatus.APPROVED,
        startTime = startTime,
        duration = duration,
        approvedBy = approvedBy,
        approvedAt = Clock.System.now(),
    )

    fun createDeniedVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        denialReason: String = "Conflict with medical procedure",
    ) = createVisit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        status = VisitStatus.DENIED,
        denialReason = denialReason,
    )

    fun createCompletedVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        checkInTime: Instant = Clock.System.now().minus(2.hours),
        checkOutTime: Instant = Clock.System.now().minus(1.hours),
    ) = createVisit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        status = VisitStatus.COMPLETED,
        startTime = checkInTime,
        duration = 1.hours,
    ).copy(
        actualCheckIn = checkInTime,
        actualCheckOut = checkOutTime,
    )

    // ============================================================
    // TIME SLOT FIXTURES
    // ============================================================

    fun createTimeSlot(
        startTime: Instant = Clock.System.now().plus(1.hours),
        endTime: Instant = Clock.System.now().plus(2.hours),
        isAvailable: Boolean = true,
        capacity: Int = 3,
        currentBookings: Int = 0,
    ) = TimeSlot(
        startTime = startTime,
        endTime = endTime,
        isAvailable = isAvailable,
        capacity = capacity,
        currentBookings = currentBookings,
    )

    fun createAvailableSlots(
        date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        startHour: Int = 9,
        endHour: Int = 17,
        slotDurationMinutes: Int = 60,
        bufferMinutes: Int = 15,
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        var currentHour = startHour

        while (currentHour < endHour) {
            val startDateTime = LocalDateTime(date, LocalTime(currentHour, 0))
            val startInstant = startDateTime.toInstant(TimeZone.currentSystemDefault())
            val endInstant = startInstant.plus(slotDurationMinutes.minutes)

            slots.add(
                TimeSlot(
                    startTime = startInstant,
                    endTime = endInstant,
                    isAvailable = true,
                    capacity = 3,
                    currentBookings = 0,
                )
            )

            currentHour += (slotDurationMinutes + bufferMinutes) / 60
        }

        return slots
    }

    // ============================================================
    // RESTRICTION FIXTURES
    // ============================================================

    fun createRestriction(
        id: String = generateId("restriction"),
        beneficiaryId: String = generateId("beneficiary"),
        type: RestrictionType = RestrictionType.BLACKOUT_DATE,
        startTime: Instant? = null,
        endTime: Instant? = null,
        dayOfWeek: DayOfWeek? = null,
        visitorId: String? = null,
        reason: String = "Test restriction",
        isActive: Boolean = true,
        createdBy: String = generateId("coordinator"),
        expiresAt: Instant? = null,
    ) = Restriction(
        id = id,
        beneficiaryId = beneficiaryId,
        type = type,
        startTime = startTime,
        endTime = endTime,
        dayOfWeek = dayOfWeek,
        visitorId = visitorId,
        reason = reason,
        isActive = isActive,
        createdBy = createdBy,
        expiresAt = expiresAt,
    )

    fun createBlackoutDateRestriction(
        beneficiaryId: String = generateId("beneficiary"),
        date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.plus(DatePeriod(days = 1)),
    ): Restriction {
        val startOfDay = LocalDateTime(date, LocalTime(0, 0)).toInstant(TimeZone.currentSystemDefault())
        val endOfDay = LocalDateTime(date, LocalTime(23, 59, 59)).toInstant(TimeZone.currentSystemDefault())

        return createRestriction(
            beneficiaryId = beneficiaryId,
            type = RestrictionType.BLACKOUT_DATE,
            startTime = startOfDay,
            endTime = endOfDay,
            reason = "Full day blackout",
        )
    }

    fun createMealTimeRestriction(
        beneficiaryId: String = generateId("beneficiary"),
        startHour: Int = 12,
        startMinute: Int = 0,
        endHour: Int = 13,
        endMinute: Int = 0,
    ) = createRestriction(
        beneficiaryId = beneficiaryId,
        type = RestrictionType.MEAL_TIME,
        reason = "Meal time - no visitors",
    ).copy(
        recurringStartTime = LocalTime(startHour, startMinute),
        recurringEndTime = LocalTime(endHour, endMinute),
    )

    fun createVisitorBlockRestriction(
        beneficiaryId: String = generateId("beneficiary"),
        blockedVisitorId: String,
        reason: String = "Visitor not permitted",
    ) = createRestriction(
        beneficiaryId = beneficiaryId,
        type = RestrictionType.VISITOR_BLOCKED,
        visitorId = blockedVisitorId,
        reason = reason,
    )

    fun createProcedureBlockRestriction(
        beneficiaryId: String = generateId("beneficiary"),
        procedureTime: Instant = Clock.System.now().plus(1.hours),
        recoveryDuration: Duration = 2.hours,
    ) = createRestriction(
        beneficiaryId = beneficiaryId,
        type = RestrictionType.MEDICAL_PROCEDURE,
        startTime = procedureTime,
        endTime = procedureTime.plus(recoveryDuration),
        reason = "Medical procedure and recovery",
    )

    // ============================================================
    // SCHEDULE REQUEST FIXTURES
    // ============================================================

    fun createScheduleVisitRequest(
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        preferredSlots: List<TimeSlot> = listOf(createTimeSlot()),
        duration: Duration = 1.hours,
        visitType: VisitType = VisitType.IN_PERSON,
        numberOfGuests: Int = 0,
        reason: String? = "Regular visit",
    ) = ScheduleVisitRequest(
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        preferredSlots = preferredSlots,
        requestedDuration = duration,
        visitType = visitType,
        numberOfGuests = numberOfGuests,
        reason = reason,
    )

    // ============================================================
    // APPROVAL FIXTURES
    // ============================================================

    fun createApprovalRequest(
        visitId: String = generateId("visit"),
        coordinatorId: String = generateId("coordinator"),
        action: ApprovalAction = ApprovalAction.APPROVE,
        notes: String? = null,
    ) = ApprovalRequest(
        visitId = visitId,
        coordinatorId = coordinatorId,
        action = action,
        notes = notes,
    )

    // ============================================================
    // CREDENTIALS & AUTH FIXTURES
    // ============================================================

    fun createCredentials(
        email: String = "test@example.com",
        password: String = "SecurePassword123!",
    ) = Credentials(
        email = email,
        password = password,
    )

    fun createInvalidCredentials() = createCredentials(
        email = "invalid@example.com",
        password = "wrongpassword",
    )

    fun createAuthToken(
        accessToken: String = "test-access-token-${generateId("token")}",
        refreshToken: String = "test-refresh-token-${generateId("token")}",
        expiresAt: Instant = Clock.System.now().plus(1.hours),
        userId: String = generateId("user"),
    ) = AuthToken(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresAt = expiresAt,
        userId = userId,
    )

    fun createMfaChallenge(
        challengeId: String = generateId("mfa"),
        userId: String = generateId("user"),
        method: MfaMethod = MfaMethod.TOTP,
        expiresAt: Instant = Clock.System.now().plus(5.minutes),
    ) = MfaChallenge(
        challengeId = challengeId,
        userId = userId,
        method = method,
        expiresAt = expiresAt,
    )

    // ============================================================
    // SESSION FIXTURES
    // ============================================================

    fun createSession(
        id: String = generateId("session"),
        userId: String = generateId("user"),
        token: AuthToken = createAuthToken(userId = userId),
        createdAt: Instant = Clock.System.now(),
        lastActivityAt: Instant = Clock.System.now(),
        isActive: Boolean = true,
        deviceInfo: DeviceInfo? = null,
    ) = Session(
        id = id,
        userId = userId,
        token = token,
        createdAt = createdAt,
        lastActivityAt = lastActivityAt,
        isActive = isActive,
        deviceInfo = deviceInfo,
    )

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    private var idCounter = 0

    fun generateId(prefix: String = "test"): String {
        return "$prefix-${++idCounter}-${Clock.System.now().toEpochMilliseconds()}"
    }

    fun resetIdCounter() {
        idCounter = 0
    }

    /**
     * Creates a future instant by adding hours to current time
     */
    fun futureInstant(hoursFromNow: Int): Instant =
        Clock.System.now().plus(hoursFromNow.hours)

    /**
     * Creates a past instant by subtracting hours from current time
     */
    fun pastInstant(hoursAgo: Int): Instant =
        Clock.System.now().minus(hoursAgo.hours)
}

// ============================================================
// DATA CLASSES FOR TEST FIXTURES
// (These would normally be in the domain layer)
// ============================================================

enum class UserRole {
    ADMINISTRATOR,
    PRIMARY_COORDINATOR,
    SECONDARY_COORDINATOR,
    APPROVED_VISITOR,
    PENDING_VISITOR;

    val defaultPermissions: Set<Permission>
        get() = when (this) {
            ADMINISTRATOR -> Permission.entries.toSet()
            PRIMARY_COORDINATOR -> setOf(
                Permission.VIEW_SCHEDULE,
                Permission.CREATE_VISIT,
                Permission.MODIFY_VISIT,
                Permission.APPROVE_VISIT,
                Permission.DENY_VISIT,
                Permission.MANAGE_RESTRICTIONS,
                Permission.VIEW_VISITORS,
                Permission.MANAGE_VISITORS,
            )
            SECONDARY_COORDINATOR -> setOf(
                Permission.VIEW_SCHEDULE,
                Permission.CREATE_VISIT,
                Permission.VIEW_VISITORS,
            )
            APPROVED_VISITOR -> setOf(
                Permission.VIEW_SCHEDULE,
                Permission.CREATE_VISIT,
                Permission.VIEW_OWN_VISITS,
            )
            PENDING_VISITOR -> setOf(
                Permission.VIEW_OWN_VISITS,
            )
        }
}

enum class Permission {
    VIEW_SCHEDULE,
    CREATE_VISIT,
    MODIFY_VISIT,
    CANCEL_VISIT,
    APPROVE_VISIT,
    DENY_VISIT,
    VIEW_OWN_VISITS,
    VIEW_ALL_VISITS,
    VIEW_VISITORS,
    MANAGE_VISITORS,
    MANAGE_RESTRICTIONS,
    MANAGE_USERS,
    ADMIN_ACCESS,
}

enum class VisitStatus {
    PENDING,
    APPROVED,
    DENIED,
    CANCELLED,
    CHECKED_IN,
    COMPLETED,
    NO_SHOW,
    WAITLISTED,
}

enum class VisitType {
    IN_PERSON,
    VIRTUAL,
    HYBRID,
}

enum class UnitType {
    GENERAL,
    ICU,
    PEDIATRIC,
    MATERNITY,
    PSYCHIATRIC,
}

enum class RestrictionType {
    BLACKOUT_DATE,
    BLACKOUT_HOURS,
    MEDICAL_PROCEDURE,
    MEAL_TIME,
    REST_PERIOD,
    VISITOR_BLOCKED,
    VISITOR_CONDITIONAL,
    CAPACITY_LIMIT,
    AGE_RESTRICTION,
}

enum class MfaMethod {
    TOTP,
    SMS,
    EMAIL,
}

enum class ApprovalAction {
    APPROVE,
    DENY,
    REQUEST_INFO,
    DELEGATE,
}

// Simplified data classes for testing
data class User(
    val id: String,
    val email: String,
    val name: String,
    val role: UserRole,
    val permissions: Set<Permission>,
    val isActive: Boolean,
    val mfaEnabled: Boolean,
    val createdAt: Instant,
    val assignedBeneficiaryIds: List<String> = emptyList(),
)

data class Beneficiary(
    val id: String,
    val name: String,
    val roomNumber: String,
    val unitType: UnitType,
    val coordinatorIds: List<String>,
    val isActive: Boolean,
)

data class Visit(
    val id: String,
    val beneficiaryId: String,
    val visitorId: String,
    val visitorName: String,
    val status: VisitStatus,
    val startTime: Instant,
    val endTime: Instant,
    val visitType: VisitType,
    val numberOfGuests: Int,
    val reason: String?,
    val notes: String?,
    val createdAt: Instant,
    val approvedBy: String?,
    val approvedAt: Instant?,
    val denialReason: String?,
    val actualCheckIn: Instant? = null,
    val actualCheckOut: Instant? = null,
)

data class TimeSlot(
    val startTime: Instant,
    val endTime: Instant,
    val isAvailable: Boolean,
    val capacity: Int,
    val currentBookings: Int,
)

data class Restriction(
    val id: String,
    val beneficiaryId: String,
    val type: RestrictionType,
    val startTime: Instant?,
    val endTime: Instant?,
    val dayOfWeek: DayOfWeek?,
    val visitorId: String?,
    val reason: String,
    val isActive: Boolean,
    val createdBy: String,
    val expiresAt: Instant?,
    val recurringStartTime: LocalTime? = null,
    val recurringEndTime: LocalTime? = null,
)

data class ScheduleVisitRequest(
    val beneficiaryId: String,
    val visitorId: String,
    val preferredSlots: List<TimeSlot>,
    val requestedDuration: Duration,
    val visitType: VisitType,
    val numberOfGuests: Int,
    val reason: String?,
)

data class ApprovalRequest(
    val visitId: String,
    val coordinatorId: String,
    val action: ApprovalAction,
    val notes: String?,
)

data class Credentials(
    val email: String,
    val password: String,
)

data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
    val userId: String,
)

data class MfaChallenge(
    val challengeId: String,
    val userId: String,
    val method: MfaMethod,
    val expiresAt: Instant,
)

data class Session(
    val id: String,
    val userId: String,
    val token: AuthToken,
    val createdAt: Instant,
    val lastActivityAt: Instant,
    val isActive: Boolean,
    val deviceInfo: DeviceInfo?,
)

data class DeviceInfo(
    val deviceId: String,
    val platform: String,
    val appVersion: String,
)
