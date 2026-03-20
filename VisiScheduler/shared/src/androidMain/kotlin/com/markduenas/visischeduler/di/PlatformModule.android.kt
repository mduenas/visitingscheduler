package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.local.DatabaseDriverFactory
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.platform.SecureStorageImpl
import com.markduenas.visischeduler.platform.UrlOpener
import com.markduenas.visischeduler.platform.UrlOpenerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android platform-specific module.
 */
actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory(androidContext()) }
    single<SecureStorage> { SecureStorageImpl(androidContext()) }
    single<UrlOpener> { UrlOpenerImpl(androidContext()) }
}
