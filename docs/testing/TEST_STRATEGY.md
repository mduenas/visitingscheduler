# VisiScheduler Test Strategy

## Overview

This document outlines the comprehensive testing strategy for VisiScheduler, a Kotlin Multiplatform (KMP) proxy scheduling application. Given the healthcare context and HIPAA compliance requirements, testing is critical for ensuring reliability, security, and correctness.

## Test Pyramid Architecture

```
                    /\
                   /  \
                  / E2E \         <- 5% of tests (critical user journeys)
                 /-------\
                / Integr. \       <- 15% of tests (API, DB, external services)
               /-----------\
              /    Unit     \     <- 80% of tests (fast, isolated, focused)
             /---------------\
```

## Coverage Targets

| Category | Minimum | Target | Critical Paths |
|----------|---------|--------|----------------|
| Overall | 80% | 90% | 95% |
| Domain Logic | 90% | 95% | 100% |
| Use Cases | 85% | 95% | 100% |
| Repositories | 80% | 90% | 95% |
| Security Code | 95% | 100% | 100% |
| UI Components | 70% | 80% | 90% |

---

## 1. Unit Testing Strategy

### 1.1 Scope
Unit tests cover isolated components with all dependencies mocked:
- Use cases (business logic)
- Domain entities and value objects
- Validators and rule engines
- Utility functions
- State management

### 1.2 Testing Framework Stack
```kotlin
// Core testing
kotlin-test         // Cross-platform assertions
kotlinx-coroutines-test  // Coroutine testing utilities

// Mocking
mockk               // Kotlin-first mocking library (JVM)
mokkery            // KMP-compatible mocking (multiplatform)

// Flow testing
turbine            // Testing kotlinx.coroutines Flow

// DI testing
koin-test          // Testing with Koin DI
```

### 1.3 Test Structure (AAA Pattern)
```kotlin
@Test
fun `should schedule visit when slot is available`() = runTest {
    // Arrange
    val repository = mockk<VisitRepository>()
    val slot = TimeSlot(startTime, endTime)
    coEvery { repository.isSlotAvailable(slot) } returns true
    coEvery { repository.save(any()) } returns visitId

    val useCase = ScheduleVisitUseCase(repository)

    // Act
    val result = useCase(ScheduleVisitRequest(visitorId, slot))

    // Assert
    result.shouldBeSuccess()
    coVerify { repository.save(any()) }
}
```

### 1.4 Naming Convention
```
`should [expected behavior] when [condition]`
`should [expected behavior] given [context]`
```

Examples:
- `should return error when credentials are invalid`
- `should auto-approve visit given visitor is whitelisted`
- `should enforce buffer time between consecutive visits`

### 1.5 Use Case Testing Checklist
For each use case, test:
- [ ] Happy path (successful execution)
- [ ] All validation failures
- [ ] Authorization failures
- [ ] Boundary conditions
- [ ] Error handling and recovery
- [ ] Side effects (events, notifications)

---

## 2. Integration Testing Strategy

### 2.1 Scope
Integration tests verify component interactions:
- Repository implementations with test database
- API client with mock server
- External service integrations
- Multi-component workflows

### 2.2 Database Testing
```kotlin
class VisitRepositoryIntegrationTest {
    private lateinit var database: TestDatabase
    private lateinit var repository: VisitRepositoryImpl

    @BeforeTest
    fun setup() {
        database = createTestDatabase()
        repository = VisitRepositoryImpl(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun `should persist and retrieve visit`() = runTest {
        // Test actual database operations
        val visit = TestFixtures.createVisit()
        val id = repository.save(visit)

        val retrieved = repository.getById(id)

        retrieved.shouldNotBeNull()
        retrieved.visitorId shouldBe visit.visitorId
    }
}
```

### 2.3 API Testing
```kotlin
class VisitApiIntegrationTest {
    private lateinit var mockServer: MockWebServer
    private lateinit var client: VisitApiClient

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        client = VisitApiClient(mockServer.url("/").toString())
    }

    @Test
    fun `should handle rate limiting gracefully`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(429))
        mockServer.enqueue(MockResponse().setBody(validResponse))

        val result = client.getVisits()

        result.shouldBeSuccess()
        mockServer.requestCount shouldBe 2  // Retried once
    }
}
```

---

## 3. UI Testing Strategy (Compose Multiplatform)

### 3.1 Component Testing
```kotlin
class VisitCardTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should display visit details correctly`() {
        val visit = TestFixtures.createApprovedVisit()

        composeTestRule.setContent {
            VisitCard(visit = visit, onClick = {})
        }

        composeTestRule.onNodeWithText(visit.visitorName).assertIsDisplayed()
        composeTestRule.onNodeWithText(visit.formattedTime).assertIsDisplayed()
        composeTestRule.onNodeWithTag("status_approved").assertIsDisplayed()
    }

    @Test
    fun `should call onClick when card is tapped`() {
        var clicked = false

        composeTestRule.setContent {
            VisitCard(
                visit = TestFixtures.createVisit(),
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithTag("visit_card").performClick()

        clicked shouldBe true
    }
}
```

### 3.2 Screen Testing
```kotlin
class ScheduleScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `should show loading state initially`() {
        composeTestRule.setContent {
            ScheduleScreen(viewModel = FakeScheduleViewModel(isLoading = true))
        }

        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
    }

    @Test
    fun `should navigate to visit details on card click`() {
        var navigatedToId: String? = null

        composeTestRule.setContent {
            ScheduleScreen(
                viewModel = FakeScheduleViewModel(visits = listOf(testVisit)),
                onNavigateToVisit = { navigatedToId = it }
            )
        }

        composeTestRule.onNodeWithTag("visit_${testVisit.id}").performClick()

        navigatedToId shouldBe testVisit.id
    }
}
```

---

## 4. Platform-Specific Testing

### 4.1 Android-Specific Tests
Location: `androidTest/`

```kotlin
@RunWith(AndroidJUnit4::class)
class BiometricAuthTest {
    @Test
    fun shouldAuthenticateWithFingerprint() {
        // Test BiometricPrompt integration
    }

    @Test
    fun shouldHandleBiometricNotEnrolled() {
        // Test fallback to PIN/password
    }
}

@RunWith(AndroidJUnit4::class)
class NotificationChannelTest {
    @Test
    fun shouldCreateVisitReminderChannel() {
        // Verify notification channel setup
    }
}
```

### 4.2 iOS-Specific Tests
Location: `iosTest/`

```kotlin
class FaceIdAuthTest {
    @Test
    fun shouldAuthenticateWithFaceId() = runTest {
        // Test LAContext integration
    }

    @Test
    fun shouldHandleFaceIdNotAvailable() = runTest {
        // Test fallback behavior
    }
}
```

---

## 5. Security Testing (HIPAA Compliance)

### 5.1 Authentication Tests
```kotlin
class AuthenticationSecurityTest {
    @Test
    fun `should lock account after 5 failed attempts`() = runTest {
        repeat(5) {
            authService.login(validEmail, wrongPassword)
        }

        val result = authService.login(validEmail, correctPassword)

        result shouldBe AuthResult.AccountLocked
    }

    @Test
    fun `should enforce MFA for sensitive operations`() = runTest {
        val user = authenticatedUser

        val result = visitService.accessMedicalInfo(user)

        result shouldBe AccessResult.MfaRequired
    }

    @Test
    fun `should invalidate session after password change`() = runTest {
        val oldSession = sessionManager.currentSession

        authService.changePassword(newPassword)

        sessionManager.isValid(oldSession) shouldBe false
    }
}
```

### 5.2 Data Protection Tests
```kotlin
class DataProtectionTest {
    @Test
    fun `should encrypt PHI at rest`() {
        val visit = TestFixtures.createVisitWithMedicalInfo()

        repository.save(visit)

        val rawData = database.rawQuery("SELECT * FROM visits")
        rawData.shouldNotContain(visit.medicalNotes)  // Should be encrypted
    }

    @Test
    fun `should mask PHI in logs`() {
        val visit = TestFixtures.createVisit()
        logger.info("Processing visit: $visit")

        val logs = logCaptor.logs

        logs.shouldNotContain(visit.patientName)
        logs.shouldContain("***REDACTED***")
    }

    @Test
    fun `should clear sensitive data on logout`() = runTest {
        val sensitiveData = loadSensitiveData()

        authService.logout()

        memoryCache.contains(sensitiveData) shouldBe false
        secureStorage.contains(sensitiveData) shouldBe false
    }
}
```

### 5.3 Access Control Tests
```kotlin
class AccessControlTest {
    @Test
    fun `visitor should not access other patients visits`() = runTest {
        val visitor = authenticateAs(UserRole.VISITOR)

        val result = visitRepository.getVisitsForPatient(otherPatientId)

        result shouldBe AccessDenied
    }

    @Test
    fun `coordinator should only access assigned beneficiaries`() = runTest {
        val coordinator = authenticateAs(UserRole.COORDINATOR)

        val result = beneficiaryService.access(unassignedBeneficiary)

        result shouldBe AccessDenied
    }
}
```

### 5.4 Audit Trail Tests
```kotlin
class AuditTrailTest {
    @Test
    fun `should log all PHI access`() = runTest {
        val user = authenticatedUser

        visitService.viewMedicalInfo(visitId)

        val auditLog = auditRepository.getLatestEntry()
        auditLog.userId shouldBe user.id
        auditLog.action shouldBe "VIEW_MEDICAL_INFO"
        auditLog.resourceId shouldBe visitId
        auditLog.timestamp.shouldBeRecent()
    }
}
```

---

## 6. Performance Testing

### 6.1 Criteria
| Metric | Acceptable | Target |
|--------|------------|--------|
| App launch (cold) | < 3s | < 2s |
| App launch (warm) | < 1s | < 500ms |
| Screen transition | < 300ms | < 200ms |
| API response (local) | < 100ms | < 50ms |
| API response (remote) | < 2s | < 1s |
| Database query | < 50ms | < 20ms |
| Memory usage (idle) | < 150MB | < 100MB |

### 6.2 Performance Test Examples
```kotlin
class PerformanceTest {
    @Test
    fun `should load schedule within 500ms`() = runTest {
        val startTime = measureTimeMillis {
            viewModel.loadSchedule(date)
            advanceUntilIdle()
        }

        startTime shouldBeLessThan 500
    }

    @Test
    fun `should handle 1000 visits without memory issues`() = runTest {
        val initialMemory = Runtime.getRuntime().totalMemory()

        repeat(1000) {
            repository.save(TestFixtures.createVisit())
        }

        val finalMemory = Runtime.getRuntime().totalMemory()
        val increase = finalMemory - initialMemory

        increase shouldBeLessThan (50 * 1024 * 1024)  // 50MB max increase
    }
}
```

---

## 7. Edge Case Testing Matrix

### 7.1 Time-Related Edge Cases
| Scenario | Test |
|----------|------|
| Midnight boundary | Visit spanning 23:59 to 00:01 |
| DST transition | Visit during clock change |
| Timezone handling | Users in different timezones |
| Leap year | February 29th scheduling |
| Year boundary | December 31 to January 1 |

### 7.2 Capacity Edge Cases
| Scenario | Test |
|----------|------|
| Maximum capacity | Exactly at limit |
| Over capacity | Request when full |
| Concurrent requests | Multiple simultaneous bookings |
| Buffer time boundary | Back-to-back visits |

### 7.3 User Role Edge Cases
| Scenario | Test |
|----------|------|
| Role change mid-session | Coordinator demoted during use |
| Multiple roles | User is both visitor and coordinator |
| Coordinator handoff | Mid-visit coordinator change |
| Account suspension | Access during suspension process |

---

## 8. Test Data Management

### 8.1 Test Fixtures
Use factory functions for consistent test data:

```kotlin
object TestFixtures {
    fun createVisit(
        id: String = UUID.randomUUID().toString(),
        status: VisitStatus = VisitStatus.PENDING,
        startTime: Instant = Clock.System.now().plus(1.days),
        duration: Duration = 1.hours
    ): Visit = Visit(
        id = id,
        status = status,
        startTime = startTime,
        endTime = startTime.plus(duration),
        // ... other defaults
    )

    fun createCoordinator(
        permissions: Set<Permission> = Permission.DEFAULT_COORDINATOR
    ): User = User(
        role = UserRole.PRIMARY_COORDINATOR,
        permissions = permissions,
        // ...
    )
}
```

### 8.2 Test Isolation
- Each test must be independent
- Database reset between tests
- No shared mutable state
- Deterministic time handling with TestClock

---

## 9. Continuous Integration

### 9.1 PR Checks (Required)
1. Unit tests pass (all platforms)
2. Coverage >= 80%
3. No security vulnerabilities
4. Static analysis clean

### 9.2 Nightly Builds
1. Full integration test suite
2. Performance regression tests
3. Security scan
4. Coverage trend analysis

### 9.3 Release Gates
1. All tests pass
2. Coverage >= 85%
3. No critical/high security issues
4. Performance benchmarks within tolerance

---

## 10. Test Environment Configuration

### 10.1 Local Development
```bash
# Run all tests
./gradlew allTests

# Run common tests only
./gradlew :shared:testDebugUnitTest

# Run with coverage
./gradlew koverReport

# Run specific test class
./gradlew :shared:testDebugUnitTest --tests "*LoginUseCaseTest*"
```

### 10.2 CI Environment Variables
```yaml
HIPAA_TEST_MODE: true
TEST_DATABASE_URL: ${{ secrets.TEST_DB_URL }}
MOCK_EXTERNAL_SERVICES: true
COVERAGE_THRESHOLD: 80
```

---

## 11. Testing Responsibilities

| Component | Team | Review Required |
|-----------|------|-----------------|
| Unit tests | Developer | Peer review |
| Integration tests | Developer | Tech lead |
| Security tests | Security team | Security officer |
| E2E tests | QA team | Product owner |
| Performance tests | DevOps | Tech lead |

---

## 12. Test Documentation

### 12.1 Required Documentation
- Test plan for each feature
- Edge case analysis
- Security test coverage report
- Performance baseline records

### 12.2 Test Naming
All tests must include:
- Feature being tested
- Scenario/condition
- Expected outcome

---

## Appendix A: Testing Libraries Reference

| Library | Version | Purpose |
|---------|---------|---------|
| kotlin-test | 1.9.22 | Cross-platform assertions |
| kotlinx-coroutines-test | 1.8.0 | Coroutine testing |
| mockk | 1.13.9 | JVM mocking |
| turbine | 1.0.0 | Flow testing |
| koin-test | 3.5.3 | DI testing |
| kotest | 5.8.0 | Property-based testing |
| ktor-client-mock | 2.3.7 | HTTP client mocking |

## Appendix B: HIPAA Testing Checklist

- [ ] Access controls tested for all roles
- [ ] Encryption verified at rest and in transit
- [ ] Audit logging complete and tamper-proof
- [ ] Session management secure
- [ ] PHI masking in logs verified
- [ ] Data retention policies enforced
- [ ] Breach notification workflow tested
- [ ] Emergency access procedures tested
