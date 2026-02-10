package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.entities.VisitType
import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.plus
import kotlin.test.*

/**
 * Tests for visit scheduling logic covering conflict detection and validation.
 *
 * @test Schedule Visit Logic
 * @prerequisites Mocked repositories
 */
class ScheduleVisitUseCaseTest {

    private lateinit var visitRepository: FakeVisitRepository
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testVisitorId = "visitor-1"

    @BeforeTest
    fun setup() {
        visitRepository = FakeVisitRepository()
        testClock = TestClock.fixed(2024, 6, 15, 10, 0) // Saturday, June 15, 2024, 10:00 AM
        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        visitRepository.clear()
    }

    // ============================================================
    // VALID SCHEDULING TESTS
    // ============================================================

    @Test
    fun `should create visit in empty slot`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        val visit = TestFixtures.createVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            scheduledDate = scheduledDate,
            startTime = startTime,
            endTime = endTime,
            status = VisitStatus.PENDING,
        )

        // Act
        val result = visitRepository.save(visit)

        // Assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `should detect slot availability correctly`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        // Initially slot should be available
        var isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, scheduledDate, startTime, endTime
        ).getOrNull()

        assertTrue(isAvailable == true, "Slot should be available when empty")

        // Add an approved visit
        val visit = TestFixtures.createApprovedVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            scheduledDate = scheduledDate,
            startTime = startTime,
            endTime = endTime,
        )
        visitRepository.save(visit)

        // Now slot should not be available
        isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, scheduledDate, startTime, endTime
        ).getOrNull()

        assertFalse(isAvailable == true, "Slot should not be available after booking")
    }

    // ============================================================
    // CONFLICT DETECTION TESTS
    // ============================================================

    @Test
    fun `should detect overlapping visits`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)

        // Add existing visit from 10:00 to 11:00
        val existingVisit = TestFixtures.createApprovedVisit(
            id = "existing-visit",
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            scheduledDate = scheduledDate,
            startTime = LocalTime(10, 0),
            endTime = LocalTime(11, 0),
        )
        visitRepository.save(existingVisit)

        // Check for conflicts with overlapping slot (10:30 to 11:30)
        val conflicts = visitRepository.getConflictingVisits(
            testBeneficiaryId,
            scheduledDate,
            LocalTime(10, 30),
            LocalTime(11, 30),
        ).getOrNull()

        // Assert
        assertNotNull(conflicts)
        assertEquals(1, conflicts.size)
        assertEquals("existing-visit", conflicts.first().id)
    }

    @Test
    fun `should not detect non-overlapping visits as conflicts`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)

        // Add existing visit from 10:00 to 11:00
        val existingVisit = TestFixtures.createApprovedVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            scheduledDate = scheduledDate,
            startTime = LocalTime(10, 0),
            endTime = LocalTime(11, 0),
        )
        visitRepository.save(existingVisit)

        // Check for conflicts with non-overlapping slot (14:00 to 15:00)
        val conflicts = visitRepository.getConflictingVisits(
            testBeneficiaryId,
            scheduledDate,
            LocalTime(14, 0),
            LocalTime(15, 0),
        ).getOrNull()

        // Assert
        assertNotNull(conflicts)
        assertTrue(conflicts.isEmpty(), "Should not find conflicts for non-overlapping times")
    }

    // ============================================================
    // VISIT STATUS TESTS
    // ============================================================

    @Test
    fun `pending visit should block slot`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        val pendingVisit = TestFixtures.createVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            scheduledDate = scheduledDate,
            startTime = startTime,
            endTime = endTime,
            status = VisitStatus.PENDING,
        )
        visitRepository.save(pendingVisit)

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, scheduledDate, startTime, endTime
        ).getOrNull()

        // Assert
        assertFalse(isAvailable == true, "Pending visit should block slot")
    }

    @Test
    fun `cancelled visit should not block slot`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        val cancelledVisit = TestFixtures.createVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            scheduledDate = scheduledDate,
            startTime = startTime,
            endTime = endTime,
            status = VisitStatus.CANCELLED,
        )
        visitRepository.save(cancelledVisit)

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, scheduledDate, startTime, endTime
        ).getOrNull()

        // Assert
        assertTrue(isAvailable == true, "Cancelled visit should not block slot")
    }

    @Test
    fun `denied visit should not block slot`() = runTest {
        // Arrange
        val scheduledDate = TestFixtures.futureDate(1)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        val deniedVisit = TestFixtures.createDeniedVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
        ).copy(
            scheduledDate = scheduledDate,
            startTime = startTime,
            endTime = endTime,
        )
        visitRepository.save(deniedVisit)

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, scheduledDate, startTime, endTime
        ).getOrNull()

        // Assert
        assertTrue(isAvailable == true, "Denied visit should not block slot")
    }

    // ============================================================
    // DATE RANGE QUERY TESTS
    // ============================================================

    @Test
    fun `should get visits within date range`() = runTest {
        // Arrange
        val today = TestFixtures.futureDate(0)

        // Add visits on different dates
        val visit1 = TestFixtures.createVisit(
            id = "v1",
            beneficiaryId = testBeneficiaryId,
            scheduledDate = today,
        )
        val visit2 = TestFixtures.createVisit(
            id = "v2",
            beneficiaryId = testBeneficiaryId,
            scheduledDate = today.plus(DatePeriod(days = 1)),
        )
        val visit3 = TestFixtures.createVisit(
            id = "v3",
            beneficiaryId = testBeneficiaryId,
            scheduledDate = today.plus(DatePeriod(days = 7)), // Outside range
        )

        visitRepository.save(visit1)
        visitRepository.save(visit2)
        visitRepository.save(visit3)

        // Act
        val visitsInRange = visitRepository.getByDateRange(
            testBeneficiaryId,
            today,
            today.plus(DatePeriod(days = 3)),
        ).getOrNull()

        // Assert
        assertNotNull(visitsInRange)
        assertEquals(2, visitsInRange.size)
        assertTrue(visitsInRange.any { it.id == "v1" })
        assertTrue(visitsInRange.any { it.id == "v2" })
        assertFalse(visitsInRange.any { it.id == "v3" })
    }

    // ============================================================
    // VISITOR QUERY TESTS
    // ============================================================

    @Test
    fun `should get visits for specific visitor`() = runTest {
        // Arrange
        val visit1 = TestFixtures.createVisit(
            id = "v1",
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
        )
        val visit2 = TestFixtures.createVisit(
            id = "v2",
            beneficiaryId = "other-beneficiary",
            visitorId = testVisitorId,
        )
        val visit3 = TestFixtures.createVisit(
            id = "v3",
            beneficiaryId = testBeneficiaryId,
            visitorId = "other-visitor",
        )

        visitRepository.save(visit1)
        visitRepository.save(visit2)
        visitRepository.save(visit3)

        // Act
        val visitorVisits = visitRepository.getByVisitor(testVisitorId).getOrNull()

        // Assert
        assertNotNull(visitorVisits)
        assertEquals(2, visitorVisits.size)
        assertTrue(visitorVisits.all { it.visitorId == testVisitorId })
    }

    // ============================================================
    // VISIT UPDATE TESTS
    // ============================================================

    @Test
    fun `should update visit status`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(
            id = "update-test",
            beneficiaryId = testBeneficiaryId,
            status = VisitStatus.PENDING,
        )
        visitRepository.save(visit)

        // Act
        val approved = visit.copy(
            status = VisitStatus.APPROVED,
            approvedBy = "coordinator-1",
            approvedAt = testClock.now(),
        )
        val result = visitRepository.update(approved)

        // Assert
        assertTrue(result.isSuccess)

        val retrieved = visitRepository.getById("update-test").getOrNull()
        assertEquals(VisitStatus.APPROVED, retrieved?.status)
        assertEquals("coordinator-1", retrieved?.approvedBy)
    }

    @Test
    fun `should cancel visit`() = runTest {
        // Arrange
        val visit = TestFixtures.createApprovedVisit(
            id = "cancel-test",
            beneficiaryId = testBeneficiaryId,
        )
        visitRepository.save(visit)

        // Act
        val cancelled = visit.copy(
            status = VisitStatus.CANCELLED,
            cancellationReason = "Schedule conflict",
            cancelledBy = "visitor-1",
            cancelledAt = testClock.now(),
        )
        visitRepository.update(cancelled)

        // Assert
        val retrieved = visitRepository.getById("cancel-test").getOrNull()
        assertEquals(VisitStatus.CANCELLED, retrieved?.status)
        assertEquals("Schedule conflict", retrieved?.cancellationReason)
    }
}
