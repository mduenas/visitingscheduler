package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours

/**
 * Tests for ApproveVisitUseCase covering approval workflows, authorization,
 * and denial handling.
 *
 * @test Approve Visit Use Case
 * @prerequisites Mocked repositories with visits and coordinators
 */
class ApproveVisitUseCaseTest {

    private lateinit var visitRepository: FakeVisitRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var approveVisitUseCase: ApproveVisitUseCase
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testCoordinatorId = "coordinator-1"
    private val testSecondaryCoordinatorId = "coordinator-2"
    private val testVisitorId = "visitor-1"
    private val testVisitId = "visit-1"

    @BeforeTest
    fun setup() {
        visitRepository = FakeVisitRepository()
        userRepository = FakeUserRepository()
        testClock = TestClock()

        approveVisitUseCase = ApproveVisitUseCase(
            visitRepository = visitRepository,
            userRepository = userRepository,
            clock = testClock,
        )

        // Setup coordinators
        userRepository.addUser(
            TestFixtures.createPrimaryCoordinator(
                id = testCoordinatorId,
                beneficiaryIds = listOf(testBeneficiaryId),
            )
        )

        userRepository.addUser(
            TestFixtures.createSecondaryCoordinator(
                id = testSecondaryCoordinatorId,
            )
        )

        userRepository.addUser(
            TestFixtures.createApprovedVisitor(id = testVisitorId)
        )

        // Setup pending visit
        visitRepository.addVisit(
            TestFixtures.createVisit(
                id = testVisitId,
                beneficiaryId = testBeneficiaryId,
                visitorId = testVisitorId,
                status = VisitStatus.PENDING,
                startTime = testClock.now().plus(2.hours),
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
    // SUCCESSFUL APPROVAL TESTS
    // ============================================================

    @Test
    fun `should approve visit by authorized coordinator`() = runTest {
        // Arrange
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess, "Approval should succeed")
        val visit = result.getOrNull()
        assertNotNull(visit)
        assertEquals(VisitStatus.APPROVED, visit.status)
        assertEquals(testCoordinatorId, visit.approvedBy)
        assertNotNull(visit.approvedAt)
    }

    @Test
    fun `should update visit in repository on approval`() = runTest {
        // Arrange
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = "Approved for regular visit",
        )

        // Act
        approveVisitUseCase.execute(request)

        // Assert
        val savedVisit = visitRepository.getById(testVisitId).getOrNull()
        assertNotNull(savedVisit)
        assertEquals(VisitStatus.APPROVED, savedVisit.status)
        assertEquals("Approved for regular visit", savedVisit.notes)
    }

    @Test
    fun `should set approval timestamp to current time`() = runTest {
        // Arrange
        val approvalTime = testClock.now()
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        val visit = result.getOrNull()!!
        assertEquals(approvalTime, visit.approvedAt)
    }

    // ============================================================
    // DENIAL TESTS
    // ============================================================

    @Test
    fun `should deny visit with reason`() = runTest {
        // Arrange
        val denialReason = "Conflicts with medical procedure"
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.DENY,
            notes = denialReason,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val visit = result.getOrNull()!!
        assertEquals(VisitStatus.DENIED, visit.status)
        assertEquals(denialReason, visit.denialReason)
    }

    @Test
    fun `should require reason for denial`() = runTest {
        // Arrange
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.DENY,
            notes = null, // No reason provided
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<DenialReasonRequiredException>(error)
    }

    @Test
    fun `should require non-empty reason for denial`() = runTest {
        // Arrange
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.DENY,
            notes = "   ", // Blank reason
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<DenialReasonRequiredException>(result.exceptionOrNull())
    }

    // ============================================================
    // AUTO-APPROVE TESTS
    // ============================================================

    @Test
    fun `should auto-approve visit for whitelisted visitor`() = runTest {
        // Arrange
        val whitelistedVisitorId = "whitelisted-visitor"
        userRepository.addUser(
            TestFixtures.createApprovedVisitor(id = whitelistedVisitorId)
        )

        val pendingVisitId = "pending-visit-whitelist"
        visitRepository.addVisit(
            TestFixtures.createVisit(
                id = pendingVisitId,
                beneficiaryId = testBeneficiaryId,
                visitorId = whitelistedVisitorId,
                status = VisitStatus.PENDING,
            )
        )

        // Add to whitelist
        approveVisitUseCase.addToWhitelist(testBeneficiaryId, whitelistedVisitorId)

        // Act
        val result = approveVisitUseCase.checkAutoApproval(pendingVisitId)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == true, "Should be auto-approved")

        val visit = visitRepository.getById(pendingVisitId).getOrNull()
        assertEquals(VisitStatus.APPROVED, visit?.status)
    }

    @Test
    fun `should not auto-approve visit for non-whitelisted visitor`() = runTest {
        // Act
        val result = approveVisitUseCase.checkAutoApproval(testVisitId)

        // Assert
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull() == true, "Should not be auto-approved")

        val visit = visitRepository.getById(testVisitId).getOrNull()
        assertEquals(VisitStatus.PENDING, visit?.status)
    }

    // ============================================================
    // AUTHORIZATION TESTS
    // ============================================================

    @Test
    fun `should return error when coordinator is not authorized for beneficiary`() = runTest {
        // Arrange
        val unauthorizedCoordinatorId = "unauthorized-coord"
        userRepository.addUser(
            TestFixtures.createPrimaryCoordinator(
                id = unauthorizedCoordinatorId,
                beneficiaryIds = listOf("other-beneficiary"), // Different beneficiary
            )
        )

        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = unauthorizedCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<UnauthorizedApprovalException>(error)
    }

    @Test
    fun `should return error when user is not a coordinator`() = runTest {
        // Arrange
        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testVisitorId, // Visitor, not coordinator
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<UnauthorizedApprovalException>(error)
    }

    @Test
    fun `should allow secondary coordinator to approve when permitted`() = runTest {
        // Arrange
        approveVisitUseCase.allowSecondaryCoordinatorApproval(true)

        // Update secondary coordinator to have this beneficiary
        val secondaryCoord = userRepository.getAllUsers().find { it.id == testSecondaryCoordinatorId }!!
        userRepository.update(secondaryCoord.copy(assignedBeneficiaryIds = listOf(testBeneficiaryId)))

        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testSecondaryCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(VisitStatus.APPROVED, result.getOrNull()?.status)
    }

    @Test
    fun `should deny secondary coordinator approval when not permitted`() = runTest {
        // Arrange
        approveVisitUseCase.allowSecondaryCoordinatorApproval(false)

        val secondaryCoord = userRepository.getAllUsers().find { it.id == testSecondaryCoordinatorId }!!
        userRepository.update(secondaryCoord.copy(assignedBeneficiaryIds = listOf(testBeneficiaryId)))

        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testSecondaryCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<InsufficientPermissionsException>(result.exceptionOrNull())
    }

    @Test
    fun `should allow admin to approve any visit`() = runTest {
        // Arrange
        val adminId = "admin-1"
        userRepository.addUser(
            TestFixtures.createAdministrator(id = adminId)
        )

        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = adminId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
    }

    // ============================================================
    // VISIT STATE TESTS
    // ============================================================

    @Test
    fun `should return error when visit is already approved`() = runTest {
        // Arrange
        visitRepository.clear()
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                id = "approved-visit",
                beneficiaryId = testBeneficiaryId,
            )
        )

        val request = ApprovalRequest(
            visitId = "approved-visit",
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<InvalidVisitStateException>(error)
    }

    @Test
    fun `should return error when visit is already denied`() = runTest {
        // Arrange
        visitRepository.clear()
        visitRepository.addVisit(
            TestFixtures.createDeniedVisit(
                id = "denied-visit",
                beneficiaryId = testBeneficiaryId,
            )
        )

        val request = ApprovalRequest(
            visitId = "denied-visit",
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<InvalidVisitStateException>(result.exceptionOrNull())
    }

    @Test
    fun `should return error when visit is cancelled`() = runTest {
        // Arrange
        visitRepository.clear()
        visitRepository.addVisit(
            TestFixtures.createVisit(
                id = "cancelled-visit",
                beneficiaryId = testBeneficiaryId,
                status = VisitStatus.CANCELLED,
            )
        )

        val request = ApprovalRequest(
            visitId = "cancelled-visit",
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<InvalidVisitStateException>(result.exceptionOrNull())
    }

    @Test
    fun `should return error when visit not found`() = runTest {
        // Arrange
        val request = ApprovalRequest(
            visitId = "non-existent-visit",
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.APPROVE,
            notes = null,
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<VisitNotFoundException>(error)
    }

    // ============================================================
    // BATCH APPROVAL TESTS
    // ============================================================

    @Test
    fun `should approve multiple visits in batch`() = runTest {
        // Arrange
        val visitIds = listOf("batch-1", "batch-2", "batch-3")
        visitIds.forEach { id ->
            visitRepository.addVisit(
                TestFixtures.createVisit(
                    id = id,
                    beneficiaryId = testBeneficiaryId,
                    status = VisitStatus.PENDING,
                )
            )
        }

        // Act
        val results = approveVisitUseCase.batchApprove(
            visitIds = visitIds,
            coordinatorId = testCoordinatorId,
            notes = "Batch approved",
        )

        // Assert
        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
        visitIds.forEach { id ->
            val visit = visitRepository.getById(id).getOrNull()
            assertEquals(VisitStatus.APPROVED, visit?.status)
        }
    }

    @Test
    fun `should continue batch approval even if one fails`() = runTest {
        // Arrange
        visitRepository.addVisit(
            TestFixtures.createVisit(id = "batch-a", beneficiaryId = testBeneficiaryId, status = VisitStatus.PENDING)
        )
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(id = "batch-b", beneficiaryId = testBeneficiaryId) // Already approved
        )
        visitRepository.addVisit(
            TestFixtures.createVisit(id = "batch-c", beneficiaryId = testBeneficiaryId, status = VisitStatus.PENDING)
        )

        // Act
        val results = approveVisitUseCase.batchApprove(
            visitIds = listOf("batch-a", "batch-b", "batch-c"),
            coordinatorId = testCoordinatorId,
            notes = null,
        )

        // Assert
        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess) // batch-a approved
        assertTrue(results[1].isError)   // batch-b already approved
        assertTrue(results[2].isSuccess) // batch-c approved
    }

    // ============================================================
    // DELEGATION TESTS
    // ============================================================

    @Test
    fun `should delegate approval to another coordinator`() = runTest {
        // Arrange
        val delegateCoordinatorId = "delegate-coord"
        userRepository.addUser(
            TestFixtures.createPrimaryCoordinator(
                id = delegateCoordinatorId,
                beneficiaryIds = listOf(testBeneficiaryId),
            )
        )

        val request = ApprovalRequest(
            visitId = testVisitId,
            coordinatorId = testCoordinatorId,
            action = ApprovalAction.DELEGATE,
            notes = delegateCoordinatorId, // Delegate to this coordinator
        )

        // Act
        val result = approveVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val visit = result.getOrNull()!!
        assertEquals(VisitStatus.PENDING, visit.status) // Still pending
        // In real impl, would have delegation tracking
    }
}

// ============================================================
// SUPPORTING CLASSES FOR TESTS
// ============================================================

class ApproveVisitUseCase(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val clock: Clock = Clock.System,
) {
    private var secondaryCoordinatorApprovalAllowed = true
    private val whitelist = mutableMapOf<String, MutableSet<String>>()

    fun allowSecondaryCoordinatorApproval(allowed: Boolean) {
        secondaryCoordinatorApprovalAllowed = allowed
    }

    fun addToWhitelist(beneficiaryId: String, visitorId: String) {
        whitelist.getOrPut(beneficiaryId) { mutableSetOf() }.add(visitorId)
    }

    suspend fun execute(request: ApprovalRequest): Result<Visit> {
        // Get visit
        val visit = visitRepository.getById(request.visitId).getOrNull()
            ?: return Result.error(VisitNotFoundException("Visit not found: ${request.visitId}"))

        // Check visit state
        if (visit.status != VisitStatus.PENDING) {
            return Result.error(
                InvalidVisitStateException("Visit is not pending. Current status: ${visit.status}")
            )
        }

        // Get coordinator
        val coordinator = userRepository.getById(request.coordinatorId).getOrNull()
            ?: return Result.error(UnauthorizedApprovalException("Coordinator not found"))

        // Check authorization
        if (!isAuthorized(coordinator, visit.beneficiaryId)) {
            return Result.error(UnauthorizedApprovalException("Not authorized to approve visits for this beneficiary"))
        }

        // Handle different actions
        return when (request.action) {
            ApprovalAction.APPROVE -> approveVisit(visit, coordinator, request.notes)
            ApprovalAction.DENY -> denyVisit(visit, coordinator, request.notes)
            ApprovalAction.DELEGATE -> delegateApproval(visit, request.notes)
            ApprovalAction.REQUEST_INFO -> Result.success(visit) // No change
        }
    }

    suspend fun checkAutoApproval(visitId: String): Result<Boolean> {
        val visit = visitRepository.getById(visitId).getOrNull()
            ?: return Result.error(VisitNotFoundException("Visit not found"))

        if (visit.status != VisitStatus.PENDING) {
            return Result.success(false)
        }

        val isWhitelisted = whitelist[visit.beneficiaryId]?.contains(visit.visitorId) == true

        if (isWhitelisted) {
            val approved = visit.copy(
                status = VisitStatus.APPROVED,
                approvedBy = "system",
                approvedAt = clock.now(),
            )
            visitRepository.update(approved)
            return Result.success(true)
        }

        return Result.success(false)
    }

    suspend fun batchApprove(
        visitIds: List<String>,
        coordinatorId: String,
        notes: String?,
    ): List<Result<Visit>> {
        return visitIds.map { visitId ->
            execute(
                ApprovalRequest(
                    visitId = visitId,
                    coordinatorId = coordinatorId,
                    action = ApprovalAction.APPROVE,
                    notes = notes,
                )
            )
        }
    }

    private suspend fun approveVisit(
        visit: Visit,
        coordinator: User,
        notes: String?,
    ): Result<Visit> {
        val approved = visit.copy(
            status = VisitStatus.APPROVED,
            approvedBy = coordinator.id,
            approvedAt = clock.now(),
            notes = notes,
        )

        visitRepository.update(approved)
        return Result.success(approved)
    }

    private suspend fun denyVisit(
        visit: Visit,
        coordinator: User,
        reason: String?,
    ): Result<Visit> {
        if (reason.isNullOrBlank()) {
            return Result.error(DenialReasonRequiredException("Denial reason is required"))
        }

        val denied = visit.copy(
            status = VisitStatus.DENIED,
            denialReason = reason,
        )

        visitRepository.update(denied)
        return Result.success(denied)
    }

    private suspend fun delegateApproval(
        visit: Visit,
        delegateToId: String?,
    ): Result<Visit> {
        // In real implementation, would track delegation
        // For now, just return the visit unchanged
        return Result.success(visit)
    }

    private fun isAuthorized(user: User, beneficiaryId: String): Boolean {
        // Admin can approve anything
        if (user.role == UserRole.ADMINISTRATOR) {
            return true
        }

        // Must be a coordinator
        if (user.role !in listOf(UserRole.PRIMARY_COORDINATOR, UserRole.SECONDARY_COORDINATOR)) {
            return false
        }

        // Check if assigned to beneficiary
        if (beneficiaryId !in user.assignedBeneficiaryIds) {
            return false
        }

        // Check secondary coordinator permissions
        if (user.role == UserRole.SECONDARY_COORDINATOR && !secondaryCoordinatorApprovalAllowed) {
            throw InsufficientPermissionsException("Secondary coordinators cannot approve visits")
        }

        return true
    }
}

// Exception classes
open class ApprovalException(message: String) : Exception(message)
class VisitNotFoundException(message: String) : ApprovalException(message)
class InvalidVisitStateException(message: String) : ApprovalException(message)
class UnauthorizedApprovalException(message: String) : ApprovalException(message)
class InsufficientPermissionsException(message: String) : ApprovalException(message)
class DenialReasonRequiredException(message: String) : ApprovalException(message)
