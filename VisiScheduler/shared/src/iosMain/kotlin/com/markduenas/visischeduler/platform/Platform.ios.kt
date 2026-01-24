package com.markduenas.visischeduler.platform

import platform.UIKit.UIDevice
import kotlin.experimental.ExperimentalNativeApi

/**
 * iOS platform implementation.
 */
class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val version: String = UIDevice.currentDevice.systemVersion

    @OptIn(ExperimentalNativeApi::class)
    override val isDebug: Boolean = kotlin.native.Platform.isDebugBinary
}

actual fun getPlatform(): Platform = IOSPlatform()
