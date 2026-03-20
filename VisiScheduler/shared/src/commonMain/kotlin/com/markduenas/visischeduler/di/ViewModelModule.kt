package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.presentation.viewmodel.auth.*
import com.markduenas.visischeduler.presentation.viewmodel.analytics.AnalyticsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.*
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.*
import com.markduenas.visischeduler.presentation.viewmodel.messaging.*
import com.markduenas.visischeduler.presentation.viewmodel.notifications.*
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.*
import com.markduenas.visischeduler.presentation.viewmodel.settings.*
import com.markduenas.visischeduler.presentation.viewmodel.visitors.*
import org.koin.dsl.module
import org.koin.core.parameter.parametersOf

/**
 * ViewModel module providing all ViewModels.
 */
val viewModelModule = module {

    // Authentication ViewModels
    factory { LoginViewModel(get(), get()) }
    factory { RegisterViewModel(get()) }
    factory { ForgotPasswordViewModel(get()) }
    factory { MfaViewModel(get()) }

    // Dashboard ViewModels
    factory { DashboardViewModel(get(), get(), get(), get()) }

    // Schedule ViewModels
    factory { CalendarViewModel(get()) }
    factory { ScheduleVisitViewModel(get(), get(), get(), get()) }
    factory { VisitDetailsViewModel(get(), get(), get(), get()) }
    factory { PendingRequestsViewModel(get(), get()) }


    // Visitor Management ViewModels
    factory { VisitorListViewModel(get()) }
    factory { (visitorId: String) ->
        VisitorDetailsViewModel(get(), get(), visitorId)
    }
    factory { AddVisitorViewModel(get()) }
    factory { RestrictionsViewModel(get()) }
    factory { (restrictionId: String?) ->
        AddRestrictionViewModel(get())
    }
    factory { (inviteCode: String) ->
        AcceptInvitationViewModel(get(), inviteCode)
    }

    // Profile/Settings ViewModels
    factory { ProfileViewModel(get(), get(), get()) }
    factory { SettingsViewModel(get(), get(), get()) }
    factory { NotificationSettingsViewModel(get(), get()) }
    factory { SecuritySettingsViewModel(get(), get(), get()) }
    factory { MfaSetupViewModel(get()) }
    factory { (beneficiaryId: String?) ->
        BeneficiarySettingsViewModel(get(), beneficiaryId)
    }
    factory { AddBeneficiaryViewModel(get()) }

    // Check-in ViewModels
    factory { CheckInViewModel(get(), get(), get(), get()) }
    factory { CheckOutViewModel(get(), get(), get()) }
    factory { QrScannerViewModel(get()) }
    factory { TodayVisitsViewModel(get(), get()) }

    // Analytics ViewModels
    factory { AnalyticsViewModel(get(), get()) }

    // Messaging ViewModels
    factory { ConversationsViewModel(get(), get()) }
    factory { (conversationId: String) ->
        ChatViewModel(
            conversationId = conversationId,
            messageRepository = get(),
            sendMessageUseCase = get()
        )
    }
    factory { (beneficiaryId: String?) ->
        NewMessageViewModel(get(), beneficiaryId)
    }

    // Notification ViewModels
    factory { NotificationsViewModel(get(), get(), get()) }
}
