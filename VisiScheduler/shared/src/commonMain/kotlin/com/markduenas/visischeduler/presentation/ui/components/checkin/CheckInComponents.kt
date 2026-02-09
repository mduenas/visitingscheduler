package com.markduenas.visischeduler.presentation.ui.components.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.CheckInMethod
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Status card showing current check-in state.
 */
@Composable
fun CheckInStatusCard(
    isCheckedIn: Boolean,
    checkInTime: String?,
    visitorName: String,
    beneficiaryName: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCheckedIn) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isCheckedIn) Icons.Default.CheckCircle else Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isCheckedIn) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isCheckedIn) "Checked In" else "Not Checked In",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Visitor: $visitorName",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Visiting: $beneficiaryName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isCheckedIn && checkInTime != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Since: $checkInTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Status card showing current check-in state with Instant checkInTime.
 */
@Composable
fun CheckInStatusCard(
    isCheckedIn: Boolean,
    checkInTime: Instant?,
    visitorName: String = "",
    beneficiaryName: String = "",
    modifier: Modifier = Modifier
) {
    val formattedTime = checkInTime?.let { instant ->
        val zone = TimeZone.currentSystemDefault()
        val dateTime = instant.toLocalDateTime(zone)
        val hour = dateTime.hour
        val minute = dateTime.minute
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        "$displayHour:${minute.toString().padStart(2, '0')} $period"
    }

    CheckInStatusCard(
        isCheckedIn = isCheckedIn,
        checkInTime = formattedTime,
        visitorName = visitorName,
        beneficiaryName = beneficiaryName,
        modifier = modifier
    )
}

/**
 * QR Code display component.
 */
@Composable
fun QrCodeDisplay(
    qrCodeData: String,
    size: Int = 200,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Placeholder for QR code - in real implementation would use a QR code library
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .background(Color.White)
                    .border(2.dp, Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Black
                    )
                    Text(
                        text = "Scan to Check In",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Show this code at the entrance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Simple check-in action button.
 */
@Composable
fun CheckInButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    CheckInButton(
        onCheckIn = { onClick() },
        isLoading = isLoading,
        enabled = enabled,
        modifier = modifier
    )
}

/**
 * Check-in action button with method selection.
 */
@Composable
fun CheckInButton(
    onCheckIn: (CheckInMethod) -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Button(
            onClick = { expanded = true },
            enabled = enabled && !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Check In")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("QR Code") },
                onClick = {
                    expanded = false
                    onCheckIn(CheckInMethod.QR_CODE)
                },
                leadingIcon = {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Manual Entry") },
                onClick = {
                    expanded = false
                    onCheckIn(CheckInMethod.MANUAL)
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("Automatic") },
                onClick = {
                    expanded = false
                    onCheckIn(CheckInMethod.AUTOMATIC)
                },
                leadingIcon = {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                }
            )
        }
    }
}

/**
 * Visit duration display component with text.
 */
@Composable
fun VisitDurationDisplay(
    durationText: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = durationText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Visit duration display component with minutes.
 */
@Composable
fun VisitDurationDisplay(
    durationMinutes: Long,
    modifier: Modifier = Modifier
) {
    val hours = durationMinutes / 60
    val minutes = durationMinutes % 60

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes} minutes"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Star rating input component.
 */
@Composable
fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    maxRating: Int = 5,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxRating) {
            IconButton(
                onClick = { onRatingChange(i) }
            ) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Rate $i stars",
                    tint = if (i <= rating) {
                        Color(0xFFFFB300) // Gold color
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

/**
 * Check-out button component.
 */
@Composable
fun CheckOutButton(
    onCheckOut: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onCheckOut,
        enabled = enabled && !isLoading,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onError,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Default.Logout,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Check Out")
    }
}
