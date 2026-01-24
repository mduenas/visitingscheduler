package com.markduenas.visischeduler.presentation.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.presentation.ui.components.settings.AvatarPicker
import com.markduenas.visischeduler.presentation.ui.components.settings.AvatarSource
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileUiState
import com.markduenas.visischeduler.presentation.viewmodel.settings.ProfileViewModel

/**
 * Profile screen displaying user information and quick actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refreshProfile() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ProfileContent(
                uiState = uiState,
                onEditProfile = onNavigateToEditProfile,
                onAvatarSourceSelected = { source ->
                    // TODO: Handle camera/gallery selection
                },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToNotifications = onNavigateToNotifications,
                onLogout = { viewModel.logout() }
            )
        }
    }
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onEditProfile: () -> Unit,
    onAvatarSourceSelected: (AvatarSource) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    if (uiState.isLoading && uiState.user == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val user = uiState.user ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar section
        AvatarPicker(
            imageUrl = uiState.avatarUri,
            onSourceSelected = onAvatarSourceSelected,
            size = 120.dp,
            isEditable = !uiState.isUploadingAvatar,
            isUploading = uiState.isUploadingAvatar,
            initials = "${user.firstName.firstOrNull() ?: ""}${user.lastName.firstOrNull() ?: ""}"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = user.fullName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Role badge
        RoleBadge(role = user.role)

        Spacer(modifier = Modifier.height(16.dp))

        // Contact info
        ContactInfoSection(
            email = user.email,
            phone = user.phoneNumber,
            isEmailVerified = user.isEmailVerified
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Edit profile button
        OutlinedButton(
            onClick = onEditProfile,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats section
        StatsSection(
            stats = uiState.visitStats,
            isCoordinator = user.canApproveVisits()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick actions
        QuickActionsSection(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToNotifications = onNavigateToNotifications
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Logout button
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Role badge component.
 */
@Composable
private fun RoleBadge(
    role: Role,
    modifier: Modifier = Modifier
) {
    val (label, color) = when (role) {
        Role.ADMIN -> "Administrator" to MaterialTheme.colorScheme.error
        Role.PRIMARY_COORDINATOR -> "Primary Coordinator" to MaterialTheme.colorScheme.primary
        Role.SECONDARY_COORDINATOR -> "Secondary Coordinator" to MaterialTheme.colorScheme.secondary
        Role.APPROVED_VISITOR -> "Approved Visitor" to MaterialTheme.colorScheme.tertiary
        Role.PENDING_VISITOR -> "Pending Approval" to MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Contact information section.
 */
@Composable
private fun ContactInfoSection(
    email: String,
    phone: String?,
    isEmailVerified: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Email
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isEmailVerified) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Phone
        if (phone != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Statistics section showing visit counts.
 */
@Composable
private fun StatsSection(
    stats: com.markduenas.visischeduler.presentation.viewmodel.settings.VisitStats,
    isCoordinator: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isCoordinator) {
                    StatItem(
                        icon = Icons.Default.CalendarMonth,
                        value = stats.totalVisitsCoordinated.toString(),
                        label = "Coordinated"
                    )
                    StatItem(
                        icon = Icons.Default.Pending,
                        value = stats.pendingApprovals.toString(),
                        label = "Pending"
                    )
                }

                StatItem(
                    icon = Icons.Default.CalendarMonth,
                    value = stats.totalVisitsMade.toString(),
                    label = "Visits Made"
                )

                StatItem(
                    icon = Icons.Default.Schedule,
                    value = stats.upcomingVisits.toString(),
                    label = "Upcoming"
                )
            }
        }
    }
}

/**
 * Individual stat item.
 */
@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Quick actions section.
 */
@Composable
private fun QuickActionsSection(
    onNavigateToSettings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Default.Settings,
                label = "Settings",
                onClick = onNavigateToSettings,
                modifier = Modifier.weight(1f)
            )

            QuickActionCard(
                icon = Icons.Default.Notifications,
                label = "Notifications",
                onClick = onNavigateToNotifications,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Quick action card.
 */
@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
