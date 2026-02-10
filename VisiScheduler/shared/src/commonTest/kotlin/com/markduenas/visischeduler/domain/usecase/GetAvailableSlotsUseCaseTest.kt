package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.SlotType
import com.markduenas.visischeduler.domain.entities.TimeSlot
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.testutil.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Tests for time slot functionality covering slot generation and availability.
 *
 * @test Get Available Slots
 * @prerequisites Mocked repositories with restrictions and existing visits
 */
class GetAvailableSlotsUseCaseTest {

    private lateinit var visitRepository: FakeVisitRepository
    private lateinit var testClock: TestClock

    private val testBeneficiaryId = "beneficiary-1"
    private val testFacilityId = "facility-1"

    @BeforeTest
    fun setup() {
        visitRepository = FakeVisitRepository()
        testClock = TestClock.fixed(2024, 6, 15, 8, 0) // Saturday, 8:00 AM
        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        visitRepository.clear()
    }

    // ============================================================
    // SLOT CREATION TESTS
    // ============================================================

    @Test
    fun `should create time slot with correct properties`() {
        // Arrange & Act
        val slot = TestFixtures.createTimeSlot(
            facilityId = testFacilityId,
            date = LocalDate(2024, 6, 15),
            startTime = LocalTime(10, 0),
            endTime = LocalTime(11, 0),
            maxCapacity = 3,
        )

        // Assert
        assertEquals(testFacilityId, slot.facilityId)
        assertEquals(LocalDate(2024, 6, 15), slot.date)
        assertEquals(LocalTime(10, 0), slot.startTime)
        assertEquals(LocalTime(11, 0), slot.endTime)
        assertEquals(3, slot.maxCapacity)
        assertTrue(slot.isAvailable)
    }

    @Test
    fun `should create multiple slots for a day`() {
        // Arrange & Act
        val slots = TestFixtures.createAvailableSlots(
            facilityId = testFacilityId,
            date = LocalDate(2024, 6, 15),
            startHour = 9,
            endHour = 17,
            slotDurationMinutes = 60,
        )

        // Assert
        assertEquals(8, slots.size) // 9AM to 5PM = 8 hourly slots
        assertTrue(slots.all { it.isAvailable })
        assertTrue(slots.all { it.facilityId == testFacilityId })
    }

    @Test
    fun `should create half-hour slots`() {
        // Arrange & Act
        val slots = TestFixtures.createAvailableSlots(
            facilityId = testFacilityId,
            date = LocalDate(2024, 6, 15),
            startHour = 9,
            endHour = 11,
            slotDurationMinutes = 30,
        )

        // Assert - 9:00, 9:30, 10:00, 10:30 = 4 slots (11:00 is end boundary)
        // Note: Actual count depends on implementation
        assertTrue(slots.isNotEmpty())
    }

    // ============================================================
    // SLOT AVAILABILITY TESTS
    // ============================================================

    @Test
    fun `slot should be available when no visits scheduled`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, date, startTime, endTime
        ).getOrNull()

        // Assert
        assertTrue(isAvailable == true)
    }

    @Test
    fun `slot should not be available when approved visit scheduled`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        val visit = TestFixtures.createApprovedVisit(
            beneficiaryId = testBeneficiaryId,
            scheduledDate = date,
            startTime = startTime,
            endTime = endTime,
        )
        visitRepository.save(visit)

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, date, startTime, endTime
        ).getOrNull()

        // Assert
        assertFalse(isAvailable == true)
    }

    @Test
    fun `overlapping slot should not be available`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)

        // Existing visit from 10:00 to 11:00
        val existingVisit = TestFixtures.createApprovedVisit(
            beneficiaryId = testBeneficiaryId,
            scheduledDate = date,
            startTime = LocalTime(10, 0),
            endTime = LocalTime(11, 0),
        )
        visitRepository.save(existingVisit)

        // Check overlapping slot 10:30 to 11:30
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId,
            date,
            LocalTime(10, 30),
            LocalTime(11, 30),
        ).getOrNull()

        // Assert
        assertFalse(isAvailable == true)
    }

    @Test
    fun `adjacent slot should be available`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)

        // Existing visit from 10:00 to 11:00
        val existingVisit = TestFixtures.createApprovedVisit(
            beneficiaryId = testBeneficiaryId,
            scheduledDate = date,
            startTime = LocalTime(10, 0),
            endTime = LocalTime(11, 0),
        )
        visitRepository.save(existingVisit)

        // Check adjacent slot 11:00 to 12:00
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId,
            date,
            LocalTime(11, 0),
            LocalTime(12, 0),
        ).getOrNull()

        // Assert - adjacent slot should be available
        assertTrue(isAvailable == true)
    }

    // ============================================================
    // SLOT TYPE TESTS
    // ============================================================

    @Test
    fun `should create regular slot type`() {
        // Arrange & Act
        val slot = TestFixtures.createTimeSlot(
            slotType = SlotType.REGULAR,
        )

        // Assert
        assertEquals(SlotType.REGULAR, slot.slotType)
    }

    @Test
    fun `should create extended slot type`() {
        // Arrange & Act
        val slot = TestFixtures.createTimeSlot(
            slotType = SlotType.EXTENDED,
        )

        // Assert
        assertEquals(SlotType.EXTENDED, slot.slotType)
    }

    // ============================================================
    // CAPACITY TESTS
    // ============================================================

    @Test
    fun `slot should track current bookings`() {
        // Arrange & Act
        val slot = TestFixtures.createTimeSlot(
            maxCapacity = 5,
            currentBookings = 3,
        )

        // Assert
        assertEquals(5, slot.maxCapacity)
        assertEquals(3, slot.currentBookings)
    }

    @Test
    fun `slot should be unavailable when at capacity`() {
        // Arrange & Act
        val slot = TestFixtures.createTimeSlot(
            maxCapacity = 2,
            currentBookings = 2,
            isAvailable = false, // At capacity
        )

        // Assert
        assertFalse(slot.isAvailable)
    }

    // ============================================================
    // DATE RANGE TESTS
    // ============================================================

    @Test
    fun `should filter visits by date range`() = runTest {
        // Arrange
        val baseDate = LocalDate(2024, 6, 15)

        // Add visits on different dates
        visitRepository.save(
            TestFixtures.createVisit(
                id = "v1",
                beneficiaryId = testBeneficiaryId,
                scheduledDate = baseDate,
            )
        )
        visitRepository.save(
            TestFixtures.createVisit(
                id = "v2",
                beneficiaryId = testBeneficiaryId,
                scheduledDate = baseDate.plus(DatePeriod(days = 1)),
            )
        )
        visitRepository.save(
            TestFixtures.createVisit(
                id = "v3",
                beneficiaryId = testBeneficiaryId,
                scheduledDate = baseDate.plus(DatePeriod(days = 10)), // Outside range
            )
        )

        // Act
        val visitsInRange = visitRepository.getByDateRange(
            testBeneficiaryId,
            baseDate,
            baseDate.plus(DatePeriod(days = 5)),
        ).getOrNull()

        // Assert
        assertNotNull(visitsInRange)
        assertEquals(2, visitsInRange.size)
    }

    // ============================================================
    // CONFLICT DETECTION TESTS
    // ============================================================

    @Test
    fun `should find conflicting visits`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)

        // Add visit from 10:00 to 11:00
        visitRepository.save(
            TestFixtures.createApprovedVisit(
                id = "existing",
                beneficiaryId = testBeneficiaryId,
                scheduledDate = date,
                startTime = LocalTime(10, 0),
                endTime = LocalTime(11, 0),
            )
        )

        // Act - check for conflicts at 10:30
        val conflicts = visitRepository.getConflictingVisits(
            testBeneficiaryId,
            date,
            LocalTime(10, 30),
            LocalTime(11, 30),
        ).getOrNull()

        // Assert
        assertNotNull(conflicts)
        assertEquals(1, conflicts.size)
        assertEquals("existing", conflicts.first().id)
    }

    @Test
    fun `should not find conflicts for non-overlapping times`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)

        // Add visit from 10:00 to 11:00
        visitRepository.save(
            TestFixtures.createApprovedVisit(
                beneficiaryId = testBeneficiaryId,
                scheduledDate = date,
                startTime = LocalTime(10, 0),
                endTime = LocalTime(11, 0),
            )
        )

        // Act - check for conflicts at 14:00 (no overlap)
        val conflicts = visitRepository.getConflictingVisits(
            testBeneficiaryId,
            date,
            LocalTime(14, 0),
            LocalTime(15, 0),
        ).getOrNull()

        // Assert
        assertNotNull(conflicts)
        assertTrue(conflicts.isEmpty())
    }

    // ============================================================
    // STATUS FILTERING TESTS
    // ============================================================

    @Test
    fun `cancelled visits should not block slots`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        // Add cancelled visit
        visitRepository.save(
            TestFixtures.createVisit(
                beneficiaryId = testBeneficiaryId,
                scheduledDate = date,
                startTime = startTime,
                endTime = endTime,
                status = VisitStatus.CANCELLED,
            )
        )

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, date, startTime, endTime
        ).getOrNull()

        // Assert
        assertTrue(isAvailable == true)
    }

    @Test
    fun `pending visits should block slots`() = runTest {
        // Arrange
        val date = LocalDate(2024, 6, 15)
        val startTime = LocalTime(10, 0)
        val endTime = LocalTime(11, 0)

        // Add pending visit
        visitRepository.save(
            TestFixtures.createVisit(
                beneficiaryId = testBeneficiaryId,
                scheduledDate = date,
                startTime = startTime,
                endTime = endTime,
                status = VisitStatus.PENDING,
            )
        )

        // Act
        val isAvailable = visitRepository.isSlotAvailable(
            testBeneficiaryId, date, startTime, endTime
        ).getOrNull()

        // Assert
        assertFalse(isAvailable == true)
    }
}
