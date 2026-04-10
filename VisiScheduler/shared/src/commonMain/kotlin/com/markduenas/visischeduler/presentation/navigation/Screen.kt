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
import com.markduenas.visischeduler.presentation.ui.screens.auth.MfaScreen
import com.markduenas.visischeduler.presentation.ui.screens.calendar.CalendarScreen
import com.markduenas.visischeduler.presentation.ui.screens.dashboard.DashboardScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.ChatScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.ConversationsScreen
import com.markduenas.visischeduler.presentation.ui.screens.messaging.NewMessageScreen
import com.markduenas.visischeduler.presentation.ui.screens.notifications.NotificationsListScreen
import com.markduenas.visischeduler.presentation.ui.screens.profile.ChangePasswordScreen
import com.markduenas.visischeduler.presentation.ui.screens.profile.ProfileScreen
import com.markduenas.visischeduler.presentation.ui.screens.profile.EditProfileScreen
import com.markduenas.visischeduler.presentation.ui.screens.restrictions.AddRestrictionScreen
import com.markduenas.visischeduler.presentation.ui.screens.restrictions.RestrictionsScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.PendingRequestsScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.ScheduleVisitScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.EditVisitScreen
import com.markduenas.visischeduler.presentation.ui.screens.scheduling.VisitDetailsScreen
import com.markduenas.visischeduler.presentation.ui.screens.debug.ScreenshotDemoContent
import com.markduenas.visischeduler.presentation.ui.screens.debug.ScreenshotHelperScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.AboutScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.NotificationSettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.SecuritySettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.SettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.settings.BeneficiarySettingsScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.AddVisitorScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.EditVisitorScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.VisitorDetailsScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.VisitorListScreen
import com.markduenas.visischeduler.presentation.ui.screens.visitors.AcceptInvitationScreen
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.ForgotPasswordViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.RegisterViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaViewModel
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ChatViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ConversationsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.NewMessageViewModel
import com.markduenas.visischeduler.presentation.viewmodel.notifications.NotificationsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.EditVisitViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.ScheduleVisitViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDetailsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.SettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.NotificationSettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.SecuritySettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.BeneficiarySettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.EditVisitorViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorListViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorDetailsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AddVisitorViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.RestrictionsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AddRestrictionViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.AcceptInvitationViewModel
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.presentation.ui.screens.analytics.AnalyticsScreen
import com.markduenas.visischeduler.presentation.ui.screens.checkin.CheckInScreen
import com.markduenas.visischeduler.presentation.ui.screens.checkin.CheckOutScreen
import com.markduenas.visischeduler.presentation.ui.screens.checkin.QrScannerScreen
import com.markduenas.visischeduler.presentation.ui.screens.checkin.TodayVisitsScreen
import com.markduenas.visischeduler.presentation.viewmodel.analytics.AnalyticsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.CheckInViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.CheckOutViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.QrScannerViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.TodayVisitsViewModel
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

    data object Splash : AppScreen() {
        override val key: ScreenKey = "splash"
        @Composable override fun Content() { SplashScreen() }
    }

    data object Login : AppScreen() {
        override val key: ScreenKey = "login"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: LoginViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { navigator.replaceAll(Dashboard) },
                onNavigateToRegister = { navigator.push(Register) },
                onNavigateToForgotPassword = { navigator.push(ForgotPassword) }
            )
        }
    }

    data object Register : AppScreen() {
        override val key: ScreenKey = "register"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: RegisterViewModel = koinInject()
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = { navigator.pop() },
                onNavigateToLogin = { navigator.pop() }
            )
        }
    }

    data object ForgotPassword : AppScreen() {
        override val key: ScreenKey = "forgot_password"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ForgotPasswordViewModel = koinInject()
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navigator.pop() }
            )
        }
    }

    data class Mfa(val challengeId: String) : AppScreen() {
        override val key: ScreenKey = "mfa_$challengeId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: MfaViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            MfaScreen(
                challengeId = challengeId,
                onNavigateBack = { navigator.pop() },
                onVerificationSuccess = { navigator.replaceAll(Dashboard) },
                viewModel = viewModel
            )
        }
    }

    // ==================== Main Screens ====================

    data object Dashboard : AppScreen() {
        override val key: ScreenKey = "dashboard"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: DashboardViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    when {
                        route == "calendar" -> navigator.push(Calendar)
                        route == "pending_requests" -> navigator.push(PendingRequests)
                        route == "messages" -> navigator.push(Messages)
                        route == "profile" -> navigator.push(Profile)
                        route == "notifications" -> navigator.push(Notifications)
                        route == "settings" -> navigator.push(Settings)
                        route == "today_visits" -> navigator.push(TodayVisits)
                        route == "analytics" -> navigator.push(Analytics)
                        route.startsWith("visit/") -> navigator.push(VisitDetails(route.substringAfter("visit/")))
                    }
                }
            )
        }
    }

    data object Calendar : AppScreen() {
        override val key: ScreenKey = "calendar"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: CalendarViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            CalendarScreen(
                viewModel = viewModel,
                onNavigateToSchedule = { date -> navigator.push(ScheduleVisit("default")) },
                onNavigateToVisitDetails = { visitId -> navigator.push(VisitDetails(visitId)) }
            )
        }
    }

    data object PendingRequests : AppScreen() {
        override val key: ScreenKey = "pending_requests"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            PendingRequestsScreen(
                onNavigateBack = { navigator.pop() },
                onViewDetails = { visitId -> navigator.push(VisitDetails(visitId)) }
            )
        }
    }

    data object Messages : AppScreen() {
        override val key: ScreenKey = "messages"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ConversationsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            ConversationsScreen(viewModel = viewModel)
        }
    }

    data object Profile : AppScreen() {
        override val key: ScreenKey = "profile"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ProfileViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            ProfileScreen(
                viewModel = viewModel,
                onNavigateToEditProfile = { navigator.push(EditProfile) },
                onNavigateToSettings = { navigator.push(Settings) },
                onNavigateToNotifications = { navigator.push(Notifications) }
            )
        }
    }

    data object EditProfile : AppScreen() {
        override val key: ScreenKey = "edit_profile"
        @Composable override fun Content() {
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

    data class ScheduleVisit(val beneficiaryId: String) : AppScreen() {
        override val key: ScreenKey = "schedule_visit_$beneficiaryId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ScheduleVisitViewModel = koinInject()
            HandleEvents(viewModel, navigator)
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

    data class VisitDetails(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "visit_details_$visitId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: VisitDetailsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            VisitDetailsScreen(
                visitId = visitId,
                onNavigateBack = { navigator.pop() },
                onEditVisit = { id -> navigator.push(EditVisit(id)) }
            )
        }
    }

    data class EditVisit(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "edit_visit_$visitId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: EditVisitViewModel = koinInject(parameters = { parametersOf(visitId) })
            HandleEvents(viewModel, navigator)
            EditVisitScreen(
                visitId = visitId,
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    // ==================== Visitor Screens ====================

    data object VisitorList : AppScreen() {
        override val key: ScreenKey = "visitor_list"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: VisitorListViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            VisitorListScreen(
                onNavigateBack = { navigator.pop() },
                onAddVisitor = { navigator.push(AddVisitor) },
                onViewVisitor = { visitorId -> navigator.push(VisitorDetails(visitorId)) },
                viewModel = viewModel
            )
        }
    }

    data class VisitorDetails(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = "visitor_details_$visitorId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: VisitorDetailsViewModel = koinInject { parametersOf(visitorId) }
            HandleEvents(viewModel, navigator)
            VisitorDetailsScreen(
                visitorId = visitorId,
                onNavigateBack = { navigator.pop() },
                onEditVisitor = { id -> navigator.push(EditVisitor(id)) },
                viewModel = viewModel
            )
        }
    }

    data object AddVisitor : AppScreen() {
        override val key: ScreenKey = "add_visitor"
        @Composable override fun Content() {
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

    data class EditVisitor(val visitorId: String) : AppScreen() {
        override val key: ScreenKey = "edit_visitor_$visitorId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: EditVisitorViewModel = koinInject(parameters = { parametersOf(visitorId) })
            HandleEvents(viewModel, navigator)
            EditVisitorScreen(
                visitorId = visitorId,
                onNavigateBack = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    // ==================== Restriction Screens ====================

    data object Restrictions : AppScreen() {
        override val key: ScreenKey = "restrictions"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: RestrictionsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            RestrictionsScreen(
                onNavigateBack = { navigator.pop() },
                onAddRestriction = { navigator.push(AddRestriction) },
                onEditRestriction = { id -> navigator.push(EditRestriction(id)) },
                viewModel = viewModel
            )
        }
    }

    data object AddRestriction : AppScreen() {
        override val key: ScreenKey = "add_restriction"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: AddRestrictionViewModel = koinInject { parametersOf(null) }
            HandleEvents(viewModel, navigator)
            AddRestrictionScreen(
                onNavigateBack = { navigator.pop() },
                onRestrictionAdded = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    data class EditRestriction(val id: String) : AppScreen() {
        override val key: ScreenKey = "edit_restriction_$id"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: AddRestrictionViewModel = koinInject { parametersOf(id) }
            HandleEvents(viewModel, navigator)
            AddRestrictionScreen(
                onNavigateBack = { navigator.pop() },
                onRestrictionAdded = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    // ==================== Settings Screens ====================

    data object Settings : AppScreen() {
        override val key: ScreenKey = "settings"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: SettingsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            SettingsScreen(onNavigateBack = { navigator.pop() }, viewModel = viewModel)
        }
    }

    data object NotificationSettings : AppScreen() {
        override val key: ScreenKey = "notification_settings"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: NotificationSettingsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            NotificationSettingsScreen(onNavigateBack = { navigator.pop() }, viewModel = viewModel)
        }
    }

    data class BeneficiarySettings(val beneficiaryId: String? = null) : AppScreen() {
        override val key: ScreenKey = "beneficiary_settings_${beneficiaryId ?: "default"}"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: BeneficiarySettingsViewModel = koinInject { parametersOf(beneficiaryId) }
            HandleEvents(viewModel, navigator)
            BeneficiarySettingsScreen(onNavigateBack = { navigator.pop() }, viewModel = viewModel)
        }
    }

    data object SecuritySettings : AppScreen() {
        override val key: ScreenKey = "security_settings"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: SecuritySettingsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            SecuritySettingsScreen(onNavigateBack = { navigator.pop() }, viewModel = viewModel)
        }
    }

    data object ChangePassword : AppScreen() {
        override val key: ScreenKey = "change_password"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: SecuritySettingsViewModel = koinInject()
            ChangePasswordScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    data object MfaSetup : AppScreen() {
        override val key: ScreenKey = "mfa_setup"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.MfaSetupViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            com.markduenas.visischeduler.presentation.ui.screens.settings.MfaSetupScreen(
                onNavigateBack = { navigator.pop() },
                onSetupComplete = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    data class AddBeneficiary(val beneficiaryId: String? = null) : AppScreen() {
        override val key: ScreenKey = "add_beneficiary_${beneficiaryId ?: "new"}"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: com.markduenas.visischeduler.presentation.viewmodel.settings.AddBeneficiaryViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            com.markduenas.visischeduler.presentation.ui.screens.settings.AddBeneficiaryScreen(
                beneficiaryId = beneficiaryId,
                onNavigateBack = { navigator.pop() },
                onSaveSuccess = { navigator.pop() },
                viewModel = viewModel
            )
        }
    }

    data object About : AppScreen() {
        override val key: ScreenKey = "about"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: SettingsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            AboutScreen(onNavigateBack = { navigator.pop() }, viewModel = viewModel)
        }
    }

    data object ScreenshotHelper : AppScreen() {
        override val key: ScreenKey = "screenshot_helper"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            ScreenshotHelperScreen(
                onNavigateBack = { navigator.pop() },
                onScenarioSelected = { scenarioId -> navigator.push(ScreenshotDemo(scenarioId)) }
            )
        }
    }

    data class ScreenshotDemo(val scenarioId: String) : AppScreen() {
        override val key: ScreenKey = "screenshot_demo_$scenarioId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            ScreenshotDemoContent(
                scenarioId = scenarioId,
                onNavigateBack = { navigator.pop() }
            )
        }
    }

    // ==================== Message Detail Screens ====================

    data class MessageThread(val conversationId: String) : AppScreen() {
        override val key: ScreenKey = "message_thread_$conversationId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: ChatViewModel = koinInject { parametersOf(conversationId) }
            val userRepository: UserRepository = koinInject()
            val currentUser by userRepository.currentUser.collectAsState(initial = null)
            HandleEvents(viewModel, navigator)
            ChatScreen(viewModel = viewModel, currentUserId = currentUser?.id ?: "")
        }
    }

    data class ComposeMessage(val recipientId: String? = null) : AppScreen() {
        override val key: ScreenKey = "compose_message_${recipientId ?: "new"}"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: NewMessageViewModel = koinInject { parametersOf(recipientId) }
            HandleEvents(viewModel, navigator)
            NewMessageScreen(viewModel = viewModel)
        }
    }

    // ==================== Notifications ====================

    data object Notifications : AppScreen() {
        override val key: ScreenKey = "notifications"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: NotificationsViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            NotificationsListScreen(onNavigateBack = { navigator.pop() })
        }
    }

    // ==================== Invitation Screens ====================

    data class AcceptInvitation(val inviteCode: String) : AppScreen() {
        override val key: ScreenKey = "accept_invitation_$inviteCode"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: AcceptInvitationViewModel = koinInject { parametersOf(inviteCode) }
            HandleEvents(viewModel, navigator)
            AcceptInvitationScreen(
                inviteCode = inviteCode,
                onNavigateBack = { navigator.pop() },
                onInvitationAccepted = { navigator.replaceAll(Dashboard) },
                viewModel = viewModel
            )
        }
    }

    // ==================== Check-In / Check-Out Screens ====================

    data class CheckIn(val visitId: String) : AppScreen() {
        override val key: ScreenKey = "check_in_$visitId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: CheckInViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            CheckInScreen(
                viewModel = viewModel,
                visitId = visitId,
                onNavigateBack = { navigator.pop() },
                onNavigateToCheckOut = { checkInId -> navigator.push(CheckOut(checkInId)) }
            )
        }
    }

    data class CheckOut(val checkInId: String) : AppScreen() {
        override val key: ScreenKey = "check_out_$checkInId"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: CheckOutViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            CheckOutScreen(
                viewModel = viewModel,
                checkInId = checkInId,
                onNavigateBack = { navigator.pop() },
                onNavigateToDashboard = { navigator.popUntilRoot() },
                onNavigateToSchedule = { beneficiaryId -> navigator.push(ScheduleVisit(beneficiaryId)) }
            )
        }
    }

    data object QrScanner : AppScreen() {
        override val key: ScreenKey = "qr_scanner"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: QrScannerViewModel = koinInject()
            HandleEvents(viewModel, navigator)
            QrScannerScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.pop() },
                onNavigateToVisitDetails = { visitId -> navigator.push(VisitDetails(visitId)) },
                onNavigateToCheckOut = { checkInId -> navigator.push(CheckOut(checkInId)) }
            )
        }
    }

    data object TodayVisits : AppScreen() {
        override val key: ScreenKey = "today_visits"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: TodayVisitsViewModel = koinInject()
            TodayVisitsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.pop() },
                onNavigateToVisitDetails = { visitId -> navigator.push(VisitDetails(visitId)) },
                onNavigateToQrScanner = { navigator.push(QrScanner) }
            )
        }
    }

    // ==================== Analytics Screen ====================

    data object Analytics : AppScreen() {
        override val key: ScreenKey = "analytics"
        @Composable override fun Content() {
            val navigator = LocalNavigator.currentOrThrow
            val viewModel: AnalyticsViewModel = koinInject()
            AnalyticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.pop() }
            )
        }
    }
}

@Composable
private fun HandleEvents(viewModel: BaseViewModel<*>, navigator: Navigator) {
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.Navigate -> {
                    val route = event.route
                    when {
                        route.startsWith("chat/") -> navigator.push(AppScreen.MessageThread(route.substringAfter("chat/")))
                        route == "new-message" -> navigator.push(AppScreen.ComposeMessage())
                        route == "calendar" -> navigator.push(AppScreen.Calendar)
                        route == "pending_requests" -> navigator.push(AppScreen.PendingRequests)
                        route == "messages" -> navigator.push(AppScreen.Messages)
                        route == "profile" -> navigator.push(AppScreen.Profile)
                        route == "notifications" -> navigator.push(AppScreen.Notifications)
                        route == "settings" -> navigator.push(AppScreen.Settings)
                        route == "notification_settings" -> navigator.push(AppScreen.NotificationSettings)
                        route == "security_settings" -> navigator.push(AppScreen.SecuritySettings)
                        route.startsWith("mfa/") -> navigator.push(AppScreen.Mfa(route.substringAfter("mfa/")))
                        route == "mfa_setup" -> navigator.push(AppScreen.MfaSetup)
                        route == "change_password" || route == "settings/security/change-password" -> navigator.push(AppScreen.ChangePassword)
                        route == "add_beneficiary" -> navigator.push(AppScreen.AddBeneficiary())
                        route.startsWith("edit_beneficiary/") -> navigator.push(AppScreen.AddBeneficiary(route.substringAfter("edit_beneficiary/")))
                        route == "about" -> navigator.push(AppScreen.About)
                        route == "screenshot_helper" -> navigator.push(AppScreen.ScreenshotHelper)
                        route == "beneficiary_settings" -> navigator.push(AppScreen.BeneficiarySettings())
                        route.startsWith("beneficiary_settings/") -> navigator.push(AppScreen.BeneficiarySettings(route.substringAfter("beneficiary_settings/")))
                        route == "visitor_list" -> navigator.push(AppScreen.VisitorList)
                        route == "restrictions" -> navigator.push(AppScreen.Restrictions)
                        route.startsWith("visit/") -> navigator.push(AppScreen.VisitDetails(route.substringAfter("visit/")))
                        route.startsWith("visitDetails/") -> navigator.push(AppScreen.VisitDetails(route.substringAfter("visitDetails/")))
                        route.startsWith("checkin/") -> navigator.push(AppScreen.CheckIn(route.substringAfter("checkin/")))
                        route.startsWith("checkout/") -> navigator.push(AppScreen.CheckOut(route.substringAfter("checkout/")))
                        route == "scanner" -> navigator.push(AppScreen.QrScanner)
                        route == "today_visits" -> navigator.push(AppScreen.TodayVisits)
                        route == "analytics" -> navigator.push(AppScreen.Analytics)
                    }
                }
                is UiEvent.NavigateBack -> navigator.pop()
                else -> {}
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, onNavigateBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Coming Soon", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onNavigateBack) { Text("Go Back") }
        }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MFA = "mfa/{challengeId}"
    const val DASHBOARD = "dashboard"
    const val CALENDAR = "calendar"
    const val PENDING_REQUESTS = "pending_requests"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val SCHEDULE_VISIT = "schedule_visit/{beneficiaryId}"
    const val VISIT_DETAILS = "visit_details/{visitId}"
    const val EDIT_VISIT = "edit_visit/{visitId}"
    const val VISITOR_LIST = "visitor_list"
    const val VISITOR_DETAILS = "visitor_details/{visitorId}"
    const val ADD_VISITOR = "add_visitor"
    const val EDIT_VISITOR = "edit_visitor/{visitorId}"
    const val RESTRICTIONS = "restrictions"
    const val ADD_RESTRICTION = "add_restriction"
    const val EDIT_RESTRICTION = "edit_restriction/{restrictionId}"
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
    const val BENEFICIARY_SETTINGS = "beneficiary_settings"
    const val SECURITY_SETTINGS = "security_settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val ABOUT = "about"
    const val MESSAGE_THREAD = "message_thread/{conversationId}"
    const val COMPOSE_MESSAGE = "compose_message"
    const val NOTIFICATIONS = "notifications"
    const val ACCEPT_INVITATION = "accept_invitation/{inviteCode}"
}
