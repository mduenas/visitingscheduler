package com.markduenas.visischeduler.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGalleryLauncher(onResult: (ByteArray?) -> Unit): () -> Unit {
    // iOS photo picker implementation — post-RC
    return {}
}

@Composable
actual fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): () -> Unit {
    // iOS camera implementation — post-RC
    return {}
}
