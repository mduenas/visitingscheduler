package com.markduenas.visischeduler.testutil

import com.markduenas.visischeduler.domain.entities.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Test fixtures factory for creating consistent test data using actual domain entities.
 * All factories use sensible defaults that can be overridden.
 */
object TestFixtures {

    private val now: Instant get() = Clock.System.now()
    private val today: LocalDate get() = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

    // ============================================================
    // USER FIXTURES
    // ============================================================

    fun createUser(
        id: String = generateId("user"),
        email: String = "test@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        role: Role = Role.APPROVED_VISITOR,
        phoneNumber: String? = null,
        profileImageUrl: String? = null,
        isActive: Boolean = true,
        isEmailVerified: Boolean = true,
        createdAt: Instant = now,
        updatedAt: Instant = now,
        lastLoginAt: Instant? = null,
        associatedBeneficiaryIds: List<String> = emptyList(),
        notificationPreferences: NotificationPreferences = NotificationPreferences()
    ) = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role,
        phoneNumber = phoneNumber,
        profileImageUrl = profileImageUrl,
        isActive = isActive,
        isEmailVerified = isEmailVerified,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastLoginAt = lastLoginAt,
        associatedBeneficiaryIds = associatedBeneficiaryIds,
        notificationPreferences = notificationPreferences
    )

    fun createAdmin(
        id: String = generateId("admin"),
        email: String = "admin@visischeduler.com",
        firstName: String = "Admin",
        lastName: String = "User"
    ) = createUser(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = Role.ADMIN
    )

    fun createPrimaryCoordinator(
        id: String = generateId("coord"),
        email: String = "coordinator@example.com",
        firstName: String = "Primary",
        lastName: String = "Coordinator",
        associatedBeneficiaryIds: List<String> = emptyList()
    ) = createUser(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = Role.PRIMARY_COORDINATOR,
        associatedBeneficiaryIds = associatedBeneficiaryIds
    )

    fun createSecondaryCoordinator(
        id: String = generateId("coord2"),
        email: String = "secondary@example.com",
        firstName: String = "Secondary",
        lastName: String = "Coordinator"
    ) = createUser(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = Role.SECONDARY_COORDINATOR
    )

    fun createApprovedVisitor(
        id: String = generateId("visitor"),
        email: String = "visitor@example.com",
        firstName: String = "Approved",
        lastName: String = "Visitor",
        associatedBeneficiaryIds: List<String> = emptyList()
    ) = createUser(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = Role.APPROVED_VISITOR,
        associatedBeneficiaryIds = associatedBeneficiaryIds
    )

    fun createPendingVisitor(
        id: String = generateId("pending"),
        email: String = "pending@example.com",
        firstName: String = "Pending",
        lastName: String = "Visitor"
    ) = createUser(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = Role.PENDING_VISITOR,
        isEmailVerified = false
    )

    // ============================================================
    // BENEFICIARY FIXTURES
    // ============================================================

    fun createBeneficiary(
        id: String = generateId("beneficiary"),
        firstName: String = "Test",
        lastName: String = "Patient",
        dateOfBirth: LocalDate? = null,
        facilityId: String = generateId("facility"),
        roomNumber: String? = "101A",
        status: BeneficiaryStatus = BeneficiaryStatus.ACTIVE,
        specialInstructions: String? = null,
        maxVisitorsPerSlot: Int = 2,
        maxVisitsPerDay: Int = 2,
        maxVisitsPerWeek: Int = 7,
        photoUrl: String? = null,
        emergencyContact: EmergencyContact? = null,
        restrictions: List<String> = emptyList(),
        createdAt: Instant = now,
        updatedAt: Instant = now
    ) = Beneficiary(
        id = id,
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        facilityId = facilityId,
        roomNumber = roomNumber,
        status = status,
        specialInstructions = specialInstructions,
        maxVisitorsPerSlot = maxVisitorsPerSlot,
        maxVisitsPerDay = maxVisitsPerDay,
        maxVisitsPerWeek = maxVisitsPerWeek,
        photoUrl = photoUrl,
        emergencyContact = emergencyContact,
        restrictions = restrictions,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    // ============================================================
    // VISIT FIXTURES
    // ============================================================

    fun createVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        scheduledDate: LocalDate = today.plus(DatePeriod(days = 1)),
        startTime: LocalTime = LocalTime(10, 0),
        endTime: LocalTime = LocalTime(11, 0),
        status: VisitStatus = VisitStatus.PENDING,
        visitType: VisitType = VisitType.IN_PERSON,
        purpose: String? = "Regular visit",
        notes: String? = null,
        additionalVisitors: List<AdditionalVisitor> = emptyList(),
        checkInTime: Instant? = null,
        checkOutTime: Instant? = null,
        approvedBy: String? = null,
        approvedAt: Instant? = null,
        denialReason: String? = null,
        cancellationReason: String? = null,
        cancelledBy: String? = null,
        cancelledAt: Instant? = null,
        createdAt: Instant = now,
        updatedAt: Instant = now
    ) = Visit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        scheduledDate = scheduledDate,
        startTime = startTime,
        endTime = endTime,
        status = status,
        visitType = visitType,
        purpose = purpose,
        notes = notes,
        additionalVisitors = additionalVisitors,
        checkInTime = checkInTime,
        checkOutTime = checkOutTime,
        approvedBy = approvedBy,
        approvedAt = approvedAt,
        denialReason = denialReason,
        cancellationReason = cancellationReason,
        cancelledBy = cancelledBy,
        cancelledAt = cancelledAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun createApprovedVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        scheduledDate: LocalDate = today.plus(DatePeriod(days = 1)),
        startTime: LocalTime = LocalTime(10, 0),
        endTime: LocalTime = LocalTime(11, 0),
        approvedBy: String = generateId("coordinator")
    ) = createVisit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        scheduledDate = scheduledDate,
        startTime = startTime,
        endTime = endTime,
        status = VisitStatus.APPROVED,
        approvedBy = approvedBy,
        approvedAt = now
    )

    fun createDeniedVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        denialReason: String = "Conflict with medical procedure"
    ) = createVisit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        status = VisitStatus.DENIED,
        denialReason = denialReason
    )

    fun createCompletedVisit(
        id: String = generateId("visit"),
        beneficiaryId: String = generateId("beneficiary"),
        visitorId: String = generateId("visitor"),
        scheduledDate: LocalDate = today,
        checkInTime: Instant = now.minus(2.hours),
        checkOutTime: Instant = now.minus(1.hours)
    ) = createVisit(
        id = id,
        beneficiaryId = beneficiaryId,
        visitorId = visitorId,
        scheduledDate = scheduledDate,
        status = VisitStatus.COMPLETED,
        checkInTime = checkInTime,
        checkOutTime = checkOutTime
    )

    fun createAdditionalVisitor(
        id: String = generateId("additional"),
        firstName: String = "Additional",
        lastName: String = "Visitor",
        relationship: String = "Friend",
        isMinor: Boolean = false,
        age: Int? = null
    ) = AdditionalVisitor(
        id = id,
        firstName = firstName,
        lastName = lastName,
        relationship = relationship,
        isMinor = isMinor,
        age = age
    )

    // ============================================================
    // TIME SLOT FIXTURES
    // ============================================================

    fun createTimeSlot(
        id: String = generateId("slot"),
        facilityId: String = generateId("facility"),
        date: LocalDate = today.plus(DatePeriod(days = 1)),
        startTime: LocalTime = LocalTime(10, 0),
        endTime: LocalTime = LocalTime(11, 0),
        maxCapacity: Int = 3,
        currentBookings: Int = 0,
        isAvailable: Boolean = true,
        slotType: SlotType = SlotType.REGULAR,
        notes: String? = null
    ) = TimeSlot(
        id = id,
        facilityId = facilityId,
        date = date,
        startTime = startTime,
        endTime = endTime,
        maxCapacity = maxCapacity,
        currentBookings = currentBookings,
        isAvailable = isAvailable,
        slotType = slotType,
        notes = notes
    )

    fun createAvailableSlots(
        facilityId: String = generateId("facility"),
        date: LocalDate = today.plus(DatePeriod(days = 1)),
        startHour: Int = 9,
        endHour: Int = 17,
        slotDurationMinutes: Int = 60
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()
        var currentMinutes = startHour * 60

        while (currentMinutes < endHour * 60) {
            val startHr = currentMinutes / 60
            val startMin = currentMinutes % 60
            val endMinutes = currentMinutes + slotDurationMinutes
            val endHr = endMinutes / 60
            val endMin = endMinutes % 60

            slots.add(
                createTimeSlot(
                    facilityId = facilityId,
                    date = date,
                    startTime = LocalTime(startHr, startMin),
                    endTime = LocalTime(endHr, endMin),
                    isAvailable = true
                )
            )
            currentMinutes += slotDurationMinutes
        }

        return slots
    }

    // ============================================================
    // RESTRICTION FIXTURES
    // ============================================================

    fun createRestriction(
        id: String = generateId("restriction"),
        name: String = "Test Restriction",
        description: String = "Test restriction description",
        type: RestrictionType = RestrictionType.TIME_BASED,
        scope: RestrictionScope = RestrictionScope.FACILITY_WIDE,
        priority: Int = 0,
        isActive: Boolean = true,
        effectiveFrom: LocalDate = today,
        effectiveUntil: LocalDate? = null,
        timeConstraints: TimeConstraints? = null,
        visitorConstraints: VisitorConstraints? = null,
        beneficiaryConstraints: BeneficiaryConstraints? = null,
        facilityId: String? = generateId("facility"),
        createdBy: String = generateId("admin"),
        createdAt: Instant = now,
        updatedAt: Instant = now
    ) = Restriction(
        id = id,
        name = name,
        description = description,
        type = type,
        scope = scope,
        priority = priority,
        isActive = isActive,
        effectiveFrom = effectiveFrom,
        effectiveUntil = effectiveUntil,
        timeConstraints = timeConstraints,
        visitorConstraints = visitorConstraints,
        beneficiaryConstraints = beneficiaryConstraints,
        facilityId = facilityId,
        createdBy = createdBy,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    fun createTimeBasedRestriction(
        id: String = generateId("restriction"),
        name: String = "Time Restriction",
        blockedDays: List<DayOfWeek>? = null,
        earliestStartTime: LocalTime? = LocalTime(9, 0),
        latestEndTime: LocalTime? = LocalTime(17, 0),
        maxDurationMinutes: Int? = 60
    ) = createRestriction(
        id = id,
        name = name,
        type = RestrictionType.TIME_BASED,
        timeConstraints = TimeConstraints(
            blockedDays = blockedDays,
            earliestStartTime = earliestStartTime,
            latestEndTime = latestEndTime,
            maxDurationMinutes = maxDurationMinutes
        )
    )

    fun createVisitorBlockRestriction(
        id: String = generateId("restriction"),
        blockedVisitorId: String,
        reason: String = "Visitor not permitted"
    ) = createRestriction(
        id = id,
        name = "Blocked Visitor",
        description = reason,
        type = RestrictionType.VISITOR_BASED,
        scope = RestrictionScope.VISITOR_SPECIFIC,
        visitorConstraints = VisitorConstraints(
            blockedVisitorIds = listOf(blockedVisitorId)
        )
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

    /**
     * Creates a LocalDate for testing
     */
    fun futureDate(daysFromNow: Int): LocalDate =
        today.plus(DatePeriod(days = daysFromNow))

    /**
     * Creates a LocalTime for testing
     */
    fun timeAt(hour: Int, minute: Int = 0): LocalTime =
        LocalTime(hour, minute)
}
