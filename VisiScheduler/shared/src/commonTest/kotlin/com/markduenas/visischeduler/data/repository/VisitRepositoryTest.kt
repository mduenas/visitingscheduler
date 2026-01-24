package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * Tests for VisitRepository implementation covering CRUD operations,
 * queries, and flow observations.
 *
 * @test Visit Repository
 * @prerequisites Test database with schema
 */
class VisitRepositoryTest {

    private lateinit var repository: FakeVisitRepository
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testVisitorId = "visitor-1"

    @BeforeTest
    fun setup() {
        repository = FakeVisitRepository()
        testClock = TestClock()
        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        repository.clear()
    }

    // ============================================================
    // SAVE TESTS
    // ============================================================

    @Test
    fun `should save visit and return id`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
        )

        // Act
        val result = repository.save(visit)

        // Assert
        assertTrue(result.isSuccess)
        val savedId = result.getOrNull()
        assertNotNull(savedId)
        assertTrue(savedId.isNotEmpty())
    }

    @Test
    fun `should persist visit with all fields`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(
            id = "custom-id",
            beneficiaryId = testBeneficiaryId,
            visitorId = testVisitorId,
            visitorName = "John Doe",
            status = VisitStatus.PENDING,
            visitType = VisitType.IN_PERSON,
            numberOfGuests = 2,
            reason = "Family visit",
        )

        // Act
        repository.save(visit)
        val retrieved = repository.getById(visit.id).getOrNull()

        // Assert
        assertNotNull(retrieved)
        assertEquals(visit.beneficiaryId, retrieved.beneficiaryId)
        assertEquals(visit.visitorId, retrieved.visitorId)
        assertEquals(visit.visitorName, retrieved.visitorName)
        assertEquals(visit.status, retrieved.status)
        assertEquals(visit.visitType, retrieved.visitType)
        assertEquals(visit.numberOfGuests, retrieved.numberOfGuests)
        assertEquals(visit.reason, retrieved.reason)
    }

    @Test
    fun `should generate id if not provided`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(id = "")

        // Act
        val result = repository.save(visit)

        // Assert
        assertTrue(result.isSuccess)
        val savedId = result.getOrNull()
        assertNotNull(savedId)
        assertTrue(savedId.isNotEmpty())
        assertTrue(savedId.startsWith("visit-"))
    }

    // ============================================================
    // GET BY ID TESTS
    // ============================================================

    @Test
    fun `should retrieve visit by id`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(id = "test-visit-1")
        repository.save(visit)

        // Act
        val result = repository.getById("test-visit-1")

        // Assert
        assertTrue(result.isSuccess)
        val retrieved = result.getOrNull()
        assertNotNull(retrieved)
        assertEquals("test-visit-1", retrieved.id)
    }

    @Test
    fun `should return null for non-existent id`() = runTest {
        // Act
        val result = repository.getById("non-existent-id")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ============================================================
    // GET BY BENEFICIARY TESTS
    // ============================================================

    @Test
    fun `should get all visits for beneficiary`() = runTest {
        // Arrange
        repeat(5) { index ->
            repository.save(
                TestFixtures.createVisit(
                    id = "visit-$index",
                    beneficiaryId = testBeneficiaryId,
                )
            )
        }
        // Add visits for different beneficiary
        repeat(3) { index ->
            repository.save(
                TestFixtures.createVisit(
                    id = "other-visit-$index",
                    beneficiaryId = "other-beneficiary",
                )
            )
        }

        // Act
        val result = repository.getByBeneficiary(testBeneficiaryId)

        // Assert
        assertTrue(result.isSuccess)
        val visits = result.getOrNull()!!
        assertEquals(5, visits.size)
        assertTrue(visits.all { it.beneficiaryId == testBeneficiaryId })
    }

    @Test
    fun `should return empty list for beneficiary with no visits`() = runTest {
        // Act
        val result = repository.getByBeneficiary("no-visits-beneficiary")

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    // ============================================================
    // GET BY VISITOR TESTS
    // ============================================================

    @Test
    fun `should get all visits for visitor`() = runTest {
        // Arrange
        repeat(4) { index ->
            repository.save(
                TestFixtures.createVisit(
                    id = "visit-$index",
                    visitorId = testVisitorId,
                    beneficiaryId = "beneficiary-$index",
                )
            )
        }

        // Act
        val result = repository.getByVisitor(testVisitorId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()!!.size)
    }

    // ============================================================
    // GET BY DATE RANGE TESTS
    // ============================================================

    @Test
    fun `should get visits within date range`() = runTest {
        // Arrange
        val baseTime = testClock.now()

        repository.save(TestFixtures.createVisit(id = "v1", beneficiaryId = testBeneficiaryId, startTime = baseTime))
        repository.save(TestFixtures.createVisit(id = "v2", beneficiaryId = testBeneficiaryId, startTime = baseTime.plus(1.days)))
        repository.save(TestFixtures.createVisit(id = "v3", beneficiaryId = testBeneficiaryId, startTime = baseTime.plus(2.days)))
        repository.save(TestFixtures.createVisit(id = "v4", beneficiaryId = testBeneficiaryId, startTime = baseTime.plus(5.days))) // Outside range

        val rangeStart = baseTime.minus(1.hours)
        val rangeEnd = baseTime.plus(3.days)

        // Act
        val result = repository.getByDateRange(testBeneficiaryId, rangeStart, rangeEnd)

        // Assert
        assertTrue(result.isSuccess)
        val visits = result.getOrNull()!!
        assertEquals(3, visits.size)
        assertTrue(visits.none { it.id == "v4" })
    }

    @Test
    fun `should exclude visits outside date range`() = runTest {
        // Arrange
        val baseTime = testClock.now()

        repository.save(TestFixtures.createVisit(
            id = "past",
            beneficiaryId = testBeneficiaryId,
            startTime = baseTime.minus(5.days)
        ))
        repository.save(TestFixtures.createVisit(
            id = "future",
            beneficiaryId = testBeneficiaryId,
            startTime = baseTime.plus(5.days)
        ))

        val rangeStart = baseTime.minus(1.days)
        val rangeEnd = baseTime.plus(1.days)

        // Act
        val result = repository.getByDateRange(testBeneficiaryId, rangeStart, rangeEnd)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    // ============================================================
    // UPDATE TESTS
    // ============================================================

    @Test
    fun `should update existing visit`() = runTest {
        // Arrange
        val originalVisit = TestFixtures.createVisit(
            id = "update-test",
            status = VisitStatus.PENDING,
        )
        repository.save(originalVisit)

        val updatedVisit = originalVisit.copy(
            status = VisitStatus.APPROVED,
            approvedBy = "coordinator-1",
            approvedAt = testClock.now(),
        )

        // Act
        val result = repository.update(updatedVisit)

        // Assert
        assertTrue(result.isSuccess)
        val retrieved = repository.getById("update-test").getOrNull()
        assertEquals(VisitStatus.APPROVED, retrieved?.status)
        assertEquals("coordinator-1", retrieved?.approvedBy)
    }

    @Test
    fun `should return error when updating non-existent visit`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(id = "non-existent")

        // Act
        val result = repository.update(visit)

        // Assert
        assertTrue(result.isError)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    // ============================================================
    // DELETE TESTS
    // ============================================================

    @Test
    fun `should delete existing visit`() = runTest {
        // Arrange
        val visit = TestFixtures.createVisit(id = "delete-test")
        repository.save(visit)

        // Act
        val result = repository.delete("delete-test")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(repository.getById("delete-test").getOrNull())
    }

    @Test
    fun `should succeed when deleting non-existent visit`() = runTest {
        // Act
        val result = repository.delete("non-existent")

        // Assert
        assertTrue(result.isSuccess) // No-op for non-existent
    }

    // ============================================================
    // SLOT AVAILABILITY TESTS
    // ============================================================

    @Test
    fun `should return true when slot is available`() = runTest {
        // Arrange
        val slotStart = testClock.now().plus(2.hours)
        val slotEnd = slotStart.plus(1.hours)

        // No conflicting visits

        // Act
        val result = repository.isSlotAvailable(testBeneficiaryId, slotStart, slotEnd)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    @Test
    fun `should return false when slot has approved visit`() = runTest {
        // Arrange
        val slotStart = testClock.now().plus(2.hours)
        val slotEnd = slotStart.plus(1.hours)

        repository.save(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = slotStart,
            )
        )

        // Act
        val result = repository.isSlotAvailable(testBeneficiaryId, slotStart, slotEnd)

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun `should return false when slot has pending visit`() = runTest {
        // Arrange
        val slotStart = testClock.now().plus(2.hours)
        val slotEnd = slotStart.plus(1.hours)

        repository.save(
            TestFixtures.createVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = slotStart,
                status = VisitStatus.PENDING,
            )
        )

        // Act
        val result = repository.isSlotAvailable(testBeneficiaryId, slotStart, slotEnd)

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true)
    }

    @Test
    fun `should return true when only cancelled visits in slot`() = runTest {
        // Arrange
        val slotStart = testClock.now().plus(2.hours)
        val slotEnd = slotStart.plus(1.hours)

        repository.save(
            TestFixtures.createVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = slotStart,
                status = VisitStatus.CANCELLED,
            )
        )

        // Act
        val result = repository.isSlotAvailable(testBeneficiaryId, slotStart, slotEnd)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true)
    }

    // ============================================================
    // CONFLICTING VISITS TESTS
    // ============================================================

    @Test
    fun `should return conflicting visits`() = runTest {
        // Arrange
        val slotStart = testClock.now().plus(2.hours)
        val slotEnd = slotStart.plus(1.hours)

        val conflictingVisit = TestFixtures.createApprovedVisit(
            id = "conflict-1",
            beneficiaryId = testBeneficiaryId,
            startTime = slotStart.plus(30.minutes), // Overlaps
            duration = 1.hours,
        )
        repository.save(conflictingVisit)

        // Act
        val result = repository.getConflictingVisits(testBeneficiaryId, slotStart, slotEnd)

        // Assert
        assertTrue(result.isSuccess)
        val conflicts = result.getOrNull()!!
        assertEquals(1, conflicts.size)
        assertEquals("conflict-1", conflicts.first().id)
    }

    @Test
    fun `should not return non-overlapping visits as conflicts`() = runTest {
        // Arrange
        val slotStart = testClock.now().plus(2.hours)
        val slotEnd = slotStart.plus(1.hours)

        // Visit before the slot
        repository.save(
            TestFixtures.createApprovedVisit(
                id = "before",
                beneficiaryId = testBeneficiaryId,
                startTime = slotStart.minus(2.hours),
                duration = 1.hours,
            )
        )

        // Visit after the slot
        repository.save(
            TestFixtures.createApprovedVisit(
                id = "after",
                beneficiaryId = testBeneficiaryId,
                startTime = slotEnd.plus(1.hours),
                duration = 1.hours,
            )
        )

        // Act
        val result = repository.getConflictingVisits(testBeneficiaryId, slotStart, slotEnd)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    // ============================================================
    // FLOW OBSERVATION TESTS
    // ============================================================

    @Test
    fun `should observe visits by beneficiary`() = runTest {
        // Arrange
        val visit1 = TestFixtures.createVisit(id = "v1", beneficiaryId = testBeneficiaryId)
        repository.save(visit1)

        // Act
        val visits = repository.observeByBeneficiary(testBeneficiaryId).first()

        // Assert
        assertEquals(1, visits.size)
        assertEquals("v1", visits.first().id)
    }

    @Test
    fun `should emit updates when visits change`() = runTest {
        // Arrange
        repository.save(TestFixtures.createVisit(id = "initial", beneficiaryId = testBeneficiaryId))

        // Act - Add another visit
        repository.save(TestFixtures.createVisit(id = "new", beneficiaryId = testBeneficiaryId))
        val visits = repository.observeByBeneficiary(testBeneficiaryId).first()

        // Assert
        assertEquals(2, visits.size)
    }

    @Test
    fun `should observe only pending visits`() = runTest {
        // Arrange
        repository.save(TestFixtures.createVisit(id = "p1", beneficiaryId = testBeneficiaryId, status = VisitStatus.PENDING))
        repository.save(TestFixtures.createApprovedVisit(id = "a1", beneficiaryId = testBeneficiaryId))
        repository.save(TestFixtures.createDeniedVisit(id = "d1", beneficiaryId = testBeneficiaryId))

        // Act
        val pendingVisits = repository.observePending(testBeneficiaryId).first()

        // Assert
        assertEquals(1, pendingVisits.size)
        assertEquals("p1", pendingVisits.first().id)
    }

    // ============================================================
    // ERROR HANDLING TESTS
    // ============================================================

    @Test
    fun `should handle repository failure gracefully`() = runTest {
        // Arrange
        repository.shouldFail = true
        repository.failureException = RuntimeException("Database error")

        // Act
        val result = repository.save(TestFixtures.createVisit())

        // Assert
        assertTrue(result.isError)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should propagate specific exception types`() = runTest {
        // Arrange
        repository.shouldFail = true
        repository.failureException = IllegalStateException("Connection closed")

        // Act
        val result = repository.getById("any-id")

        // Assert
        assertTrue(result.isError)
        assertIs<IllegalStateException>(result.exceptionOrNull())
    }
}
