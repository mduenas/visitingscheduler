package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.presentation.ui.screens.auth.LoginScreen
import com.markduenas.visischeduler.presentation.ui.screens.auth.SplashScreen
import com.markduenas.visischeduler.presentation.ui.screens.dashboard.DashboardScreen
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginViewModel
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import org.koin.compose.koinInject

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

        @Composable
        override fun Content() {
            SplashScreen()
        }
    }

    /**
     * Login screen for user authentication.
     */
    data object Login : AppScreen() {
        override val key: ScreenKey = "login"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: LoginViewModel = koinInject()

            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navigator.replaceAll(Dashboard)
                },
                onNavigateToRegister = {
                    navigator.push(Register)
                },
                onNavigateToForgotPassword = {
                    navigator.push(ForgotPassword)
                }
            )
        }
    }

    /**
     * Registration screen for new users.
     */
    data object Register : AppScreen() {
        override val key: ScreenKey = "register"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Forgot password screen for password recovery.
     */
    data object ForgotPassword : AppScreen() {
        override val key: ScreenKey = "forgot_password"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Multi-factor authentication screen.
     *
     * @param challengeId The MFA challenge identifier
     */
    data class Mfa(val challengeId: String) : AppScreen() {
        override val key: ScreenKey = "mfa_$challengeId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Main Screens ====================

    /**
     * Main dashboard/home screen.
     */
    data object Dashboard : AppScreen() {
        override val key: ScreenKey = "dashboard"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: DashboardViewModel = koinInject()

            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    // Handle navigation based on route
                    when (route) {
                        "calendar" -> navigator.push(Calendar)
                        "pending_requests" -> navigator.push(PendingRequests)
                        "messages" -> navigator.push(Messages)
                        "profile" -> navigator.push(Profile)
                        "notifications" -> navigator.push(Notifications)
                        "settings" -> navigator.push(Settings)
                    }
                }
            )
        }
    }

    /**
     * Calendar view screen showing scheduled visits.
     */
    data object Calendar : AppScreen() {
        override val key: ScreenKey = "calendar"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Pending visit requests screen.
     */
    data object PendingRequests : AppScreen() {
        override val key: ScreenKey = "pending_requests"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Messages/inbox screen.
     */
    data object Messages : AppScreen() {
        override val key: ScreenKey = "messages"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * User profile screen.
     */
    data object Profile : AppScreen() {
        override val key: ScreenKey = "profile"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Scheduling Screens ====================

    /**
     * Schedule a new visit screen.
     *
     * @param beneficiaryId The beneficiary to schedule a visit for
     */
    data class ScheduleVisit(val beneficiaryId: String) : AppScreen() {
        override val key: ScreenKey = "schedule_visit_$beneficiaryId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Visit details screen.
     *
     * @param visitId The visit to view details for
     */
    data class VisitDetails(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "visit_details_$visitId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Edit an existing visit.
     *
     * @param visitId The visit to edit
     */
    data class EditVisit(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "edit_visit_$visitId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Visitor Screens ====================

    /**
     * List of all visitors.
     */
    data object VisitorList : AppScreen() {
        override val key: ScreenKey = "visitor_list"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Visitor details screen.
     *
     * @param visitorId The visitor to view details for
     */
    data class VisitorDetails(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = "visitor_details_$visitorId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Add a new visitor screen.
     */
    data object AddVisitor : AppScreen() {
        override val key: ScreenKey = "add_visitor"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Edit an existing visitor.
     *
     * @param visitorId The visitor to edit
     */
    data class EditVisitor(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = "edit_visitor_$visitorId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Restriction Screens ====================

    /**
     * Visitation restrictions list screen.
     */
    data object Restrictions : AppScreen() {
        override val key: ScreenKey = "restrictions"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Add a new restriction screen.
     */
    data object AddRestriction : AppScreen() {
        override val key: ScreenKey = "add_restriction"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Edit an existing restriction.
     *
     * @param restrictionId The restriction to edit
     */
    data class EditRestriction(val restrictionId: String) : AppScreen() {
        override val key: ScreenKey = "edit_restriction_$restrictionId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Settings Screens ====================

    /**
     * General settings screen.
     */
    data object Settings : AppScreen() {
        override val key: ScreenKey = "settings"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Notification settings screen.
     */
    data object NotificationSettings : AppScreen() {
        override val key: ScreenKey = "notification_settings"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Security settings screen (password, biometrics, etc.).
     */
    data object SecuritySettings : AppScreen() {
        override val key: ScreenKey = "security_settings"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Privacy settings screen.
     */
    data object PrivacySettings : AppScreen() {
        override val key: ScreenKey = "privacy_settings"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * About app screen.
     */
    data object About : AppScreen() {
        override val key: ScreenKey = "about"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Message Detail Screens ====================

    /**
     * Message thread/conversation details.
     *
     * @param conversationId The conversation to view
     */
    data class MessageThread(val conversationId: String) : AppScreen() {
        override val key: ScreenKey = "message_thread_$conversationId"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    /**
     * Compose a new message screen.
     *
     * @param recipientId Optional pre-selected recipient
     */
    data class ComposeMessage(val recipientId: String? = null) : AppScreen() {
        override val key: ScreenKey = "compose_message_${recipientId ?: "new"}"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Notifications ====================

    /**
     * Notifications list screen.
     */
    data object Notifications : AppScreen() {
        override val key: ScreenKey = "notifications"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
    }

    // ==================== Invitation Screens ====================

    /**
     * Accept an invitation via deep link.
     *
     * @param inviteCode The invitation code
     */
    data class AcceptInvitation(val inviteCode: String) : AppScreen() {
        override val key: ScreenKey = "accept_invitation_$inviteCode"

        @Composable
        override fun Content() {
            // Content handled by navigator
        }
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
