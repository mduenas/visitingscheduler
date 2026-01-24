package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for GetAvailableSlotsUseCase covering slot generation, filtering,
 * and various restriction types.
 *
 * @test Get Available Slots Use Case
 * @prerequisites Mocked repositories with restrictions and existing visits
 */
class GetAvailableSlotsUseCaseTest {

    private lateinit var visitRepository: FakeVisitRepository
    private lateinit var restrictionRepository: FakeRestrictionRepository
    private lateinit var getAvailableSlotsUseCase: GetAvailableSlotsUseCase
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testVisitorId = "visitor-1"

    @BeforeTest
    fun setup() {
        visitRepository = FakeVisitRepository()
        restrictionRepository = FakeRestrictionRepository()
        testClock = TestClock.fixed(2024, 6, 15, 8, 0) // Saturday, 8:00 AM

        getAvailableSlotsUseCase = GetAvailableSlotsUseCase(
            visitRepository = visitRepository,
            restrictionRepository = restrictionRepository,
            clock = testClock,
        )

        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        visitRepository.clear()
        restrictionRepository.clear()
    }

    // ============================================================
    // SLOT GENERATION TESTS
    // ============================================================

    @Test
    fun `should generate slots for specified date range`() = runTest {
        // Arrange
        val startDate = LocalDate(2024, 6, 15)
        val endDate = LocalDate(2024, 6, 15)
        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = startDate,
            endDate = endDate,
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        assertTrue(slots.isNotEmpty(), "Should generate slots for the day")
    }

    @Test
    fun `should generate slots with correct duration`() = runTest {
        // Arrange
        val slotDuration = 30.minutes
        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = slotDuration,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        slots.forEach { slot ->
            val duration = slot.endTime.minus(slot.startTime)
            assertEquals(slotDuration, duration, "Each slot should have the requested duration")
        }
    }

    @Test
    fun `should respect visiting hours configuration`() = runTest {
        // Arrange
        getAvailableSlotsUseCase.setVisitingHours(
            startHour = 9,
            endHour = 17,
        )

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        slots.forEach { slot ->
            val slotStart = slot.startTime.toLocalDateTime(timezone)
            val slotEnd = slot.endTime.toLocalDateTime(timezone)
            assertTrue(slotStart.hour >= 9, "Slot should start after 9 AM")
            assertTrue(slotEnd.hour <= 17, "Slot should end by 5 PM")
        }
    }

    @Test
    fun `should generate slots for multiple days`() = runTest {
        // Arrange
        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 17), // 3 days
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        // Check that slots exist for multiple days
        val uniqueDays = slots.map { it.startTime.toLocalDateTime(timezone).date }.toSet()
        assertEquals(3, uniqueDays.size, "Should have slots for 3 days")
    }

    // ============================================================
    // BLACKOUT DATE FILTERING TESTS
    // ============================================================

    @Test
    fun `should exclude slots on blackout dates`() = runTest {
        // Arrange
        val blackoutDate = LocalDate(2024, 6, 16)
        restrictionRepository.addRestriction(
            TestFixtures.createBlackoutDateRestriction(
                beneficiaryId = testBeneficiaryId,
                date = blackoutDate,
            )
        )

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 17),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        val blackoutSlots = slots.filter {
            it.startTime.toLocalDateTime(timezone).date == blackoutDate
        }
        assertTrue(blackoutSlots.isEmpty(), "Should have no slots on blackout date")
    }

    @Test
    fun `should include slots on non-blackout dates`() = runTest {
        // Arrange
        restrictionRepository.addRestriction(
            TestFixtures.createBlackoutDateRestriction(
                beneficiaryId = testBeneficiaryId,
                date = LocalDate(2024, 6, 20), // Different date
            )
        )

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isNotEmpty())
    }

    // ============================================================
    // MEAL TIME EXCLUSION TESTS
    // ============================================================

    @Test
    fun `should exclude slots during meal times`() = runTest {
        // Arrange
        restrictionRepository.addRestriction(
            TestFixtures.createMealTimeRestriction(
                beneficiaryId = testBeneficiaryId,
                startHour = 12,
                startMinute = 0,
                endHour = 13,
                endMinute = 0,
            )
        )

        getAvailableSlotsUseCase.setVisitingHours(9, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        val lunchSlots = slots.filter { slot ->
            val hour = slot.startTime.toLocalDateTime(timezone).hour
            hour == 12
        }
        assertTrue(lunchSlots.isEmpty(), "Should have no slots during lunch (12:00-13:00)")
    }

    @Test
    fun `should exclude breakfast time slots`() = runTest {
        // Arrange
        restrictionRepository.addRestriction(
            TestFixtures.createMealTimeRestriction(
                beneficiaryId = testBeneficiaryId,
                startHour = 8,
                startMinute = 0,
                endHour = 9,
                endMinute = 0,
            )
        )

        getAvailableSlotsUseCase.setVisitingHours(8, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        val breakfastSlots = slots.filter {
            it.startTime.toLocalDateTime(timezone).hour == 8
        }
        assertTrue(breakfastSlots.isEmpty(), "Should have no slots during breakfast")
    }

    // ============================================================
    // PROCEDURE BLOCKING TESTS
    // ============================================================

    @Test
    fun `should exclude slots during medical procedures`() = runTest {
        // Arrange
        val procedureStart = LocalDateTime(2024, 6, 15, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())

        restrictionRepository.addRestriction(
            TestFixtures.createProcedureBlockRestriction(
                beneficiaryId = testBeneficiaryId,
                procedureTime = procedureStart,
                recoveryDuration = 2.hours,
            )
        )

        getAvailableSlotsUseCase.setVisitingHours(9, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        // Should have no slots from 10:00-12:00 (procedure + recovery)
        val blockedSlots = slots.filter { slot ->
            val hour = slot.startTime.toLocalDateTime(timezone).hour
            hour in 10..11
        }
        assertTrue(blockedSlots.isEmpty(), "Should have no slots during procedure and recovery")
    }

    @Test
    fun `should include slots after procedure recovery`() = runTest {
        // Arrange
        val procedureStart = LocalDateTime(2024, 6, 15, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())

        restrictionRepository.addRestriction(
            TestFixtures.createProcedureBlockRestriction(
                beneficiaryId = testBeneficiaryId,
                procedureTime = procedureStart,
                recoveryDuration = 2.hours,
            )
        )

        getAvailableSlotsUseCase.setVisitingHours(9, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        // Should have slots after 12:00
        val afternoonSlots = slots.filter { slot ->
            slot.startTime.toLocalDateTime(timezone).hour >= 12
        }
        assertTrue(afternoonSlots.isNotEmpty(), "Should have slots after procedure recovery")
    }

    // ============================================================
    // VISITOR-SPECIFIC AVAILABILITY TESTS
    // ============================================================

    @Test
    fun `should mark slots as unavailable for blocked visitor`() = runTest {
        // Arrange
        val blockedVisitorId = "blocked-visitor"
        restrictionRepository.addRestriction(
            TestFixtures.createVisitorBlockRestriction(
                beneficiaryId = testBeneficiaryId,
                blockedVisitorId = blockedVisitorId,
            )
        )

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
            visitorId = blockedVisitorId,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        assertTrue(slots.all { !it.isAvailable }, "All slots should be unavailable for blocked visitor")
    }

    @Test
    fun `should show available slots for non-blocked visitor`() = runTest {
        // Arrange
        restrictionRepository.addRestriction(
            TestFixtures.createVisitorBlockRestriction(
                beneficiaryId = testBeneficiaryId,
                blockedVisitorId = "other-visitor",
            )
        )

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
            visitorId = testVisitorId,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        assertTrue(slots.any { it.isAvailable }, "Should have available slots for non-blocked visitor")
    }

    // ============================================================
    // EXISTING VISIT FILTERING TESTS
    // ============================================================

    @Test
    fun `should mark slots as unavailable when existing visit`() = runTest {
        // Arrange
        val existingStart = LocalDateTime(2024, 6, 15, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())

        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = existingStart,
                duration = 1.hours,
            )
        )

        getAvailableSlotsUseCase.setVisitingHours(9, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        val tenAmSlot = slots.find {
            it.startTime.toLocalDateTime(timezone).hour == 10
        }
        assertNotNull(tenAmSlot)
        assertFalse(tenAmSlot.isAvailable, "10 AM slot should be unavailable due to existing visit")
    }

    @Test
    fun `should show capacity when multiple visits allowed`() = runTest {
        // Arrange
        val maxSimultaneous = 3
        getAvailableSlotsUseCase.setMaxSimultaneousVisitors(maxSimultaneous)

        val slotStart = LocalDateTime(2024, 6, 15, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())

        // Add one existing visit
        visitRepository.addVisit(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                startTime = slotStart,
            )
        )

        getAvailableSlotsUseCase.setVisitingHours(9, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        val tenAmSlot = slots.find {
            it.startTime.toLocalDateTime(timezone).hour == 10
        }
        assertNotNull(tenAmSlot)
        assertEquals(maxSimultaneous, tenAmSlot.capacity)
        assertEquals(1, tenAmSlot.currentBookings)
        assertTrue(tenAmSlot.isAvailable, "Should still be available (1 of 3 booked)")
    }

    @Test
    fun `should mark slot unavailable when at capacity`() = runTest {
        // Arrange
        val maxSimultaneous = 2
        getAvailableSlotsUseCase.setMaxSimultaneousVisitors(maxSimultaneous)

        val slotStart = LocalDateTime(2024, 6, 15, 10, 0)
            .toInstant(TimeZone.currentSystemDefault())

        // Fill to capacity
        repeat(maxSimultaneous) { index ->
            visitRepository.addVisit(
                TestFixtures.createApprovedVisit(
                    id = "visit-$index",
                    beneficiaryId = testBeneficiaryId,
                    visitorId = "visitor-$index",
                    startTime = slotStart,
                )
            )
        }

        getAvailableSlotsUseCase.setVisitingHours(9, 17)

        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        val timezone = TimeZone.currentSystemDefault()

        val tenAmSlot = slots.find {
            it.startTime.toLocalDateTime(timezone).hour == 10
        }
        assertNotNull(tenAmSlot)
        assertFalse(tenAmSlot.isAvailable, "Should be unavailable when at capacity")
    }

    // ============================================================
    // EDGE CASES
    // ============================================================

    @Test
    fun `should handle empty date range`() = runTest {
        // Arrange
        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 14), // End before start
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `should handle past dates`() = runTest {
        // Arrange
        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 10), // Past date
            endDate = LocalDate(2024, 6, 10),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        // Past dates should have no available slots
        val slots = result.getOrNull()!!
        assertTrue(slots.all { !it.isAvailable }, "Past slots should not be available")
    }

    @Test
    fun `should handle very short slot durations`() = runTest {
        // Arrange
        val request = SlotRequest(
            beneficiaryId = testBeneficiaryId,
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 15.minutes,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isSuccess)
        val slots = result.getOrNull()!!
        assertTrue(slots.size > 10, "Should generate many short slots")
    }

    @Test
    fun `should return error for invalid beneficiary`() = runTest {
        // Arrange
        val request = SlotRequest(
            beneficiaryId = "",
            startDate = LocalDate(2024, 6, 15),
            endDate = LocalDate(2024, 6, 15),
            slotDuration = 1.hours,
        )

        // Act
        val result = getAvailableSlotsUseCase.execute(request)

        // Assert
        assertTrue(result.isError)
        assertIs<InvalidBeneficiaryException>(result.exceptionOrNull())
    }
}

// ============================================================
// SUPPORTING CLASSES FOR TESTS
// ============================================================

data class SlotRequest(
    val beneficiaryId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val slotDuration: kotlin.time.Duration,
    val visitorId: String? = null,
)

class GetAvailableSlotsUseCase(
    private val visitRepository: VisitRepository,
    private val restrictionRepository: RestrictionRepository,
    private val clock: Clock = Clock.System,
) {
    private var visitingStartHour = 9
    private var visitingEndHour = 17
    private var maxSimultaneous = Int.MAX_VALUE

    fun setVisitingHours(startHour: Int, endHour: Int) {
        visitingStartHour = startHour
        visitingEndHour = endHour
    }

    fun setMaxSimultaneousVisitors(max: Int) {
        maxSimultaneous = max
    }

    suspend fun execute(request: SlotRequest): Result<List<TimeSlot>> {
        if (request.beneficiaryId.isBlank()) {
            return Result.error(InvalidBeneficiaryException("Beneficiary ID is required"))
        }

        if (request.startDate > request.endDate) {
            return Result.success(emptyList())
        }

        val timezone = TimeZone.currentSystemDefault()
        val slots = mutableListOf<TimeSlot>()

        // Get restrictions
        val restrictions = restrictionRepository.getActiveByBeneficiary(request.beneficiaryId)
            .getOrNull() ?: emptyList()

        // Check if visitor is blocked
        val visitorBlocked = request.visitorId?.let { visitorId ->
            restrictions.any {
                it.type == RestrictionType.VISITOR_BLOCKED && it.visitorId == visitorId
            }
        } ?: false

        // Generate slots for each day
        var currentDate = request.startDate
        while (currentDate <= request.endDate) {
            val daySlots = generateDaySlots(
                date = currentDate,
                slotDuration = request.slotDuration,
                beneficiaryId = request.beneficiaryId,
                restrictions = restrictions,
                visitorBlocked = visitorBlocked,
                timezone = timezone,
            )
            slots.addAll(daySlots)
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }

        return Result.success(slots)
    }

    private suspend fun generateDaySlots(
        date: LocalDate,
        slotDuration: kotlin.time.Duration,
        beneficiaryId: String,
        restrictions: List<Restriction>,
        visitorBlocked: Boolean,
        timezone: TimeZone,
    ): List<TimeSlot> {
        val slots = mutableListOf<TimeSlot>()

        // Check blackout date
        val isBlackoutDate = restrictions.any { restriction ->
            restriction.type == RestrictionType.BLACKOUT_DATE &&
                restriction.startTime?.let { start ->
                    val restrictionDate = start.toLocalDateTime(timezone).date
                    restrictionDate == date
                } ?: false
        }

        if (isBlackoutDate) {
            return emptyList()
        }

        // Generate slots within visiting hours
        var currentHour = visitingStartHour
        var currentMinute = 0

        while (currentHour < visitingEndHour) {
            val slotStart = LocalDateTime(date, LocalTime(currentHour, currentMinute))
                .toInstant(timezone)
            val slotEnd = slotStart.plus(slotDuration)

            // Check if slot end exceeds visiting hours
            val slotEndLocal = slotEnd.toLocalDateTime(timezone)
            if (slotEndLocal.hour > visitingEndHour ||
                (slotEndLocal.hour == visitingEndHour && slotEndLocal.minute > 0)
            ) {
                break
            }

            // Check restrictions
            val isRestricted = isSlotRestricted(slotStart, slotEnd, restrictions, timezone)

            // Check existing visits
            val existingVisits = visitRepository.getConflictingVisits(
                beneficiaryId = beneficiaryId,
                startTime = slotStart,
                endTime = slotEnd,
            ).getOrNull()?.filter {
                it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING)
            } ?: emptyList()

            val currentBookings = existingVisits.size
            val isPast = slotStart < clock.now()

            val isAvailable = !visitorBlocked &&
                !isRestricted &&
                !isPast &&
                currentBookings < maxSimultaneous

            slots.add(
                TimeSlot(
                    startTime = slotStart,
                    endTime = slotEnd,
                    isAvailable = isAvailable,
                    capacity = maxSimultaneous,
                    currentBookings = currentBookings,
                )
            )

            // Move to next slot
            val totalMinutes = currentHour * 60 + currentMinute + slotDuration.inWholeMinutes.toInt()
            currentHour = totalMinutes / 60
            currentMinute = totalMinutes % 60
        }

        return slots
    }

    private fun isSlotRestricted(
        slotStart: Instant,
        slotEnd: Instant,
        restrictions: List<Restriction>,
        timezone: TimeZone,
    ): Boolean {
        return restrictions.any { restriction ->
            when (restriction.type) {
                RestrictionType.MEAL_TIME, RestrictionType.REST_PERIOD, RestrictionType.BLACKOUT_HOURS -> {
                    val slotStartTime = slotStart.toLocalDateTime(timezone).time
                    restriction.recurringStartTime?.let { start ->
                        restriction.recurringEndTime?.let { end ->
                            slotStartTime >= start && slotStartTime < end
                        }
                    } ?: false
                }
                RestrictionType.MEDICAL_PROCEDURE -> {
                    restriction.startTime?.let { start ->
                        restriction.endTime?.let { end ->
                            hasOverlap(slotStart, slotEnd, start, end)
                        }
                    } ?: false
                }
                else -> false
            }
        }
    }

    private fun hasOverlap(
        start1: Instant,
        end1: Instant,
        start2: Instant,
        end2: Instant,
    ): Boolean {
        return start1 < end2 && start2 < end1
    }
}

class InvalidBeneficiaryException(message: String) : Exception(message)
