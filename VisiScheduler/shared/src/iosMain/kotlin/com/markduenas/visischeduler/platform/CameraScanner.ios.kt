package com.markduenas.visischeduler.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CATransaction
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun CameraScanner(
    onQrCodeScanned: (String) -> Unit,
    isFlashEnabled: Boolean,
    modifier: Modifier
) {
    val scannerState = remember {
        IosCameraScanner(onQrCodeScanned)
    }

    UIKitView(
        factory = {
            scannerState.start()
            scannerState.containerView
        },
        update = { _ ->
            val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo)
            if (device != null && device.hasTorch) {
                try {
                    device.lockForConfiguration(null)
                    device.torchMode = if (isFlashEnabled) {
                        AVCaptureTorchModeOn
                    } else {
                        AVCaptureTorchModeOff
                    }
                    device.unlockForConfiguration()
                } catch (_: Exception) {}
            }
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalForeignApi::class)
private class IosCameraScanner(
    private val onQrCodeScanned: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    val containerView = UIView()
    private val session = AVCaptureSession()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var hasScanned = false

    fun start() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVMediaTypeVideo) ?: return
        val input = AVCaptureDeviceInput.deviceInputWithDevice(device, null) ?: return

        session.addInput(input)

        val metadataOutput = AVCaptureMetadataOutput()
        session.addOutput(metadataOutput)
        metadataOutput.setMetadataObjectsDelegate(this, queue = NSOperationQueue.mainQueue)
        metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)

        val layer = AVCaptureVideoPreviewLayer(session = session)
        layer.frame = CGRectMake(0.0, 0.0, 0.0, 0.0)
        containerView.layer.addSublayer(layer)
        previewLayer = layer

        CATransaction.begin()
        CATransaction.setDisableActions(true)
        session.startRunning()
        CATransaction.commit()
    }

    override fun captureOutput(
        output: platform.AVFoundation.AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: platform.AVFoundation.AVCaptureConnection
    ) {
        if (hasScanned) return
        val qrObject = didOutputMetadataObjects
            .filterIsInstance<AVMetadataMachineReadableCodeObject>()
            .firstOrNull { it.type == AVMetadataObjectTypeQRCode }
            ?: return

        qrObject.stringValue?.let { value ->
            hasScanned = true
            onQrCodeScanned(value)
        }
    }
}
