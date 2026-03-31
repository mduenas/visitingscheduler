package com.markduenas.visischeduler.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/**
 * Loads an image from a URL. Platform-specific implementation handles caching.
 * Falls back to a transparent placeholder when the URL is null/blank.
 */
@Composable
expect fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
)
