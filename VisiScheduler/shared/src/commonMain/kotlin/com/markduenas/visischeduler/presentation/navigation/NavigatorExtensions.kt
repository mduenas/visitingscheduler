package com.markduenas.visischeduler.presentation.navigation

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Extension functions for Voyager Navigator providing common navigation patterns.
 */

// ==================== Stack Inspection ====================

/**
 * Get the current stack depth.
 */
val Navigator.stackDepth: Int
    get() = items.size

/**
 * Check if a specific screen type exists in the stack.
 */
inline fun <reified T : Screen> Navigator.hasScreenInStack(): Boolean =
    items.any { it is T }

/**
 * Find a screen of a specific type in the stack.
 */
inline fun <reified T : Screen> Navigator.findScreen(): T? =
    items.filterIsInstance<T>().lastOrNull()

/**
 * Get all screens of a specific type in the stack.
 */
inline fun <reified T : Screen> Navigator.findAllScreens(): List<T> =
    items.filterIsInstance<T>()

/**
 * Check if the current screen is of a specific type.
 */
inline fun <reified T : Screen> Navigator.isCurrentScreen(): Boolean =
    lastItem is T

// ==================== Navigation with Results ====================

/**
 * Result key for navigation results.
 */
typealias NavigationResultKey = String

/**
 * Navigation result holder for passing data back between screens.
 */
object NavigationResultManager {
    private val _results = MutableSharedFlow<Pair<NavigationResultKey, Any?>>(extraBufferCapacity = 10)
    val results: SharedFlow<Pair<NavigationResultKey, Any?>> = _results.asSharedFlow()

    private val pendingResults = mutableMapOf<NavigationResultKey, Any?>()

    /**
     * Set a result for a specific key.
     */
    fun setResult(key: NavigationResultKey, result: Any?) {
        pendingResults[key] = result
        _results.tryEmit(key to result)
    }

    /**
     * Get and consume a pending result.
     */
    fun consumeResult(key: NavigationResultKey): Any? {
        return pendingResults.remove(key)
    }

    /**
     * Check if a result is pending.
     */
    fun hasResult(key: NavigationResultKey): Boolean =
        pendingResults.containsKey(key)

    /**
     * Clear all pending results.
     */
    fun clearAll() {
        pendingResults.clear()
    }
}

/**
 * Navigate to a screen and expect a result.
 */
fun Navigator.pushForResult(
    screen: AppScreen,
    resultKey: NavigationResultKey
) {
    push(screen)
}

/**
 * Pop and set a result.
 */
fun Navigator.popWithResult(
    resultKey: NavigationResultKey,
    result: Any?
) {
    NavigationResultManager.setResult(resultKey, result)
    pop()
}

// ==================== Conditional Navigation ====================

/**
 * Push a screen only if it's not already at the top of the stack.
 */
fun Navigator.pushIfNotCurrent(screen: AppScreen) {
    if (lastItem != screen) {
        push(screen)
    }
}

/**
 * Replace the current screen only if it's of a different type.
 */
inline fun <reified T : Screen> Navigator.replaceIfNotType(screen: AppScreen) {
    if (lastItem !is T) {
        replace(screen)
    }
}

/**
 * Push a screen only if it doesn't exist in the stack.
 */
fun Navigator.pushIfNotInStack(screen: AppScreen) {
    if (!items.contains(screen)) {
        push(screen)
    }
}

// ==================== Pop Variations ====================

/**
 * Pop multiple screens at once.
 */
fun Navigator.popMultiple(count: Int) {
    repeat(minOf(count, items.size - 1)) {
        pop()
    }
}

/**
 * Pop to a screen matching the predicate.
 */
fun Navigator.popUntilScreen(predicate: (Screen) -> Boolean) {
    while (items.size > 1 && !predicate(lastItem)) {
        pop()
    }
}

/**
 * Pop all screens above a specific screen type.
 */
inline fun <reified T : Screen> Navigator.popToScreenType() {
    popUntilScreen { it is T }
}

// ==================== Replace Variations ====================

/**
 * Replace all screens with a list of screens.
 */
fun Navigator.replaceAllWith(screens: List<AppScreen>) {
    if (screens.isNotEmpty()) {
        replaceAll(screens.first())
        screens.drop(1).forEach { push(it) }
    }
}

/**
 * Replace the current screen and all screens above it with a new screen.
 */
fun Navigator.replaceFromRoot(screen: AppScreen) {
    replaceAll(screen)
}

// ==================== History Management ====================

/**
 * Get the navigation history as a list of screen keys.
 */
val Navigator.history: List<String>
    get() = items.map { it.key.toString() }

/**
 * Check if we can go back a specific number of steps.
 */
fun Navigator.canGoBack(steps: Int): Boolean =
    items.size > steps

/**
 * Go back a specific number of steps if possible.
 */
fun Navigator.goBack(steps: Int = 1): Boolean {
    return if (canGoBack(steps)) {
        popMultiple(steps)
        true
    } else {
        false
    }
}

// ==================== Screen Lifecycle Helpers ====================

/**
 * Execute an action only if on a specific screen.
 */
inline fun <reified T : AppScreen> Navigator.onScreen(action: (T) -> Unit) {
    (lastItem as? T)?.let(action)
}

/**
 * Get the parent screen (previous screen in stack).
 */
fun Navigator.getParentScreen(): Screen? =
    items.getOrNull(items.size - 2)

/**
 * Check if the previous screen is of a specific type.
 */
inline fun <reified T : Screen> Navigator.isPreviousScreen(): Boolean =
    getParentScreen() is T

// ==================== Debug Helpers ====================

/**
 * Print the current navigation stack for debugging.
 */
fun Navigator.printStack(): String {
    return buildString {
        appendLine("Navigation Stack (${items.size} screens):")
        items.forEachIndexed { index, screen ->
            val marker = if (index == items.lastIndex) "-> " else "   "
            appendLine("$marker[$index] ${screen.key}")
        }
    }
}

/**
 * Get a snapshot of the current stack state.
 */
fun Navigator.getStackSnapshot(): NavigationStackSnapshot {
    return NavigationStackSnapshot(
        screens = items.map { it.key.toString() },
        currentScreen = lastItem.key.toString(),
        depth = items.size
    )
}

/**
 * Snapshot of the navigation stack.
 */
data class NavigationStackSnapshot(
    val screens: List<String>,
    val currentScreen: String,
    val depth: Int
) {
    override fun toString(): String = buildString {
        appendLine("NavigationStackSnapshot:")
        appendLine("  Current: $currentScreen")
        appendLine("  Depth: $depth")
        appendLine("  Stack: ${screens.joinToString(" -> ")}")
    }
}

// ==================== Safe Navigation ====================

/**
 * Safely execute navigation, catching any exceptions.
 */
inline fun Navigator.safeNavigate(action: Navigator.() -> Unit): Boolean {
    return try {
        action()
        true
    } catch (e: Exception) {
        println("Navigation error: ${e.message}")
        false
    }
}

/**
 * Navigate with a delay (useful for animations).
 */
suspend fun Navigator.navigateWithDelay(
    screen: AppScreen,
    delayMs: Long = 300
) {
    kotlinx.coroutines.delay(delayMs)
    push(screen)
}
