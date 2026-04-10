package com.markduenas.visischeduler.platform

import platform.UIKit.UIDevice

actual fun getDeviceInfo(): DeviceInfo = object : DeviceInfo {
    override val deviceName: String = UIDevice.currentDevice.name
    override val deviceType: String = "iOS"
}
