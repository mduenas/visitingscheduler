package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.repository.*
import com.markduenas.visischeduler.domain.repository.*
import org.koin.dsl.module

/**
 * Repository module providing all repository implementations.
 */
val repositoryModule = module {

    // Authentication Repository
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            database = get(),
            secureStorage = get(),
            json = get()
        )
    }

    // User Repository
    single<UserRepository> {
        UserRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }

    // Beneficiary Repository
    single<BeneficiaryRepository> {
        BeneficiaryRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }

    // Visit/Schedule Repository
    single<VisitRepository> {
        VisitRepositoryImpl(
            api = get(),
            database = get(),
            json = get(),
            syncManager = get()
        )
    }

    // Restriction/Rules Repository
    single<RestrictionRepository> {
        RestrictionRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }

    // Time Slot Repository
    single<TimeSlotRepository> {
        TimeSlotRepositoryImpl(
            database = get()
        )
    }

    // Check-In Repository
    single<CheckInRepository> {
        CheckInRepositoryImpl(
            api = get(),
            database = get()
        )
    }

    // Message/Conversation Repository
    single<MessageRepository> {
        MessageRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }
}
