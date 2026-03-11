package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.domain.usecase.ApproveVisitUseCase
import com.markduenas.visischeduler.domain.usecase.CheckInUseCase
import com.markduenas.visischeduler.domain.usecase.CheckOutUseCase
import com.markduenas.visischeduler.domain.usecase.EvaluateRulesUseCase
import com.markduenas.visischeduler.domain.usecase.GenerateQrCodeUseCase
import com.markduenas.visischeduler.domain.usecase.GetAvailableSlotsUseCase
import com.markduenas.visischeduler.domain.usecase.GetConversationsUseCase
import com.markduenas.visischeduler.domain.usecase.GetNotificationsUseCase
import com.markduenas.visischeduler.domain.usecase.GetSuggestedSlotsUseCase
import com.markduenas.visischeduler.domain.usecase.LoginUseCase
import com.markduenas.visischeduler.domain.usecase.MarkNotificationReadUseCase
import com.markduenas.visischeduler.domain.usecase.ScheduleVisitUseCase
import com.markduenas.visischeduler.domain.usecase.SendMessageUseCase
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

    // ==========================================
    // Check-in/Check-out Use Cases
    // ==========================================
    factory { CheckInUseCase(get(), get(), get()) }
    factory { CheckOutUseCase(get(), get()) }

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
}
