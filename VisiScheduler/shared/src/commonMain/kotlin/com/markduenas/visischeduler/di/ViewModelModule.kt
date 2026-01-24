package com.markduenas.visischeduler.di

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
    // viewModel { LoginViewModel(get(), get()) }
    // viewModel { RegisterViewModel(get()) }
    // viewModel { ForgotPasswordViewModel(get()) }
    // viewModel { MfaVerificationViewModel(get()) }

    // ==========================================
    // Main/Dashboard ViewModels
    // ==========================================
    // viewModel { DashboardViewModel(get(), get(), get()) }
    // viewModel { HomeViewModel(get(), get()) }

    // ==========================================
    // Schedule ViewModels
    // ==========================================
    // viewModel { ScheduleViewModel(get(), get(), get()) }
    // viewModel { CreateVisitViewModel(get(), get(), get()) }
    // viewModel { VisitDetailsViewModel(get(), get()) }
    // viewModel { CalendarViewModel(get(), get()) }

    // ==========================================
    // Approval ViewModels
    // ==========================================
    // viewModel { ApprovalListViewModel(get()) }
    // viewModel { ApprovalDetailsViewModel(get(), get()) }

    // ==========================================
    // Visitor Management ViewModels
    // ==========================================
    // viewModel { VisitorListViewModel(get()) }
    // viewModel { VisitorDetailsViewModel(get(), get()) }
    // viewModel { VisitorRequestViewModel(get()) }

    // ==========================================
    // Restriction/Rules ViewModels
    // ==========================================
    // viewModel { RestrictionListViewModel(get()) }
    // viewModel { CreateRestrictionViewModel(get()) }
    // viewModel { EditRestrictionViewModel(get(), get()) }

    // ==========================================
    // Notification ViewModels
    // ==========================================
    // viewModel { NotificationListViewModel(get(), get()) }
    // viewModel { NotificationSettingsViewModel(get()) }

    // ==========================================
    // Profile/Settings ViewModels
    // ==========================================
    // viewModel { ProfileViewModel(get(), get()) }
    // viewModel { SettingsViewModel(get()) }
    // viewModel { SecuritySettingsViewModel(get(), get()) }

    // ==========================================
    // Analytics ViewModels
    // ==========================================
    // viewModel { AnalyticsDashboardViewModel(get()) }
    // viewModel { ReportsViewModel(get()) }

    // ==========================================
    // Video Call ViewModels
    // ==========================================
    // viewModel { VideoCallViewModel(get(), get()) }

    // ==========================================
    // Care Circle ViewModels
    // ==========================================
    // viewModel { CareCircleViewModel(get(), get(), get()) }
    // viewModel { MemberManagementViewModel(get()) }

    // ==========================================
    // Check-in ViewModels
    // ==========================================
    // viewModel { CheckInViewModel(get(), get()) }
    // viewModel { QrScannerViewModel(get()) }
}
