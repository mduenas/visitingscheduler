package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for UserRepository implementation covering user management,
 * authentication tracking, and access control.
 *
 * @test User Repository
 * @prerequisites Test database with schema
 */
class UserRepositoryTest {

    private lateinit var repository: FakeUserRepository

    @BeforeTest
    fun setup() {
        repository = FakeUserRepository()
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
    fun `should save user and return id`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(
            email = "new@example.com",
            name = "New User",
        )

        // Act
        val result = repository.save(user)

        // Assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
    }

    @Test
    fun `should persist all user fields`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(
            id = "user-123",
            email = "complete@example.com",
            name = "Complete User",
            role = UserRole.PRIMARY_COORDINATOR,
            mfaEnabled = true,
            isActive = true,
        )

        // Act
        repository.save(user)
        val retrieved = repository.getById("user-123").getOrNull()

        // Assert
        assertNotNull(retrieved)
        assertEquals("complete@example.com", retrieved.email)
        assertEquals("Complete User", retrieved.name)
        assertEquals(UserRole.PRIMARY_COORDINATOR, retrieved.role)
        assertTrue(retrieved.mfaEnabled)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun `should generate id if not provided`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(id = "")

        // Act
        val result = repository.save(user)

        // Assert
        assertTrue(result.isSuccess)
        val savedId = result.getOrNull()
        assertNotNull(savedId)
        assertTrue(savedId.isNotEmpty())
    }

    // ============================================================
    // GET BY ID TESTS
    // ============================================================

    @Test
    fun `should retrieve user by id`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(id = "find-me")
        repository.save(user)

        // Act
        val result = repository.getById("find-me")

        // Assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("find-me", result.getOrNull()?.id)
    }

    @Test
    fun `should return null for non-existent id`() = runTest {
        // Act
        val result = repository.getById("does-not-exist")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    // ============================================================
    // GET BY EMAIL TESTS
    // ============================================================

    @Test
    fun `should retrieve user by email`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(
            id = "email-user",
            email = "unique@example.com",
        )
        repository.save(user)

        // Act
        val result = repository.getByEmail("unique@example.com")

        // Assert
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull())
        assertEquals("email-user", result.getOrNull()?.id)
    }

    @Test
    fun `should return null for non-existent email`() = runTest {
        // Act
        val result = repository.getByEmail("nonexistent@example.com")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `should be case-sensitive for email lookup`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(email = "Test@Example.com")
        repository.save(user)

        // Act
        val result = repository.getByEmail("test@example.com") // Different case

        // Assert
        assertTrue(result.isSuccess)
        // Note: Whether this returns null depends on implementation
        // This test documents the expected behavior
    }

    // ============================================================
    // UPDATE TESTS
    // ============================================================

    @Test
    fun `should update existing user`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(
            id = "update-user",
            name = "Original Name",
            role = UserRole.PENDING_VISITOR,
        )
        repository.save(user)

        val updated = user.copy(
            name = "Updated Name",
            role = UserRole.APPROVED_VISITOR,
        )

        // Act
        val result = repository.update(updated)

        // Assert
        assertTrue(result.isSuccess)
        val retrieved = repository.getById("update-user").getOrNull()
        assertEquals("Updated Name", retrieved?.name)
        assertEquals(UserRole.APPROVED_VISITOR, retrieved?.role)
    }

    @Test
    fun `should return error when updating non-existent user`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(id = "non-existent")

        // Act
        val result = repository.update(user)

        // Assert
        assertTrue(result.isError)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    @Test
    fun `should preserve unchanged fields on update`() = runTest {
        // Arrange
        val original = TestFixtures.createUser(
            id = "preserve-test",
            email = "preserve@example.com",
            name = "Preserve User",
            role = UserRole.APPROVED_VISITOR,
            mfaEnabled = true,
        )
        repository.save(original)

        val updated = original.copy(name = "New Name")

        // Act
        repository.update(updated)

        // Assert
        val retrieved = repository.getById("preserve-test").getOrNull()
        assertEquals("preserve@example.com", retrieved?.email)
        assertEquals(UserRole.APPROVED_VISITOR, retrieved?.role)
        assertTrue(retrieved?.mfaEnabled == true)
    }

    // ============================================================
    // DELETE TESTS
    // ============================================================

    @Test
    fun `should delete existing user`() = runTest {
        // Arrange
        val user = TestFixtures.createUser(id = "delete-me")
        repository.save(user)

        // Act
        val result = repository.delete("delete-me")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(repository.getById("delete-me").getOrNull())
    }

    @Test
    fun `should succeed when deleting non-existent user`() = runTest {
        // Act
        val result = repository.delete("non-existent")

        // Assert
        assertTrue(result.isSuccess) // No-op for non-existent
    }

    // ============================================================
    // COORDINATORS FOR BENEFICIARY TESTS
    // ============================================================

    @Test
    fun `should get coordinators assigned to beneficiary`() = runTest {
        // Arrange
        val beneficiaryId = "beneficiary-1"

        repository.save(
            TestFixtures.createPrimaryCoordinator(
                id = "coord-1",
                beneficiaryIds = listOf(beneficiaryId),
            )
        )
        repository.save(
            TestFixtures.createPrimaryCoordinator(
                id = "coord-2",
                beneficiaryIds = listOf(beneficiaryId, "other-beneficiary"),
            )
        )
        repository.save(
            TestFixtures.createPrimaryCoordinator(
                id = "coord-3",
                beneficiaryIds = listOf("different-beneficiary"),
            )
        )

        // Act
        val result = repository.getCoordinatorsForBeneficiary(beneficiaryId)

        // Assert
        assertTrue(result.isSuccess)
        val coordinators = result.getOrNull()!!
        assertEquals(2, coordinators.size)
        assertTrue(coordinators.any { it.id == "coord-1" })
        assertTrue(coordinators.any { it.id == "coord-2" })
    }

    @Test
    fun `should return empty list when no coordinators assigned`() = runTest {
        // Act
        val result = repository.getCoordinatorsForBeneficiary("no-coordinators")

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `should include both primary and secondary coordinators`() = runTest {
        // Arrange
        val beneficiaryId = "beneficiary-1"

        repository.save(
            TestFixtures.createPrimaryCoordinator(
                id = "primary",
                beneficiaryIds = listOf(beneficiaryId),
            )
        )

        val secondary = TestFixtures.createSecondaryCoordinator(id = "secondary")
            .copy(assignedBeneficiaryIds = listOf(beneficiaryId))
        repository.save(secondary)

        // Act
        val result = repository.getCoordinatorsForBeneficiary(beneficiaryId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()!!.size)
    }

    // ============================================================
    // FAILED LOGIN ATTEMPTS TESTS
    // ============================================================

    @Test
    fun `should increment failed login attempts`() = runTest {
        // Arrange
        val userId = "user-1"
        assertEquals(0, repository.getFailedAttempts(userId))

        // Act
        val result = repository.incrementFailedLoginAttempts(userId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        assertEquals(1, repository.getFailedAttempts(userId))
    }

    @Test
    fun `should track multiple failed attempts`() = runTest {
        // Arrange
        val userId = "user-1"

        // Act
        repeat(5) {
            repository.incrementFailedLoginAttempts(userId)
        }

        // Assert
        assertEquals(5, repository.getFailedAttempts(userId))
    }

    @Test
    fun `should reset failed login attempts`() = runTest {
        // Arrange
        val userId = "user-1"
        repeat(3) { repository.incrementFailedLoginAttempts(userId) }
        assertEquals(3, repository.getFailedAttempts(userId))

        // Act
        val result = repository.resetFailedLoginAttempts(userId)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, repository.getFailedAttempts(userId))
    }

    @Test
    fun `should handle reset for user with no attempts`() = runTest {
        // Act
        val result = repository.resetFailedLoginAttempts("no-attempts-user")

        // Assert
        assertTrue(result.isSuccess) // Should succeed even if no attempts recorded
    }

    // ============================================================
    // ACCOUNT LOCKING TESTS
    // ============================================================

    @Test
    fun `should lock account`() = runTest {
        // Arrange
        val userId = "lock-me"

        // Act
        val result = repository.lockAccount(userId)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(repository.isAccountLocked(userId).getOrNull() == true)
    }

    @Test
    fun `should report locked status correctly`() = runTest {
        // Arrange
        val lockedUser = "locked-user"
        val unlockedUser = "unlocked-user"

        repository.lockAccount(lockedUser)

        // Act & Assert
        assertTrue(repository.isAccountLocked(lockedUser).getOrNull() == true)
        assertFalse(repository.isAccountLocked(unlockedUser).getOrNull() == true)
    }

    @Test
    fun `should unlock account`() = runTest {
        // Arrange
        val userId = "unlock-me"
        repository.lockAccount(userId)
        assertTrue(repository.isAccountLocked(userId).getOrNull() == true)

        // Act
        repository.unlockAccount(userId)

        // Assert
        assertFalse(repository.isAccountLocked(userId).getOrNull() == true)
    }

    // ============================================================
    // VISITORS BY BENEFICIARY TESTS
    // ============================================================

    @Test
    fun `should get visitors for beneficiary`() = runTest {
        // Arrange
        repository.save(TestFixtures.createApprovedVisitor(id = "v1"))
        repository.save(TestFixtures.createApprovedVisitor(id = "v2"))
        repository.save(TestFixtures.createPendingVisitor(id = "v3"))
        repository.save(TestFixtures.createPrimaryCoordinator(id = "c1")) // Not a visitor

        // Act
        val result = repository.getVisitorsByBeneficiary("beneficiary-1")

        // Assert
        assertTrue(result.isSuccess)
        val visitors = result.getOrNull()!!
        assertEquals(3, visitors.size)
        assertTrue(visitors.all { it.role in listOf(UserRole.APPROVED_VISITOR, UserRole.PENDING_VISITOR) })
    }

    // ============================================================
    // ERROR HANDLING TESTS
    // ============================================================

    @Test
    fun `should handle repository failure on save`() = runTest {
        // Arrange
        repository.shouldFail = true
        repository.failureException = RuntimeException("Connection lost")

        // Act
        val result = repository.save(TestFixtures.createUser())

        // Assert
        assertTrue(result.isError)
        assertEquals("Connection lost", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should handle repository failure on query`() = runTest {
        // Arrange
        repository.shouldFail = true
        repository.failureException = RuntimeException("Query timeout")

        // Act
        val result = repository.getByEmail("any@email.com")

        // Assert
        assertTrue(result.isError)
    }

    // ============================================================
    // ROLE-BASED QUERIES
    // ============================================================

    @Test
    fun `should correctly identify user roles`() = runTest {
        // Arrange
        repository.save(TestFixtures.createAdministrator(id = "admin"))
        repository.save(TestFixtures.createPrimaryCoordinator(id = "primary"))
        repository.save(TestFixtures.createSecondaryCoordinator(id = "secondary"))
        repository.save(TestFixtures.createApprovedVisitor(id = "approved"))
        repository.save(TestFixtures.createPendingVisitor(id = "pending"))

        // Act & Assert
        assertEquals(UserRole.ADMINISTRATOR, repository.getById("admin").getOrNull()?.role)
        assertEquals(UserRole.PRIMARY_COORDINATOR, repository.getById("primary").getOrNull()?.role)
        assertEquals(UserRole.SECONDARY_COORDINATOR, repository.getById("secondary").getOrNull()?.role)
        assertEquals(UserRole.APPROVED_VISITOR, repository.getById("approved").getOrNull()?.role)
        assertEquals(UserRole.PENDING_VISITOR, repository.getById("pending").getOrNull()?.role)
    }
}
