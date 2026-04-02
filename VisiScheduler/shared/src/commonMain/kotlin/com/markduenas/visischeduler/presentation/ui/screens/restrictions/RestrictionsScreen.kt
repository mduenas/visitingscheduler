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
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.presentation.ui.components.visitors.RestrictionCard
import com.markduenas.visischeduler.presentation.viewmodel.visitors.RestrictionsViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionsScreen(
    onNavigateBack: () -> Unit,
    onAddRestriction: () -> Unit,
    onEditRestriction: (String) -> Unit,
    viewModel: RestrictionsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restrictions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
        if (uiState.isLoading && !uiState.hasRestrictions) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filters
                item {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = if (uiState.selectedType == null) 0 else RestrictionType.entries.indexOf(uiState.selectedType) + 1,
                        edgePadding = 0.dp,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = uiState.selectedType == null,
                            onClick = { viewModel.onFilterChange(null) },
                            text = { Text("All") }
                        )
                        RestrictionType.entries.forEach { type ->
                            Tab(
                                selected = uiState.selectedType == type,
                                onClick = { viewModel.onFilterChange(type) },
                                text = { Text(viewModel.getRestrictionTypeDisplayName(type)) }
                            )
                        }
                    }
                }

                if (!uiState.hasRestrictions) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No restrictions found", style = MaterialTheme.typography.titleMedium)
                                Text("Create one to start managing visits", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Grouped display if no specific filter
                if (uiState.selectedType == null) {
                    renderGroup(
                        title = "Time-Based",
                        items = uiState.restrictions.timeBasedRestrictions,
                        viewModel = viewModel,
                        onEdit = onEditRestriction
                    )
                    renderGroup(
                        title = "Capacity",
                        items = uiState.restrictions.capacityBasedRestrictions,
                        viewModel = viewModel,
                        onEdit = onEditRestriction
                    )
                    renderGroup(
                        title = "Visitor-Based",
                        items = uiState.restrictions.visitorBasedRestrictions,
                        viewModel = viewModel,
                        onEdit = onEditRestriction
                    )
                    renderGroup(
                        title = "Beneficiary-Specific",
                        items = uiState.restrictions.beneficiaryBasedRestrictions,
                        viewModel = viewModel,
                        onEdit = onEditRestriction
                    )
                } else {
                    // Filtered list
                    items(uiState.filteredRestrictions) { restriction ->
                        RestrictionCard(
                            restriction = restriction,
                            onToggle = { viewModel.toggleRestriction(restriction) },
                            onClick = { onEditRestriction(restriction.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Delete Confirmation
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Delete Restriction") },
            text = { Text("Are you sure you want to delete '${uiState.restrictionToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRestriction() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.renderGroup(
    title: String,
    items: List<Restriction>,
    viewModel: RestrictionsViewModel,
    onEdit: (String) -> Unit
) {
    if (items.isNotEmpty()) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        items(items) { restriction ->
            RestrictionCard(
                restriction = restriction,
                onToggle = { viewModel.toggleRestriction(restriction) },
                onClick = { onEdit(restriction.id) }
            )
        }
    }
}
