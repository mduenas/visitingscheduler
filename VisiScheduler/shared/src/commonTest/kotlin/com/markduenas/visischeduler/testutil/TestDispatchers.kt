package com.markduenas.visischeduler.testutil

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Coroutine test utilities for VisiScheduler testing.
 */

/**
 * Interface for coroutine dispatchers to enable testing.
 */
interface AppDispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

/**
 * Production dispatcher implementation using actual Dispatchers.
 */
object ProductionDispatchers : AppDispatchers {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

/**
 * Test dispatcher implementation that uses TestDispatcher for all dispatchers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestAppDispatchers(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : AppDispatchers {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}

/**
 * Test rule/helper for setting up coroutine tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestCoroutineRule {
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
    val testScope: TestScope = TestScope(testDispatcher)
    val dispatchers: TestAppDispatchers = TestAppDispatchers(testDispatcher)

    fun before() {
        Dispatchers.setMain(testDispatcher)
    }

    fun after() {
        Dispatchers.resetMain()
    }

    /**
     * Run a test with proper setup and teardown.
     */
    fun runBlockingTest(block: suspend TestScope.() -> Unit) {
        before()
        try {
            kotlinx.coroutines.test.runTest {
                block()
            }
        } finally {
            after()
        }
    }
}

/**
 * Controllable clock for testing time-dependent code.
 */
class TestClock(
    private var currentInstant: Instant = Clock.System.now()
) : Clock {

    override fun now(): Instant = currentInstant

    /**
     * Advance the clock by the specified duration.
     */
    fun advanceBy(duration: kotlin.time.Duration) {
        currentInstant = currentInstant.plus(duration)
    }

    /**
     * Set the clock to a specific instant.
     */
    fun setInstant(instant: Instant) {
        currentInstant = instant
    }

    /**
     * Set the clock to a specific date and time.
     */
    fun setDateTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ) {
        val localDateTime = LocalDateTime(year, month, day, hour, minute, second)
        currentInstant = localDateTime.toInstant(timeZone)
    }

    /**
     * Reset to current system time.
     */
    fun reset() {
        currentInstant = Clock.System.now()
    }

    companion object {
        /**
         * Create a clock fixed at a specific instant.
         */
        fun fixed(instant: Instant): TestClock = TestClock(instant)

        /**
         * Create a clock fixed at a specific date/time.
         */
        fun fixed(
            year: Int,
            month: Int,
            day: Int,
            hour: Int = 0,
            minute: Int = 0,
            second: Int = 0,
            timeZone: TimeZone = TimeZone.currentSystemDefault()
        ): TestClock {
            val localDateTime = LocalDateTime(year, month, day, hour, minute, second)
            return TestClock(localDateTime.toInstant(timeZone))
        }
    }
}

/**
 * Extension to run test with a TestScope.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runTestWithDispatchers(
    dispatchers: TestAppDispatchers = TestAppDispatchers(),
    block: suspend TestScope.() -> Unit
) {
    Dispatchers.setMain(dispatchers.main)
    try {
        kotlinx.coroutines.test.runTest {
            block()
        }
    } finally {
        Dispatchers.resetMain()
    }
}

/**
 * Extension for immediate dispatcher execution in tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runUnconfinedTest(block: suspend TestScope.() -> Unit) {
    val dispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(dispatcher)
    try {
        kotlinx.coroutines.test.runTest {
            block()
        }
    } finally {
        Dispatchers.resetMain()
    }
}

/**
 * Utility object for creating test time values.
 */
object TestTimeUtils {
    private val defaultTimeZone = TimeZone.currentSystemDefault()

    /**
     * Create an Instant for a specific date at midnight.
     */
    fun dateAt(
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone = defaultTimeZone
    ): Instant {
        val localDate = LocalDate(year, month, day)
        return localDate.atStartOfDayIn(timeZone)
    }

    /**
     * Create an Instant for a specific date and time.
     */
    fun dateTimeAt(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
        second: Int = 0,
        timeZone: TimeZone = defaultTimeZone
    ): Instant {
        val localDateTime = LocalDateTime(year, month, day, hour, minute, second)
        return localDateTime.toInstant(timeZone)
    }

    /**
     * Get today's date at a specific time.
     */
    fun todayAt(
        hour: Int,
        minute: Int = 0,
        second: Int = 0,
        timeZone: TimeZone = defaultTimeZone
    ): Instant {
        val today = Clock.System.now().toLocalDateTime(timeZone).date
        val localDateTime = LocalDateTime(today, LocalTime(hour, minute, second))
        return localDateTime.toInstant(timeZone)
    }

    /**
     * Get tomorrow's date at a specific time.
     */
    fun tomorrowAt(
        hour: Int,
        minute: Int = 0,
        second: Int = 0,
        timeZone: TimeZone = defaultTimeZone
    ): Instant {
        val tomorrow = Clock.System.now()
            .toLocalDateTime(timeZone)
            .date
            .plus(DatePeriod(days = 1))
        val localDateTime = LocalDateTime(tomorrow, LocalTime(hour, minute, second))
        return localDateTime.toInstant(timeZone)
    }

    /**
     * Get a date N days from now at a specific time.
     */
    fun daysFromNowAt(
        days: Int,
        hour: Int,
        minute: Int = 0,
        second: Int = 0,
        timeZone: TimeZone = defaultTimeZone
    ): Instant {
        val targetDate = Clock.System.now()
            .toLocalDateTime(timeZone)
            .date
            .plus(DatePeriod(days = days))
        val localDateTime = LocalDateTime(targetDate, LocalTime(hour, minute, second))
        return localDateTime.toInstant(timeZone)
    }
}

/**
 * Assertion helpers for async operations.
 */
object AsyncAssertions {
    /**
     * Wait for a condition to be true within a timeout.
     */
    suspend fun waitUntil(
        timeoutMs: Long = 5000,
        intervalMs: Long = 100,
        condition: () -> Boolean
    ) {
        val startTime = Clock.System.now().toEpochMilliseconds()
        while (!condition()) {
            val elapsed = Clock.System.now().toEpochMilliseconds() - startTime
            if (elapsed > timeoutMs) {
                throw AssertionError("Condition not met within ${timeoutMs}ms timeout")
            }
            kotlinx.coroutines.delay(intervalMs)
        }
    }

    /**
     * Assert that an operation completes within a timeout.
     */
    suspend fun <T> assertCompletesWithin(
        timeoutMs: Long = 5000,
        block: suspend () -> T
    ): T {
        return kotlinx.coroutines.withTimeout(timeoutMs) {
            block()
        }
    }
}
