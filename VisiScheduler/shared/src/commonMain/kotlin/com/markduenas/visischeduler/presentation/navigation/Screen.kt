package com.markduenas.visischeduler.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.markduenas.visischeduler.presentation.state.UiEvent
import com.markduenas.visischeduler.presentation.ui.screens.auth.ForgotPasswordScreen
import com.markduenas.visischeduler.presentation.ui.screens.auth.LoginScreen
import com.markduenas.visischeduler.presentation.ui.screens.auth.RegisterScreen
import com.markduenas.visischeduler.presentation.ui.screens.auth.SplashScreen
import com.markduenas.visischeduler.presentation.ui.screens.calendar.CalendarScreen
import com.markduenas.visischeduler.presentation.ui.screens.dashboard.DashboardScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.ChatScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.ConversationsScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.NewMessageScreen
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
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.ForgotPasswordViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.RegisterViewModel
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ChatViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ConversationsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.NewMessageViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel
import com.markduenas.visischeduler.domain.repository.UserRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

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
        override val key: ScreenKey = \"splash\"

        @Composable
        override fun Content() {
            SplashScreen()
        }
    }

    /**
     * Login screen for user authentication.
     */
    data object Login : AppScreen() {
        override val key: ScreenKey = \"login\"

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
        override val key: ScreenKey = \"register\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: RegisterViewModel = koinInject()

            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = {
                    // After successful registration, go back to login
                    navigator.pop()
                },
                onNavigateToLogin = {
                    navigator.pop()
                }
            )
        }
    }

    /**
     * Forgot password screen for password recovery.
     */
    data object ForgotPassword : AppScreen() {
        override val key: ScreenKey = \"forgot_password\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ForgotPasswordViewModel = koinInject()

            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navigator.pop()
                }
            )
        }
    }

    /**
     * Multi-factor authentication screen.
     *
     * @param challengeId The MFA challenge identifier
     */
    data class Mfa(val challengeId: String) : AppScreen() {
        override val key: ScreenKey = \"mfa_$challengeId\"

        @Composable
        override fun Content() {
            // TODO: Implement MfaScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = \"MFA Verification\",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    // ==================== Main Screens ====================

    /**
     * Main dashboard/home screen.
     */
    data object Dashboard : AppScreen() {
        override val key: ScreenKey = \"dashboard\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: DashboardViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    when (route) {
                        \"calendar\" -> navigator.push(Calendar)
                        \"pending_requests\" -> navigator.push(PendingRequests)
                        \"messages\" -> navigator.push(Messages)
                        \"profile\" -> navigator.push(Profile)
                        \"notifications\" -> navigator.push(Notifications)
                        \"settings\" -> navigator.push(Settings)
                    }
                }
            )
        }
    }

    /**
     * Calendar view screen showing scheduled visits.
     */
    data object Calendar : AppScreen() {
        override val key: ScreenKey = \"calendar\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: CalendarViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            CalendarScreen(
                viewModel = viewModel,
                onNavigateToSchedule = { date ->
                    // Navigate to schedule with a default beneficiary
                    navigator.push(ScheduleVisit(\"default\"))
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
        override val key: ScreenKey = \"pending_requests\"

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
        override val key: ScreenKey = \"messages\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ConversationsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            ConversationsScreen(
                viewModel = viewModel
            )
        }
    }

    /**
     * User profile screen.
     */
    data object Profile : AppScreen() {
        override val key: ScreenKey = \"profile\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ProfileViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            ProfileScreen(
                viewModel = viewModel,
                onNavigateToEditProfile = {
                    navigator.push(EditProfile)
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

    /**
     * Edit user profile screen.
     */
    data object EditProfile : AppScreen() {
        override val key: ScreenKey = \"edit_profile\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ProfileViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            EditProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.pop() }
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
        override val key: ScreenKey = \"schedule_visit_$beneficiaryId\"

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
        override val key: ScreenKey = \"visit_details_$visitId\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: VisitDetailsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            VisitDetailsScreen(
                visitId = visitId,
                onNavigateBack = { navigator.pop() },
                onEditVisit = { id ->
                    navigator.push(EditVisit(id))
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
        override val key: ScreenKey = \"edit_visit_$visitId\"

        @Composable
        override fun Content() {
            // TODO: Implement EditVisitScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = \"Edit Visit\",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    // ==================== Visitor Screens ====================

    /**
     * List of all visitors.
     */
    data object VisitorList : AppScreen() {
        override val key: ScreenKey = \"visitor_list\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorListViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            VisitorListScreen(
                onNavigateBack = { navigator.pop() },
                onAddVisitor = { navigator.push(AddVisitor) },
                onViewVisitor = { visitorId ->
                    navigator.push(VisitorDetails(visitorId))
                },
                viewModel = viewModel
            )
        }
    }

    /**
     * Visitor details screen.
     *
     * @param visitorId The visitor to view details for
     */
    data class VisitorDetails(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = \"visitor_details_$visitorId\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorDetailsViewModel = koinInject { parametersOf(visitorId) }

            HandleEvents(viewModel, navigator)

            VisitorDetailsScreen(
                visitorId = visitorId,
                onNavigateBack = { navigator.pop() },
                onEditVisitor = { id ->
                    navigator.push(EditVisitor(id))
                },
                viewModel = viewModel
            )
        }
    }

    /**
     * Add a new visitor screen.
     */
    data object AddVisitor : AppScreen() {
        override val key: ScreenKey = \"add_visitor\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: AddVisitorViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            AddVisitorScreen(
                onNavigateBack = { navigator.pop() },
                onVisitorAdded = { visitorId ->
                    navigator.pop()
                    navigator.push(VisitorDetails(visitorId))
                },
                viewModel = viewModel
            )
        }
    }

    /**
     * Edit an existing visitor.
     *
     * @param visitorId The visitor to edit
     */
    data class EditVisitor(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = \"edit_visitor_$visitorId\"

        @Composable
        override fun Content() {
            // TODO: Implement EditVisitorScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = \"Edit Visitor\",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    // ==================== Restriction Screens ====================

    /**
     * Visitation restrictions list screen.
     */
    data object Restrictions : AppScreen() {
        override val key: ScreenKey = \"restrictions\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.visitors.RestrictionsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            RestrictionsScreen(
                onNavigateBack = { navigator.pop() },
                onAddRestriction = { navigator.push(AddRestriction) },
                onEditRestriction = { restrictionId ->
                    navigator.push(EditRestriction(restrictionId))
                },
                viewModel = viewModel
            )
        }
    }

    /**
     * Add a new restriction screen.
     */
    data object AddRestriction : AppScreen() {
        override val key: ScreenKey = \"add_restriction\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.visitors.AddRestrictionViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            AddRestrictionScreen(
                onNavigateBack = { navigator.pop() },
                onRestrictionAdded = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    /**
     * Edit an existing restriction.
     *
     * @param restrictionId The restriction to edit
     */
    data class EditRestriction(val restrictionId: String) : AppScreen() {
        override val key: ScreenKey = \"edit_restriction_$restrictionId\"

        @Composable
        override fun Content() {
            // TODO: Implement EditRestrictionScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = \"Edit Restriction\",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    // ==================== Settings Screens ====================

    /**
     * General settings screen.
     */
    data object Settings : AppScreen() {
        override val key: ScreenKey = \"settings\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.SettingsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            SettingsScreen(
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    /**
     * Notification settings screen.
     */
    data object NotificationSettings : AppScreen() {
        override val key: ScreenKey = \"notification_settings\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.NotificationSettingsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            NotificationSettingsScreen(
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    /**
     * Beneficiary-specific visit settings.
     *
     * @param beneficiaryId Optional ID, defaults to primary if null
     */
    data class BeneficiarySettings(val beneficiaryId: String? = null) : AppScreen() {
        override val key: ScreenKey = \"beneficiary_settings_${beneficiaryId ?: \"default\"}\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.BeneficiarySettingsViewModel = koinInject { parametersOf(beneficiaryId) }

            HandleEvents(viewModel, navigator)

            BeneficiarySettingsScreen(
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    /**
     * Security settings screen (password, biometrics, etc.).
     */
    data object SecuritySettings : AppScreen() {
        override val key: ScreenKey = \"security_settings\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.SecuritySettingsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            SecuritySettingsScreen(
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    /**
     * Privacy settings screen.
     */
    data object PrivacySettings : AppScreen() {
        override val key: ScreenKey = \"privacy_settings\"

        @Composable
        override fun Content() {
            // TODO: Implement PrivacySettingsScreen
            val navigator = LocalNavigator.currentOrThrow
            PlaceholderScreen(
                title = \"Privacy Settings\",
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    /**
     * About app screen.
     */
    data object About : AppScreen() {
        override val key: ScreenKey = \"about\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.SettingsViewModel = koinInject()

            HandleEvents(viewModel, navigator)

            AboutScreen(
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
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
        override val key: ScreenKey = \"message_thread_$conversationId\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ChatViewModel = koinInject { parametersOf(conversationId) }
            val userRepository: UserRepository = koinInject()
            val currentUser by userRepository.currentUser.collectAsState(initial = null)

            HandleEvents(viewModel, navigator)

            ChatScreen(
                viewModel = viewModel,
                currentUserId = currentUser?.id ?: \"\"
            )
        }
    }

    /**
     * Compose a new message screen.
     *
     * @param recipientId Optional pre-selected recipient
     */
    data class ComposeMessage(val recipientId: String? = null) : AppScreen() {
        override val key: ScreenKey = \"compose_message_${recipientId ?: \"new\"}\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: NewMessageViewModel = koinInject { parametersOf(recipientId) }

            HandleEvents(viewModel, navigator)

            NewMessageScreen(
                viewModel = viewModel
            )
        }
    }

    // ==================== Notifications ====================

    /**
     * Notifications list screen.
     */
    data object Notifications : AppScreen() {
        override val key: ScreenKey = \"notifications\"

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
        override val key: ScreenKey = \"accept_invitation_$inviteCode\"

        @Composable
        override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.visitors.AcceptInvitationViewModel = koinInject { parametersOf(inviteCode) }

            HandleEvents(viewModel, navigator)

            AcceptInvitationScreen(
                inviteCode = inviteCode,
                onNavigateBack = { navigator.pop() },
                onInvitationAccepted = {
                    navigator.replaceAll(Dashboard)
                },
                viewModel = viewModel
            )
        }
    }
}

/**
 * Helper to handle UI events from ViewModels.
 */
@Composable
private fun HandleEvents(
    viewModel: BaseViewModel<*>,
    navigator: Navigator
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.Navigate -> {
                    val route = event.route
                    when {
                        route.startsWith(\"chat/\") -> {
                            val id = route.substringAfter(\"chat/\")
                            navigator.push(AppScreen.MessageThread(id))
                        }
                        route == \"new-message\" -> {
                            navigator.push(AppScreen.ComposeMessage())
                        }
                        route == \"calendar\" -> navigator.push(AppScreen.Calendar)
                        route == \"pending_requests\" -> navigator.push(AppScreen.PendingRequests)
                        route == \"messages\" -> navigator.push(AppScreen.Messages)
                        route == \"profile\" -> navigator.push(AppScreen.Profile)
                        route == \"notifications\" -> navigator.push(AppScreen.Notifications)
                        route == \"settings\" -> navigator.push(AppScreen.Settings)
                        route == \"notification_settings\" -> navigator.push(AppScreen.NotificationSettings)
                        route == \"security_settings\" -> navigator.push(AppScreen.SecuritySettings)
                        route == \"about\" -> navigator.push(AppScreen.About)
                        route == \"beneficiary_settings\" -> navigator.push(AppScreen.BeneficiarySettings())
                        route.startsWith(\"beneficiary_settings/\") -> {
                            val id = route.substringAfter(\"beneficiary_settings/\")
                            navigator.push(AppScreen.BeneficiarySettings(id))
                        }
                        route == \"visitor_list\" -> navigator.push(AppScreen.VisitorList)
                        route == \"restrictions\" -> navigator.push(AppScreen.Restrictions)
                        route.startsWith(\"visit/\") -> {
                            val id = route.substringAfter(\"visit/\")
                            navigator.push(AppScreen.VisitDetails(id))
                        }
                        // Handle other routes
                    }
                }
                is UiEvent.NavigateBack -> {
                    navigator.pop()
                }
                is UiEvent.ShowSnackbar -> {
                    // TODO: Connect with Snackbar host
                }
                is UiEvent.ShowToast -> {
                    // TODO: Connect with Toast manager
                }
                else -> {}
            }
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
                text = \"Coming Soon\",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateBack) {
                Text(\"Go Back\")
            }
        }
    }
}

/**
 * Route constants for deep linking and analytics.
 */
object Routes {
    // Auth
    const val SPLASH = \"splash\"
    const val LOGIN = \"login\"
    const val REGISTER = \"register\"
    const val FORGOT_PASSWORD = \"forgot_password\"
    const val MFA = \"mfa/{challengeId}\"

    // Main
    const val DASHBOARD = \"dashboard\"
    const val CALENDAR = \"calendar\"
    const val PENDING_REQUESTS = \"pending_requests\"
    const val MESSAGES = \"messages\"
    const val PROFILE = \"profile\"

    // Scheduling
    const val SCHEDULE_VISIT = \"schedule_visit/{beneficiaryId}\"
    const val VISIT_DETAILS = \"visit_details/{visitId}\"
    const val EDIT_VISIT = \"edit_visit/{visitId}\"

    // Visitors
    const val VISITOR_LIST = \"visitor_list\"
    const val VISITOR_DETAILS = \"visitor_details/{visitorId}\"
    const val ADD_VISITOR = \"add_visitor\"
    const val EDIT_VISITOR = \"edit_visitor/{visitorId}\"

    // Restrictions
    const val RESTRICTIONS = \"restrictions\"
    const val ADD_RESTRICTION = \"add_restriction\"
    const val EDIT_RESTRICTION = \"edit_restriction/{restrictionId}\"

    // Settings
    const val SETTINGS = \"settings\"
    const val NOTIFICATION_SETTINGS = \"notification_settings\"
    const val BENEFICIARY_SETTINGS = \"beneficiary_settings\"
    const val SECURITY_SETTINGS = \"security_settings\"
    const val PRIVACY_SETTINGS = \"privacy_settings\"
    const val ABOUT = \"about\"

    // Messages
    const val MESSAGE_THREAD = \"message_thread/{conversationId}\"
    const val COMPOSE_MESSAGE = \"compose_message\"

    // Notifications
    const val NOTIFICATIONS = \"notifications\"

    // Invitations
    const val ACCEPT_INVITATION = \"accept_invitation/{inviteCode}\"
}
