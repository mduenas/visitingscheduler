package com.markduenas.visischeduler.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

// ---------------------------------------------------------------------------
// Delegate — holds strong reference to itself so ARC doesn't release it early
// ---------------------------------------------------------------------------

private class PickerDelegate(
    private val onResult: (ByteArray?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage]) as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(image?.toJpegBytes())
        // Drop self-reference so ARC can collect
        activeDelegate = null
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(null)
        activeDelegate = null
    }
}

/** Keeps the delegate alive until the picker is dismissed. */
private var activeDelegate: PickerDelegate? = null

@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toJpegBytes(): ByteArray? {
    val data: NSData = UIImageJPEGRepresentation(this, 0.9) ?: return null
    val bytes = data.bytes ?: return null
    val length = data.length.toInt()
    @Suppress("UNCHECKED_CAST")
    val ptr = bytes as CPointer<ByteVar>
    return ByteArray(length) { ptr[it] }
}

private fun presentPicker(sourceType: UIImagePickerControllerSourceType, onResult: (ByteArray?) -> Unit) {
    if (!UIImagePickerController.isSourceTypeAvailable(sourceType)) {
        onResult(null)
        return
    }
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: run {
        onResult(null)
        return
    }
    val picker = UIImagePickerController()
    picker.sourceType = sourceType
    picker.allowsEditing = true
    val delegate = PickerDelegate(onResult)
    activeDelegate = delegate
    picker.delegate = delegate
    rootVC.presentViewController(picker, animated = true, completion = null)
}

// ---------------------------------------------------------------------------
// Expect actuals
// ---------------------------------------------------------------------------

@Composable
actual fun rememberGalleryLauncher(onResult: (ByteArray?) -> Unit): () -> Unit {
    return remember(onResult) {
        {
            presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary, onResult)
        }
    }
}

@Composable
actual fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): () -> Unit {
    return remember(onResult) {
        {
            presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera, onResult)
        }
    }
}
