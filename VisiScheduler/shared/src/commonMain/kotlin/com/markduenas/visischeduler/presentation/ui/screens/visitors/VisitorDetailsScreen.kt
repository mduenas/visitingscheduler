package com.markduenas.visischeduler.presentation.ui.screens.visitors

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
import com.markduenas.visischeduler.presentation.ui.components.visitors.AvatarSize
import com.markduenas.visischeduler.presentation.ui.components.visitors.VisitorAvatar
import com.markduenas.visischeduler.presentation.ui.components.visitors.RelationshipChip
import com.markduenas.visischeduler.presentation.viewmodel.visitors.VisitorDetailsViewModel
import org.koin.compose.koinInject
import kotlinx.datetime.*
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorDetailsScreen(
    visitorId: String,
    onNavigateBack: () -> Unit,
    onEditVisitor: (String) -> Unit,
    viewModel: VisitorDetailsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(visitorId) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visitor Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.canEdit) {
                        IconButton(onClick = { onEditVisitor(visitorId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (uiState.canBlock) {
                            val blockLabel = if (uiState.isBlocked) "Unblock Visitor" else "Block Visitor"
                            DropdownMenuItem(
                                text = { Text(blockLabel) },
                                onClick = {
                                    showMenu = false
                                    if (uiState.isBlocked) viewModel.unblockVisitor() else viewModel.showBlockDialog()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = null)
                                }
                            )
                        }
                        if (uiState.canRemove) {
                            DropdownMenuItem(
                                text = { Text("Remove Visitor", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    viewModel.showRemoveDialog()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.visitor == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.visitor == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Visitor not found")
            }
        } else {
            val visitor = uiState.visitor!!
            
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            VisitorAvatar(
                                fullName = visitor.fullName,
                                size = AvatarSize.LARGE
                            )
                            Text(
                                text = visitor.fullName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RelationshipChip(relationship = visitor.metadata["relationship"] ?: "Visitor")
                                Badge(
                                    containerColor = when {
                                        uiState.isBlocked -> MaterialTheme.colorScheme.errorContainer
                                        uiState.isPending -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    }
                                ) {
                                    Text(
                                        text = uiState.visitorStatus,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Approval Actions
                if (uiState.isPending && uiState.canApprove) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Pending Approval", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("This visitor has requested access to the care circle.", style = MaterialTheme.typography.bodyMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.removeVisitor() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Deny")
                                    }
                                    Button(
                                        onClick = { viewModel.approveVisitor() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }

                // Contact Info
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Contact Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(visitor.email)
                            }
                            visitor.phoneNumber?.let { phone ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(phone)
                                }
                            }
                        }
                    }
                }

                // Visit Statistics
                item {
                    val stats = uiState.visitFrequency
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Visit Statistics",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stats.totalVisits.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Total Visits",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stats.upcomingVisits.toString(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Upcoming",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Last Visit")
                                Text(
                                    text = stats.lastVisitDate?.let { formatDate(it) } ?: "Never",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Recent Visits
                if (uiState.visitHistory.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(uiState.visitHistory.take(5)) { visit ->
                        Card(
                            onClick = { viewModel.onVisitClick(visit.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Event,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(formatDate(visit.scheduledDate))
                                        Text(
                                            text = visit.status.name.lowercase().capitalize(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Dialogs
    if (uiState.showBlockDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideBlockDialog() },
            title = { Text("Block Visitor?") },
            text = {
                Text("${uiState.visitor?.fullName} will no longer be able to request or schedule visits. You can unblock them later.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.blockVisitor() }
                ) {
                    Text("Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideBlockDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRemoveDialog() },
            title = { Text("Remove Visitor?") },
            text = {
                Text("This will permanently remove ${uiState.visitor?.fullName} from your visitor list. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.removeVisitor() }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRemoveDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helpers
private fun formatDate(date: LocalDate): String {
    return \"${date.month} ${date.dayOfMonth}, ${date.year}\"
}

private fun formatDate(instant: Instant): String {
    // simplified
    return instant.toString().take(10)
}

private fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
