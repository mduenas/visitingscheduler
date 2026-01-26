package com.markduenas.visischeduler.presentation.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.Visit

/**
 * Coordinator dashboard content showing facility-wide statistics and pending approvals.
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Beneficiary filter
        if (beneficiaries.isNotEmpty()) {
            BeneficiaryFilterSection(
                beneficiaries = beneficiaries,
                selectedBeneficiaryId = selectedBeneficiaryId,
                onBeneficiarySelected = onBeneficiarySelected,
                onClearFilter = onClearBeneficiaryFilter
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pending Requests Section
        if (pendingRequests.isNotEmpty()) {
            SectionHeader(
                title = "Pending Requests",
                actionText = "View All",
                onAction = onViewAllPendingClick
            )
            Spacer(modifier = Modifier.height(8.dp))

            pendingRequests.take(3).forEach { visit ->
                PendingRequestCard(
                    visit = visit,
                    visitorName = "Visitor", // Would need to be passed in
                    beneficiaryName = beneficiaries.find { it.id == visit.beneficiaryId }?.fullName ?: "Unknown",
                    onApprove = { /* Handle approve */ },
                    onDeny = { /* Handle deny */ },
                    onClick = { onVisitClick(visit.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Today's Visits Section
        if (todayVisits.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "Today's Visits",
                actionText = if (todayVisits.size > 3) "View All" else null,
                onAction = { /* Navigate to today's visits */ }
            )
            Spacer(modifier = Modifier.height(8.dp))

            todayVisits.take(5).forEach { visit ->
                VisitCard(
                    visit = visit,
                    visitorName = "Visitor",
                    beneficiaryName = beneficiaries.find { it.id == visit.beneficiaryId }?.fullName ?: "Unknown",
                    onClick = { onVisitClick(visit.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Empty state
        if (pendingRequests.isEmpty() && todayVisits.isEmpty()) {
            EmptyDashboardState(
                message = "No pending requests or visits today",
                icon = Icons.Default.EventAvailable
            )
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

        // Upcoming Visits Section
        SectionHeader(
            title = "Upcoming Visits",
            actionText = if (upcomingVisits.size > 3) "View Calendar" else null,
            onAction = onViewCalendarClick
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (upcomingVisits.isEmpty()) {
            EmptyStateCard(
                message = "No upcoming visits scheduled",
                actionText = "Schedule Now",
                onAction = onViewCalendarClick
            )
        } else {
            upcomingVisits.take(3).forEach { visit ->
                CompactVisitCard(
                    visit = visit,
                    visitorName = "You",
                    onClick = { onVisitClick(visit.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Next visit highlight card for visitors.
 */
@Composable
private fun NextVisitCard(
    visit: Visit,
    beneficiaryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Next Visit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = beneficiaryName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${visit.scheduledDate.monthNumber}/${visit.scheduledDate.dayOfMonth}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${visit.startTime.hour}:${visit.startTime.minute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Beneficiary filter chips.
 */
@Composable
private fun BeneficiaryFilterSection(
    beneficiaries: List<Beneficiary>,
    selectedBeneficiaryId: String?,
    onBeneficiarySelected: (String) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Filter by Resident",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedBeneficiaryId == null,
                    onClick = onClearFilter,
                    label = { Text("All") }
                )
            }
            items(beneficiaries) { beneficiary ->
                FilterChip(
                    selected = beneficiary.id == selectedBeneficiaryId,
                    onClick = { onBeneficiarySelected(beneficiary.id) },
                    label = { Text(beneficiary.fullName) }
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
private fun EmptyDashboardState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) {
                Text(actionText)
            }
        }
    }
}
