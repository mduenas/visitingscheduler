package com.markduenas.visischeduler.presentation.navigation

import cafe.adriel.voyager.navigator.Navigator

/**
 * Navigation actions providing type-safe navigation throughout the app.
 * These are extension functions on Navigator for convenient access.
 */

// ==================== Auth Navigation ====================

/**
 * Navigate to the login screen, clearing the back stack.
 */
fun Navigator.navigateToLogin() {
    replaceAll(AppScreen.Login)
}

/**
 * Navigate to registration screen.
 */
fun Navigator.navigateToRegister() {
    push(AppScreen.Register)
}

/**
 * Navigate to forgot password screen.
 */
fun Navigator.navigateToForgotPassword() {
    push(AppScreen.ForgotPassword)
}

/**
 * Navigate to MFA screen.
 *
 * @param challengeId The MFA challenge identifier
 */
fun Navigator.navigateToMfa(challengeId: String) {
    push(AppScreen.Mfa(challengeId))
}

/**
 * Navigate to dashboard after successful authentication.
 * Clears the entire auth stack.
 */
fun Navigator.navigateToDashboardFromAuth() {
    replaceAll(AppScreen.Dashboard)
}

// ==================== Main Navigation ====================

/**
 * Navigate to the dashboard screen.
 */
fun Navigator.navigateToDashboard() {
    push(AppScreen.Dashboard)
}

/**
 * Navigate to the calendar screen.
 */
fun Navigator.navigateToCalendar() {
    push(AppScreen.Calendar)
}

/**
 * Navigate to pending requests screen.
 */
fun Navigator.navigateToPendingRequests() {
    push(AppScreen.PendingRequests)
}

/**
 * Navigate to messages screen.
 */
fun Navigator.navigateToMessages() {
    push(AppScreen.Messages)
}

/**
 * Navigate to profile screen.
 */
fun Navigator.navigateToProfile() {
    push(AppScreen.Profile)
}

// ==================== Visit Navigation ====================

/**
 * Navigate to schedule visit screen.
 *
 * @param beneficiaryId The beneficiary to schedule a visit for
 */
fun Navigator.navigateToScheduleVisit(beneficiaryId: String) {
    push(AppScreen.ScheduleVisit(beneficiaryId))
}

/**
 * Navigate to visit details screen.
 *
 * @param visitId The visit to view
 */
fun Navigator.navigateToVisitDetails(visitId: String) {
    push(AppScreen.VisitDetails(visitId))
}

/**
 * Navigate to edit visit screen.
 *
 * @param visitId The visit to edit
 */
fun Navigator.navigateToEditVisit(visitId: String) {
    push(AppScreen.EditVisit(visitId))
}

// ==================== Visitor Navigation ====================

/**
 * Navigate to visitor list screen.
 */
fun Navigator.navigateToVisitorList() {
    push(AppScreen.VisitorList)
}

/**
 * Navigate to visitor details screen.
 *
 * @param visitorId The visitor to view
 */
fun Navigator.navigateToVisitorDetails(visitorId: String) {
    push(AppScreen.VisitorDetails(visitorId))
}

/**
 * Navigate to add visitor screen.
 */
fun Navigator.navigateToAddVisitor() {
    push(AppScreen.AddVisitor)
}

/**
 * Navigate to edit visitor screen.
 *
 * @param visitorId The visitor to edit
 */
fun Navigator.navigateToEditVisitor(visitorId: String) {
    push(AppScreen.EditVisitor(visitorId))
}

// ==================== Restriction Navigation ====================

/**
 * Navigate to restrictions list screen.
 */
fun Navigator.navigateToRestrictions() {
    push(AppScreen.Restrictions)
}

/**
 * Navigate to add restriction screen.
 */
fun Navigator.navigateToAddRestriction() {
    push(AppScreen.AddRestriction)
}

/**
 * Navigate to edit restriction screen.
 *
 * @param restrictionId The restriction to edit
 */
fun Navigator.navigateToEditRestriction(restrictionId: String) {
    push(AppScreen.EditRestriction(restrictionId))
}

// ==================== Settings Navigation ====================

/**
 * Navigate to settings screen.
 */
fun Navigator.navigateToSettings() {
    push(AppScreen.Settings)
}

/**
 * Navigate to notification settings screen.
 */
fun Navigator.navigateToNotificationSettings() {
    push(AppScreen.NotificationSettings)
}

/**
 * Navigate to security settings screen.
 */
fun Navigator.navigateToSecuritySettings() {
    push(AppScreen.SecuritySettings)
}

/**
 * Navigate to privacy settings screen.
 */
fun Navigator.navigateToPrivacySettings() {
    push(AppScreen.PrivacySettings)
}

/**
 * Navigate to about screen.
 */
fun Navigator.navigateToAbout() {
    push(AppScreen.About)
}

// ==================== Message Navigation ====================

/**
 * Navigate to a message thread.
 *
 * @param conversationId The conversation to view
 */
fun Navigator.navigateToMessageThread(conversationId: String) {
    push(AppScreen.MessageThread(conversationId))
}

/**
 * Navigate to compose message screen.
 *
 * @param recipientId Optional pre-selected recipient
 */
fun Navigator.navigateToComposeMessage(recipientId: String? = null) {
    push(AppScreen.ComposeMessage(recipientId))
}

// ==================== Notification Navigation ====================

/**
 * Navigate to notifications screen.
 */
fun Navigator.navigateToNotifications() {
    push(AppScreen.Notifications)
}

// ==================== Invitation Navigation ====================

/**
 * Navigate to accept invitation screen.
 *
 * @param inviteCode The invitation code
 */
fun Navigator.navigateToAcceptInvitation(inviteCode: String) {
    push(AppScreen.AcceptInvitation(inviteCode))
}

// ==================== Stack Management ====================

/**
 * Pop to the root screen (first screen in stack).
 */
fun Navigator.popToRoot() {
    popUntilRoot()
}

/**
 * Pop all screens and replace with a new screen.
 *
 * @param screen The screen to navigate to
 */
fun Navigator.clearAndNavigate(screen: AppScreen) {
    replaceAll(screen)
}

/**
 * Pop to a specific screen type in the stack.
 * If the screen is not found, does nothing.
 *
 * @param predicate Predicate to match the target screen
 */
inline fun Navigator.popTo(crossinline predicate: (AppScreen) -> Boolean) {
    val targetIndex = items.indexOfLast { it is AppScreen && predicate(it as AppScreen) }
    if (targetIndex >= 0) {
        val itemsToPop = items.size - targetIndex - 1
        repeat(itemsToPop) { pop() }
    }
}

/**
 * Check if we can navigate back.
 */
val Navigator.canGoBack: Boolean
    get() = items.size > 1

/**
 * Get the current screen if it's an AppScreen.
 */
val Navigator.currentAppScreen: AppScreen?
    get() = lastItem as? AppScreen

/**
 * Get the previous screen if it exists and is an AppScreen.
 */
val Navigator.previousAppScreen: AppScreen?
    get() = items.getOrNull(items.size - 2) as? AppScreen

/**
 * Navigate up with optional result data.
 * Uses Voyager's built-in navigation result mechanism.
 */
fun Navigator.navigateUp() {
    if (canGoBack) {
        pop()
    }
}

/**
 * Navigate back to a specific screen, popping all screens above it.
 *
 * @param screen The target screen
 */
fun Navigator.navigateBackTo(screen: AppScreen) {
    popUntil { it == screen }
}
