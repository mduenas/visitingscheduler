package com.markduenas.visischeduler.presentation.navigation

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey

/**
 * Sealed class hierarchy defining all screens in VisiScheduler.
 * Each screen has a unique key for navigation state management.
 */
sealed class AppScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    // ==================== Auth Screens ====================

    /**
     * Splash screen shown on app launch.
     */
    data object Splash : AppScreen() {
        override val key: ScreenKey = "splash"
    }

    /**
     * Login screen for user authentication.
     */
    data object Login : AppScreen() {
        override val key: ScreenKey = "login"
    }

    /**
     * Registration screen for new users.
     */
    data object Register : AppScreen() {
        override val key: ScreenKey = "register"
    }

    /**
     * Forgot password screen for password recovery.
     */
    data object ForgotPassword : AppScreen() {
        override val key: ScreenKey = "forgot_password"
    }

    /**
     * Multi-factor authentication screen.
     *
     * @param challengeId The MFA challenge identifier
     */
    data class Mfa(val challengeId: String) : AppScreen() {
        override val key: ScreenKey = "mfa_$challengeId"
    }

    // ==================== Main Screens ====================

    /**
     * Main dashboard/home screen.
     */
    data object Dashboard : AppScreen() {
        override val key: ScreenKey = "dashboard"
    }

    /**
     * Calendar view screen showing scheduled visits.
     */
    data object Calendar : AppScreen() {
        override val key: ScreenKey = "calendar"
    }

    /**
     * Pending visit requests screen.
     */
    data object PendingRequests : AppScreen() {
        override val key: ScreenKey = "pending_requests"
    }

    /**
     * Messages/inbox screen.
     */
    data object Messages : AppScreen() {
        override val key: ScreenKey = "messages"
    }

    /**
     * User profile screen.
     */
    data object Profile : AppScreen() {
        override val key: ScreenKey = "profile"
    }

    // ==================== Scheduling Screens ====================

    /**
     * Schedule a new visit screen.
     *
     * @param beneficiaryId The beneficiary to schedule a visit for
     */
    data class ScheduleVisit(val beneficiaryId: String) : AppScreen() {
        override val key: ScreenKey = "schedule_visit_$beneficiaryId"
    }

    /**
     * Visit details screen.
     *
     * @param visitId The visit to view details for
     */
    data class VisitDetails(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "visit_details_$visitId"
    }

    /**
     * Edit an existing visit.
     *
     * @param visitId The visit to edit
     */
    data class EditVisit(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "edit_visit_$visitId"
    }

    // ==================== Visitor Screens ====================

    /**
     * List of all visitors.
     */
    data object VisitorList : AppScreen() {
        override val key: ScreenKey = "visitor_list"
    }

    /**
     * Visitor details screen.
     *
     * @param visitorId The visitor to view details for
     */
    data class VisitorDetails(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = "visitor_details_$visitorId"
    }

    /**
     * Add a new visitor screen.
     */
    data object AddVisitor : AppScreen() {
        override val key: ScreenKey = "add_visitor"
    }

    /**
     * Edit an existing visitor.
     *
     * @param visitorId The visitor to edit
     */
    data class EditVisitor(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = "edit_visitor_$visitorId"
    }

    // ==================== Restriction Screens ====================

    /**
     * Visitation restrictions list screen.
     */
    data object Restrictions : AppScreen() {
        override val key: ScreenKey = "restrictions"
    }

    /**
     * Add a new restriction screen.
     */
    data object AddRestriction : AppScreen() {
        override val key: ScreenKey = "add_restriction"
    }

    /**
     * Edit an existing restriction.
     *
     * @param restrictionId The restriction to edit
     */
    data class EditRestriction(val restrictionId: String) : AppScreen() {
        override val key: ScreenKey = "edit_restriction_$restrictionId"
    }

    // ==================== Settings Screens ====================

    /**
     * General settings screen.
     */
    data object Settings : AppScreen() {
        override val key: ScreenKey = "settings"
    }

    /**
     * Notification settings screen.
     */
    data object NotificationSettings : AppScreen() {
        override val key: ScreenKey = "notification_settings"
    }

    /**
     * Security settings screen (password, biometrics, etc.).
     */
    data object SecuritySettings : AppScreen() {
        override val key: ScreenKey = "security_settings"
    }

    /**
     * Privacy settings screen.
     */
    data object PrivacySettings : AppScreen() {
        override val key: ScreenKey = "privacy_settings"
    }

    /**
     * About app screen.
     */
    data object About : AppScreen() {
        override val key: ScreenKey = "about"
    }

    // ==================== Message Detail Screens ====================

    /**
     * Message thread/conversation details.
     *
     * @param conversationId The conversation to view
     */
    data class MessageThread(val conversationId: String) : AppScreen() {
        override val key: ScreenKey = "message_thread_$conversationId"
    }

    /**
     * Compose a new message screen.
     *
     * @param recipientId Optional pre-selected recipient
     */
    data class ComposeMessage(val recipientId: String? = null) : AppScreen() {
        override val key: ScreenKey = "compose_message_${recipientId ?: "new"}"
    }

    // ==================== Notifications ====================

    /**
     * Notifications list screen.
     */
    data object Notifications : AppScreen() {
        override val key: ScreenKey = "notifications"
    }

    // ==================== Invitation Screens ====================

    /**
     * Accept an invitation via deep link.
     *
     * @param inviteCode The invitation code
     */
    data class AcceptInvitation(val inviteCode: String) : AppScreen() {
        override val key: ScreenKey = "accept_invitation_$inviteCode"
    }
}

/**
 * Route constants for deep linking and analytics.
 */
object Routes {
    // Auth
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MFA = "mfa/{challengeId}"

    // Main
    const val DASHBOARD = "dashboard"
    const val CALENDAR = "calendar"
    const val PENDING_REQUESTS = "pending_requests"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"

    // Scheduling
    const val SCHEDULE_VISIT = "schedule_visit/{beneficiaryId}"
    const val VISIT_DETAILS = "visit_details/{visitId}"
    const val EDIT_VISIT = "edit_visit/{visitId}"

    // Visitors
    const val VISITOR_LIST = "visitor_list"
    const val VISITOR_DETAILS = "visitor_details/{visitorId}"
    const val ADD_VISITOR = "add_visitor"
    const val EDIT_VISITOR = "edit_visitor/{visitorId}"

    // Restrictions
    const val RESTRICTIONS = "restrictions"
    const val ADD_RESTRICTION = "add_restriction"
    const val EDIT_RESTRICTION = "edit_restriction/{restrictionId}"

    // Settings
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val SECURITY_SETTINGS = "security_settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val ABOUT = "about"

    // Messages
    const val MESSAGE_THREAD = "message_thread/{conversationId}"
    const val COMPOSE_MESSAGE = "compose_message"

    // Notifications
    const val NOTIFICATIONS = "notifications"

    // Invitations
    const val ACCEPT_INVITATION = "accept_invitation/{inviteCode}"
}
