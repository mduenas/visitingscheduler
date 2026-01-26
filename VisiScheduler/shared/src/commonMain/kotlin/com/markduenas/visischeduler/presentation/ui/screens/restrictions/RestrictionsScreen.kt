package com.markduenas.visischeduler.presentation.ui.screens.restrictions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.presentation.ui.components.visitors.RestrictionCard
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    onNavigateBack: () -> Unit,
    onAddRestriction: () -> Unit,
    onEditRestriction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = Clock.System.now()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Mock data using domain Restriction entity
    val restrictions = remember {
        mutableStateListOf(
            Restriction(
                id = "1",
                name = "Visiting Hours",
                description = "Visits allowed 9:00 AM - 8:00 PM daily",
                type = RestrictionType.TIME_BASED,
                scope = RestrictionScope.FACILITY_WIDE,
                isActive = true,
                effectiveFrom = today,
                createdBy = "system",
                createdAt = now,
                updatedAt = now
            ),
            Restriction(
                id = "2",
                name = "Meal Times",
                description = "No visits during 12:00 PM - 1:00 PM and 6:00 PM - 7:00 PM",
                type = RestrictionType.TIME_BASED,
                scope = RestrictionScope.FACILITY_WIDE,
                isActive = true,
                effectiveFrom = today,
                createdBy = "system",
                createdAt = now,
                updatedAt = now
            ),
            Restriction(
                id = "3",
                name = "Rest Period",
                description = "No visits 2:00 PM - 3:00 PM (afternoon rest)",
                type = RestrictionType.TIME_BASED,
                scope = RestrictionScope.FACILITY_WIDE,
                isActive = true,
                effectiveFrom = today,
                createdBy = "system",
                createdAt = now,
                updatedAt = now
            ),
            Restriction(
                id = "4",
                name = "Maximum Visitors",
                description = "Maximum 3 visitors at the same time",
                type = RestrictionType.CAPACITY_BASED,
                scope = RestrictionScope.FACILITY_WIDE,
                isActive = true,
                effectiveFrom = today,
                createdBy = "system",
                createdAt = now,
                updatedAt = now
            ),
            Restriction(
                id = "5",
                name = "Daily Limit",
                description = "Maximum 6 visits per day",
                type = RestrictionType.CAPACITY_BASED,
                scope = RestrictionScope.FACILITY_WIDE,
                isActive = true,
                effectiveFrom = today,
                createdBy = "system",
                createdAt = now,
                updatedAt = now
            ),
            Restriction(
                id = "6",
                name = "No Children",
                description = "Visitors must be 12 years or older",
                type = RestrictionType.VISITOR_BASED,
                scope = RestrictionScope.FACILITY_WIDE,
                isActive = false,
                effectiveFrom = today,
                createdBy = "system",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    fun toggleRestriction(id: String, enabled: Boolean) {
        val index = restrictions.indexOfFirst { it.id == id }
        if (index >= 0) {
            restrictions[index] = restrictions[index].copy(isActive = enabled)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restrictions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRestriction) {
                Icon(Icons.Default.Add, contentDescription = "Add restriction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Time-based restrictions
            item {
                Text(
                    text = "Time-Based",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(restrictions.filter { it.type == RestrictionType.TIME_BASED }) { restriction ->
                RestrictionCard(
                    restriction = restriction,
                    onToggle = { enabled -> toggleRestriction(restriction.id, enabled) },
                    onClick = { onEditRestriction(restriction.id) }
                )
            }

            // Capacity restrictions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Capacity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(restrictions.filter { it.type == RestrictionType.CAPACITY_BASED }) { restriction ->
                RestrictionCard(
                    restriction = restriction,
                    onToggle = { enabled -> toggleRestriction(restriction.id, enabled) },
                    onClick = { onEditRestriction(restriction.id) }
                )
            }

            // Visitor restrictions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Visitor-Based",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(restrictions.filter { it.type == RestrictionType.VISITOR_BASED }) { restriction ->
                RestrictionCard(
                    restriction = restriction,
                    onToggle = { enabled -> toggleRestriction(restriction.id, enabled) },
                    onClick = { onEditRestriction(restriction.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
