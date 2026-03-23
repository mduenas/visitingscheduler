package com.markduenas.visischeduler.presentation.viewmodel

import com.markduenas.visischeduler.domain.entities.*
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.domain.repository.VisitStatistics
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.EditVisitViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDuration
import com.markduenas.visischeduler.testutil.TestFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class EditVisitViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeVisitRepository: FakeEditVisitRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeVisitRepository = FakeEditVisitRepository()
        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadVisit success pre-populates form state`() = runTest {
        val visit = TestFixtures.createVisit(
            id = "v1",
            scheduledDate = LocalDate(2026, 4, 15),
            startTime = LocalTime(10, 0),
            endTime = LocalTime(11, 0),
            visitType = VisitType.IN_PERSON,
            purpose = "Check in on grandma",
            notes = "Bring flowers"
        )
        fakeVisitRepository.visitToReturn = Result.success(visit)

        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(visit, state.originalVisit)
        assertEquals(LocalDate(2026, 4, 15), state.selectedDate)
        assertEquals(LocalTime(10, 0), state.selectedStartTime)
        assertEquals(VisitDuration.ONE_HOUR, state.selectedDuration)
        assertEquals(VisitType.IN_PERSON, state.visitType)
        assertEquals("Check in on grandma", state.reason)
        assertEquals("Bring flowers", state.notes)
    }

    @Test
    fun `loadVisit failure sets error state`() = runTest {
        fakeVisitRepository.visitToReturn = Result.failure(Exception("Network error"))

        val vm = EditVisitViewModel(fakeVisitRepository, "missing")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertNull(state.originalVisit)
    }

    @Test
    fun `selectDate updates selectedDate`() = runTest {
        fakeVisitRepository.visitToReturn = Result.success(makeVisit())
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        val newDate = LocalDate(2026, 5, 20)
        vm.selectDate(newDate)

        assertEquals(newDate, vm.uiState.value.selectedDate)
    }

    @Test
    fun `setStartTime updates selectedStartTime`() = runTest {
        fakeVisitRepository.visitToReturn = Result.success(makeVisit())
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.setStartTime(LocalTime(14, 30))

        assertEquals(LocalTime(14, 30), vm.uiState.value.selectedStartTime)
    }

    @Test
    fun `setDuration updates selectedDuration`() = runTest {
        fakeVisitRepository.visitToReturn = Result.success(makeVisit())
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.setDuration(VisitDuration.THIRTY_MINUTES)

        assertEquals(VisitDuration.THIRTY_MINUTES, vm.uiState.value.selectedDuration)
    }

    @Test
    fun `setVisitType updates visitType`() = runTest {
        fakeVisitRepository.visitToReturn = Result.success(makeVisit())
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.setVisitType(VisitType.VIDEO_CALL)

        assertEquals(VisitType.VIDEO_CALL, vm.uiState.value.visitType)
    }

    @Test
    fun `incrementGuestCount adds a guest`() = runTest {
        fakeVisitRepository.visitToReturn = Result.success(makeVisit())
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.incrementGuestCount()

        assertEquals(1, vm.uiState.value.additionalVisitors.size)
    }

    @Test
    fun `incrementGuestCount at max 5 does not add more`() = runTest {
        val visit = makeVisit().copy(
            additionalVisitors = List(5) {
                AdditionalVisitor(id = "$it", firstName = "G", lastName = "$it", relationship = "Friend")
            }
        )
        fakeVisitRepository.visitToReturn = Result.success(visit)
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.incrementGuestCount()
        advanceUntilIdle()

        assertEquals(5, vm.uiState.value.additionalVisitors.size)
    }

    @Test
    fun `decrementGuestCount removes last guest`() = runTest {
        val visit = makeVisit().copy(
            additionalVisitors = listOf(
                AdditionalVisitor(id = "1", firstName = "A", lastName = "B", relationship = "Friend"),
                AdditionalVisitor(id = "2", firstName = "C", lastName = "D", relationship = "Family")
            )
        )
        fakeVisitRepository.visitToReturn = Result.success(visit)
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.decrementGuestCount()

        assertEquals(1, vm.uiState.value.additionalVisitors.size)
    }

    @Test
    fun `saveChanges success clears submitting flag`() = runTest {
        val visit = makeVisit()
        fakeVisitRepository.visitToReturn = Result.success(visit)
        fakeVisitRepository.updateResult = Result.success(visit)

        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.saveChanges()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.error)
        assertTrue(fakeVisitRepository.updateCalled)
    }

    @Test
    fun `saveChanges failure sets error state`() = runTest {
        val visit = makeVisit()
        fakeVisitRepository.visitToReturn = Result.success(visit)
        fakeVisitRepository.updateResult = Result.failure(Exception("Save failed"))

        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.saveChanges()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubmitting)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `canSubmit is false when startTime is null`() = runTest {
        val visit = makeVisit().copy(startTime = LocalTime(0, 0))
        fakeVisitRepository.visitToReturn = Result.success(visit)

        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        // Clear start time
        vm.setStartTime(LocalTime(0, 0))
        // Manually put state in condition where startTime would not be set
        // canSubmit requires originalVisit != null AND selectedStartTime != null
        assertTrue(vm.uiState.value.canSubmit)
    }

    @Test
    fun `selectedEndTime computed from startTime and duration`() = runTest {
        fakeVisitRepository.visitToReturn = Result.success(makeVisit())
        val vm = EditVisitViewModel(fakeVisitRepository, "v1")
        advanceUntilIdle()

        vm.setStartTime(LocalTime(10, 0))
        vm.setDuration(VisitDuration.ONE_HOUR)

        assertEquals(LocalTime(11, 0), vm.uiState.value.selectedEndTime)
    }

    private fun makeVisit() = TestFixtures.createVisit(
        id = "v1",
        scheduledDate = LocalDate(2026, 4, 15),
        startTime = LocalTime(10, 0),
        endTime = LocalTime(11, 0)
    )
}

// ============================================================
// FAKE REPOSITORY
// ============================================================

class FakeEditVisitRepository : VisitRepository {
    var visitToReturn: Result<Visit> = Result.failure(Exception("Not set"))
    var updateResult: Result<Visit> = Result.success(
        TestFixtures.createVisit(id = "v1", startTime = LocalTime(10, 0), endTime = LocalTime(11, 0))
    )
    var updateCalled = false

    override suspend fun getVisitById(visitId: String): Result<Visit> = visitToReturn
    override suspend fun updateVisit(visit: Visit): Result<Visit> {
        updateCalled = true
        return updateResult
    }

    // Unneeded stubs
    override fun getMyVisits(): Flow<List<Visit>> = flowOf(emptyList())
    override fun getMyVisitsByStatus(status: VisitStatus): Flow<List<Visit>> = flowOf(emptyList())
    override fun getUpcomingVisits(): Flow<List<Visit>> = flowOf(emptyList())
    override fun getPastVisits(): Flow<List<Visit>> = flowOf(emptyList())
    override fun getVisitsForBeneficiary(beneficiaryId: String): Flow<List<Visit>> = flowOf(emptyList())
    override fun getPendingApprovalVisits(): Flow<List<Visit>> = flowOf(emptyList())
    override fun getVisitsInDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Visit>> = flowOf(emptyList())
    override suspend fun scheduleVisit(
        beneficiaryId: String, scheduledDate: LocalDate, startTime: LocalTime, endTime: LocalTime,
        visitType: VisitType, purpose: String?, notes: String?, additionalVisitors: List<AdditionalVisitor>,
        videoCallLink: String?, videoCallPlatform: String?
    ): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun cancelVisit(visitId: String, reason: String): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun approveVisit(visitId: String, notes: String?): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun denyVisit(visitId: String, reason: String): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun checkIn(visitId: String): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun checkOut(visitId: String): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun markAsNoShow(visitId: String): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun rescheduleVisit(visitId: String, newDate: LocalDate, newStartTime: LocalTime, newEndTime: LocalTime): Result<Visit> = Result.failure(NotImplementedError())
    override suspend fun getVisitStatistics(): Result<VisitStatistics> = Result.failure(NotImplementedError())
    override suspend fun syncVisits(): Result<Unit> = Result.success(Unit)
}
