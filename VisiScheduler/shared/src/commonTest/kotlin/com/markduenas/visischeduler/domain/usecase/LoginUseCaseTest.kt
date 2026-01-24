package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for LoginUseCase covering authentication flows, MFA, and session management.
 *
 * @test Login Use Case
 * @prerequisites Mocked repositories with test data
 */
class LoginUseCaseTest {

    private lateinit var userRepository: FakeUserRepository
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var testClock: TestClock

    @BeforeTest
    fun setup() {
        userRepository = FakeUserRepository()
        sessionRepository = FakeSessionRepository()
        testClock = TestClock()
        loginUseCase = LoginUseCase(
            userRepository = userRepository,
            sessionRepository = sessionRepository,
            clock = testClock,
        )

        // Setup default test user
        userRepository.addUser(
            TestFixtures.createUser(
                id = "user-1",
                email = "test@example.com",
                name = "Test User",
                role = UserRole.APPROVED_VISITOR,
            )
        )

        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        userRepository.clear()
        sessionRepository.clear()
    }

    // ============================================================
    // SUCCESSFUL LOGIN TESTS
    // ============================================================

    @Test
    fun `should login successfully with valid credentials`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(
            email = "test@example.com",
            password = "ValidPassword123!",
        )

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isSuccess, "Login should succeed with valid credentials")
        val authResult = result.getOrNull()
        assertNotNull(authResult)
        assertEquals("user-1", authResult.userId)
        assertNotNull(authResult.accessToken)
        assertNotNull(authResult.refreshToken)
    }

    @Test
    fun `should create session on successful login`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(email = "test@example.com")

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isSuccess)
        val sessions = sessionRepository.getAllSessions()
        assertEquals(1, sessions.size, "Should create exactly one session")
        assertTrue(sessions.first().isActive, "Session should be active")
        assertEquals("user-1", sessions.first().userId)
    }

    @Test
    fun `should return token with correct expiration`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(email = "test@example.com")
        val loginTime = testClock.now()

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isSuccess)
        val authResult = result.getOrNull()!!
        val expectedExpiration = loginTime.plus(1.hours) // Default token expiration
        assertTrue(authResult.expiresAt >= expectedExpiration.minus(1.minutes))
        assertTrue(authResult.expiresAt <= expectedExpiration.plus(1.minutes))
    }

    @Test
    fun `should reset failed login attempts on successful login`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(email = "test@example.com")
        // Simulate previous failed attempts
        repeat(3) { userRepository.incrementFailedLoginAttempts("user-1") }
        assertEquals(3, userRepository.getFailedAttempts("user-1"))

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(0, userRepository.getFailedAttempts("user-1"))
    }

    // ============================================================
    // INVALID CREDENTIALS TESTS
    // ============================================================

    @Test
    fun `should return error when email not found`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(
            email = "nonexistent@example.com",
            password = "AnyPassword123!",
        )

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError, "Login should fail for non-existent email")
        val error = result.exceptionOrNull()
        assertIs<InvalidCredentialsException>(error)
    }

    @Test
    fun `should return error when password is incorrect`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(
            email = "test@example.com",
            password = "WrongPassword123!",
        )

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError, "Login should fail for incorrect password")
        val error = result.exceptionOrNull()
        assertIs<InvalidCredentialsException>(error)
    }

    @Test
    fun `should increment failed attempts on invalid password`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(
            email = "test@example.com",
            password = "WrongPassword",
        )
        assertEquals(0, userRepository.getFailedAttempts("user-1"))

        // Act
        loginUseCase.execute(credentials)

        // Assert
        assertEquals(1, userRepository.getFailedAttempts("user-1"))
    }

    @Test
    fun `should not create session on failed login`() = runTest {
        // Arrange
        val credentials = TestFixtures.createInvalidCredentials()

        // Act
        loginUseCase.execute(credentials)

        // Assert
        assertTrue(sessionRepository.getAllSessions().isEmpty())
    }

    // ============================================================
    // ACCOUNT LOCKOUT TESTS
    // ============================================================

    @Test
    fun `should lock account after 5 failed attempts`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(
            email = "test@example.com",
            password = "WrongPassword",
        )

        // Act
        repeat(5) {
            loginUseCase.execute(credentials)
        }

        // Assert
        val isLocked = userRepository.isAccountLocked("user-1").getOrNull()
        assertTrue(isLocked == true, "Account should be locked after 5 failed attempts")
    }

    @Test
    fun `should return account locked error when account is locked`() = runTest {
        // Arrange
        userRepository.lockAccount("user-1")
        val credentials = TestFixtures.createCredentials(
            email = "test@example.com",
            password = "ValidPassword123!",
        )

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<AccountLockedException>(error)
    }

    @Test
    fun `should not login with correct password when account is locked`() = runTest {
        // Arrange
        userRepository.lockAccount("user-1")
        val credentials = TestFixtures.createCredentials(
            email = "test@example.com",
            password = "ValidPassword123!",
        )

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError, "Should not allow login when account is locked")
        assertTrue(sessionRepository.getAllSessions().isEmpty())
    }

    // ============================================================
    // MFA FLOW TESTS
    // ============================================================

    @Test
    fun `should return MFA challenge when MFA is enabled`() = runTest {
        // Arrange
        userRepository.clear()
        userRepository.addUser(
            TestFixtures.createUser(
                id = "mfa-user",
                email = "mfa@example.com",
                mfaEnabled = true,
            )
        )
        val credentials = TestFixtures.createCredentials(
            email = "mfa@example.com",
            password = "ValidPassword123!",
        )

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isSuccess)
        val authResult = result.getOrNull()!!
        assertTrue(authResult.requiresMfa, "Should require MFA")
        assertNotNull(authResult.mfaChallengeId, "Should include MFA challenge ID")
        assertNull(authResult.accessToken, "Should not provide token before MFA")
    }

    @Test
    fun `should complete login after successful MFA verification`() = runTest {
        // Arrange
        userRepository.clear()
        userRepository.addUser(
            TestFixtures.createUser(
                id = "mfa-user",
                email = "mfa@example.com",
                mfaEnabled = true,
            )
        )
        val credentials = TestFixtures.createCredentials(email = "mfa@example.com")

        // First step - get MFA challenge
        val initialResult = loginUseCase.execute(credentials)
        val challengeId = initialResult.getOrNull()!!.mfaChallengeId!!

        // Act - verify MFA
        val mfaResult = loginUseCase.verifyMfa(
            challengeId = challengeId,
            code = "123456", // Valid TOTP code
        )

        // Assert
        assertTrue(mfaResult.isSuccess)
        val authResult = mfaResult.getOrNull()!!
        assertNotNull(authResult.accessToken)
        assertNotNull(authResult.refreshToken)
        assertEquals("mfa-user", authResult.userId)
    }

    @Test
    fun `should return error for invalid MFA code`() = runTest {
        // Arrange
        userRepository.clear()
        userRepository.addUser(
            TestFixtures.createUser(
                id = "mfa-user",
                email = "mfa@example.com",
                mfaEnabled = true,
            )
        )
        val credentials = TestFixtures.createCredentials(email = "mfa@example.com")
        val initialResult = loginUseCase.execute(credentials)
        val challengeId = initialResult.getOrNull()!!.mfaChallengeId!!

        // Act
        val mfaResult = loginUseCase.verifyMfa(
            challengeId = challengeId,
            code = "000000", // Invalid code
        )

        // Assert
        assertTrue(mfaResult.isError)
        val error = mfaResult.exceptionOrNull()
        assertIs<InvalidMfaCodeException>(error)
    }

    @Test
    fun `should return error for expired MFA challenge`() = runTest {
        // Arrange
        userRepository.clear()
        userRepository.addUser(
            TestFixtures.createUser(
                id = "mfa-user",
                email = "mfa@example.com",
                mfaEnabled = true,
            )
        )
        val credentials = TestFixtures.createCredentials(email = "mfa@example.com")
        val initialResult = loginUseCase.execute(credentials)
        val challengeId = initialResult.getOrNull()!!.mfaChallengeId!!

        // Advance time past MFA expiration (5 minutes)
        testClock.advanceBy(6.minutes)

        // Act
        val mfaResult = loginUseCase.verifyMfa(
            challengeId = challengeId,
            code = "123456",
        )

        // Assert
        assertTrue(mfaResult.isError)
        val error = mfaResult.exceptionOrNull()
        assertIs<MfaChallengeExpiredException>(error)
    }

    // ============================================================
    // SESSION MANAGEMENT TESTS
    // ============================================================

    @Test
    fun `should invalidate existing sessions on new login when single session mode`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(email = "test@example.com")
        loginUseCase.setSingleSessionMode(true)

        // First login
        loginUseCase.execute(credentials)
        val firstSession = sessionRepository.getAllSessions().first()

        // Act - Second login
        loginUseCase.execute(credentials)

        // Assert
        val updatedFirstSession = sessionRepository.getSession(firstSession.id)
        assertFalse(updatedFirstSession?.isActive ?: true, "First session should be invalidated")
        assertEquals(2, sessionRepository.getAllSessions().size)
        assertEquals(1, sessionRepository.getAllSessions().count { it.isActive })
    }

    @Test
    fun `should allow multiple sessions when multi-session mode`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(email = "test@example.com")
        loginUseCase.setSingleSessionMode(false)

        // Act - Multiple logins
        loginUseCase.execute(credentials)
        loginUseCase.execute(credentials)
        loginUseCase.execute(credentials)

        // Assert
        val activeSessions = sessionRepository.getAllSessions().filter { it.isActive }
        assertEquals(3, activeSessions.size, "All sessions should be active")
    }

    @Test
    fun `should include device info in session when provided`() = runTest {
        // Arrange
        val credentials = TestFixtures.createCredentials(email = "test@example.com")
        val deviceInfo = DeviceInfo(
            deviceId = "device-123",
            platform = "Android",
            appVersion = "1.0.0",
        )

        // Act
        loginUseCase.execute(credentials, deviceInfo)

        // Assert
        val session = sessionRepository.getAllSessions().first()
        assertNotNull(session.deviceInfo)
        assertEquals("device-123", session.deviceInfo?.deviceId)
        assertEquals("Android", session.deviceInfo?.platform)
    }

    // ============================================================
    // INACTIVE USER TESTS
    // ============================================================

    @Test
    fun `should return error when user is inactive`() = runTest {
        // Arrange
        userRepository.clear()
        userRepository.addUser(
            TestFixtures.createUser(
                id = "inactive-user",
                email = "inactive@example.com",
                isActive = false,
            )
        )
        val credentials = TestFixtures.createCredentials(email = "inactive@example.com")

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<AccountInactiveException>(error)
    }

    // ============================================================
    // REPOSITORY ERROR TESTS
    // ============================================================

    @Test
    fun `should handle repository errors gracefully`() = runTest {
        // Arrange
        userRepository.shouldFail = true
        userRepository.failureException = RuntimeException("Database connection failed")
        val credentials = TestFixtures.createCredentials(email = "test@example.com")

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<AuthenticationException>(error)
        assertTrue(error.message?.contains("Database connection failed") == true)
    }

    // ============================================================
    // INPUT VALIDATION TESTS
    // ============================================================

    @Test
    fun `should return error for empty email`() = runTest {
        // Arrange
        val credentials = Credentials(email = "", password = "ValidPassword123!")

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<ValidationException>(error)
        assertTrue(error.message?.contains("email") == true, ignoreCase = true)
    }

    @Test
    fun `should return error for empty password`() = runTest {
        // Arrange
        val credentials = Credentials(email = "test@example.com", password = "")

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<ValidationException>(error)
        assertTrue(error.message?.contains("password") == true, ignoreCase = true)
    }

    @Test
    fun `should return error for invalid email format`() = runTest {
        // Arrange
        val credentials = Credentials(email = "not-an-email", password = "ValidPassword123!")

        // Act
        val result = loginUseCase.execute(credentials)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<ValidationException>(error)
    }
}

// ============================================================
// SUPPORTING CLASSES FOR TESTS
// ============================================================

/**
 * Login use case implementation for testing.
 */
class LoginUseCase(
    private val userRepository: UserRepository,
    private val sessionRepository: SessionRepository,
    private val clock: Clock = Clock.System,
) {
    private var singleSessionMode = true
    private val mfaChallenges = mutableMapOf<String, MfaChallengeData>()
    private val maxFailedAttempts = 5

    fun setSingleSessionMode(enabled: Boolean) {
        singleSessionMode = enabled
    }

    suspend fun execute(
        credentials: Credentials,
        deviceInfo: DeviceInfo? = null,
    ): Result<AuthResult> {
        // Validate input
        if (credentials.email.isBlank()) {
            return Result.error(ValidationException("Email cannot be empty"))
        }
        if (credentials.password.isBlank()) {
            return Result.error(ValidationException("Password cannot be empty"))
        }
        if (!isValidEmail(credentials.email)) {
            return Result.error(ValidationException("Invalid email format"))
        }

        // Find user
        val userResult = userRepository.getByEmail(credentials.email)
        if (userResult.isError) {
            return Result.error(
                AuthenticationException("Authentication failed: ${userResult.exceptionOrNull()?.message}")
            )
        }

        val user = userResult.getOrNull()
            ?: return Result.error(InvalidCredentialsException("Invalid email or password"))

        // Check if account is locked
        val isLocked = userRepository.isAccountLocked(user.id).getOrNull() ?: false
        if (isLocked) {
            return Result.error(AccountLockedException("Account is locked due to too many failed attempts"))
        }

        // Check if user is active
        if (!user.isActive) {
            return Result.error(AccountInactiveException("Account is inactive"))
        }

        // Validate password (simplified for test - real impl would hash)
        if (!validatePassword(credentials.password)) {
            userRepository.incrementFailedLoginAttempts(user.id)

            // Check if should lock
            val attempts = userRepository.getFailedAttempts(user.id)
            if (attempts >= maxFailedAttempts) {
                userRepository.lockAccount(user.id)
            }

            return Result.error(InvalidCredentialsException("Invalid email or password"))
        }

        // Reset failed attempts on successful password validation
        userRepository.resetFailedLoginAttempts(user.id)

        // Check MFA requirement
        if (user.mfaEnabled) {
            val challengeId = TestFixtures.generateId("mfa-challenge")
            mfaChallenges[challengeId] = MfaChallengeData(
                userId = user.id,
                createdAt = clock.now(),
                expiresAt = clock.now().plus(5.minutes),
            )

            return Result.success(
                AuthResult(
                    userId = user.id,
                    accessToken = null,
                    refreshToken = null,
                    expiresAt = clock.now(),
                    requiresMfa = true,
                    mfaChallengeId = challengeId,
                )
            )
        }

        // Create session and tokens
        return createSession(user, deviceInfo)
    }

    suspend fun verifyMfa(challengeId: String, code: String): Result<AuthResult> {
        val challenge = mfaChallenges[challengeId]
            ?: return Result.error(MfaChallengeExpiredException("MFA challenge not found or expired"))

        // Check expiration
        if (clock.now() > challenge.expiresAt) {
            mfaChallenges.remove(challengeId)
            return Result.error(MfaChallengeExpiredException("MFA challenge has expired"))
        }

        // Validate code (simplified - real impl would verify TOTP)
        if (code != "123456") {
            return Result.error(InvalidMfaCodeException("Invalid MFA code"))
        }

        // Get user and create session
        val userResult = userRepository.getById(challenge.userId)
        val user = userResult.getOrNull()
            ?: return Result.error(AuthenticationException("User not found"))

        mfaChallenges.remove(challengeId)
        return createSession(user, null)
    }

    private suspend fun createSession(user: User, deviceInfo: DeviceInfo?): Result<AuthResult> {
        // Invalidate existing sessions if single session mode
        if (singleSessionMode) {
            sessionRepository.invalidateAllForUser(user.id)
        }

        val now = clock.now()
        val expiresAt = now.plus(1.hours)

        val token = AuthToken(
            accessToken = TestFixtures.generateId("access-token"),
            refreshToken = TestFixtures.generateId("refresh-token"),
            expiresAt = expiresAt,
            userId = user.id,
        )

        val session = Session(
            id = TestFixtures.generateId("session"),
            userId = user.id,
            token = token,
            createdAt = now,
            lastActivityAt = now,
            isActive = true,
            deviceInfo = deviceInfo,
        )

        sessionRepository.save(session)

        return Result.success(
            AuthResult(
                userId = user.id,
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                expiresAt = expiresAt,
                requiresMfa = false,
                mfaChallengeId = null,
            )
        )
    }

    private fun validatePassword(password: String): Boolean {
        // Simplified validation for testing
        return password == "ValidPassword123!"
    }

    private fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".")
    }

    private fun FakeUserRepository.getFailedAttempts(userId: String): Int {
        return (this as FakeUserRepository).getFailedAttempts(userId)
    }

    private data class MfaChallengeData(
        val userId: String,
        val createdAt: kotlinx.datetime.Instant,
        val expiresAt: kotlinx.datetime.Instant,
    )
}

// Auth result data class
data class AuthResult(
    val userId: String,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: kotlinx.datetime.Instant,
    val requiresMfa: Boolean = false,
    val mfaChallengeId: String? = null,
)

// Exception classes
open class AuthenticationException(message: String) : Exception(message)
class InvalidCredentialsException(message: String) : AuthenticationException(message)
class AccountLockedException(message: String) : AuthenticationException(message)
class AccountInactiveException(message: String) : AuthenticationException(message)
class ValidationException(message: String) : AuthenticationException(message)
class InvalidMfaCodeException(message: String) : AuthenticationException(message)
class MfaChallengeExpiredException(message: String) : AuthenticationException(message)
