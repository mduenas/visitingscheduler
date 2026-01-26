package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.domain.usecase.ApproveVisitUseCase
import com.markduenas.visischeduler.domain.usecase.CheckInUseCase
import com.markduenas.visischeduler.domain.usecase.CheckOutUseCase
import com.markduenas.visischeduler.domain.usecase.GenerateQrCodeUseCase
import com.markduenas.visischeduler.domain.usecase.GetAvailableSlotsUseCase
import com.markduenas.visischeduler.domain.usecase.GetConversationsUseCase
import com.markduenas.visischeduler.domain.usecase.LoginUseCase
import com.markduenas.visischeduler.domain.usecase.ScheduleVisitUseCase
import com.markduenas.visischeduler.domain.usecase.SendMessageUseCase
import org.koin.dsl.module

/**
 * Domain module providing use cases.
 *
 * Use cases encapsulate single business operations and coordinate
 * between repositories to fulfill complex business requirements.
 */
val domainModule = module {

    // ==========================================
    // Authentication Use Cases
    // ==========================================
    factory { LoginUseCase(get()) }

    // ==========================================
    // Visit/Scheduling Use Cases
    // ==========================================
    factory { ScheduleVisitUseCase(get(), get()) }
    factory { GetAvailableSlotsUseCase(get(), get()) }
    factory { ApproveVisitUseCase(get(), get()) }

    // ==========================================
    // Check-in/Check-out Use Cases
    // ==========================================
    factory { CheckInUseCase(get(), get()) }
    factory { CheckOutUseCase(get()) }
    factory { GenerateQrCodeUseCase(get(), get()) }

    // ==========================================
    // Messaging Use Cases
    // ==========================================
    factory { GetConversationsUseCase(get()) }
    factory { SendMessageUseCase(get()) }
}
