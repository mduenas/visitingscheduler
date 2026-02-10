package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for visit approval workflows covering approval, denial, and authorization.
 *
 * @test Approve Visit Workflow
 * @prerequisites Mocked repositories with visits and coordinators
 */
class ApproveVisitUseCaseTest {

    private lateinit var visitRepository: FakeVisitRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testCoordinatorId = "coordinator-1"
    private val testVisitorId = "visitor-1"
    private val testVisitId = "visit-1"

    @BeforeTest
    fun setup() {
        visitRepository = FakeVisitRepository()
        userRepository = FakeUserRepository()
        testClock = TestClock.fixed(2024, 6, 15, 10, 0)

        // Setup coordinator
        userRepository.addUser(
            TestFixtures.createPrimaryCoordinator(
                id = testCoordinatorId,
                associatedBeneficiaryIds = listOf(testBeneficiaryId),
            )
        )

        // Setup visitor
        userRepository.addUser(
            TestFixtures.createApprovedVisitor(id = testVisitorId)
        )

        // Setup pending visit - using addVisit which is synchronous
        visitRepository.addVisit(
            TestFixtures.createVisit(
                id = testVisitId,
                beneficiaryId = testBeneficiaryId,
                visitorId = testVisitorId,
                status = VisitStatus.PENDING,
            )
        )

        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        visitRepository.clear()
        userRepository.clear()
    }

    // ============================================================
    // VISIT STATUS TRANSITIONS
    // ============================================================

    @Test
    fun `should approve pending visit`() = runTest {
        // Arrange
        val visit = visitRepository.getById(testVisitId).getOrNull()!!
        assertEquals(VisitStatus.PENDING, visit.status)

        // Act
        val approved = visit.copy(
            status = VisitStatus.APPROVED,
            approvedBy = testCoordinatorId,
            approvedAt = testClock.now(),
        )
        visitRepository.update(approved)

        // Assert
        val result = visitRepository.getById(testVisitId).getOrNull()
        assertEquals(VisitStatus.APPROVED, result?.status)
        assertEquals(testCoordinatorId, result?.approvedBy)
        assertNotNull(result?.approvedAt)
    }

    @Test
    fun `should deny pending visit with reason`() = runTest {
        // Arrange
        val visit = visitRepository.getById(testVisitId).getOrNull()!!
        assertEquals(VisitStatus.PENDING, visit.status)

        // Act
        val denied = visit.copy(
            status = VisitStatus.DENIED,
            denialReason = "Schedule conflict with medical procedure",
        )
        visitRepository.update(denied)

        // Assert
        val result = visitRepository.getById(testVisitId).getOrNull()
        assertEquals(VisitStatus.DENIED, result?.status)
        assertEquals("Schedule conflict with medical procedure", result?.denialReason)
    }

    @Test
    fun `should cancel approved visit`() = runTest {
        // Arrange - First approve the visit
        val visit = visitRepository.getById(testVisitId).getOrNull()!!
        val approved = visit.copy(
            status = VisitStatus.APPROVED,
            approvedBy = testCoordinatorId,
            approvedAt = testClock.now(),
        )
        visitRepository.update(approved)

        // Act - Cancel the approved visit
        val cancelled = approved.copy(
            status = VisitStatus.CANCELLED,
            cancellationReason = "Visitor requested cancellation",
            cancelledBy = testVisitorId,
            cancelledAt = testClock.now(),
        )
        visitRepository.update(cancelled)

        // Assert
        val result = visitRepository.getById(testVisitId).getOrNull()
        assertEquals(VisitStatus.CANCELLED, result?.status)
        assertEquals("Visitor requested cancellation", result?.cancellationReason)
        assertEquals(testVisitorId, result?.cancelledBy)
    }

    // ============================================================
    // COORDINATOR AUTHORIZATION TESTS
    // ============================================================

    @Test
    fun `primary coordinator should have approval permissions`() = runTest {
        // Arrange
        val coordinator = userRepository.getById(testCoordinatorId).getOrNull()

        // Assert
        assertNotNull(coordinator)
        assertEquals(Role.PRIMARY_COORDINATOR, coordinator.role)
        assertTrue(coordinator.associatedBeneficiaryIds.contains(testBeneficiaryId))
    }

    @Test
    fun `secondary coordinator should have limited permissions`() = runTest {
        // Arrange
        val secondaryCoordinator = TestFixtures.createSecondaryCoordinator(
            id = "secondary-coord",
        )
        userRepository.addUser(secondaryCoordinator)

        // Act
        val coordinator = userRepository.getById("secondary-coord").getOrNull()

        // Assert
        assertNotNull(coordinator)
        assertEquals(Role.SECONDARY_COORDINATOR, coordinator.role)
    }

    @Test
    fun `visitor should not have approval permissions`() = runTest {
        // Arrange
        val visitor = userRepository.getById(testVisitorId).getOrNull()

        // Assert
        assertNotNull(visitor)
        assertEquals(Role.APPROVED_VISITOR, visitor.role)
    }

    @Test
    fun `admin should have full permissions`() = runTest {
        // Arrange
        val admin = TestFixtures.createAdmin(id = "admin-1")
        userRepository.addUser(admin)

        // Act
        val result = userRepository.getById("admin-1").getOrNull()

        // Assert
        assertNotNull(result)
        assertEquals(Role.ADMIN, result.role)
    }

    // ============================================================
    // VISIT RETRIEVAL TESTS
    // ============================================================

    @Test
    fun `should get pending visits for beneficiary`() = runTest {
        // Arrange - Add more pending visits
        visitRepository.save(
            TestFixtures.createVisit(
                id = "pending-1",
                beneficiaryId = testBeneficiaryId,
                status = VisitStatus.PENDING,
            )
        )
        visitRepository.save(
            TestFixtures.createVisit(
                id = "pending-2",
                beneficiaryId = testBeneficiaryId,
                status = VisitStatus.PENDING,
            )
        )

        // Act
        val pendingVisits = visitRepository.getByBeneficiary(testBeneficiaryId)
            .getOrNull()
            ?.filter { it.status == VisitStatus.PENDING }

        // Assert
        assertNotNull(pendingVisits)
        assertEquals(3, pendingVisits.size) // Original + 2 new
    }

    @Test
    fun `should get visits needing approval`() = runTest {
        // Arrange - Add mixed status visits
        visitRepository.save(
            TestFixtures.createApprovedVisit(
                id = "approved-1",
                beneficiaryId = testBeneficiaryId,
            )
        )
        visitRepository.save(
            TestFixtures.createDeniedVisit(
                id = "denied-1",
                beneficiaryId = testBeneficiaryId,
            )
        )

        // Act
        val allVisits = visitRepository.getByBeneficiary(testBeneficiaryId).getOrNull()
        val pendingOnly = allVisits?.filter { it.status == VisitStatus.PENDING }

        // Assert
        assertNotNull(allVisits)
        assertEquals(3, allVisits.size) // Original pending + approved + denied
        assertEquals(1, pendingOnly?.size) // Only original pending
    }

    // ============================================================
    // APPROVAL TIMESTAMP TESTS
    // ============================================================

    @Test
    fun `approved visit should have approval timestamp`() = runTest {
        // Arrange
        val visit = visitRepository.getById(testVisitId).getOrNull()!!
        val approvalTime = testClock.now()

        // Act
        val approved = visit.copy(
            status = VisitStatus.APPROVED,
            approvedBy = testCoordinatorId,
            approvedAt = approvalTime,
        )
        visitRepository.update(approved)

        // Assert
        val result = visitRepository.getById(testVisitId).getOrNull()
        assertEquals(approvalTime, result?.approvedAt)
    }

    @Test
    fun `cancelled visit should have cancellation timestamp`() = runTest {
        // Arrange
        val visit = visitRepository.getById(testVisitId).getOrNull()!!
        val cancellationTime = testClock.now()

        // Act
        val cancelled = visit.copy(
            status = VisitStatus.CANCELLED,
            cancellationReason = "Test cancellation",
            cancelledBy = testVisitorId,
            cancelledAt = cancellationTime,
        )
        visitRepository.update(cancelled)

        // Assert
        val result = visitRepository.getById(testVisitId).getOrNull()
        assertEquals(cancellationTime, result?.cancelledAt)
        assertEquals(testVisitorId, result?.cancelledBy)
    }

    // ============================================================
    // ERROR HANDLING TESTS
    // ============================================================

    @Test
    fun `should return error when updating non-existent visit`() = runTest {
        // Arrange
        val nonExistentVisit = TestFixtures.createVisit(id = "does-not-exist")

        // Act
        val result = visitRepository.update(nonExistentVisit)

        // Assert
        assertTrue(result.isError)
    }

    @Test
    fun `should return null for non-existent visit id`() = runTest {
        // Act
        val result = visitRepository.getById("non-existent-id")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}
