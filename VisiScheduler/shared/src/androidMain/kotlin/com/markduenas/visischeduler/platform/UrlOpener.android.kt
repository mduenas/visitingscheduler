package com.markduenas.visischeduler.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

actual class UrlOpenerImpl(private val context: Context) : UrlOpener {
    override fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
