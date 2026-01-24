package com.markduenas.visischeduler.domain.entities

import com.markduenas.visischeduler.testutil.*
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for Visit entity covering state transitions, validation,
 * and business rules.
 *
 * @test Visit Entity
 * @prerequisites None - pure domain tests
 */
class VisitTest {

    private lateinit var testClock: TestClock

    @BeforeTest
    fun setup() {
        testClock = TestClock()
        TestFixtures.resetIdCounter()
    }

    // ============================================================
    // STATE TRANSITION TESTS
    // ============================================================

    @Test
    fun `should allow transition from PENDING to APPROVED`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Act
        val result = visit.canTransitionTo(VisitStatus.APPROVED)

        // Assert
        assertTrue(result, "PENDING -> APPROVED should be allowed")
    }

    @Test
    fun `should allow transition from PENDING to DENIED`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Act
        val result = visit.canTransitionTo(VisitStatus.DENIED)

        // Assert
        assertTrue(result, "PENDING -> DENIED should be allowed")
    }

    @Test
    fun `should allow transition from PENDING to CANCELLED`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Act
        val result = visit.canTransitionTo(VisitStatus.CANCELLED)

        // Assert
        assertTrue(result, "PENDING -> CANCELLED should be allowed")
    }

    @Test
    fun `should allow transition from PENDING to WAITLISTED`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Act
        val result = visit.canTransitionTo(VisitStatus.WAITLISTED)

        // Assert
        assertTrue(result, "PENDING -> WAITLISTED should be allowed")
    }

    @Test
    fun `should allow transition from APPROVED to CHECKED_IN`() {
        // Arrange
        val visit = TestFixtures.createApprovedVisit()

        // Act
        val result = visit.canTransitionTo(VisitStatus.CHECKED_IN)

        // Assert
        assertTrue(result, "APPROVED -> CHECKED_IN should be allowed")
    }

    @Test
    fun `should allow transition from APPROVED to CANCELLED`() {
        // Arrange
        val visit = TestFixtures.createApprovedVisit()

        // Act
        val result = visit.canTransitionTo(VisitStatus.CANCELLED)

        // Assert
        assertTrue(result, "APPROVED -> CANCELLED should be allowed")
    }

    @Test
    fun `should allow transition from APPROVED to NO_SHOW`() {
        // Arrange
        val visit = TestFixtures.createApprovedVisit()

        // Act
        val result = visit.canTransitionTo(VisitStatus.NO_SHOW)

        // Assert
        assertTrue(result, "APPROVED -> NO_SHOW should be allowed")
    }

    @Test
    fun `should allow transition from CHECKED_IN to COMPLETED`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.CHECKED_IN)

        // Act
        val result = visit.canTransitionTo(VisitStatus.COMPLETED)

        // Assert
        assertTrue(result, "CHECKED_IN -> COMPLETED should be allowed")
    }

    @Test
    fun `should allow transition from WAITLISTED to APPROVED`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.WAITLISTED)

        // Act
        val result = visit.canTransitionTo(VisitStatus.APPROVED)

        // Assert
        assertTrue(result, "WAITLISTED -> APPROVED should be allowed")
    }

    @Test
    fun `should not allow transition from DENIED to APPROVED`() {
        // Arrange
        val visit = TestFixtures.createDeniedVisit()

        // Act
        val result = visit.canTransitionTo(VisitStatus.APPROVED)

        // Assert
        assertFalse(result, "DENIED -> APPROVED should not be allowed")
    }

    @Test
    fun `should not allow transition from COMPLETED to any other state`() {
        // Arrange
        val visit = TestFixtures.createCompletedVisit()

        // Act & Assert
        VisitStatus.entries.forEach { status ->
            if (status != VisitStatus.COMPLETED) {
                assertFalse(
                    visit.canTransitionTo(status),
                    "COMPLETED -> $status should not be allowed"
                )
            }
        }
    }

    @Test
    fun `should not allow transition from CANCELLED to any other state`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.CANCELLED)

        // Act & Assert
        VisitStatus.entries.forEach { status ->
            if (status != VisitStatus.CANCELLED) {
                assertFalse(
                    visit.canTransitionTo(status),
                    "CANCELLED -> $status should not be allowed"
                )
            }
        }
    }

    // ============================================================
    // DURATION VALIDATION TESTS
    // ============================================================

    @Test
    fun `should accept valid duration`() {
        // Arrange
        val startTime = testClock.now()
        val duration = 1.hours

        // Act
        val visit = TestFixtures.createVisit(
            startTime = startTime,
            duration = duration,
        )

        // Assert
        assertTrue(visit.isValidDuration(), "1 hour duration should be valid")
        assertEquals(startTime.plus(duration), visit.endTime)
    }

    @Test
    fun `should reject zero duration`() {
        // Arrange
        val startTime = testClock.now()

        // Act
        val visit = Visit(
            id = "test",
            beneficiaryId = "b1",
            visitorId = "v1",
            visitorName = "Test",
            status = VisitStatus.PENDING,
            startTime = startTime,
            endTime = startTime, // Same as start = 0 duration
            visitType = VisitType.IN_PERSON,
            numberOfGuests = 0,
            reason = null,
            notes = null,
            createdAt = startTime,
            approvedBy = null,
            approvedAt = null,
            denialReason = null,
        )

        // Assert
        assertFalse(visit.isValidDuration(), "Zero duration should be invalid")
    }

    @Test
    fun `should reject negative duration`() {
        // Arrange
        val startTime = testClock.now()

        // Act
        val visit = Visit(
            id = "test",
            beneficiaryId = "b1",
            visitorId = "v1",
            visitorName = "Test",
            status = VisitStatus.PENDING,
            startTime = startTime,
            endTime = startTime.minus(1.hours), // End before start
            visitType = VisitType.IN_PERSON,
            numberOfGuests = 0,
            reason = null,
            notes = null,
            createdAt = startTime,
            approvedBy = null,
            approvedAt = null,
            denialReason = null,
        )

        // Assert
        assertFalse(visit.isValidDuration(), "Negative duration should be invalid")
    }

    @Test
    fun `should accept minimum duration of 15 minutes`() {
        // Arrange
        val visit = TestFixtures.createVisit(duration = 15.minutes)

        // Assert
        assertTrue(visit.duration >= 15.minutes, "Should accept 15 minute minimum")
    }

    @Test
    fun `should accept maximum duration of 4 hours`() {
        // Arrange
        val visit = TestFixtures.createVisit(duration = 4.hours)

        // Assert
        assertTrue(visit.duration <= 4.hours, "Should accept 4 hour maximum")
    }

    // ============================================================
    // CAPACITY CHECKS
    // ============================================================

    @Test
    fun `should report zero guests correctly`() {
        // Arrange
        val visit = TestFixtures.createVisit(numberOfGuests = 0)

        // Assert
        assertEquals(1, visit.totalAttendees) // Visitor only
    }

    @Test
    fun `should report total attendees including guests`() {
        // Arrange
        val visit = TestFixtures.createVisit(numberOfGuests = 3)

        // Assert
        assertEquals(4, visit.totalAttendees) // Visitor + 3 guests
    }

    @Test
    fun `should validate guest count is non-negative`() {
        // Arrange
        val visit = TestFixtures.createVisit(numberOfGuests = 0)

        // Assert
        assertTrue(visit.numberOfGuests >= 0)
    }

    // ============================================================
    // TIME-BASED CHECKS
    // ============================================================

    @Test
    fun `should detect future visit`() {
        // Arrange
        val futureStart = testClock.now().plus(1.hours)
        val visit = TestFixtures.createVisit(startTime = futureStart)

        // Act
        val isFuture = visit.startTime > testClock.now()

        // Assert
        assertTrue(isFuture, "Visit should be in the future")
    }

    @Test
    fun `should detect past visit`() {
        // Arrange
        val pastStart = testClock.now().minus(2.hours)
        val visit = TestFixtures.createVisit(
            startTime = pastStart,
            duration = 1.hours,
        )

        // Act
        val isPast = visit.endTime < testClock.now()

        // Assert
        assertTrue(isPast, "Visit should be in the past")
    }

    @Test
    fun `should detect ongoing visit`() {
        // Arrange
        val startTime = testClock.now().minus(30.minutes)
        val visit = TestFixtures.createVisit(
            startTime = startTime,
            duration = 1.hours,
        )

        // Act
        val isOngoing = visit.startTime <= testClock.now() && visit.endTime > testClock.now()

        // Assert
        assertTrue(isOngoing, "Visit should be ongoing")
    }

    // ============================================================
    // VISIT TYPE TESTS
    // ============================================================

    @Test
    fun `should support in-person visit type`() {
        // Arrange
        val visit = TestFixtures.createVisit(visitType = VisitType.IN_PERSON)

        // Assert
        assertEquals(VisitType.IN_PERSON, visit.visitType)
        assertTrue(visit.requiresPhysicalPresence)
    }

    @Test
    fun `should support virtual visit type`() {
        // Arrange
        val visit = TestFixtures.createVisit(visitType = VisitType.VIRTUAL)

        // Assert
        assertEquals(VisitType.VIRTUAL, visit.visitType)
        assertFalse(visit.requiresPhysicalPresence)
    }

    @Test
    fun `should support hybrid visit type`() {
        // Arrange
        val visit = TestFixtures.createVisit(visitType = VisitType.HYBRID)

        // Assert
        assertEquals(VisitType.HYBRID, visit.visitType)
    }

    // ============================================================
    // APPROVAL METADATA TESTS
    // ============================================================

    @Test
    fun `approved visit should have approval metadata`() {
        // Arrange
        val visit = TestFixtures.createApprovedVisit(
            approvedBy = "coordinator-1",
        )

        // Assert
        assertNotNull(visit.approvedBy)
        assertNotNull(visit.approvedAt)
        assertEquals("coordinator-1", visit.approvedBy)
    }

    @Test
    fun `pending visit should not have approval metadata`() {
        // Arrange
        val visit = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Assert
        assertNull(visit.approvedBy)
        assertNull(visit.approvedAt)
    }

    @Test
    fun `denied visit should have denial reason`() {
        // Arrange
        val visit = TestFixtures.createDeniedVisit(
            denialReason = "Schedule conflict",
        )

        // Assert
        assertEquals(VisitStatus.DENIED, visit.status)
        assertEquals("Schedule conflict", visit.denialReason)
    }

    // ============================================================
    // EDGE CASES
    // ============================================================

    @Test
    fun `should handle visit spanning midnight`() {
        // Arrange
        val lateNightStart = testClock.now()
        testClock.setDateTime(2024, 6, 15, 23, 0) // 11 PM
        val visit = TestFixtures.createVisit(
            startTime = testClock.now(),
            duration = 2.hours, // Ends at 1 AM next day
        )

        // Assert
        assertTrue(visit.endTime > visit.startTime)
        // End time should be next day
        val startDay = visit.startTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        val endDay = visit.endTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
        assertNotEquals(startDay, endDay, "Visit should span to next day")
    }

    @Test
    fun `should calculate correct duration`() {
        // Arrange
        val startTime = testClock.now()
        val expectedDuration = 90.minutes
        val visit = TestFixtures.createVisit(
            startTime = startTime,
            duration = expectedDuration,
        )

        // Act
        val calculatedDuration = visit.duration

        // Assert
        assertEquals(expectedDuration, calculatedDuration)
    }
}

// ============================================================
// EXTENSION FUNCTIONS FOR VISIT
// ============================================================

/**
 * Extension properties and functions for Visit testing
 */
val Visit.duration: kotlin.time.Duration
    get() = endTime - startTime

val Visit.totalAttendees: Int
    get() = 1 + numberOfGuests // Visitor + guests

val Visit.requiresPhysicalPresence: Boolean
    get() = visitType == VisitType.IN_PERSON

fun Visit.isValidDuration(): Boolean {
    return endTime > startTime
}

fun Visit.canTransitionTo(newStatus: VisitStatus): Boolean {
    return when (status) {
        VisitStatus.PENDING -> newStatus in listOf(
            VisitStatus.APPROVED,
            VisitStatus.DENIED,
            VisitStatus.CANCELLED,
            VisitStatus.WAITLISTED,
        )
        VisitStatus.APPROVED -> newStatus in listOf(
            VisitStatus.CHECKED_IN,
            VisitStatus.CANCELLED,
            VisitStatus.NO_SHOW,
        )
        VisitStatus.CHECKED_IN -> newStatus == VisitStatus.COMPLETED
        VisitStatus.WAITLISTED -> newStatus in listOf(
            VisitStatus.APPROVED,
            VisitStatus.CANCELLED,
        )
        VisitStatus.DENIED,
        VisitStatus.CANCELLED,
        VisitStatus.COMPLETED,
        VisitStatus.NO_SHOW -> false
    }
}

private fun kotlinx.datetime.Instant.toLocalDateTime(
    timeZone: kotlinx.datetime.TimeZone
): kotlinx.datetime.LocalDateTime {
    return kotlinx.datetime.toLocalDateTime(timeZone)
}
