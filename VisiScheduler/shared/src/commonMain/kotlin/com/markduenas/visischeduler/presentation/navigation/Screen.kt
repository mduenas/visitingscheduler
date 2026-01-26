package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.presentation.ui.screens.auth.LoginScreen
import com.markduenas.visischeduler.presentation.ui.screens.auth.SplashScreen
import com.markduenas.visischeduler.presentation.ui.screens.calendar.CalendarScreen
import com.markduenas.visischeduler.presentation.ui.screens.dashboard.DashboardScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.ConversationsScreen
import com.markduenas.visischeduler.presentation.ui.screens.notifications.NotificationsListScreen
import com.markduenas.visischeduler.presentation.ui.screens.profile.ProfileScreen
import com.markduenas.visischeduler.presentation.ui.screens.restrictions.AddRestrictionScreen
import com.markduenas.visischeduler.presentation.ui.screens.restrictions.RestrictionsScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.PendingRequestsScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.ScheduleVisitScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.VisitDetailsScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.AboutScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.NotificationSettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.SecuritySettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.SettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.AddVisitorScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.VisitorDetailsScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.VisitorListScreen
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginViewModel
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ConversationsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
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
            // TODO: Implement RegisterScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Register",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    /**
     * Forgot password screen for password recovery.
     */
    data object ForgotPassword : AppScreen() {
        override val key: ScreenKey = "forgot_password"

        @Composable
        override fun Content() {
            // TODO: Implement ForgotPasswordScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Forgot Password",
                onNavigateBack = { navigator.pop() }
            )
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
            // TODO: Implement MfaScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "MFA Verification",
                onNavigateBack = { navigator.pop() }
            )
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
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: CalendarViewModel = koinInject()

            CalendarScreen(
                viewModel = viewModel,
                onNavigateToSchedule = { date ->
                    // Navigate to schedule with a default beneficiary
                    navigator.push(ScheduleVisit("default"))
                },
                onNavigateToVisitDetails = { visitId ->
                    navigator.push(VisitDetails(visitId))
                }
            )
        }
    }

    /**
     * Pending visit requests screen.
     */
    data object PendingRequests : AppScreen() {
        override val key: ScreenKey = "pending_requests"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            PendingRequestsScreen(
                onNavigateBack = { navigator.pop() },
                onViewDetails = { visitId ->
                    navigator.push(VisitDetails(visitId))
                }
            )
        }
    }

    /**
     * Messages/inbox screen.
     */
    data object Messages : AppScreen() {
        override val key: ScreenKey = "messages"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ConversationsViewModel = koinInject()

            ConversationsScreen(
                viewModel = viewModel
            )
        }
    }

    /**
     * User profile screen.
     */
    data object Profile : AppScreen() {
        override val key: ScreenKey = "profile"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ProfileViewModel = koinInject()

            ProfileScreen(
                viewModel = viewModel,
                onNavigateToEditProfile = {
                    // TODO: Navigate to edit profile
                },
                onNavigateToSettings = {
                    navigator.push(Settings)
                },
                onNavigateToNotifications = {
                    navigator.push(Notifications)
                }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            ScheduleVisitScreen(
                beneficiaryId = beneficiaryId,
                onNavigateBack = { navigator.pop() },
                onVisitScheduled = { visitId ->
                    navigator.pop()
                    navigator.push(VisitDetails(visitId))
                }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            VisitDetailsScreen(
                visitId = visitId,
                onNavigateBack = { navigator.pop() },
                onEditVisit = { id ->
                    navigator.push(EditVisit(id))
                },
                onCheckIn = { id ->
                    // TODO: Navigate to check-in
                }
            )
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
            // TODO: Implement EditVisitScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Edit Visit",
                onNavigateBack = { navigator.pop() }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            VisitorListScreen(
                onNavigateBack = { navigator.pop() },
                onAddVisitor = { navigator.push(AddVisitor) },
                onViewVisitor = { visitorId ->
                    navigator.push(VisitorDetails(visitorId))
                }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            VisitorDetailsScreen(
                visitorId = visitorId,
                onNavigateBack = { navigator.pop() },
                onEditVisitor = { id ->
                    navigator.push(EditVisitor(id))
                }
            )
        }
    }

    /**
     * Add a new visitor screen.
     */
    data object AddVisitor : AppScreen() {
        override val key: ScreenKey = "add_visitor"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            AddVisitorScreen(
                onNavigateBack = { navigator.pop() },
                onVisitorAdded = { visitorId ->
                    navigator.pop()
                    navigator.push(VisitorDetails(visitorId))
                }
            )
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
            // TODO: Implement EditVisitorScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Edit Visitor",
                onNavigateBack = { navigator.pop() }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            RestrictionsScreen(
                onNavigateBack = { navigator.pop() },
                onAddRestriction = { navigator.push(AddRestriction) },
                onEditRestriction = { restrictionId ->
                    navigator.push(EditRestriction(restrictionId))
                }
            )
        }
    }

    /**
     * Add a new restriction screen.
     */
    data object AddRestriction : AppScreen() {
        override val key: ScreenKey = "add_restriction"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            AddRestrictionScreen(
                onNavigateBack = { navigator.pop() },
                onRestrictionAdded = { navigator.pop() }
            )
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
            // TODO: Implement EditRestrictionScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Edit Restriction",
                onNavigateBack = { navigator.pop() }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            SettingsScreen(
                onNavigateBack = { navigator.pop() },
                onNavigateToProfile = { navigator.push(Profile) },
                onNavigateToNotifications = { navigator.push(NotificationSettings) },
                onNavigateToSecurity = { navigator.push(SecuritySettings) },
                onNavigateToAppearance = { /* TODO: Navigate to appearance */ },
                onNavigateToBeneficiarySettings = { /* TODO: Navigate to beneficiary settings */ },
                onNavigateToAbout = { navigator.push(About) },
                onRemoveAdsClick = { /* TODO: Handle remove ads */ },
                onLogout = { navigator.replaceAll(Login) }
            )
        }
    }

    /**
     * Notification settings screen.
     */
    data object NotificationSettings : AppScreen() {
        override val key: ScreenKey = "notification_settings"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            NotificationSettingsScreen(
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    /**
     * Security settings screen (password, biometrics, etc.).
     */
    data object SecuritySettings : AppScreen() {
        override val key: ScreenKey = "security_settings"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            SecuritySettingsScreen(
                onNavigateBack = { navigator.pop() },
                onChangePassword = {
                    // TODO: Navigate to change password screen
                }
            )
        }
    }

    /**
     * Privacy settings screen.
     */
    data object PrivacySettings : AppScreen() {
        override val key: ScreenKey = "privacy_settings"

        @Composable
        override fun Content() {
            // TODO: Implement PrivacySettingsScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Privacy Settings",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    /**
     * About app screen.
     */
    data object About : AppScreen() {
        override val key: ScreenKey = "about"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow

            AboutScreen(
                onNavigateBack = { navigator.pop() }
            )
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
            // TODO: Implement ChatScreen with proper ViewModel
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Messages",
                onNavigateBack = { navigator.pop() }
            )
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
            // TODO: Implement NewMessageScreen with proper ViewModel
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "New Message",
                onNavigateBack = { navigator.pop() }
            )
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
            val navigator = LocalNavigator.currentOrThrow

            NotificationsListScreen(
                onNavigateBack = { navigator.pop() },
                onNotificationClick = { notification ->
                    // Handle notification click based on type
                    // TODO: Navigate to relevant screen based on notification type
                }
            )
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
            // TODO: Implement AcceptInvitationScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = "Accept Invitation",
                onNavigateBack = { navigator.pop() }
            )
        }
    }
}

/**
 * Placeholder screen for screens that haven't been implemented yet.
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Coming Soon",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateBack) {
                Text("Go Back")
            }
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
