package com.markduenas.visischeduler.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.*
import kotlin.time.Duration

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
    suspend fun getByDateRange(beneficiaryId: String, start: Instant, end: Instant): Result<List<Visit>>
    suspend fun save(visit: Visit): Result<String>
    suspend fun update(visit: Visit): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun isSlotAvailable(beneficiaryId: String, startTime: Instant, endTime: Instant): Result<Boolean>
    suspend fun getConflictingVisits(beneficiaryId: String, startTime: Instant, endTime: Instant): Result<List<Visit>>
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
    suspend fun getByBeneficiary(beneficiaryId: String): Result<List<Restriction>>
    suspend fun getActiveByBeneficiary(beneficiaryId: String): Result<List<Restriction>>
    suspend fun save(restriction: Restriction): Result<String>
    suspend fun update(restriction: Restriction): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    suspend fun getApplicableRestrictions(
        beneficiaryId: String,
        visitorId: String?,
        timeSlot: TimeSlot
    ): Result<List<Restriction>>
}

interface SessionRepository {
    suspend fun save(session: Session): Result<Unit>
    suspend fun getByToken(token: String): Result<Session?>
    suspend fun invalidate(sessionId: String): Result<Unit>
    suspend fun invalidateAllForUser(userId: String): Result<Unit>
    suspend fun updateLastActivity(sessionId: String): Result<Unit>
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
    var simulateNetworkDelay = false
    var networkDelayMs: Long = 100

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
        start: Instant,
        end: Instant
    ): Result<List<Visit>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            visits.values.filter {
                it.beneficiaryId == beneficiaryId &&
                    it.startTime >= start &&
                    it.startTime <= end
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
        startTime: Instant,
        endTime: Instant
    ): Result<Boolean> {
        if (shouldFail) return Result.error(failureException)
        val conflicting = visits.values.any {
            it.beneficiaryId == beneficiaryId &&
                it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING) &&
                hasOverlap(it.startTime, it.endTime, startTime, endTime)
        }
        return Result.success(!conflicting)
    }

    override suspend fun getConflictingVisits(
        beneficiaryId: String,
        startTime: Instant,
        endTime: Instant
    ): Result<List<Visit>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            visits.values.filter {
                it.beneficiaryId == beneficiaryId &&
                    it.status in listOf(VisitStatus.APPROVED, VisitStatus.PENDING) &&
                    hasOverlap(it.startTime, it.endTime, startTime, endTime)
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

    private fun hasOverlap(
        start1: Instant,
        end1: Instant,
        start2: Instant,
        end2: Instant
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
                it.role in listOf(UserRole.PRIMARY_COORDINATOR, UserRole.SECONDARY_COORDINATOR) &&
                    beneficiaryId in it.assignedBeneficiaryIds
            }
        )
    }

    override suspend fun getVisitorsByBeneficiary(beneficiaryId: String): Result<List<User>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(
            users.values.filter {
                it.role in listOf(UserRole.APPROVED_VISITOR, UserRole.PENDING_VISITOR)
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

    override suspend fun getByBeneficiary(beneficiaryId: String): Result<List<Restriction>> {
        if (shouldFail) return Result.error(failureException)
        return Result.success(restrictions.values.filter { it.beneficiaryId == beneficiaryId })
    }

    override suspend fun getActiveByBeneficiary(beneficiaryId: String): Result<List<Restriction>> {
        if (shouldFail) return Result.error(failureException)
        val now = Clock.System.now()
        return Result.success(
            restrictions.values.filter {
                it.beneficiaryId == beneficiaryId &&
                    it.isActive &&
                    (it.expiresAt == null || it.expiresAt > now)
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
        beneficiaryId: String,
        visitorId: String?,
        timeSlot: TimeSlot
    ): Result<List<Restriction>> {
        if (shouldFail) return Result.error(failureException)
        val now = Clock.System.now()
        return Result.success(
            restrictions.values.filter { restriction ->
                restriction.beneficiaryId == beneficiaryId &&
                    restriction.isActive &&
                    (restriction.expiresAt == null || restriction.expiresAt > now) &&
                    isRestrictionApplicable(restriction, visitorId, timeSlot)
            }
        )
    }

    private fun isRestrictionApplicable(
        restriction: Restriction,
        visitorId: String?,
        timeSlot: TimeSlot
    ): Boolean {
        return when (restriction.type) {
            RestrictionType.VISITOR_BLOCKED -> {
                visitorId != null && restriction.visitorId == visitorId
            }
            RestrictionType.BLACKOUT_DATE,
            RestrictionType.MEDICAL_PROCEDURE -> {
                restriction.startTime != null && restriction.endTime != null &&
                    hasTimeOverlap(
                        restriction.startTime,
                        restriction.endTime,
                        timeSlot.startTime,
                        timeSlot.endTime
                    )
            }
            RestrictionType.MEAL_TIME,
            RestrictionType.REST_PERIOD,
            RestrictionType.BLACKOUT_HOURS -> {
                // Check recurring time restrictions
                isRecurringTimeApplicable(restriction, timeSlot)
            }
            else -> false
        }
    }

    private fun hasTimeOverlap(
        start1: Instant,
        end1: Instant,
        start2: Instant,
        end2: Instant
    ): Boolean {
        return start1 < end2 && start2 < end1
    }

    private fun isRecurringTimeApplicable(restriction: Restriction, timeSlot: TimeSlot): Boolean {
        val slotLocalTime = timeSlot.startTime
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .time

        val recurStart = restriction.recurringStartTime ?: return false
        val recurEnd = restriction.recurringEndTime ?: return false

        return slotLocalTime >= recurStart && slotLocalTime < recurEnd
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

class FakeSessionRepository : SessionRepository {
    private val sessions = mutableMapOf<String, Session>()
    private val tokenIndex = mutableMapOf<String, String>() // token -> sessionId

    var shouldFail = false
    var failureException: Throwable = Exception("Simulated failure")

    override suspend fun save(session: Session): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        sessions[session.id] = session
        tokenIndex[session.token.accessToken] = session.id
        return Result.success(Unit)
    }

    override suspend fun getByToken(token: String): Result<Session?> {
        if (shouldFail) return Result.error(failureException)
        val sessionId = tokenIndex[token]
        return Result.success(sessionId?.let { sessions[it] })
    }

    override suspend fun invalidate(sessionId: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        val session = sessions[sessionId]
        if (session != null) {
            tokenIndex.remove(session.token.accessToken)
            sessions[sessionId] = session.copy(isActive = false)
        }
        return Result.success(Unit)
    }

    override suspend fun invalidateAllForUser(userId: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        sessions.values
            .filter { it.userId == userId }
            .forEach { session ->
                tokenIndex.remove(session.token.accessToken)
                sessions[session.id] = session.copy(isActive = false)
            }
        return Result.success(Unit)
    }

    override suspend fun updateLastActivity(sessionId: String): Result<Unit> {
        if (shouldFail) return Result.error(failureException)
        val session = sessions[sessionId]
        if (session != null) {
            sessions[sessionId] = session.copy(lastActivityAt = Clock.System.now())
        }
        return Result.success(Unit)
    }

    // Test helpers
    fun addSession(session: Session) {
        sessions[session.id] = session
        tokenIndex[session.token.accessToken] = session.id
    }

    fun clear() {
        sessions.clear()
        tokenIndex.clear()
    }

    fun getSession(id: String): Session? = sessions[id]

    fun getAllSessions(): List<Session> = sessions.values.toList()
}
