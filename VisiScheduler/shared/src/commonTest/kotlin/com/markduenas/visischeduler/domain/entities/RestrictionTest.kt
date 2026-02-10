package com.markduenas.visischeduler.domain.entities

import com.markduenas.visischeduler.testutil.*
import kotlinx.datetime.*
import kotlin.test.*

/**
 * Tests for Restriction entity covering creation and property validation.
 *
 * @test Restriction Entity
 * @prerequisites None - pure domain tests
 */
class RestrictionTest {

    @BeforeTest
    fun setup() {
        TestFixtures.resetIdCounter()
    }

    // ============================================================
    // CREATION TESTS
    // ============================================================

    @Test
    fun `should create restriction with required fields`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction(
            name = "Test Restriction",
            description = "A test restriction",
        )

        // Assert
        assertNotNull(restriction)
        assertEquals("Test Restriction", restriction.name)
        assertEquals("A test restriction", restriction.description)
        assertTrue(restriction.isActive)
    }

    @Test
    fun `should create time-based restriction`() {
        // Arrange & Act
        val restriction = TestFixtures.createTimeBasedRestriction(
            name = "Business Hours Only",
            earliestStartTime = LocalTime(9, 0),
            latestEndTime = LocalTime(17, 0),
        )

        // Assert
        assertEquals(RestrictionType.TIME_BASED, restriction.type)
        assertNotNull(restriction.timeConstraints)
        assertEquals(LocalTime(9, 0), restriction.timeConstraints?.earliestStartTime)
        assertEquals(LocalTime(17, 0), restriction.timeConstraints?.latestEndTime)
    }

    @Test
    fun `should create time-based restriction with blocked days`() {
        // Arrange & Act
        val restriction = TestFixtures.createTimeBasedRestriction(
            name = "No Weekends",
            blockedDays = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
        )

        // Assert
        assertNotNull(restriction.timeConstraints?.blockedDays)
        assertEquals(2, restriction.timeConstraints?.blockedDays?.size)
        assertTrue(restriction.timeConstraints?.blockedDays?.contains(DayOfWeek.SATURDAY) == true)
        assertTrue(restriction.timeConstraints?.blockedDays?.contains(DayOfWeek.SUNDAY) == true)
    }

    @Test
    fun `should create visitor block restriction`() {
        // Arrange
        val blockedVisitorId = "blocked-visitor-123"

        // Act
        val restriction = TestFixtures.createVisitorBlockRestriction(
            blockedVisitorId = blockedVisitorId,
            reason = "Not permitted on premises",
        )

        // Assert
        assertEquals(RestrictionType.VISITOR_BASED, restriction.type)
        assertEquals(RestrictionScope.VISITOR_SPECIFIC, restriction.scope)
        assertNotNull(restriction.visitorConstraints)
        assertTrue(restriction.visitorConstraints?.blockedVisitorIds?.contains(blockedVisitorId) == true)
    }

    // ============================================================
    // SCOPE TESTS
    // ============================================================

    @Test
    fun `should set correct scope for facility-wide restriction`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction(
            scope = RestrictionScope.FACILITY_WIDE,
        )

        // Assert
        assertEquals(RestrictionScope.FACILITY_WIDE, restriction.scope)
    }

    @Test
    fun `should set correct scope for beneficiary-specific restriction`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction(
            scope = RestrictionScope.BENEFICIARY_SPECIFIC,
        )

        // Assert
        assertEquals(RestrictionScope.BENEFICIARY_SPECIFIC, restriction.scope)
    }

    // ============================================================
    // PRIORITY TESTS
    // ============================================================

    @Test
    fun `should set priority value`() {
        // Arrange & Act
        val highPriority = TestFixtures.createRestriction(priority = 100)
        val lowPriority = TestFixtures.createRestriction(priority = 1)

        // Assert
        assertEquals(100, highPriority.priority)
        assertEquals(1, lowPriority.priority)
    }

    // ============================================================
    // EFFECTIVE DATE TESTS
    // ============================================================

    @Test
    fun `should have effective date range`() {
        // Arrange
        val startDate = LocalDate(2024, 6, 1)
        val endDate = LocalDate(2024, 12, 31)

        // Act
        val restriction = TestFixtures.createRestriction(
            effectiveFrom = startDate,
            effectiveUntil = endDate,
        )

        // Assert
        assertEquals(startDate, restriction.effectiveFrom)
        assertEquals(endDate, restriction.effectiveUntil)
    }

    @Test
    fun `should support null end date for indefinite restrictions`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction(
            effectiveUntil = null,
        )

        // Assert
        assertNull(restriction.effectiveUntil)
    }

    // ============================================================
    // ACTIVE STATUS TESTS
    // ============================================================

    @Test
    fun `should be active by default`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction()

        // Assert
        assertTrue(restriction.isActive)
    }

    @Test
    fun `should allow creating inactive restriction`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction(
            isActive = false,
        )

        // Assert
        assertFalse(restriction.isActive)
    }

    // ============================================================
    // TIME CONSTRAINTS TESTS
    // ============================================================

    @Test
    fun `should set max duration minutes in time constraints`() {
        // Arrange & Act
        val restriction = TestFixtures.createTimeBasedRestriction(
            maxDurationMinutes = 60,
        )

        // Assert
        assertEquals(60, restriction.timeConstraints?.maxDurationMinutes)
    }

    @Test
    fun `should allow null time constraints`() {
        // Arrange & Act
        val restriction = TestFixtures.createRestriction(
            timeConstraints = null,
        )

        // Assert
        assertNull(restriction.timeConstraints)
    }

    // ============================================================
    // VISITOR CONSTRAINTS TESTS
    // ============================================================

    @Test
    fun `should create restriction with blocked visitor list`() {
        // Arrange
        val blockedIds = listOf("visitor-1", "visitor-2", "visitor-3")

        // Act
        val restriction = TestFixtures.createRestriction(
            type = RestrictionType.VISITOR_BASED,
            visitorConstraints = VisitorConstraints(
                blockedVisitorIds = blockedIds,
            ),
        )

        // Assert
        assertEquals(blockedIds.size, restriction.visitorConstraints?.blockedVisitorIds?.size)
        assertTrue(restriction.visitorConstraints?.blockedVisitorIds?.containsAll(blockedIds) == true)
    }

    // ============================================================
    // FACILITY TESTS
    // ============================================================

    @Test
    fun `should associate restriction with facility`() {
        // Arrange
        val facilityId = "facility-123"

        // Act
        val restriction = TestFixtures.createRestriction(
            facilityId = facilityId,
        )

        // Assert
        assertEquals(facilityId, restriction.facilityId)
    }

    // ============================================================
    // COPY TESTS
    // ============================================================

    @Test
    fun `should copy restriction with modified fields`() {
        // Arrange
        val original = TestFixtures.createRestriction(
            name = "Original",
            isActive = true,
        )

        // Act
        val modified = original.copy(
            name = "Modified",
            isActive = false,
        )

        // Assert
        assertEquals("Modified", modified.name)
        assertFalse(modified.isActive)
        assertEquals(original.id, modified.id) // ID preserved
        assertEquals(original.type, modified.type) // Other fields preserved
    }
}
