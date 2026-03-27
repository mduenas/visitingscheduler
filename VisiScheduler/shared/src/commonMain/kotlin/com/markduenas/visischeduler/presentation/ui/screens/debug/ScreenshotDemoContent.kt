package com.markduenas.visischeduler.presentation.ui.screens.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenshotDemoContent(
    scenarioId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (scenarioId) {
        "dashboard" -> DashboardDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        "visit_details" -> VisitDetailsDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        "schedule_visit" -> ScheduleVisitDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        "visitor_list" -> VisitorListDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        "check_in" -> CheckInDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        "calendar" -> CalendarDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        "settings" -> SettingsDemo(onNavigateBack = onNavigateBack, modifier = modifier)
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Unknown Demo") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Unknown scenario: $scenarioId")
                }
            }
        }
    }
}

// ==================== Dashboard ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "KindVisit",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        BadgedBox(badge = {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError, fontSize = 9.sp)
                            }
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("6", "Upcoming"),
                    Pair("2", "Pending"),
                    Pair("1", "Today")
                ).forEach { (number, label) ->
                    ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = number,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Today's Visits section
            Text(
                text = "Today's Visits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            DashboardVisitCard(
                name = "James Mitchell",
                timeRange = "2:00 PM – 4:00 PM",
                statusLabel = "APPROVED",
                statusColor = Color(0xFF2E7D32),
                statusContainerColor = Color(0xFFE8F5E9)
            )

            DashboardVisitCard(
                name = "Sarah Chen",
                timeRange = "10:00 AM – 11:30 AM",
                statusLabel = "PENDING",
                statusColor = Color(0xFFE65100),
                statusContainerColor = Color(0xFFFFF3E0)
            )

            // Recent Activity section
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Robert Kim checked in at 9:15 AM",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardVisitCard(
    name: String,
    timeRange: String,
    statusLabel: String,
    statusColor: Color,
    statusContainerColor: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = timeRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                modifier = Modifier
                    .background(statusContainerColor, MaterialTheme.shapes.small)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(text = statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==================== Visit Details ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitDetailsDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visit Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visit ID + Status chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visit #A3F9B2C1",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = {},
                    label = { Text("APPROVED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE8F5E9),
                        labelColor = Color(0xFF2E7D32)
                    )
                )
            }

            // Date and Time card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Wednesday, March 25, 2026", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("2:00 PM – 4:00 PM", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Visitor card
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("James Mitchell", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Primary Visitor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AssistChip(
                            onClick = {},
                            label = { Text("Approved Visitor", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFE8F5E9), labelColor = Color(0xFF2E7D32))
                        )
                    }
                }
            }

            // Notes card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Family visit - please have wheelchair accessible room", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Visit")
                }
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Check In")
                }
            }
        }
    }
}

// ==================== Schedule Visit ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleVisitDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Visit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Select Date
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Date", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf("Mon 23", "Tue 24", "Wed 25", "Thu 26", "Fri 27", "Sat 28", "Sun 29")
                    days.forEachIndexed { index, day ->
                        FilterChip(
                            selected = index == 2,
                            onClick = {},
                            label = { Text(day, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Select Time
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Pair("10:00 AM", false), Pair("2:00 PM", true), Pair("4:00 PM", false)).forEach { (time, selected) ->
                        FilterChip(selected = selected, onClick = {}, label = { Text(time) })
                    }
                }
            }

            // Duration
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Pair("30 min", false), Pair("1 hour", true), Pair("2 hours", false)).forEach { (dur, selected) ->
                        FilterChip(selected = selected, onClick = {}, label = { Text(dur) })
                    }
                }
            }

            // Visit Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Visit Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Pair("In Person", true), Pair("Video Call", false)).forEach { (type, selected) ->
                        FilterChip(selected = selected, onClick = {}, label = { Text(type) })
                    }
                }
            }

            // Reason
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Reason (Optional)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = "Monthly family visit",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Visit")
            }
        }
    }
}

// ==================== Visitor List ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitorListDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    data class VisitorEntry(
        val name: String,
        val email: String,
        val role: String,
        val chipContainerColor: Color
    )

    val visitors = listOf(
        VisitorEntry("James Mitchell", "james.m@email.com", "Primary Visitor", Color(0xFFE8F5E9)),
        VisitorEntry("Sarah Chen", "sarah.chen@email.com", "Approved Visitor", Color(0xFFE3F2FD)),
        VisitorEntry("Robert Kim", "r.kim@email.com", "Approved Visitor", Color(0xFFE3F2FD)),
        VisitorEntry("Emily Torres", "e.torres@email.com", "Pending Approval", Color(0xFFFFF8E1)),
        VisitorEntry("David Park", "d.park@email.com", "Approved Visitor", Color(0xFFE3F2FD))
    )

    val chipTextColors = mapOf(
        Color(0xFFE8F5E9) to Color(0xFF2E7D32),
        Color(0xFFE3F2FD) to Color(0xFF1565C0),
        Color(0xFFFFF8E1) to Color(0xFFE65100)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Visitors") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "Add Visitor")
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(visitors.size) { index ->
                val visitor = visitors[index]
                val initials = visitor.name.split(" ").take(2).mapNotNull { it.firstOrNull()?.toString() }.joinToString("")
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(visitor.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(visitor.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        SuggestionChip(
                            onClick = {},
                            label = { Text(visitor.role, style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = visitor.chipContainerColor,
                                labelColor = chipTextColors[visitor.chipContainerColor] ?: MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==================== Check-In ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckInDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Visitor info card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "JM",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text("James Mitchell", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Visit #A3F9B2C1", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Today  2:00 PM – 4:00 PM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scan QR button
            OutlinedButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR Code")
            }

            // Manual Check-In button
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manual Check-In")
            }

            HorizontalDivider()

            Text(
                text = "Or verify visitor badge",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== Calendar ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    // March 2026: starts on Sunday (day index 0), 31 days
    val dayOfWeekHeaders = listOf("S", "M", "T", "W", "T", "F", "S")
    // March 1, 2026 is a Sunday, so startOffset = 0
    val startOffset = 0
    val daysInMonth = 31
    val today = 25

    // Dots: date -> list of colors
    val dots = mapOf(
        18 to listOf(Color(0xFF2E7D32)),
        20 to listOf(Color(0xFFE65100)),
        25 to listOf(Color(0xFF2E7D32), Color(0xFFE65100)),
        28 to listOf(Color(0xFF2E7D32))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("March 2026") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Day of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dayOfWeekHeaders.forEach { header ->
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Calendar grid: 6 rows to cover all days
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - startOffset + 1
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day in 1..daysInMonth) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = if (day == today) {
                                            Modifier
                                                .size(28.dp)
                                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        } else {
                                            Modifier.size(28.dp)
                                        },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (day == today) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    // Dots
                                    val dayDots = dots[day]
                                    if (dayDots != null) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            dayDots.forEach { dotColor ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(dotColor, CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== Settings ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDemo(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            // Profile card
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AC",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Alex Coordinator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Primary Coordinator", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text("alex@kindvisit.org", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Preferences section
            item {
                Text("Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Notifications") },
                            trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Theme") },
                            trailingContent = { Text("System", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Language") },
                            trailingContent = { Text("English", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    }
                }
            }

            // Security section
            item {
                Text("Security", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Biometric Login") },
                            trailingContent = { Switch(checked = false, onCheckedChange = {}) }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text("Two-Factor Auth") },
                            trailingContent = { Switch(checked = true, onCheckedChange = {}) }
                        )
                        HorizontalDivider()
                        ListItem(headlineContent = { Text("Change Password") })
                    }
                }
            }

            // Support section
            item {
                Text("Support", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(headlineContent = { Text("About") })
                        HorizontalDivider()
                        ListItem(headlineContent = { Text("Contact Support") })
                        HorizontalDivider()
                        ListItem(headlineContent = { Text("Rate the App") })
                    }
                }
            }

            // Logout
            item {
                OutlinedButton(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
