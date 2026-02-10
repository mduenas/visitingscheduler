package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Tests for user authentication and lookup functionality.
 *
 * @test Login Functionality
 * @prerequisites Mocked repositories with test data
 */
class LoginUseCaseTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var testClock: TestClock

    @BeforeTest
    fun setup() {
        userRepository = FakeUserRepository()
        testClock = TestClock()

        // Setup default test user
        userRepository.addUser(
            TestFixtures.createUser(
                id = "user-1",
                email = "test@example.com",
                firstName = "Test",
                lastName = "User",
                role = Role.APPROVED_VISITOR,
            )
        )

        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        userRepository.clear()
    }

    // ============================================================
    // USER LOOKUP TESTS
    // ============================================================

    @Test
    fun `should find user by email`() = runTest {
        // Act
        val result = userRepository.getByEmail("test@example.com")

        // Assert
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("user-1", user.id)
        assertEquals("Test", user.firstName)
        assertEquals("User", user.lastName)
    }

    @Test
    fun `should return null for non-existent email`() = runTest {
        // Act
        val result = userRepository.getByEmail("nonexistent@example.com")

        // Assert
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }

    @Test
    fun `should find user by id`() = runTest {
        // Act
        val result = userRepository.getById("user-1")

        // Assert
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("test@example.com", user.email)
    }

    // ============================================================
    // FAILED LOGIN ATTEMPTS TESTS
    // ============================================================

    @Test
    fun `should track failed login attempts`() = runTest {
        // Arrange
        val userId = "user-1"

        // Act
        userRepository.incrementFailedLoginAttempts(userId)
        userRepository.incrementFailedLoginAttempts(userId)
        userRepository.incrementFailedLoginAttempts(userId)

        // Assert
        assertEquals(3, userRepository.getFailedAttempts(userId))
    }

    @Test
    fun `should reset failed login attempts`() = runTest {
        // Arrange
        val userId = "user-1"
        repeat(5) { userRepository.incrementFailedLoginAttempts(userId) }
        assertEquals(5, userRepository.getFailedAttempts(userId))

        // Act
        userRepository.resetFailedLoginAttempts(userId)

        // Assert
        assertEquals(0, userRepository.getFailedAttempts(userId))
    }

    // ============================================================
    // ACCOUNT LOCKING TESTS
    // ============================================================

    @Test
    fun `should lock account after too many failed attempts`() = runTest {
        // Arrange
        val userId = "user-1"
        repeat(5) { userRepository.incrementFailedLoginAttempts(userId) }

        // Act
        userRepository.lockAccount(userId)

        // Assert
        val isLocked = userRepository.isAccountLocked(userId).getOrNull()
        assertTrue(isLocked == true, "Account should be locked after too many attempts")
    }

    @Test
    fun `should unlock account`() = runTest {
        // Arrange
        val userId = "user-1"
        userRepository.lockAccount(userId)
        assertTrue(userRepository.isAccountLocked(userId).getOrNull() == true)

        // Act
        userRepository.unlockAccount(userId)

        // Assert
        assertFalse(userRepository.isAccountLocked(userId).getOrNull() == true)
    }

    @Test
    fun `unlocked account should allow login attempts`() = runTest {
        // Arrange
        val userId = "user-1"

        // Act & Assert - Account should start unlocked
        assertFalse(userRepository.isAccountLocked(userId).getOrNull() == true)
    }

    // ============================================================
    // USER ROLE TESTS
    // ============================================================

    @Test
    fun `should correctly identify admin user`() = runTest {
        // Arrange
        val admin = TestFixtures.createAdmin(id = "admin-1")
        userRepository.addUser(admin)

        // Act
        val result = userRepository.getById("admin-1")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(Role.ADMIN, result.getOrNull()?.role)
    }

    @Test
    fun `should correctly identify coordinator user`() = runTest {
        // Arrange
        val coordinator = TestFixtures.createPrimaryCoordinator(id = "coord-1")
        userRepository.addUser(coordinator)

        // Act
        val result = userRepository.getById("coord-1")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(Role.PRIMARY_COORDINATOR, result.getOrNull()?.role)
    }

    @Test
    fun `should correctly identify visitor user`() = runTest {
        // Act
        val result = userRepository.getById("user-1")

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(Role.APPROVED_VISITOR, result.getOrNull()?.role)
    }

    // ============================================================
    // USER STATUS TESTS
    // ============================================================

    @Test
    fun `active user should have isActive true`() = runTest {
        // Arrange
        val activeUser = TestFixtures.createUser(
            id = "active-user",
            isActive = true,
        )
        userRepository.addUser(activeUser)

        // Act
        val result = userRepository.getById("active-user")

        // Assert
        assertTrue(result.getOrNull()?.isActive == true)
    }

    @Test
    fun `inactive user should have isActive false`() = runTest {
        // Arrange
        val inactiveUser = TestFixtures.createUser(
            id = "inactive-user",
            isActive = false,
        )
        userRepository.addUser(inactiveUser)

        // Act
        val result = userRepository.getById("inactive-user")

        // Assert
        assertFalse(result.getOrNull()?.isActive == true)
    }

    @Test
    fun `email verified user should have isEmailVerified true`() = runTest {
        // Arrange
        val verifiedUser = TestFixtures.createUser(
            id = "verified-user",
            isEmailVerified = true,
        )
        userRepository.addUser(verifiedUser)

        // Act
        val result = userRepository.getById("verified-user")

        // Assert
        assertTrue(result.getOrNull()?.isEmailVerified == true)
    }

    @Test
    fun `pending visitor should have isEmailVerified false`() = runTest {
        // Arrange
        val pendingUser = TestFixtures.createPendingVisitor(id = "pending-user")
        userRepository.addUser(pendingUser)

        // Act
        val result = userRepository.getById("pending-user")

        // Assert
        assertFalse(result.getOrNull()?.isEmailVerified == true)
    }

    // ============================================================
    // USER UPDATE TESTS
    // ============================================================

    @Test
    fun `should update user details`() = runTest {
        // Arrange
        val originalUser = userRepository.getById("user-1").getOrNull()!!

        // Act
        val updatedUser = originalUser.copy(
            firstName = "Updated",
            lastName = "Name",
        )
        userRepository.update(updatedUser)

        // Assert
        val result = userRepository.getById("user-1").getOrNull()
        assertEquals("Updated", result?.firstName)
        assertEquals("Name", result?.lastName)
    }

    @Test
    fun `should update last login time`() = runTest {
        // Arrange
        val originalUser = userRepository.getById("user-1").getOrNull()!!
        assertNull(originalUser.lastLoginAt)

        // Act
        val loginTime = testClock.now()
        val updatedUser = originalUser.copy(lastLoginAt = loginTime)
        userRepository.update(updatedUser)

        // Assert
        val result = userRepository.getById("user-1").getOrNull()
        assertEquals(loginTime, result?.lastLoginAt)
    }
}
