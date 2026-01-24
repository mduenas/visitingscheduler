package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.domain.usecase.ApproveVisitUseCase
import com.markduenas.visischeduler.domain.usecase.GetAvailableSlotsUseCase
import com.markduenas.visischeduler.domain.usecase.LoginUseCase
import com.markduenas.visischeduler.domain.usecase.ScheduleVisitUseCase
import org.koin.dsl.module

/**
 * Domain module providing use cases.
 */
val domainModule = module {
    // Use Cases
    factory { LoginUseCase(get()) }

    factory { ScheduleVisitUseCase(get(), get()) }

    factory { ApproveVisitUseCase(get(), get()) }

    factory { GetAvailableSlotsUseCase(get(), get()) }
}
