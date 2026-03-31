package com.markduenas.visischeduler.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

// iOS image loading via URL requires Coil 3 (multiplatform). Until then, the
// caller's placeholder/initials fallback is rendered instead.
@Composable
actual fun NetworkImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    Box(modifier = modifier)
}
