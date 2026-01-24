package com.markduenas.visischeduler.presentation.ui.components.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus

/**
 * A dropdown selector for choosing a beneficiary.
 *
 * @param beneficiaries List of available beneficiaries
 * @param selectedBeneficiary Currently selected beneficiary
 * @param onBeneficiarySelected Callback when a beneficiary is selected
 * @param onClearSelection Callback when selection is cleared
 * @param modifier Modifier for the component
 */
@Composable
fun BeneficiarySelector(
    beneficiaries: List<Beneficiary>,
    selectedBeneficiary: Beneficiary?,
    onBeneficiarySelected: (String) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    BeneficiaryAvatar(
                        name = selectedBeneficiary?.fullName ?: "All",
                        photoUrl = selectedBeneficiary?.photoUrl,
                        size = 36
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = selectedBeneficiary?.fullName ?: "All Beneficiaries",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (selectedBeneficiary != null) {
                            Text(
                                text = selectedBeneficiary.roomNumber?.let { "Room $it" } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedBeneficiary != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { onClearSelection() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear selection",
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Select beneficiary",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            // All option
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BeneficiaryAvatar(name = "All", size = 32)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("All Beneficiaries")
                    }
                },
                onClick = {
                    onClearSelection()
                    expanded = false
                },
                trailingIcon = {
                    if (selectedBeneficiary == null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Individual beneficiaries
            beneficiaries.forEach { beneficiary ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BeneficiaryAvatar(
                                name = beneficiary.fullName,
                                photoUrl = beneficiary.photoUrl,
                                size = 32
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(beneficiary.fullName)
                                beneficiary.roomNumber?.let { room ->
                                    Text(
                                        text = "Room $room",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onBeneficiarySelected(beneficiary.id)
                        expanded = false
                    },
                    trailingIcon = {
                        if (selectedBeneficiary?.id == beneficiary.id) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    enabled = beneficiary.status == BeneficiaryStatus.ACTIVE
                )
            }
        }
    }
}

/**
 * Horizontal chip-based beneficiary selector.
 */
@Composable
fun BeneficiaryChipSelector(
    beneficiaries: List<Beneficiary>,
    selectedBeneficiaryId: String?,
    onBeneficiarySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All chip
        FilterChip(
            selected = selectedBeneficiaryId == null,
            onClick = { onBeneficiarySelected(null) },
            label = { Text("All") },
            leadingIcon = {
                if (selectedBeneficiaryId == null) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )

        // Individual beneficiary chips
        beneficiaries.forEach { beneficiary ->
            FilterChip(
                selected = selectedBeneficiaryId == beneficiary.id,
                onClick = { onBeneficiarySelected(beneficiary.id) },
                label = { Text(beneficiary.firstName) },
                leadingIcon = {
                    if (selectedBeneficiaryId == beneficiary.id) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        BeneficiaryAvatar(
                            name = beneficiary.fullName,
                            photoUrl = beneficiary.photoUrl,
                            size = 18
                        )
                    }
                },
                enabled = beneficiary.status == BeneficiaryStatus.ACTIVE,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

/**
 * Beneficiary info card for visitor dashboard.
 */
@Composable
fun BeneficiaryInfoCard(
    beneficiary: Beneficiary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BeneficiaryAvatar(
                name = beneficiary.fullName,
                photoUrl = beneficiary.photoUrl,
                size = 56
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = beneficiary.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                beneficiary.roomNumber?.let { room ->
                    Text(
                        text = "Room $room",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                BeneficiaryStatusChip(status = beneficiary.status)
            }
        }
    }
}

/**
 * Status chip for beneficiary status.
 */
@Composable
fun BeneficiaryStatusChip(
    status: BeneficiaryStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        BeneficiaryStatus.ACTIVE -> Color(0xFF4CAF50) to "Available"
        BeneficiaryStatus.TEMPORARILY_UNAVAILABLE -> MaterialTheme.colorScheme.tertiary to "Temporarily Unavailable"
        BeneficiaryStatus.MEDICAL_HOLD -> MaterialTheme.colorScheme.error to "Medical Hold"
        BeneficiaryStatus.TRANSFERRED -> MaterialTheme.colorScheme.outline to "Transferred"
        BeneficiaryStatus.INACTIVE -> MaterialTheme.colorScheme.outline to "Inactive"
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Simple avatar component for beneficiaries.
 */
@Composable
fun BeneficiaryAvatar(
    name: String,
    photoUrl: String? = null,
    size: Int = 40,
    modifier: Modifier = Modifier
) {
    val initials = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.size(size.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size.dp)
        ) {
            if (photoUrl != null) {
                // In a real app, use an image loading library
                // For now, show initials
                Text(
                    text = initials,
                    style = when {
                        size < 24 -> MaterialTheme.typography.labelSmall
                        size < 40 -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Text(
                    text = initials,
                    style = when {
                        size < 24 -> MaterialTheme.typography.labelSmall
                        size < 40 -> MaterialTheme.typography.labelMedium
                        else -> MaterialTheme.typography.titleMedium
                    },
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
