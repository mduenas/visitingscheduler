package com.markduenas.visischeduler.platform

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberGalleryLauncher(onResult: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        onResult(bytes)
    }
    return { launcher.launch("image/*") }
}

@Composable
actual fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        onResult(stream.toByteArray())
    }
    return { launcher.launch(null) }
}
