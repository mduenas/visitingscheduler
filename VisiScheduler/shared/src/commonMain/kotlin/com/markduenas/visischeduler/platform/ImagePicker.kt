package com.markduenas.visischeduler.platform

import androidx.compose.runtime.Composable

/**
 * Returns a lambda that launches the system image gallery picker.
 * The callback receives the selected image as bytes, or null if cancelled.
 */
@Composable
expect fun rememberGalleryLauncher(onResult: (ByteArray?) -> Unit): () -> Unit

/**
 * Returns a lambda that launches the device camera for a photo.
 * The callback receives the captured image as bytes, or null if cancelled.
 */
@Composable
expect fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): () -> Unit
