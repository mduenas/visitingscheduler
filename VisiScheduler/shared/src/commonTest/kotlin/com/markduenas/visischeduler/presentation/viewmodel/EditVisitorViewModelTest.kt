package com.markduenas.visischeduler.presentation.viewmodel

import com.markduenas.visischeduler.domain.entities.*
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.presentation.viewmodel.visitors.EditVisitorViewModel
import com.markduenas.visischeduler.testutil.TestFixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class EditVisitorViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeUserRepository: FakeEditUserRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeUserRepository = FakeEditUserRepository()
        TestFixtures.resetIdCounter()
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadVisitor success pre-populates form fields`() = runTest {
        val user = TestFixtures.createApprovedVisitor(
            id = "u1",
            email = "jane@example.com",
            firstName = "Jane",
            lastName = "Smith"
        ).copy(phoneNumber = "555-1234")
        fakeUserRepository.userToReturn = Result.success(user)

        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("Jane", state.firstName)
        assertEquals("Smith", state.lastName)
        assertEquals("555-1234", state.phoneNumber)
        assertEquals("jane@example.com", state.email)
    }

    @Test
    fun `loadVisitor failure sets error state`() = runTest {
        fakeUserRepository.userToReturn = Result.failure(Exception("User not found"))

        val vm = EditVisitorViewModel(fakeUserRepository, "missing")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("", state.firstName)
    }

    @Test
    fun `setFirstName updates firstName`() = runTest {
        fakeUserRepository.userToReturn = Result.success(makeUser())
        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        vm.setFirstName("Alice")

        assertEquals("Alice", vm.uiState.value.firstName)
    }

    @Test
    fun `setLastName updates lastName`() = runTest {
        fakeUserRepository.userToReturn = Result.success(makeUser())
        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        vm.setLastName("Wonder")

        assertEquals("Wonder", vm.uiState.value.lastName)
    }

    @Test
    fun `setPhoneNumber updates phoneNumber`() = runTest {
        fakeUserRepository.userToReturn = Result.success(makeUser())
        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        vm.setPhoneNumber("800-555-0000")

        assertEquals("800-555-0000", vm.uiState.value.phoneNumber)
    }

    @Test
    fun `canSubmit requires non-blank firstName and lastName`() = runTest {
        fakeUserRepository.userToReturn = Result.success(makeUser())
        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        // Should be submittable with loaded user
        assertTrue(vm.uiState.value.canSubmit)

        // Clear firstName
        vm.setFirstName("")
        assertFalse(vm.uiState.value.canSubmit)

        // Restore firstName, clear lastName
        vm.setFirstName("Jane")
        vm.setLastName("")
        assertFalse(vm.uiState.value.canSubmit)
    }

    @Test
    fun `saveChanges success clears submitting flag`() = runTest {
        val user = makeUser()
        fakeUserRepository.userToReturn = Result.success(user)
        fakeUserRepository.updateProfileResult = Result.success(user)

        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        vm.saveChanges()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubmitting)
        assertNull(vm.uiState.value.error)
        assertTrue(fakeUserRepository.updateProfileCalled)
    }

    @Test
    fun `saveChanges failure sets error state`() = runTest {
        fakeUserRepository.userToReturn = Result.success(makeUser())
        fakeUserRepository.updateProfileResult = Result.failure(Exception("Network error"))

        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()

        vm.saveChanges()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isSubmitting)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `saveChanges passes blank phone as null`() = runTest {
        val user = makeUser()
        fakeUserRepository.userToReturn = Result.success(user)
        fakeUserRepository.updateProfileResult = Result.success(user)

        val vm = EditVisitorViewModel(fakeUserRepository, "u1")
        advanceUntilIdle()
        vm.setPhoneNumber("") // blank

        vm.saveChanges()
        advanceUntilIdle()

        assertTrue(fakeUserRepository.updateProfileCalled)
        assertNull(fakeUserRepository.lastPhoneNumber) // blank is converted to null
    }

    private fun makeUser() = TestFixtures.createApprovedVisitor(
        id = "u1",
        email = "visitor@example.com",
        firstName = "Test",
        lastName = "Visitor"
    )
}

// ============================================================
// FAKE REPOSITORY
// ============================================================

class FakeEditUserRepository : UserRepository {
    var userToReturn: Result<User> = Result.failure(Exception("Not set"))
    var updateProfileResult: Result<User> = Result.failure(Exception("Not set"))
    var updateProfileCalled = false
    var lastPhoneNumber: String? = "UNSET"

    override val currentUser: Flow<User?> = MutableStateFlow(null)

    override suspend fun getUserById(userId: String): Result<User> = userToReturn

    override suspend fun updateProfile(
        firstName: String?,
        lastName: String?,
        phoneNumber: String?,
        profileImageUrl: String?
    ): Result<User> {
        updateProfileCalled = true
        lastPhoneNumber = phoneNumber
        return updateProfileResult
    }

    // Unneeded stubs
    override suspend fun getUserByEmail(email: String): Result<User> = Result.failure(NotImplementedError())
    override fun getAllUsers(): Flow<List<User>> = MutableStateFlow(emptyList())
    override fun getUsersByRole(role: Role): Flow<List<User>> = MutableStateFlow(emptyList())
    override fun getPendingVisitors(): Flow<List<User>> = MutableStateFlow(emptyList())
    override suspend fun searchUsers(query: String): Result<List<User>> = Result.success(emptyList())
    override suspend fun updateNotificationPreferences(preferences: NotificationPreferences): Result<User> = Result.failure(NotImplementedError())
    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun updateUserRole(userId: String, newRole: Role): Result<User> = Result.failure(NotImplementedError())
    override suspend fun approveVisitor(userId: String): Result<User> = Result.failure(NotImplementedError())
    override suspend fun denyVisitor(userId: String, reason: String): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun deactivateUser(userId: String): Result<User> = Result.failure(NotImplementedError())
    override suspend fun reactivateUser(userId: String): Result<User> = Result.failure(NotImplementedError())
    override suspend fun associateBeneficiary(beneficiaryId: String): Result<User> = Result.failure(NotImplementedError())
    override suspend fun removeBeneficiaryAssociation(beneficiaryId: String): Result<User> = Result.failure(NotImplementedError())
    override suspend fun uploadProfileImage(imageData: ByteArray): Result<String> = Result.failure(NotImplementedError())
    override suspend fun deleteAccount(): Result<Unit> = Result.failure(NotImplementedError())
    override suspend fun syncUser(): Result<User> = Result.failure(NotImplementedError())
}
