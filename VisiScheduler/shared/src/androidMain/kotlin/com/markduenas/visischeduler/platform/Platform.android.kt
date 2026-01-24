package com.markduenas.visischeduler.platform

import android.os.Build

/**
 * Android platform implementation.
 */
class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val version: String = Build.VERSION.RELEASE
    override val isDebug: Boolean = android.os.Debug.isDebuggerConnected()
}

actual fun getPlatform(): Platform = AndroidPlatform()
