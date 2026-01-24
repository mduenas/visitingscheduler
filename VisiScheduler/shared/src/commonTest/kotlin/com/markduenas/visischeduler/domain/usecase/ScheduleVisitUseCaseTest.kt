package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for ScheduleVisitUseCase covering scheduling logic, conflict detection,
 * buffer time enforcement, and capacity management.
 *
 * @test Schedule Visit Use Case
 * @prerequisites Mocked repositories and test restrictions
 */
class ScheduleVisitUseCaseTest {

    private lateinit var visitRepository: FakeVisitRepository
    private lateinit var userRepository: FakeUserRepository
    private lateinit var restrictionRepository: FakeRestrictionRepository
    private lateinit var scheduleVisitUseCase: ScheduleVisitUseCase
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testVisitorId = "visitor-1"
    private val testCoordinatorId = "coordinator-1"

    @BeforeTest
    fun setup() {
        visitRepository = FakeVisitRepository()
        userRepository = FakeUserRepository()
        restrictionRepository = FakeRestrictionRepository()
        testClock = TestClock.fixed(2024, 6, 15, 10, 0) // Saturday, June 15, 2024, 10:00 AM

        scheduleVisitUseCase = ScheduleVisitUseCase(
            visitRepository = visitRepository,
            userRepository = userRepository,
            restrictionRepository = restrictionRepository,
            clock = testClock,
        )

        // Setup default test users
        userRepository.addUser(
            TestFixtures.createApprovedVisitor(
                id = testVisitorId,
                email = "visitor@example.com",
            )
        )

        userRepository.addUser(
            TestFixtures.createPrimaryCoordinator(
                id = testCoordinatorId,
                beneficiaryIds = listOf(testBeneficiaryId),
            )
        )

        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        visitRepository.clear()
        userRepository.clear()
        restrictionRepository.clear()
    }

    // ============================================================
    // VALID SCHEDULING TESTS
    // ============================================================

    @Test
    fun `should schedule visit when slot is available`() = runTest {
        // Arrange
        val startTime = testClock.now().plus(2.hours)
        val request = createScheduleRequest(startTime = startTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess, "Should successfully schedule visit")
        val visit = result.getOrNull()
        assertNotNull(visit)
        assertEquals(VisitStatus.PENDING, visit.status)
        assertEquals(testBeneficiaryId, visit.beneficiaryId)
        assertEquals(testVisitorId, visit.visitorId)
    }

    @Test
    fun `should persist visit to repository`() = runTest {
        // Arrange
        val startTime = testClock.now().plus(2.hours)
        val request = createScheduleRequest(startTime = startTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val visitId = result.getOrNull()!!.id
        val savedVisit = visitRepository.getById(visitId).getOrNull()
        assertNotNull(savedVisit, "Visit should be persisted")
        assertEquals(startTime, savedVisit.startTime)
    }

    @Test
    fun `should create visit with correct end time based on duration`() = runTest {
        // Arrange
        val startTime = testClock.now().plus(2.hours)
        val duration = 90.minutes
        val request = createScheduleRequest(
            startTime = startTime,
            duration = duration,
        )

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val visit = result.getOrNull()!!
        assertEquals(startTime.plus(duration), visit.endTime)
    }

    @Test
    fun `should auto-approve visit for whitelisted visitor`() = runTest {
        // Arrange
        val whitelistedVisitorId = "whitelisted-visitor"
        userRepository.addUser(
            TestFixtures.createApprovedVisitor(
                id = whitelistedVisitorId,
                email = "whitelisted@example.com",
            )
        )

        val startTime = testClock.now().plus(2.hours)
        val request = createScheduleRequest(
            visitorId = whitelistedVisitorId,
            startTime = startTime,
            autoApproveWhitelisted = true,
        )

        // Simulate visitor being on whitelist
        scheduleVisitUseCase.addToWhitelist(testBeneficiaryId, whitelistedVisitorId)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val visit = result.getOrNull()!!
        assertEquals(VisitStatus.APPROVED, visit.status)
        assertNotNull(visit.approvedAt)
    }

    // ============================================================
    // CONFLICT DETECTION TESTS
    // ============================================================

    @Test
    fun `should return error when slot has existing visit`() = runTest {
        // Arrange
        val startTime = testClock.now().plus(2.hours)

        // Create existing visit
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = startTime,
                duration = 1.hours,
            )
        )

        val request = createScheduleRequest(startTime = startTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError, "Should fail due to conflict")
        val error = result.exceptionOrNull()
        assertIs<SchedulingConflictException>(error)
    }

    @Test
    fun `should return error when visit overlaps with existing`() = runTest {
        // Arrange
        val existingStart = testClock.now().plus(2.hours)
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = existingStart,
                duration = 1.hours,
            )
        )

        // Try to schedule overlapping visit (starts 30 min into existing)
        val overlappingStart = existingStart.plus(30.minutes)
        val request = createScheduleRequest(startTime = overlappingStart)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<SchedulingConflictException>(result.exceptionOrNull())
    }

    @Test
    fun `should allow scheduling after existing visit ends`() = runTest {
        // Arrange
        val existingStart = testClock.now().plus(1.hours)
        val existingEnd = existingStart.plus(1.hours)

        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = existingStart,
                duration = 1.hours,
            )
        )

        // Schedule immediately after existing visit
        val request = createScheduleRequest(startTime = existingEnd)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess, "Should allow scheduling after existing visit")
    }

    @Test
    fun `should not conflict with cancelled visits`() = runTest {
        // Arrange
        val startTime = testClock.now().plus(2.hours)
        visitRepository.addVisit(
            TestFixtures.createVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = startTime,
                status = VisitStatus.CANCELLED,
            )
        )

        val request = createScheduleRequest(startTime = startTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess, "Should allow scheduling over cancelled visit")
    }

    // ============================================================
    // BUFFER TIME ENFORCEMENT TESTS
    // ============================================================

    @Test
    fun `should enforce buffer time between consecutive visits`() = runTest {
        // Arrange
        val bufferMinutes = 15
        scheduleVisitUseCase.setBufferTime(bufferMinutes.minutes)

        val existingEnd = testClock.now().plus(3.hours)
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = existingEnd.minus(1.hours),
                duration = 1.hours,
            )
        )

        // Try to schedule within buffer time
        val newStart = existingEnd.plus(10.minutes) // Only 10 min gap, need 15
        val request = createScheduleRequest(startTime = newStart)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError, "Should enforce buffer time")
        val error = result.exceptionOrNull()
        assertIs<BufferTimeViolationException>(error)
    }

    @Test
    fun `should allow scheduling after buffer time`() = runTest {
        // Arrange
        val bufferMinutes = 15
        scheduleVisitUseCase.setBufferTime(bufferMinutes.minutes)

        val existingEnd = testClock.now().plus(3.hours)
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = existingEnd.minus(1.hours),
                duration = 1.hours,
            )
        )

        // Schedule after buffer time
        val newStart = existingEnd.plus(bufferMinutes.minutes)
        val request = createScheduleRequest(startTime = newStart)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess, "Should allow scheduling after buffer time")
    }

    @Test
    fun `should enforce buffer time before existing visits too`() = runTest {
        // Arrange
        val bufferMinutes = 15
        scheduleVisitUseCase.setBufferTime(bufferMinutes.minutes)

        val existingStart = testClock.now().plus(3.hours)
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = existingStart,
                duration = 1.hours,
            )
        )

        // Try to schedule ending within buffer of existing
        val newStart = existingStart.minus(1.hours).minus(10.minutes)
        val request = createScheduleRequest(
            startTime = newStart,
            duration = 1.hours, // Ends 10 min before existing starts
        )

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError, "Should enforce buffer before next visit")
    }

    // ============================================================
    // CAPACITY LIMITS TESTS
    // ============================================================

    @Test
    fun `should enforce simultaneous visitor limit`() = runTest {
        // Arrange
        val maxSimultaneous = 2
        scheduleVisitUseCase.setMaxSimultaneousVisitors(maxSimultaneous)

        val startTime = testClock.now().plus(2.hours)

        // Add max visitors already
        repeat(maxSimultaneous) { index ->
            visitRepository.addVisit(
                TestFixtures.createApprovedVisit(
                    beneficiaryId = testBeneficiaryId,
                    visitorId = "visitor-$index",
                    startTime = startTime,
                )
            )
        }

        val request = createScheduleRequest(startTime = startTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<CapacityExceededException>(error)
        assertTrue(error.message?.contains("simultaneous") == true, ignoreCase = true)
    }

    @Test
    fun `should enforce daily visit cap`() = runTest {
        // Arrange
        val dailyCap = 5
        scheduleVisitUseCase.setDailyVisitCap(dailyCap)

        // Add daily cap visits
        repeat(dailyCap) { index ->
            val visitTime = testClock.now().plus((index + 1).hours)
            visitRepository.addVisit(
                TestFixtures.createApprovedVisit(
                    beneficiaryId = testBeneficiaryId,
                    startTime = visitTime,
                )
            )
        }

        // Try to add one more
        val request = createScheduleRequest(
            startTime = testClock.now().plus((dailyCap + 2).hours),
        )

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<CapacityExceededException>(error)
        assertTrue(error.message?.contains("daily") == true, ignoreCase = true)
    }

    @Test
    fun `should enforce per-visitor frequency limit`() = runTest {
        // Arrange
        val visitorWeeklyCap = 3
        scheduleVisitUseCase.setVisitorWeeklyLimit(visitorWeeklyCap)

        // Add max visits for this visitor this week
        repeat(visitorWeeklyCap) { index ->
            visitRepository.addVisit(
                TestFixtures.createApprovedVisit(
                    beneficiaryId = testBeneficiaryId,
                    visitorId = testVisitorId,
                    startTime = testClock.now().plus((index + 1).hours),
                )
            )
        }

        val request = createScheduleRequest(
            startTime = testClock.now().plus((visitorWeeklyCap + 2).hours),
        )

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<VisitorFrequencyException>(error)
    }

    @Test
    fun `should allow scheduling when under capacity`() = runTest {
        // Arrange
        val maxSimultaneous = 3
        scheduleVisitUseCase.setMaxSimultaneousVisitors(maxSimultaneous)

        val startTime = testClock.now().plus(2.hours)

        // Add one less than max
        repeat(maxSimultaneous - 1) { index ->
            visitRepository.addVisit(
                TestFixtures.createApprovedVisit(
                    beneficiaryId = testBeneficiaryId,
                    visitorId = "visitor-$index",
                    startTime = startTime,
                )
            )
        }

        val request = createScheduleRequest(startTime = startTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess, "Should allow when under capacity")
    }

    // ============================================================
    // RESTRICTION TESTS
    // ============================================================

    @Test
    fun `should return error when slot is during blackout date`() = runTest {
        // Arrange
        val blackoutDate = testClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        restrictionRepository.addRestriction(
            TestFixtures.createBlackoutDateRestriction(
                beneficiaryId = testBeneficiaryId,
                date = blackoutDate,
            )
        )

        val request = createScheduleRequest(startTime = testClock.now().plus(2.hours))

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<RestrictionViolationException>(error)
    }

    @Test
    fun `should return error when slot is during meal time`() = runTest {
        // Arrange
        restrictionRepository.addRestriction(
            TestFixtures.createMealTimeRestriction(
                beneficiaryId = testBeneficiaryId,
                startHour = 12,
                endHour = 13,
            )
        )

        // Schedule during lunch
        testClock.setDateTime(2024, 6, 15, 11, 0)
        val lunchTime = testClock.now().plus(1.hours) // 12:00
        val request = createScheduleRequest(startTime = lunchTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<RestrictionViolationException>(result.exceptionOrNull())
    }

    @Test
    fun `should return error when visitor is blocked`() = runTest {
        // Arrange
        val blockedVisitorId = "blocked-visitor"
        userRepository.addUser(
            TestFixtures.createPendingVisitor(id = blockedVisitorId)
        )

        restrictionRepository.addRestriction(
            TestFixtures.createVisitorBlockRestriction(
                beneficiaryId = testBeneficiaryId,
                blockedVisitorId = blockedVisitorId,
            )
        )

        val request = createScheduleRequest(visitorId = blockedVisitorId)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<VisitorBlockedException>(error)
    }

    @Test
    fun `should return error when slot conflicts with medical procedure`() = runTest {
        // Arrange
        val procedureTime = testClock.now().plus(2.hours)
        restrictionRepository.addRestriction(
            TestFixtures.createProcedureBlockRestriction(
                beneficiaryId = testBeneficiaryId,
                procedureTime = procedureTime,
                recoveryDuration = 2.hours,
            )
        )

        // Try to schedule during procedure/recovery
        val request = createScheduleRequest(startTime = procedureTime.plus(30.minutes))

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<RestrictionViolationException>(result.exceptionOrNull())
    }

    // ============================================================
    // VALIDATION TESTS
    // ============================================================

    @Test
    fun `should return error for past time slot`() = runTest {
        // Arrange
        val pastTime = testClock.now().minus(1.hours)
        val request = createScheduleRequest(startTime = pastTime)

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<InvalidTimeSlotException>(error)
        assertTrue(error.message?.contains("past") == true, ignoreCase = true)
    }

    @Test
    fun `should return error for duration less than minimum`() = runTest {
        // Arrange
        val request = createScheduleRequest(duration = 10.minutes) // Min is 15 minutes

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<InvalidDurationException>(error)
    }

    @Test
    fun `should return error for duration more than maximum`() = runTest {
        // Arrange
        val request = createScheduleRequest(duration = 5.hours) // Max is 4 hours

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<InvalidDurationException>(error)
    }

    @Test
    fun `should return error for too many guests`() = runTest {
        // Arrange
        val request = createScheduleRequest(numberOfGuests = 10) // Max is 5

        // Act
        val result = scheduleVisitUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        val error = result.exceptionOrNull()
        assertIs<TooManyGuestsException>(error)
    }

    // ============================================================
    // HELPER FUNCTIONS
    // ============================================================

    private fun createScheduleRequest(
        beneficiaryId: String = testBeneficiaryId,
        visitorId: String = testVisitorId,
        startTime: Instant = testClock.now().plus(2.hours),
        duration: kotlin.time.Duration = 1.hours,
        visitType: VisitType = VisitType.IN_PERSON,
        numberOfGuests: Int = 0,
        reason: String? = "Regular visit",
        autoApproveWhitelisted: Boolean = false,
    ): ScheduleVisitInput {
        return ScheduleVisitInput(
            beneficiaryId = beneficiaryId,
            visitorId = visitorId,
            startTime = startTime,
            duration = duration,
            visitType = visitType,
            numberOfGuests = numberOfGuests,
            reason = reason,
            autoApproveWhitelisted = autoApproveWhitelisted,
        )
    }
}

// ============================================================
// SUPPORTING CLASSES FOR TESTS
// ============================================================

data class ScheduleVisitInput(
    val beneficiaryId: String,
    val visitorId: String,
    val startTime: Instant,
    val duration: kotlin.time.Duration,
    val visitType: VisitType,
    val numberOfGuests: Int,
    val reason: String?,
    val autoApproveWhitelisted: Boolean = false,
)

class ScheduleVisitUseCase(
    private val visitRepository: VisitRepository,
    private val userRepository: UserRepository,
    private val restrictionRepository: RestrictionRepository,
    private val clock: Clock = Clock.System,
) {
    private var bufferTime = 0.minutes
    private var maxSimultaneous = Int.MAX_VALUE
    private var dailyCap = Int.MAX_VALUE
    private var visitorWeeklyLimit = Int.MAX_VALUE
    private val whitelist = mutableMapOf<String, MutableSet<String>>() // beneficiaryId -> visitorIds
    private val minDuration = 15.minutes
    private val maxDuration = 4.hours
    private val maxGuests = 5

    fun setBufferTime(duration: kotlin.time.Duration) {
        bufferTime = duration
    }

    fun setMaxSimultaneousVisitors(max: Int) {
        maxSimultaneous = max
    }

    fun setDailyVisitCap(cap: Int) {
        dailyCap = cap
    }

    fun setVisitorWeeklyLimit(limit: Int) {
        visitorWeeklyLimit = limit
    }

    fun addToWhitelist(beneficiaryId: String, visitorId: String) {
        whitelist.getOrPut(beneficiaryId) { mutableSetOf() }.add(visitorId)
    }

    suspend fun execute(input: ScheduleVisitInput): Result<Visit> {
        // Validate time slot
        if (input.startTime < clock.now()) {
            return Result.error(InvalidTimeSlotException("Cannot schedule visits in the past"))
        }

        // Validate duration
        if (input.duration < minDuration) {
            return Result.error(InvalidDurationException("Duration must be at least $minDuration"))
        }
        if (input.duration > maxDuration) {
            return Result.error(InvalidDurationException("Duration cannot exceed $maxDuration"))
        }

        // Validate guests
        if (input.numberOfGuests > maxGuests) {
            return Result.error(TooManyGuestsException("Maximum $maxGuests guests allowed"))
        }

        val endTime = input.startTime.plus(input.duration)
        val timeSlot = TimeSlot(
            startTime = input.startTime,
            endTime = endTime,
            isAvailable = true,
            capacity = maxSimultaneous,
            currentBookings = 0,
        )

        // Check restrictions
        val restrictions = restrictionRepository.getApplicableRestrictions(
            beneficiaryId = input.beneficiaryId,
            visitorId = input.visitorId,
            timeSlot = timeSlot,
        ).getOrNull() ?: emptyList()

        if (restrictions.any { it.type == RestrictionType.VISITOR_BLOCKED }) {
            return Result.error(VisitorBlockedException("Visitor is not permitted to visit"))
        }

        if (restrictions.isNotEmpty()) {
            val restriction = restrictions.first()
            return Result.error(
                RestrictionViolationException("Visit conflicts with restriction: ${restriction.reason}")
            )
        }

        // Check for conflicts
        val conflictingVisits = visitRepository.getConflictingVisits(
            beneficiaryId = input.beneficiaryId,
            startTime = input.startTime,
            endTime = endTime,
        ).getOrNull() ?: emptyList()

        if (conflictingVisits.isNotEmpty()) {
            return Result.error(SchedulingConflictException("Time slot conflicts with existing visit"))
        }

        // Check buffer time
        val bufferStart = input.startTime.minus(bufferTime)
        val bufferEnd = endTime.plus(bufferTime)

        val bufferConflicts = visitRepository.getConflictingVisits(
            beneficiaryId = input.beneficiaryId,
            startTime = bufferStart,
            endTime = bufferEnd,
        ).getOrNull() ?: emptyList()

        val actualConflicts = bufferConflicts.filter {
            // Check if conflict is within buffer zone (not actual overlap)
            (it.endTime > input.startTime.minus(bufferTime) && it.endTime <= input.startTime) ||
                (it.startTime >= endTime && it.startTime < endTime.plus(bufferTime))
        }

        if (actualConflicts.isNotEmpty()) {
            return Result.error(
                BufferTimeViolationException("Insufficient buffer time between visits")
            )
        }

        // Check simultaneous capacity
        val simultaneousVisits = visitRepository.getConflictingVisits(
            beneficiaryId = input.beneficiaryId,
            startTime = input.startTime,
            endTime = endTime,
        ).getOrNull()?.filter { it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING) }
            ?: emptyList()

        if (simultaneousVisits.size >= maxSimultaneous) {
            return Result.error(
                CapacityExceededException("Maximum simultaneous visitors ($maxSimultaneous) exceeded")
            )
        }

        // Check daily cap
        val dayStart = input.startTime.toLocalDateTime(TimeZone.currentSystemDefault()).date
            .atStartOfDayIn(TimeZone.currentSystemDefault())
        val dayEnd = dayStart.plus(24.hours)

        val dailyVisits = visitRepository.getByDateRange(
            beneficiaryId = input.beneficiaryId,
            start = dayStart,
            end = dayEnd,
        ).getOrNull()?.filter { it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING) }
            ?: emptyList()

        if (dailyVisits.size >= dailyCap) {
            return Result.error(
                CapacityExceededException("Daily visit cap ($dailyCap) exceeded")
            )
        }

        // Check visitor weekly limit
        val weekStart = input.startTime.minus((7 * 24).hours)
        val visitorVisitsThisWeek = visitRepository.getByVisitor(input.visitorId)
            .getOrNull()
            ?.filter {
                it.beneficiaryId == input.beneficiaryId &&
                    it.startTime >= weekStart &&
                    it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING, VisitStatus.COMPLETED)
            }
            ?: emptyList()

        if (visitorVisitsThisWeek.size >= visitorWeeklyLimit) {
            return Result.error(
                VisitorFrequencyException("Visitor weekly limit ($visitorWeeklyLimit) exceeded")
            )
        }

        // Determine status based on whitelist
        val isWhitelisted = whitelist[input.beneficiaryId]?.contains(input.visitorId) == true
        val status = if (input.autoApproveWhitelisted && isWhitelisted) {
            VisitStatus.APPROVED
        } else {
            VisitStatus.PENDING
        }

        // Create visit
        val visit = Visit(
            id = TestFixtures.generateId("visit"),
            beneficiaryId = input.beneficiaryId,
            visitorId = input.visitorId,
            visitorName = userRepository.getById(input.visitorId).getOrNull()?.name ?: "Unknown",
            status = status,
            startTime = input.startTime,
            endTime = endTime,
            visitType = input.visitType,
            numberOfGuests = input.numberOfGuests,
            reason = input.reason,
            notes = null,
            createdAt = clock.now(),
            approvedBy = if (status == VisitStatus.APPROVED) "system" else null,
            approvedAt = if (status == VisitStatus.APPROVED) clock.now() else null,
            denialReason = null,
        )

        visitRepository.save(visit)

        return Result.success(visit)
    }
}

// Exception classes
open class SchedulingException(message: String) : Exception(message)
class SchedulingConflictException(message: String) : SchedulingException(message)
class BufferTimeViolationException(message: String) : SchedulingException(message)
class CapacityExceededException(message: String) : SchedulingException(message)
class VisitorFrequencyException(message: String) : SchedulingException(message)
class RestrictionViolationException(message: String) : SchedulingException(message)
class VisitorBlockedException(message: String) : SchedulingException(message)
class InvalidTimeSlotException(message: String) : SchedulingException(message)
class InvalidDurationException(message: String) : SchedulingException(message)
class TooManyGuestsException(message: String) : SchedulingException(message)
