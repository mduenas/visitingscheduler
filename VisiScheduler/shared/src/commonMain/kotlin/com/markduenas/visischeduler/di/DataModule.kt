package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.repository.AuthRepositoryImpl
import com.markduenas.visischeduler.data.repository.RestrictionRepositoryImpl
import com.markduenas.visischeduler.data.repository.TimeSlotRepositoryImpl
import com.markduenas.visischeduler.data.repository.UserRepositoryImpl
import com.markduenas.visischeduler.data.repository.VisitRepositoryImpl
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import org.koin.dsl.module

/**
 * Data module providing repository implementations.
 */
val dataModule = module {
    // Repositories
    single<AuthRepository> {
        AuthRepositoryImpl(
            api = get(),
            database = get(),
            secureStorage = get(),
            json = get()
        )
    }

    single<VisitRepository> {
        VisitRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }

    single<UserRepository> {
        UserRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }

    single<RestrictionRepository> {
        RestrictionRepositoryImpl(
            api = get(),
            database = get(),
            json = get()
        )
    }

    single<TimeSlotRepository> {
        TimeSlotRepositoryImpl(
            api = get(),
            database = get()
        )
    }
}
