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
import com.markduenas.visischeduler.presentation.ui.components.visitors.VisitorAvatar
import com.markduenas.visischeduler.presentation.ui.components.visitors.RelationshipChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorDetailsScreen(
    visitorId: String,
    onNavigateBack: () -> Unit,
    onEditVisitor: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBlockDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    // Mock data
    val visitorName = "John Smith"
    val visitorInitials = "JS"
    val relationship = "Son"
    val email = "john.smith@email.com"
    val phone = "+1 (555) 123-4567"
    val accessLevel = "Auto-approve"
    val totalVisits = 24
    val lastVisit = "January 20, 2026"

    val recentVisits = listOf(
        "Jan 20, 2026 - 2:00 PM (1 hour)",
        "Jan 13, 2026 - 3:00 PM (45 min)",
        "Jan 6, 2026 - 2:30 PM (1 hour)",
        "Dec 30, 2025 - 1:00 PM (30 min)",
        "Dec 23, 2025 - 2:00 PM (1 hour)"
    )

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
                    IconButton(onClick = { onEditVisitor(visitorId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Block Visitor") },
                            onClick = {
                                showMenu = false
                                showBlockDialog = true
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Block, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Remove Visitor", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showRemoveDialog = true
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
            )
        }
    ) { padding ->
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
                            initials = visitorInitials,
                            size = 80
                        )
                        Text(
                            text = visitorName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        RelationshipChip(relationship = relationship)
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
                            Text(email)
                        }
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

            // Access Level
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Permissions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Access Level")
                            AssistChip(
                                onClick = { },
                                label = { Text(accessLevel) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Visit Statistics
            item {
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
                                    text = totalVisits.toString(),
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
                                    text = "Weekly",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Frequency",
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
                                text = lastVisit,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Recent Visits
            item {
                Text(
                    text = "Recent Visits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(recentVisits) { visit ->
                Card(
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
                            Text(visit)
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Block Dialog
    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block Visitor?") },
            text = {
                Text("$visitorName will no longer be able to request or schedule visits. You can unblock them later.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        // TODO: Block visitor
                    }
                ) {
                    Text("Block", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Dialog
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Visitor?") },
            text = {
                Text("This will permanently remove $visitorName from your visitor list. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveDialog = false
                        onNavigateBack()
                        // TODO: Remove visitor
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
