package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.billing.StoreKitHelperProvider
import com.markduenas.visischeduler.data.repository.AdRepositoryImpl
import com.markduenas.visischeduler.domain.repository.AdRepository
import org.koin.dsl.module

/**
 * iOS Koin module for StoreKit and Ad dependencies.
 *
 * Note: StoreKitHelper is accessed via StoreKitHelperProvider.shared directly
 * because Koin can't use KClass reflection on classes that extend NSObject.
 */
val iosAdModule = module {
    // Ad Repository - accesses StoreKitHelper via provider singleton
    single<AdRepository> { AdRepositoryImpl(StoreKitHelperProvider.shared) }
}
