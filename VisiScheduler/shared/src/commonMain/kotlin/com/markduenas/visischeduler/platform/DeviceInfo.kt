package com.markduenas.visischeduler.platform

/**
 * Platform-specific device metadata used for session tracking.
 */
interface DeviceInfo {
    /** Human-readable device name (e.g. "Pixel 8 Pro", "Mark's iPhone"). */
    val deviceName: String
    /** Platform type: "Android" or "iOS". */
    val deviceType: String
}

expect fun getDeviceInfo(): DeviceInfo
