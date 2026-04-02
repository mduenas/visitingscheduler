package com.markduenas.visischeduler.presentation.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.TimeSlot

/**
 * Coordinator dashboard content showing pending requests and stats.
 */
@Composable
fun CoordinatorDashboardContent(
    pendingRequests: List<Visit>,
    todayVisits: List<Visit>,
    beneficiaries: List<Beneficiary>,
    selectedBeneficiaryId: String?,
    onVisitClick: (String) -> Unit,
    onViewAllPendingClick: () -> Unit,
    onBeneficiarySelected: (String) -> Unit,
    onClearBeneficiaryFilter: () -> Unit,
    onTodayVisitsClick: () -> Unit = {},
    onAnalyticsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Beneficiary Selector
        BeneficiarySelector(
            beneficiaries = beneficiaries,
            selectedBeneficiaryId = selectedBeneficiaryId,
            onBeneficiarySelected = onBeneficiarySelected,
            onClearFilter = onClearBeneficiaryFilter
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = onTodayVisitsClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Today's Visitors", style = MaterialTheme.typography.labelSmall)
            }
            androidx.compose.material3.OutlinedButton(
                onClick = onAnalyticsClick,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Analytics", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pending Requests Section
        SectionHeader(
            title = "Pending Requests",
            actionText = "View All",
            onAction = onViewAllPendingClick
        )
        
        if (pendingRequests.isEmpty()) {
            EmptyStateCard(
                message = "No pending visit requests",
                icon = Icons.Default.CheckCircle,
                actionText = "Refresh",
                onAction = {} // Handled by pull to refresh
            )
        } else {
            pendingRequests.take(3).forEach { request ->
                PendingRequestCard(
                    visit = request,
                    onApprove = { }, // Logic in VM via onVisitClick or separate
                    onDeny = { },
                    onClick = { onVisitClick(request.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Visits
        SectionHeader(title = "Today's Visits")
        if (todayVisits.isEmpty()) {
            Text(
                "No visits scheduled for today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            todayVisits.forEach { visit ->
                CompactVisitCard(
                    visit = visit,
                    onClick = { onVisitClick(visit.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Visitor dashboard content showing their scheduled visits and beneficiaries.
 */
@Composable
fun VisitorDashboardContent(
    nextVisit: Visit?,
    upcomingVisits: List<Visit>,
    suggestedSlots: List<TimeSlot> = emptyList(),
    beneficiary: Beneficiary?,
    onVisitClick: (String) -> Unit,
    onViewCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Next visit highlight
        nextVisit?.let { visit ->
            NextVisitCard(
                visit = visit,
                beneficiaryName = beneficiary?.fullName ?: "Your loved one",
                onClick = { onVisitClick(visit.id) }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Suggested Slots Section
        if (suggestedSlots.isNotEmpty()) {
            SectionHeader(
                title = "Suggested Slots",
                actionText = "View All",
                onAction = onViewCalendarClick
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                items(suggestedSlots) { slot ->
                    SuggestedSlotCard(
                        slot = slot,
                        onClick = { onViewCalendarClick() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        SectionHeader(
            title = "Upcoming Visits",
            actionText = if (upcomingVisits.size > 3) "View Calendar" else null,
            onAction = onViewCalendarClick
        )

        if (upcomingVisits.isEmpty()) {
            EmptyStateCard(
                message = "No upcoming visits scheduled",
                icon = Icons.AutoMirrored.Filled.EventNote,
                actionText = "Schedule a Visit",
                onAction = onViewCalendarClick
            )
        } else {
            upcomingVisits.take(3).forEach { visit ->
                CompactVisitCard(
                    visit = visit,
                    onClick = { onVisitClick(visit.id) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SuggestedSlotCard(
    slot: TimeSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.width(160.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "${slot.startTime.hour}:${slot.startTime.minute.toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${(slot.availabilityPercentage * 100).toInt()}% likely free",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        if (actionText != null) {
            TextButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    icon: ImageVector,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun NextVisitCard(
    visit: Visit,
    beneficiaryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Next Visit", style = MaterialTheme.typography.labelMedium)
            Text(beneficiaryName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("${visit.startTime} - ${visit.endTime}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CompactVisitCard(
    visit: Visit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(visit.scheduledDate.toString(), style = MaterialTheme.typography.bodySmall)
                Text("${visit.startTime} - ${visit.endTime}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun BeneficiarySelector(
    beneficiaries: List<Beneficiary>,
    selectedBeneficiaryId: String?,
    onBeneficiarySelected: (String) -> Unit,
    onClearFilter: () -> Unit
) {
    // Simplified selector
}

@Composable
fun PendingRequestCard(
    visit: Visit,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(visit.visitorName, fontWeight = FontWeight.Bold)
            Text("${visit.scheduledDate} ${visit.startTime}")
        }
    }
}
