package com.markduenas.visischeduler.di

import org.koin.dsl.module

/**
 * Use Case module providing all business logic use cases.
 *
 * Use cases encapsulate single business operations and coordinate
 * between repositories to fulfill complex business requirements.
 */
val useCaseModule = module {

    // ==========================================
    // Authentication Use Cases
    // ==========================================
    // factory { LoginUseCase(get()) }
    // factory { LogoutUseCase(get()) }
    // factory { RegisterUseCase(get()) }
    // factory { RefreshTokenUseCase(get()) }
    // factory { VerifyMfaUseCase(get()) }
    // factory { BiometricAuthUseCase(get()) }
    // factory { RequestPasswordResetUseCase(get()) }

    // ==========================================
    // Visit/Scheduling Use Cases
    // ==========================================
    // factory { ScheduleVisitUseCase(get(), get(), get()) }
    // factory { GetAvailableSlotsUseCase(get(), get()) }
    // factory { UpdateVisitUseCase(get(), get()) }
    // factory { CancelVisitUseCase(get()) }
    // factory { GetUpcomingVisitsUseCase(get()) }
    // factory { GetVisitHistoryUseCase(get()) }
    // factory { CheckInUseCase(get()) }
    // factory { CheckOutUseCase(get()) }
    // factory { GenerateQrCodeUseCase(get()) }

    // ==========================================
    // Approval Use Cases
    // ==========================================
    // factory { ApproveVisitUseCase(get(), get()) }
    // factory { DenyVisitUseCase(get()) }
    // factory { GetPendingApprovalsUseCase(get()) }

    // ==========================================
    // Visitor Management Use Cases
    // ==========================================
    // factory { RequestVisitorAccessUseCase(get()) }
    // factory { ApproveVisitorUseCase(get()) }
    // factory { RevokeVisitorAccessUseCase(get()) }
    // factory { GetVisitorListUseCase(get()) }

    // ==========================================
    // Restriction/Rules Use Cases
    // ==========================================
    // factory { CreateRestrictionUseCase(get()) }
    // factory { UpdateRestrictionUseCase(get()) }
    // factory { DeleteRestrictionUseCase(get()) }
    // factory { EvaluateRestrictionsUseCase(get()) }
    // factory { GetActiveRestrictionsUseCase(get()) }

    // ==========================================
    // Notification Use Cases
    // ==========================================
    // factory { GetNotificationsUseCase(get()) }
    // factory { MarkNotificationReadUseCase(get()) }
    // factory { UpdateNotificationPreferencesUseCase(get()) }

    // ==========================================
    // Calendar Sync Use Cases
    // ==========================================
    // factory { SyncCalendarUseCase(get()) }
    // factory { ConnectCalendarProviderUseCase(get()) }
    // factory { DisconnectCalendarProviderUseCase(get()) }

    // ==========================================
    // Analytics Use Cases
    // ==========================================
    // factory { GetDashboardDataUseCase(get()) }
    // factory { GetVisitMetricsUseCase(get()) }
    // factory { ExportAnalyticsReportUseCase(get()) }

    // ==========================================
    // Video Call Use Cases
    // ==========================================
    // factory { InitiateVideoCallUseCase(get()) }
    // factory { JoinVideoCallUseCase(get()) }
    // factory { EndVideoCallUseCase(get()) }

    // ==========================================
    // Care Circle Use Cases
    // ==========================================
    // factory { GetCareCircleUseCase(get()) }
    // factory { AddCareCircleMemberUseCase(get()) }
    // factory { RemoveCareCircleMemberUseCase(get()) }
    // factory { UpdateMemberRoleUseCase(get()) }
}
