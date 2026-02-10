package com.markduenas.visischeduler.testutil

import com.markduenas.visischeduler.domain.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Fake repository implementations for testing.
 * These provide in-memory storage with full control over behavior.
 */

// ============================================================
// RESULT TYPE
// ============================================================

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
    fun exceptionOrNull(): Throwable? = (this as? Error)?.exception

    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
    }

    companion object {
        fun <T> success(data: T): Result<T> = Success(data)
        fun error(exception: Throwable): Result<Nothing> = Error(exception)
    }
}

// ============================================================
// REPOSITORY INTERFACES
// ============================================================

interface VisitRepository {
    suspend fun getById(id: String): Result<Visit?>
    suspend fun getByBeneficiary(beneficiaryId: String): Result<List<Visit>>
    suspend fun getByVisitor(visitorId: String): Result<List<Visit>>
    suspend fun getByDateRange(beneficiaryId: String, startDate: LocalDate, endDate: LocalDate): Result<List<Visit>>
    suspend fun save(visit: Visit): Result<String>
    suspend fun update(visit: Visit): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun isSlotAvailable(beneficiaryId: String, date: LocalDate, startTime: LocalTime, endTime: LocalTime): Result<Boolean>
    suspend fun getConflictingVisits(beneficiaryId: String, date: LocalDate, startTime: LocalTime, endTime: LocalTime): Result<List<Visit>>
    fun observeByBeneficiary(beneficiaryId: String): Flow<List<Visit>>
    fun observePending(beneficiaryId: String): Flow<List<Visit>>
}

interface UserRepository {
    suspend fun getById(id: String): Result<User?>
    suspend fun getByEmail(email: String): Result<User?>
    suspend fun save(user: User): Result<String>
    suspend fun update(user: User): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun getCoordinatorsForBeneficiary(beneficiaryId: String): Result<List<User>>
    suspend fun getVisitorsByBeneficiary(beneficiaryId: String): Result<List<User>>
    suspend fun incrementFailedLoginAttempts(userId: String): Result<Int>
    suspend fun resetFailedLoginAttempts(userId: String): Result<Unit>
    suspend fun lockAccount(userId: String): Result<Unit>
    suspend fun isAccountLocked(userId: String): Result<Boolean>
}

interface RestrictionRepository {
    suspend fun getById(id: String): Result<Restriction?>
    suspend fun getByFacility(facilityId: String): Result<List<Restriction>>
    suspend fun getActiveByFacility(facilityId: String): Result<List<Restriction>>
    suspend fun save(restriction: Restriction): Result<String>
    suspend fun update(restriction: Restriction): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun getApplicableRestrictions(
        facilityId: String,
        visitorId: String?,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Result<List<Restriction>>
}

interface BeneficiaryRepository {
    suspend fun getById(id: String): Result<Beneficiary?>
    suspend fun getByFacility(facilityId: String): Result<List<Beneficiary>>
    suspend fun save(beneficiary: Beneficiary): Result<String>
    suspend fun update(beneficiary: Beneficiary): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}

interface TimeSlotRepository {
    suspend fun getById(id: String): Result<TimeSlot?>
    suspend fun getByFacilityAndDate(facilityId: String, date: LocalDate): Result<List<TimeSlot>>
    suspend fun getAvailable(facilityId: String, date: LocalDate): Result<List<TimeSlot>>
    suspend fun save(timeSlot: TimeSlot): Result<String>
    suspend fun update(timeSlot: TimeSlot): Result<Unit>
}

// ============================================================
// FAKE IMPLEMENTATIONS
// ============================================================

class FakeVisitRepository : VisitRepository {
    private val visits = mutableMapOf<String, Visit>()
    private val visitsFlow = MutableStateFlow<List<Visit>>(emptyList())

    // Test control flags
    var shouldFail = false
    var failureException: Throwable = Exception("Simulated failure")

    override suspend fun getById(id: String): Result<Visit?> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(visits[id])
    }

    override suspend fun getByBeneficiary(beneficiaryId: String): Result<List<Visit>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(visits.values.filter { it.beneficiaryId == beneficiaryId })
    }

    override suspend fun getByVisitor(visitorId: String): Result<List<Visit>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(visits.values.filter { it.visitorId == visitorId })
    }

    override suspend fun getByDateRange(
        beneficiaryId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<Visit>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            visits.values.filter {
                it.beneficiaryId == beneficiaryId &&
                    it.scheduledDate >= startDate &&
                    it.scheduledDate <= endDate
            }
        )
    }

    override suspend fun save(visit: Visit): Result<String> {
        if (shouldFail) return Result.error(failureException)
        val id = visit.id.ifEmpty { TestFixtures.generateId("visit") }
        val savedVisit = visit.copy(id = id)
        visits[id] = savedVisit
        updateFlow()
        return Result.success(id)
    }

    override suspend fun update(visit: Visit): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        if (!visits.containsKey(visit.id)) {
            return Result.error(IllegalArgumentException("Visit not found: ${visit.id}"))
        }
        visits[visit.id] = visit
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        visits.remove(id)
        updateFlow()
        return Result.success(Unit)
    }

    override suspend fun isSlotAvailable(
        beneficiaryId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Result<Boolean> {
        if (shouldFail) return Result.error(failureException)
        val conflicting = visits.values.any {
            it.beneficiaryId == beneficiaryId &&
                it.scheduledDate == date &&
                it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING) &&
                hasTimeOverlap(it.startTime, it.endTime, startTime, endTime)
        }
        return Result.success(!conflicting)
    }

    override suspend fun getConflictingVisits(
        beneficiaryId: String,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Result<List<Visit>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            visits.values.filter {
                it.beneficiaryId == beneficiaryId &&
                    it.scheduledDate == date &&
                    it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING) &&
                    hasTimeOverlap(it.startTime, it.endTime, startTime, endTime)
            }
        )
    }

    override fun observeByBeneficiary(beneficiaryId: String): Flow<List<Visit>> {
        return visitsFlow.map { list ->
            list.filter { it.beneficiaryId == beneficiaryId }
        }
    }

    override fun observePending(beneficiaryId: String): Flow<List<Visit>> {
        return visitsFlow.map { list ->
            list.filter {
                it.beneficiaryId == beneficiaryId && it.status == VisitStatus.PENDING
            }
        }
    }

    // Test helpers
    fun addVisit(visit: Visit) {
        visits[visit.id] = visit
        updateFlow()
    }

    fun clear() {
        visits.clear()
        updateFlow()
    }

    fun getAllVisits(): List<Visit> = visits.values.toList()

    private fun updateFlow() {
        visitsFlow.value = visits.values.toList()
    }

    private fun hasTimeOverlap(
        start1: LocalTime,
        end1: LocalTime,
        start2: LocalTime,
        end2: LocalTime
    ): Boolean {
        return start1 < end2 && start2 < end1
    }
}

class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()
    private val failedAttempts = mutableMapOf<String, Int>()
    private val lockedAccounts = mutableSetOf<String>()

    var shouldFail = false
    var failureException: Throwable = Exception("Simulated failure")

    override suspend fun getById(id: String): Result<User?> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(users[id])
    }

    override suspend fun getByEmail(email: String): Result<User?> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(users.values.find { it.email == email })
    }

    override suspend fun save(user: User): Result<String> {
        if (shouldFail) return Result.error(failureException)
        val id = user.id.ifEmpty { TestFixtures.generateId("user") }
        val savedUser = user.copy(id = id)
        users[id] = savedUser
        return Result.success(id)
    }

    override suspend fun update(user: User): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        if (!users.containsKey(user.id)) {
            return Result.error(IllegalArgumentException("User not found: ${user.id}"))
        }
        users[user.id] = user
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        users.remove(id)
        return Result.success(Unit)
    }

    override suspend fun getCoordinatorsForBeneficiary(beneficiaryId: String): Result<List<User>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            users.values.filter {
                it.role in listOf(Role.PRIMARY_COORDINATOR, Role.SECONDARY_COORDINATOR) &&
                    beneficiaryId in it.associatedBeneficiaryIds
            }
        )
    }

    override suspend fun getVisitorsByBeneficiary(beneficiaryId: String): Result<List<User>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            users.values.filter {
                it.role in listOf(Role.APPROVED_VISITOR, Role.PENDING_VISITOR) &&
                    beneficiaryId in it.associatedBeneficiaryIds
            }
        )
    }

    override suspend fun incrementFailedLoginAttempts(userId: String): Result<Int> {
        if (shouldFail) return Result.error(failureException)
        val current = failedAttempts.getOrDefault(userId, 0)
        val newValue = current + 1
        failedAttempts[userId] = newValue
        return Result.success(newValue)
    }

    override suspend fun resetFailedLoginAttempts(userId: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        failedAttempts.remove(userId)
        return Result.success(Unit)
    }

    override suspend fun lockAccount(userId: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        lockedAccounts.add(userId)
        return Result.success(Unit)
    }

    override suspend fun isAccountLocked(userId: String): Result<Boolean> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(userId in lockedAccounts)
    }

    // Test helpers
    fun addUser(user: User) {
        users[user.id] = user
    }

    fun clear() {
        users.clear()
        failedAttempts.clear()
        lockedAccounts.clear()
    }

    fun getAllUsers(): List<User> = users.values.toList()

    fun getFailedAttempts(userId: String): Int = failedAttempts.getOrDefault(userId, 0)

    fun unlockAccount(userId: String) {
        lockedAccounts.remove(userId)
    }
}

class FakeRestrictionRepository : RestrictionRepository {
    private val restrictions = mutableMapOf<String, Restriction>()

    var shouldFail = false
    var failureException: Throwable = Exception("Simulated failure")

    override suspend fun getById(id: String): Result<Restriction?> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(restrictions[id])
    }

    override suspend fun getByFacility(facilityId: String): Result<List<Restriction>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(restrictions.values.filter { it.facilityId == facilityId })
    }

    override suspend fun getActiveByFacility(facilityId: String): Result<List<Restriction>> {
        if (shouldFail) return Result.error(failureException)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        return Result.success(
            restrictions.values.filter {
                it.facilityId == facilityId &&
                    it.isActive &&
                    it.effectiveFrom <= today &&
                    (it.effectiveUntil == null || it.effectiveUntil >= today)
            }
        )
    }

    override suspend fun save(restriction: Restriction): Result<String> {
        if (shouldFail) return Result.error(failureException)
        val id = restriction.id.ifEmpty { TestFixtures.generateId("restriction") }
        val saved = restriction.copy(id = id)
        restrictions[id] = saved
        return Result.success(id)
    }

    override suspend fun update(restriction: Restriction): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        if (!restrictions.containsKey(restriction.id)) {
            return Result.error(IllegalArgumentException("Restriction not found: ${restriction.id}"))
        }
        restrictions[restriction.id] = restriction
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        restrictions.remove(id)
        return Result.success(Unit)
    }

    override suspend fun getApplicableRestrictions(
        facilityId: String,
        visitorId: String?,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Result<List<Restriction>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            restrictions.values.filter { restriction ->
                restriction.facilityId == facilityId &&
                    restriction.isActive &&
                    restriction.effectiveFrom <= date &&
                    (restriction.effectiveUntil == null || restriction.effectiveUntil >= date) &&
                    isRestrictionApplicable(restriction, visitorId, date, startTime, endTime)
            }
        )
    }

    private fun isRestrictionApplicable(
        restriction: Restriction,
        visitorId: String?,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Boolean {
        return when (restriction.type) {
            RestrictionType.VISITOR_BASED -> {
                val blockedIds = restriction.visitorConstraints?.blockedVisitorIds
                visitorId != null && blockedIds != null && visitorId in blockedIds
            }
            RestrictionType.TIME_BASED -> {
                val timeConstraints = restriction.timeConstraints ?: return false
                val blockedDays = timeConstraints.blockedDays
                if (blockedDays != null && date.dayOfWeek in blockedDays) return true

                val earliest = timeConstraints.earliestStartTime
                val latest = timeConstraints.latestEndTime
                if (earliest != null && startTime < earliest) return true
                if (latest != null && endTime > latest) return true

                false
            }
            else -> false
        }
    }

    // Test helpers
    fun addRestriction(restriction: Restriction) {
        restrictions[restriction.id] = restriction
    }

    fun clear() {
        restrictions.clear()
    }

    fun getAllRestrictions(): List<Restriction> = restrictions.values.toList()
}

class FakeBeneficiaryRepository : BeneficiaryRepository {
    private val beneficiaries = mutableMapOf<String, Beneficiary>()

    var shouldFail = false
    var failureException: Throwable = Exception("Simulated failure")

    override suspend fun getById(id: String): Result<Beneficiary?> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(beneficiaries[id])
    }

    override suspend fun getByFacility(facilityId: String): Result<List<Beneficiary>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(beneficiaries.values.filter { it.facilityId == facilityId })
    }

    override suspend fun save(beneficiary: Beneficiary): Result<String> {
        if (shouldFail) return Result.error(failureException)
        val id = beneficiary.id.ifEmpty { TestFixtures.generateId("beneficiary") }
        val saved = beneficiary.copy(id = id)
        beneficiaries[id] = saved
        return Result.success(id)
    }

    override suspend fun update(beneficiary: Beneficiary): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        if (!beneficiaries.containsKey(beneficiary.id)) {
            return Result.error(IllegalArgumentException("Beneficiary not found: ${beneficiary.id}"))
        }
        beneficiaries[beneficiary.id] = beneficiary
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        beneficiaries.remove(id)
        return Result.success(Unit)
    }

    // Test helpers
    fun addBeneficiary(beneficiary: Beneficiary) {
        beneficiaries[beneficiary.id] = beneficiary
    }

    fun clear() {
        beneficiaries.clear()
    }

    fun getAllBeneficiaries(): List<Beneficiary> = beneficiaries.values.toList()
}

class FakeTimeSlotRepository : TimeSlotRepository {
    private val timeSlots = mutableMapOf<String, TimeSlot>()

    var shouldFail = false
    var failureException: Throwable = Exception("Simulated failure")

    override suspend fun getById(id: String): Result<TimeSlot?> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(timeSlots[id])
    }

    override suspend fun getByFacilityAndDate(facilityId: String, date: LocalDate): Result<List<TimeSlot>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            timeSlots.values.filter { it.facilityId == facilityId && it.date == date }
        )
    }

    override suspend fun getAvailable(facilityId: String, date: LocalDate): Result<List<TimeSlot>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            timeSlots.values.filter {
                it.facilityId == facilityId && it.date == date && it.isAvailable && !it.isFull
            }
        )
    }

    override suspend fun save(timeSlot: TimeSlot): Result<String> {
        if (shouldFail) return Result.error(failureException)
        val id = timeSlot.id.ifEmpty { TestFixtures.generateId("slot") }
        val saved = timeSlot.copy(id = id)
        timeSlots[id] = saved
        return Result.success(id)
    }

    override suspend fun update(timeSlot: TimeSlot): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        if (!timeSlots.containsKey(timeSlot.id)) {
            return Result.error(IllegalArgumentException("TimeSlot not found: ${timeSlot.id}"))
        }
        timeSlots[timeSlot.id] = timeSlot
        return Result.success(Unit)
    }

    // Test helpers
    fun addTimeSlot(timeSlot: TimeSlot) {
        timeSlots[timeSlot.id] = timeSlot
    }

    fun clear() {
        timeSlots.clear()
    }

    fun getAllTimeSlots(): List<TimeSlot> = timeSlots.values.toList()
}
