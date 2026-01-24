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
import com.markduenas.visischeduler.presentation.ui.components.visitors.RestrictionCard

data class RestrictionItem(
    val id: String,
    val title: String,
    val description: String,
    val type: RestrictionType,
    val isEnabled: Boolean
)

enum class RestrictionType {
    TIME, VISITOR, CAPACITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    onNavigateBack: () -> Unit,
    onAddRestriction: () -> Unit,
    onEditRestriction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Mock data
    val restrictions = remember {
        mutableStateListOf(
            RestrictionItem(
                "1", "Visiting Hours",
                "Visits allowed 9:00 AM - 8:00 PM daily",
                RestrictionType.TIME, true
            ),
            RestrictionItem(
                "2", "Meal Times",
                "No visits during 12:00 PM - 1:00 PM and 6:00 PM - 7:00 PM",
                RestrictionType.TIME, true
            ),
            RestrictionItem(
                "3", "Rest Period",
                "No visits 2:00 PM - 3:00 PM (afternoon rest)",
                RestrictionType.TIME, true
            ),
            RestrictionItem(
                "4", "Maximum Visitors",
                "Maximum 3 visitors at the same time",
                RestrictionType.CAPACITY, true
            ),
            RestrictionItem(
                "5", "Daily Limit",
                "Maximum 6 visits per day",
                RestrictionType.CAPACITY, true
            ),
            RestrictionItem(
                "6", "No Children",
                "Visitors must be 12 years or older",
                RestrictionType.VISITOR, false
            )
        )
    }

    fun toggleRestriction(id: String) {
        val index = restrictions.indexOfFirst { it.id == id }
        if (index >= 0) {
            restrictions[index] = restrictions[index].copy(isEnabled = !restrictions[index].isEnabled)
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
            items(restrictions.filter { it.type == RestrictionType.TIME }) { restriction ->
                RestrictionCard(
                    title = restriction.title,
                    description = restriction.description,
                    isEnabled = restriction.isEnabled,
                    icon = Icons.Default.Schedule,
                    onToggle = { toggleRestriction(restriction.id) },
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
            items(restrictions.filter { it.type == RestrictionType.CAPACITY }) { restriction ->
                RestrictionCard(
                    title = restriction.title,
                    description = restriction.description,
                    isEnabled = restriction.isEnabled,
                    icon = Icons.Default.Groups,
                    onToggle = { toggleRestriction(restriction.id) },
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
            items(restrictions.filter { it.type == RestrictionType.VISITOR }) { restriction ->
                RestrictionCard(
                    title = restriction.title,
                    description = restriction.description,
                    isEnabled = restriction.isEnabled,
                    icon = Icons.Default.Person,
                    onToggle = { toggleRestriction(restriction.id) },
                    onClick = { onEditRestriction(restriction.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
