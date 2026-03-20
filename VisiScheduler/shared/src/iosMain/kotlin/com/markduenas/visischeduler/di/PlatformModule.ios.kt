package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.local.DatabaseDriverFactory
import com.markduenas.visischeduler.platform.SecureStorage
import com.markduenas.visischeduler.platform.SecureStorageImpl
import com.markduenas.visischeduler.platform.UrlOpener
import com.markduenas.visischeduler.platform.UrlOpenerImpl
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS platform-specific module.
 */
actual fun platformModule(): Module = module {
    single { DatabaseDriverFactory() }
    single<SecureStorage> { SecureStorageImpl() }
    single<UrlOpener> { UrlOpenerImpl() }
}
