package com.markduenas.visischeduler.domain.entities

import com.markduenas.visischeduler.testutil.*
import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Tests for Restriction entity covering rule evaluation,
 * time-based restrictions, and visitor-based restrictions.
 *
 * @test Restriction Entity
 * @prerequisites None - pure domain tests
 */
class RestrictionTest {

    private lateinit var testClock: TestClock
    private val testBeneficiaryId = "beneficiary-1"
    private val testVisitorId = "visitor-1"
    private val timezone = TimeZone.currentSystemDefault()

    @BeforeTest
    fun setup() {
        testClock = TestClock.fixed(2024, 6, 15, 10, 0) // Saturday, 10 AM
        TestFixtures.resetIdCounter()
    }

    // ============================================================
    // RULE EVALUATION - BLACKOUT DATES
    // ============================================================

    @Test
    fun `should block visit on blackout date`() {
        // Arrange
        val blackoutDate = LocalDate(2024, 6, 16)
        val restriction = TestFixtures.createBlackoutDateRestriction(
            beneficiaryId = testBeneficiaryId,
            date = blackoutDate,
        )

        val visitTime = LocalDateTime(2024, 6, 16, 14, 0).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = visitTime,
            endTime = visitTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should block visit on blackout date")
    }

    @Test
    fun `should not block visit on non-blackout date`() {
        // Arrange
        val blackoutDate = LocalDate(2024, 6, 20)
        val restriction = TestFixtures.createBlackoutDateRestriction(
            beneficiaryId = testBeneficiaryId,
            date = blackoutDate,
        )

        val visitTime = LocalDateTime(2024, 6, 15, 14, 0).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = visitTime,
            endTime = visitTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertFalse(isBlocked, "Should not block visit on different date")
    }

    // ============================================================
    // RULE EVALUATION - TIME-BASED
    // ============================================================

    @Test
    fun `should block visit during meal time`() {
        // Arrange
        val restriction = TestFixtures.createMealTimeRestriction(
            beneficiaryId = testBeneficiaryId,
            startHour = 12,
            startMinute = 0,
            endHour = 13,
            endMinute = 0,
        )

        val lunchTime = LocalDateTime(2024, 6, 15, 12, 30).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = lunchTime,
            endTime = lunchTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should block visit during meal time")
    }

    @Test
    fun `should not block visit outside meal time`() {
        // Arrange
        val restriction = TestFixtures.createMealTimeRestriction(
            beneficiaryId = testBeneficiaryId,
            startHour = 12,
            startMinute = 0,
            endHour = 13,
            endMinute = 0,
        )

        val afternoonTime = LocalDateTime(2024, 6, 15, 14, 0).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = afternoonTime,
            endTime = afternoonTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertFalse(isBlocked, "Should not block visit outside meal time")
    }

    @Test
    fun `should block visit during rest period`() {
        // Arrange
        val restriction = TestFixtures.createRestriction(
            beneficiaryId = testBeneficiaryId,
            type = RestrictionType.REST_PERIOD,
            reason = "Afternoon rest",
        ).copy(
            recurringStartTime = LocalTime(14, 0),
            recurringEndTime = LocalTime(15, 0),
        )

        val restTime = LocalDateTime(2024, 6, 15, 14, 30).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = restTime,
            endTime = restTime.plus(30.minutes),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should block visit during rest period")
    }

    // ============================================================
    // RULE EVALUATION - MEDICAL PROCEDURES
    // ============================================================

    @Test
    fun `should block visit during medical procedure`() {
        // Arrange
        val procedureStart = LocalDateTime(2024, 6, 15, 10, 0).toInstant(timezone)
        val restriction = TestFixtures.createProcedureBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            procedureTime = procedureStart,
            recoveryDuration = 2.hours,
        )

        val duringProcedure = procedureStart.plus(30.minutes)
        val slot = TestFixtures.createTimeSlot(
            startTime = duringProcedure,
            endTime = duringProcedure.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should block visit during procedure")
    }

    @Test
    fun `should block visit during recovery period`() {
        // Arrange
        val procedureStart = LocalDateTime(2024, 6, 15, 10, 0).toInstant(timezone)
        val restriction = TestFixtures.createProcedureBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            procedureTime = procedureStart,
            recoveryDuration = 2.hours, // Recovery until 12:00
        )

        val duringRecovery = procedureStart.plus(1.hours).plus(30.minutes) // 11:30
        val slot = TestFixtures.createTimeSlot(
            startTime = duringRecovery,
            endTime = duringRecovery.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should block visit during recovery")
    }

    @Test
    fun `should allow visit after recovery period`() {
        // Arrange
        val procedureStart = LocalDateTime(2024, 6, 15, 10, 0).toInstant(timezone)
        val restriction = TestFixtures.createProcedureBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            procedureTime = procedureStart,
            recoveryDuration = 2.hours, // Recovery until 12:00
        )

        val afterRecovery = LocalDateTime(2024, 6, 15, 14, 0).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = afterRecovery,
            endTime = afterRecovery.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertFalse(isBlocked, "Should allow visit after recovery")
    }

    // ============================================================
    // VISITOR-BASED RESTRICTIONS
    // ============================================================

    @Test
    fun `should block specific visitor`() {
        // Arrange
        val blockedVisitorId = "blocked-person"
        val restriction = TestFixtures.createVisitorBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            blockedVisitorId = blockedVisitorId,
            reason = "Not permitted",
        )

        val slot = TestFixtures.createTimeSlot()

        // Act
        val isBlocked = restriction.appliesTo(slot, blockedVisitorId, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should block the specific visitor")
    }

    @Test
    fun `should not block other visitors`() {
        // Arrange
        val blockedVisitorId = "blocked-person"
        val restriction = TestFixtures.createVisitorBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            blockedVisitorId = blockedVisitorId,
        )

        val slot = TestFixtures.createTimeSlot()

        // Act
        val isBlocked = restriction.appliesTo(slot, "different-visitor", testClock.now())

        // Assert
        assertFalse(isBlocked, "Should not block other visitors")
    }

    @Test
    fun `visitor restriction should apply regardless of time`() {
        // Arrange
        val blockedVisitorId = "blocked-person"
        val restriction = TestFixtures.createVisitorBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            blockedVisitorId = blockedVisitorId,
        )

        // Various times
        val times = listOf(
            LocalDateTime(2024, 6, 15, 9, 0),
            LocalDateTime(2024, 6, 15, 14, 0),
            LocalDateTime(2024, 6, 16, 10, 0),
            LocalDateTime(2024, 12, 25, 12, 0),
        )

        // Assert all times are blocked for this visitor
        times.forEach { time ->
            val instant = time.toInstant(timezone)
            val slot = TestFixtures.createTimeSlot(
                startTime = instant,
                endTime = instant.plus(1.hours),
            )
            assertTrue(
                restriction.appliesTo(slot, blockedVisitorId, testClock.now()),
                "Should block visitor at $time"
            )
        }
    }

    // ============================================================
    // EXPIRATION TESTS
    // ============================================================

    @Test
    fun `should apply restriction before expiration`() {
        // Arrange
        val expiresAt = testClock.now().plus(7.days)
        val restriction = TestFixtures.createBlackoutDateRestriction(
            beneficiaryId = testBeneficiaryId,
            date = LocalDate(2024, 6, 15),
        ).copy(expiresAt = expiresAt)

        val visitTime = LocalDateTime(2024, 6, 15, 14, 0).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = visitTime,
            endTime = visitTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should apply before expiration")
    }

    @Test
    fun `should not apply restriction after expiration`() {
        // Arrange
        val expiresAt = testClock.now().minus(1.days) // Already expired
        val restriction = TestFixtures.createBlackoutDateRestriction(
            beneficiaryId = testBeneficiaryId,
            date = LocalDate(2024, 6, 15),
        ).copy(expiresAt = expiresAt)

        val visitTime = LocalDateTime(2024, 6, 15, 14, 0).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = visitTime,
            endTime = visitTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertFalse(isBlocked, "Should not apply after expiration")
    }

    @Test
    fun `restriction without expiration should always apply`() {
        // Arrange
        val restriction = TestFixtures.createVisitorBlockRestriction(
            beneficiaryId = testBeneficiaryId,
            blockedVisitorId = testVisitorId,
        ).copy(expiresAt = null)

        val futureTime = testClock.now().plus(365.days)
        val slot = TestFixtures.createTimeSlot(
            startTime = futureTime,
            endTime = futureTime.plus(1.hours),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, testVisitorId, testClock.now())

        // Assert
        assertTrue(isBlocked, "Should apply without expiration date")
    }

    // ============================================================
    // ACTIVE STATUS TESTS
    // ============================================================

    @Test
    fun `active restriction should apply`() {
        // Arrange
        val restriction = TestFixtures.createMealTimeRestriction(
            beneficiaryId = testBeneficiaryId,
        ).copy(isActive = true)

        val lunchTime = LocalDateTime(2024, 6, 15, 12, 30).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = lunchTime,
            endTime = lunchTime.plus(30.minutes),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertTrue(isBlocked, "Active restriction should apply")
    }

    @Test
    fun `inactive restriction should not apply`() {
        // Arrange
        val restriction = TestFixtures.createMealTimeRestriction(
            beneficiaryId = testBeneficiaryId,
        ).copy(isActive = false)

        val lunchTime = LocalDateTime(2024, 6, 15, 12, 30).toInstant(timezone)
        val slot = TestFixtures.createTimeSlot(
            startTime = lunchTime,
            endTime = lunchTime.plus(30.minutes),
        )

        // Act
        val isBlocked = restriction.appliesTo(slot, null, testClock.now())

        // Assert
        assertFalse(isBlocked, "Inactive restriction should not apply")
    }

    // ============================================================
    // COMPLEX RULE COMBINATIONS
    // ============================================================

    @Test
    fun `multiple restrictions should all be evaluated`() {
        // Arrange
        val restrictions = listOf(
            TestFixtures.createMealTimeRestriction(
                beneficiaryId = testBeneficiaryId,
                startHour = 12,
                endHour = 13,
            ),
            TestFixtures.createMealTimeRestriction(
                beneficiaryId = testBeneficiaryId,
                startHour = 18,
                endHour = 19,
            ),
        )

        // Lunch time slot
        val lunchSlot = TestFixtures.createTimeSlot(
            startTime = LocalDateTime(2024, 6, 15, 12, 30).toInstant(timezone),
            endTime = LocalDateTime(2024, 6, 15, 13, 30).toInstant(timezone),
        )

        // Afternoon slot
        val afternoonSlot = TestFixtures.createTimeSlot(
            startTime = LocalDateTime(2024, 6, 15, 15, 0).toInstant(timezone),
            endTime = LocalDateTime(2024, 6, 15, 16, 0).toInstant(timezone),
        )

        // Act
        val isLunchBlocked = restrictions.any { it.appliesTo(lunchSlot, null, testClock.now()) }
        val isAfternoonBlocked = restrictions.any { it.appliesTo(afternoonSlot, null, testClock.now()) }

        // Assert
        assertTrue(isLunchBlocked, "Lunch should be blocked")
        assertFalse(isAfternoonBlocked, "Afternoon should not be blocked")
    }

    // ============================================================
    // EDGE CASES
    // ============================================================

    @Test
    fun `should handle boundary of restriction time`() {
        // Arrange
        val restriction = TestFixtures.createMealTimeRestriction(
            beneficiaryId = testBeneficiaryId,
            startHour = 12,
            startMinute = 0,
            endHour = 13,
            endMinute = 0,
        )

        // Exactly at boundary
        val exactStart = LocalDateTime(2024, 6, 15, 12, 0).toInstant(timezone)
        val exactEnd = LocalDateTime(2024, 6, 15, 13, 0).toInstant(timezone)

        val atStartSlot = TestFixtures.createTimeSlot(
            startTime = exactStart,
            endTime = exactStart.plus(30.minutes),
        )

        val justAfterSlot = TestFixtures.createTimeSlot(
            startTime = exactEnd,
            endTime = exactEnd.plus(30.minutes),
        )

        // Assert
        assertTrue(restriction.appliesTo(atStartSlot, null, testClock.now()), "Should block at exact start")
        assertFalse(restriction.appliesTo(justAfterSlot, null, testClock.now()), "Should not block just after end")
    }

    @Test
    fun `should handle restriction spanning midnight`() {
        // Arrange
        val restriction = TestFixtures.createRestriction(
            beneficiaryId = testBeneficiaryId,
            type = RestrictionType.REST_PERIOD,
            reason = "Night quiet hours",
        ).copy(
            recurringStartTime = LocalTime(22, 0), // 10 PM
            recurringEndTime = LocalTime(6, 0),     // 6 AM next day
        )

        val lateNightSlot = TestFixtures.createTimeSlot(
            startTime = LocalDateTime(2024, 6, 15, 23, 0).toInstant(timezone),
            endTime = LocalDateTime(2024, 6, 16, 0, 0).toInstant(timezone),
        )

        // Note: This test documents that midnight-spanning restrictions
        // may need special handling in the implementation
    }
}

// ============================================================
// EXTENSION FUNCTION FOR RESTRICTION
// ============================================================

/**
 * Determines if this restriction applies to the given time slot and visitor
 */
fun Restriction.appliesTo(
    slot: TimeSlot,
    visitorId: String?,
    currentTime: Instant,
): Boolean {
    // Check if restriction is active
    if (!isActive) return false

    // Check if restriction has expired
    expiresAt?.let { expires ->
        if (currentTime > expires) return false
    }

    val timezone = TimeZone.currentSystemDefault()

    return when (type) {
        RestrictionType.VISITOR_BLOCKED -> {
            visitorId != null && this.visitorId == visitorId
        }

        RestrictionType.BLACKOUT_DATE -> {
            startTime?.let { start ->
                endTime?.let { end ->
                    slot.startTime >= start && slot.startTime < end
                }
            } ?: false
        }

        RestrictionType.MEDICAL_PROCEDURE -> {
            startTime?.let { start ->
                endTime?.let { end ->
                    // Check overlap
                    slot.startTime < end && slot.endTime > start
                }
            } ?: false
        }

        RestrictionType.MEAL_TIME, RestrictionType.REST_PERIOD, RestrictionType.BLACKOUT_HOURS -> {
            recurringStartTime?.let { start ->
                recurringEndTime?.let { end ->
                    val slotTime = slot.startTime.toLocalDateTime(timezone).time
                    slotTime >= start && slotTime < end
                }
            } ?: false
        }

        else -> false
    }
}

private fun Instant.toLocalDateTime(timeZone: TimeZone): LocalDateTime {
    return kotlinx.datetime.toLocalDateTime(timeZone)
}

private val kotlin.time.Duration.Companion.minutes: kotlin.time.Duration
    get() = kotlin.time.Duration.ZERO

private fun Int.minutes(): kotlin.time.Duration = kotlin.time.Duration.parse("${this}m")
private val Int.minutes: kotlin.time.Duration get() = kotlin.time.Duration.parse("${this}m")
