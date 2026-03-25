package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.billing.BillingManager
import com.markduenas.visischeduler.data.repository.AdRepositoryImpl
import com.markduenas.visischeduler.domain.repository.AdRepository
import com.markduenas.visischeduler.firebase.FirebaseService
import org.koin.dsl.module

/**
 * Koin module for AdMob and Billing dependencies.
 */
val adModule = module {
    // Firebase Analytics / Crashlytics service
    single { FirebaseService() }

    // Billing Manager singleton
    single { BillingManager(get()) }

    // Ad Repository
    single<AdRepository> { AdRepositoryImpl(get(), get()) }
}
