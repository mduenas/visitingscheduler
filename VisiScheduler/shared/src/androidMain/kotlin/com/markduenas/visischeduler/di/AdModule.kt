package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.billing.BillingManager
import com.markduenas.visischeduler.data.repository.AdRepositoryImpl
import com.markduenas.visischeduler.domain.repository.AdRepository
import org.koin.dsl.module

/**
 * Koin module for AdMob and Billing dependencies.
 */
val adModule = module {
    // Billing Manager singleton
    single { BillingManager(get()) }

    // Ad Repository
    single<AdRepository> { AdRepositoryImpl(get(), get()) }
}
