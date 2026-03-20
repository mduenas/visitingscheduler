package com.markduenas.visischeduler.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual class UrlOpenerImpl : UrlOpener {
    override fun open(url: String) {
        NSURL.URLWithString(url)?.let { nsUrl ->
            UIApplication.sharedApplication.openURL(
                url = nsUrl,
                options = emptyMap<Any?, Any?>(),
                completionHandler = null
            )
        }
    }
}
