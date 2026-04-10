package com.markduenas.visischeduler.platform

import android.os.Build

actual fun getDeviceInfo(): DeviceInfo = object : DeviceInfo {
    override val deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    override val deviceType: String = "Android"
}
