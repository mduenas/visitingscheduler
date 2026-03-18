package com.markduenas.visischeduler.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific composable that renders a live camera preview and scans
 * for QR codes. When a QR code is detected, [onQrCodeScanned] is invoked with
 * the raw string value. Set [isFlashEnabled] to toggle the torch.
 */
@Composable
expect fun CameraScanner(
    onQrCodeScanned: (String) -> Unit,
    isFlashEnabled: Boolean,
    modifier: Modifier = Modifier
)
