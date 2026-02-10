package com.markduenas.visischeduler.domain.entities

import com.markduenas.visischeduler.testutil.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.*

/**
 * Tests for Visit entity covering creation, properties, and status handling.
 *
 * @test Visit Entity
 * @prerequisites None - pure domain tests
 */
class VisitTest {

    @BeforeTest
    fun setup() {
        TestFixtures.resetIdCounter()
    }

    // ============================================================
    // CREATION TESTS
    // ============================================================

    @Test
    fun `should create visit with required fields`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(
            beneficiaryId = "beneficiary-1",
            visitorId = "visitor-1",
        )

        // Assert
        assertNotNull(visit)
        assertEquals("beneficiary-1", visit.beneficiaryId)
        assertEquals("visitor-1", visit.visitorId)
    }

    @Test
    fun `should create visit with default pending status`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit()

        // Assert
        assertEquals(VisitStatus.PENDING, visit.status)
    }

    @Test
    fun `should create visit with scheduled date and times`() {
        // Arrange
        val date = LocalDate(2024, 6, 15)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        // Act
        val visit = TestFixtures.createVisit(
            scheduledDate = date,
            startTime = startTime,
            endTime = endTime,
        )

        // Assert
        assertEquals(date, visit.scheduledDate)
        assertEquals(startTime, visit.startTime)
        assertEquals(endTime, visit.endTime)
    }

    // ============================================================
    // STATUS TESTS
    // ============================================================

    @Test
    fun `should create pending visit`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Assert
        assertEquals(VisitStatus.PENDING, visit.status)
        assertNull(visit.approvedBy)
        assertNull(visit.approvedAt)
    }

    @Test
    fun `should create approved visit`() {
        // Arrange & Act
        val visit = TestFixtures.createApprovedVisit()

        // Assert
        assertEquals(VisitStatus.APPROVED, visit.status)
        assertNotNull(visit.approvedBy)
        assertNotNull(visit.approvedAt)
    }

    @Test
    fun `should create denied visit with reason`() {
        // Arrange & Act
        val visit = TestFixtures.createDeniedVisit(
            denialReason = "Conflict with medical procedure",
        )

        // Assert
        assertEquals(VisitStatus.DENIED, visit.status)
        assertEquals("Conflict with medical procedure", visit.denialReason)
    }

    @Test
    fun `should create completed visit with check-in and check-out times`() {
        // Arrange & Act
        val visit = TestFixtures.createCompletedVisit()

        // Assert
        assertEquals(VisitStatus.COMPLETED, visit.status)
        assertNotNull(visit.checkInTime)
        assertNotNull(visit.checkOutTime)
    }

    @Test
    fun `should create cancelled visit`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(status = VisitStatus.CANCELLED)

        // Assert
        assertEquals(VisitStatus.CANCELLED, visit.status)
    }

    // ============================================================
    // VISIT TYPE TESTS
    // ============================================================

    @Test
    fun `should create in-person visit`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(visitType = VisitType.IN_PERSON)

        // Assert
        assertEquals(VisitType.IN_PERSON, visit.visitType)
    }

    @Test
    fun `should create video call visit`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(visitType = VisitType.VIDEO_CALL)

        // Assert
        assertEquals(VisitType.VIDEO_CALL, visit.visitType)
    }

    // ============================================================
    // ADDITIONAL VISITORS TESTS
    // ============================================================

    @Test
    fun `should create visit with additional visitors`() {
        // Arrange
        val additionalVisitors = listOf(
            TestFixtures.createAdditionalVisitor(
                firstName = "John",
                lastName = "Doe",
                relationship = "Brother",
            ),
            TestFixtures.createAdditionalVisitor(
                firstName = "Jane",
                lastName = "Doe",
                relationship = "Sister",
                isMinor = true,
                age = 15,
            ),
        )

        // Act
        val visit = TestFixtures.createVisit(
            additionalVisitors = additionalVisitors,
        )

        // Assert
        assertEquals(2, visit.additionalVisitors.size)
        assertEquals("John", visit.additionalVisitors[0].firstName)
        assertEquals("Jane", visit.additionalVisitors[1].firstName)
        assertTrue(visit.additionalVisitors[1].isMinor)
    }

    @Test
    fun `should create additional visitor with all fields`() {
        // Arrange & Act
        val visitor = TestFixtures.createAdditionalVisitor(
            firstName = "Child",
            lastName = "Visitor",
            relationship = "Grandchild",
            isMinor = true,
            age = 10,
        )

        // Assert
        assertEquals("Child", visitor.firstName)
        assertEquals("Visitor", visitor.lastName)
        assertEquals("Grandchild", visitor.relationship)
        assertTrue(visitor.isMinor)
        assertEquals(10, visitor.age)
    }

    // ============================================================
    // PURPOSE AND NOTES TESTS
    // ============================================================

    @Test
    fun `should create visit with purpose`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(purpose = "Birthday celebration")

        // Assert
        assertEquals("Birthday celebration", visit.purpose)
    }

    @Test
    fun `should create visit with notes`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit(notes = "Bringing cake and presents")

        // Assert
        assertEquals("Bringing cake and presents", visit.notes)
    }

    // ============================================================
    // COPY TESTS
    // ============================================================

    @Test
    fun `should copy visit with modified status`() {
        // Arrange
        val original = TestFixtures.createVisit(status = VisitStatus.PENDING)

        // Act
        val approved = original.copy(
            status = VisitStatus.APPROVED,
            approvedBy = "coordinator-1",
        )

        // Assert
        assertEquals(VisitStatus.APPROVED, approved.status)
        assertEquals("coordinator-1", approved.approvedBy)
        assertEquals(original.id, approved.id) // ID preserved
        assertEquals(original.beneficiaryId, approved.beneficiaryId) // Other fields preserved
    }

    @Test
    fun `should copy visit with cancellation details`() {
        // Arrange
        val original = TestFixtures.createApprovedVisit()
        val testClock = TestClock()

        // Act
        val cancelled = original.copy(
            status = VisitStatus.CANCELLED,
            cancellationReason = "Visitor unavailable",
            cancelledBy = "visitor-1",
            cancelledAt = testClock.now(),
        )

        // Assert
        assertEquals(VisitStatus.CANCELLED, cancelled.status)
        assertEquals("Visitor unavailable", cancelled.cancellationReason)
        assertEquals("visitor-1", cancelled.cancelledBy)
        assertNotNull(cancelled.cancelledAt)
    }

    // ============================================================
    // TIMESTAMP TESTS
    // ============================================================

    @Test
    fun `should have created and updated timestamps`() {
        // Arrange & Act
        val visit = TestFixtures.createVisit()

        // Assert
        assertNotNull(visit.createdAt)
        assertNotNull(visit.updatedAt)
    }

    @Test
    fun `approved visit should have approval timestamp`() {
        // Arrange & Act
        val visit = TestFixtures.createApprovedVisit()

        // Assert
        assertNotNull(visit.approvedAt)
    }

    @Test
    fun `completed visit should have check-in and check-out timestamps`() {
        // Arrange & Act
        val visit = TestFixtures.createCompletedVisit()

        // Assert
        assertNotNull(visit.checkInTime)
        assertNotNull(visit.checkOutTime)
    }

    // ============================================================
    // TIME VALIDATION TESTS
    // ============================================================

    @Test
    fun `end time should be after start time`() {
        // Arrange
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        // Act
        val visit = TestFixtures.createVisit(
            startTime = startTime,
            endTime = endTime,
        )

        // Assert
        assertTrue(visit.endTime > visit.startTime)
    }

    @Test
    fun `should calculate visit duration`() {
        // Arrange
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 30)

        val visit = TestFixtures.createVisit(
            startTime = startTime,
            endTime = endTime,
        )

        // Assert - 90 minutes difference
        val startMinutes = visit.startTime.hour * 60 + visit.startTime.minute
        val endMinutes = visit.endTime.hour * 60 + visit.endTime.minute
        assertEquals(90, endMinutes - startMinutes)
    }
}
