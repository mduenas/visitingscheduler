package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.domain.usecase.*
import org.koin.dsl.module

/**
 * UseCase module providing all domain use cases.
 */
val useCaseModule = module {

    // ==========================================
    // Authentication Use Cases
    // ==========================================
    factory { LoginUseCase(get()) }

    // ==========================================
    // Visit/Scheduling Use Cases
    // ==========================================
    factory { EvaluateRulesUseCase(get(), get()) }
    factory { ScheduleVisitUseCase(get(), get()) }
    factory { GetAvailableSlotsUseCase(get(), get()) }
    factory { GetSuggestedSlotsUseCase(get(), get()) }
    factory { ApproveVisitUseCase(get(), get()) }
    factory { GetBeneficiaryFatigueUseCase(get()) }

    // ==========================================
    // Check-in/Check-out Use Cases
    // ==========================================
    factory { CheckInUseCase(get(), get()) }
    factory { CheckOutUseCase(get()) }

    // ==========================================
    // Messaging Use Cases
    // ==========================================
    factory { GetConversationsUseCase(get()) }
    factory { SendMessageUseCase(get()) }

    // ==========================================
    // Notification Use Cases
    // ==========================================
    factory { GetNotificationsUseCase(get()) }
    factory { MarkNotificationReadUseCase(get()) }
    factory { DeleteNotificationUseCase(get()) }
}
