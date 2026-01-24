package com.markduenas.visischeduler.di

import org.koin.dsl.module

/**
 * Repository module providing all repository implementations.
 *
 * Each repository follows the Repository pattern, abstracting data sources
 * from the domain layer and providing a clean API for data operations.
 */
val repositoryModule = module {

    // Authentication Repository
    // single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }

    // User Repository
    // single<UserRepository> { UserRepositoryImpl(get(), get()) }

    // Visit/Schedule Repository
    // single<VisitRepository> { VisitRepositoryImpl(get(), get()) }

    // Restriction/Rules Repository
    // single<RestrictionRepository> { RestrictionRepositoryImpl(get(), get()) }

    // Notification Repository
    // single<NotificationRepository> { NotificationRepositoryImpl(get(), get()) }

    // Calendar Sync Repository
    // single<CalendarRepository> { CalendarRepositoryImpl(get()) }

    // Analytics Repository
    // single<AnalyticsRepository> { AnalyticsRepositoryImpl(get()) }

    // Beneficiary Repository
    // single<BeneficiaryRepository> { BeneficiaryRepositoryImpl(get(), get()) }
}
