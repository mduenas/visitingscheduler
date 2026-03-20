package com.markduenas.visischeduler.presentation.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.usecase.FatigueAssessment
import com.markduenas.visischeduler.domain.usecase.FatigueLevel

/**
 * Banner shown when a beneficiary's visit fatigue level is elevated or higher.
 */
@Composable
fun FatigueWarningBanner(
    assessment: FatigueAssessment,
    modifier: Modifier = Modifier
) {
    if (!assessment.level.isWarning) return

    val style = when (assessment.level) {
        FatigueLevel.CRITICAL -> FatigueBannerStyle(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "⚠️ Critical Fatigue Risk",
        )
        FatigueLevel.HIGH -> FatigueBannerStyle(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.onErrorContainer,
            "🔴 High Fatigue Risk",
        )
        FatigueLevel.ELEVATED -> FatigueBannerStyle(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "🟡 Elevated Fatigue",
        )
        else -> return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = style.containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = style.contentColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = style.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = style.contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = assessment.summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = style.contentColor
                )
            }
        }
    }
}

private data class FatigueBannerStyle(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val title: String
)
