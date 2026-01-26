package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.presentation.viewmodel.auth.ForgotPasswordViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.LoginViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.MfaViewModel
import com.markduenas.visischeduler.presentation.viewmodel.auth.RegisterViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.CheckInViewModel
import com.markduenas.visischeduler.presentation.viewmodel.checkin.CheckOutViewModel
import com.markduenas.visischeduler.presentation.viewmodel.dashboard.DashboardViewModel
import com.markduenas.visischeduler.presentation.viewmodel.messaging.ConversationsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.CalendarViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.PendingRequestsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.ScheduleVisitViewModel
import com.markduenas.visischeduler.presentation.viewmodel.scheduling.VisitDetailsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel
import com.markduenas.visischeduler.presentation.viewmodel.settings.SettingsViewModel
import com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorListViewModel
import org.koin.dsl.module

/**
 * ViewModel module providing all ViewModels.
 *
 * ViewModels are scoped to their respective screens and handle
 * UI state management and user interactions.
 */
val viewModelModule = module {

    // ==========================================
    // Authentication ViewModels
    // ==========================================
    factory { LoginViewModel(get(), get()) }
    factory { RegisterViewModel(get()) }
    factory { ForgotPasswordViewModel(get()) }
    factory { MfaViewModel(get()) }

    // ==========================================
    // Main/Dashboard ViewModels
    // ==========================================
    factory { DashboardViewModel(get(), get(), get()) }

    // ==========================================
    // Schedule ViewModels
    // ==========================================
    factory { CalendarViewModel(get()) }
    factory { ScheduleVisitViewModel(get(), get()) }
    factory { VisitDetailsViewModel(get(), get(), get()) }
    factory { PendingRequestsViewModel(get(), get()) }

    // ==========================================
    // Visitor Management ViewModels
    // ==========================================
    factory { VisitorListViewModel(get()) }

    // ==========================================
    // Profile/Settings ViewModels
    // ==========================================
    factory { ProfileViewModel(get(), get()) }
    factory { SettingsViewModel(get(), get()) }

    // ==========================================
    // Check-in ViewModels
    // ==========================================
    factory { CheckInViewModel(get(), get(), get(), get()) }
    factory { CheckOutViewModel(get(), get(), get()) }

    // ==========================================
    // Messaging ViewModels
    // ==========================================
    factory { ConversationsViewModel(get(), get()) }
}
