package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.repository.AuthRepositoryImpl
import com.markduenas.visischeduler.data.repository.BeneficiaryRepositoryImpl
import com.markduenas.visischeduler.data.repository.CheckInRepositoryImpl
import com.markduenas.visischeduler.data.repository.MessageRepositoryImpl
import com.markduenas.visischeduler.data.repository.RestrictionRepositoryImpl
import com.markduenas.visischeduler.data.repository.TimeSlotRepositoryImpl
import com.markduenas.visischeduler.data.repository.UserRepositoryImpl
import com.markduenas.visischeduler.data.repository.VisitRepositoryImpl
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import org.koin.dsl.module

/**
 * Repository module providing all repository implementations.
 *
 * Each repository follows the Repository pattern, abstracting data sources
 * from the domain layer and providing a clean API for data operations.
 *
 * Note: This module is used by AppModule for iOS builds.
 * Android builds use DataModule which provides equivalent functionality.
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
            json = get()
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
            api = get(),
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
