package com.markduenas.visischeduler.presentation.ui.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.platform.NetworkImage

/**
 * Avatar picker source options.
 */
enum class AvatarSource {
    CAMERA,
    GALLERY
}

/**
 * A composable that displays a user avatar with an edit button.
 */
@Composable
fun AvatarPicker(
    imageUrl: String?,
    onSourceSelected: (AvatarSource) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    isEditable: Boolean = true,
    isUploading: Boolean = false,
    initials: String = ""
) {
    var showSourceDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Avatar container
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .then(
                    if (isEditable && !isUploading) {
                        Modifier.clickable { showSourceDialog = true }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size / 3),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (imageUrl != null && imageUrl.isNotBlank()) {
                // Placeholder shown behind; NetworkImage covers it once loaded
                AvatarPlaceholder(initials = initials, size = size)
                NetworkImage(
                    url = imageUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier.size(size).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                AvatarPlaceholder(
                    initials = initials,
                    size = size
                )
            }
        }

        // Edit button overlay
        if (isEditable && !isUploading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { showSourceDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit avatar",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Source selection dialog
    if (showSourceDialog) {
        AvatarSourceDialog(
            onDismiss = { showSourceDialog = false },
            onSourceSelected = { source ->
                showSourceDialog = false
                onSourceSelected(source)
            }
        )
    }
}

/**
 * Placeholder for avatar when no image is available.
 */
@Composable
private fun AvatarPlaceholder(
    initials: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (initials.isNotBlank()) {
            Text(
                text = initials.take(2).uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size / 2)
            )
        }
    }
}

/**
 * Dialog for selecting avatar source (camera or gallery).
 */
@Composable
private fun AvatarSourceDialog(
    onDismiss: () -> Unit,
    onSourceSelected: (AvatarSource) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Change Profile Photo",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                AvatarSourceOption(
                    icon = Icons.Default.CameraAlt,
                    title = "Take Photo",
                    description = "Use camera to take a new photo",
                    onClick = { onSourceSelected(AvatarSource.CAMERA) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                AvatarSourceOption(
                    icon = Icons.Default.Image,
                    title = "Choose from Gallery",
                    description = "Select an existing photo",
                    onClick = { onSourceSelected(AvatarSource.GALLERY) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * A single source option in the avatar source dialog.
 */
@Composable
private fun AvatarSourceOption(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Small avatar display for lists and compact views.
 */
@Composable
fun SmallAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    initials: String = "",
    borderColor: Color = Color.Transparent,
    borderWidth: Dp = 0.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        width = borderWidth,
                        color = borderColor,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AvatarPlaceholder(initials = initials, size = size)
        if (imageUrl != null && imageUrl.isNotBlank()) {
            NetworkImage(
                url = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(size).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
    }
}
