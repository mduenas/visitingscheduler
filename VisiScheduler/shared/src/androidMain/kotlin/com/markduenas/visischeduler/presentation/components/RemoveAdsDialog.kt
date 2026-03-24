package com.markduenas.visischeduler.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markduenas.visischeduler.data.billing.BillingManager
import com.markduenas.visischeduler.data.billing.PurchaseState
import com.markduenas.visischeduler.domain.repository.AdRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Dialog for purchasing ad removal.
 *
 * @param onDismiss Called when the dialog is dismissed
 * @param onPurchaseClick Called when user initiates purchase
 * @param onRestoreClick Called when user wants to restore purchases
 */
@Composable
fun RemoveAdsDialog(
    onDismiss: () -> Unit,
    billingManager: BillingManager = koinInject(),
    adRepository: AdRepository = koinInject()
) {
    val purchaseState by billingManager.purchaseState.collectAsState()
    val hasRemovedAds by billingManager.hasRemovedAds.collectAsState()
    val scope = rememberCoroutineScope()
    var price by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        price = billingManager.getAdRemovalPrice()
    }

    // Auto-dismiss when purchase is successful
    LaunchedEffect(hasRemovedAds) {
        if (hasRemovedAds) {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.RemoveCircleOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Remove Ads",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enjoy KindVisit without ads!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Benefits
                RemoveAdsBenefit(text = "No banner ads")
                RemoveAdsBenefit(text = "Cleaner interface")
                RemoveAdsBenefit(text = "One-time purchase")
                RemoveAdsBenefit(text = "Support app development")

                Spacer(modifier = Modifier.height(24.dp))

                // Price
                if (price != null) {
                    Text(
                        text = price!!,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                // Purchase state message
                when (purchaseState) {
                    is PurchaseState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (purchaseState as PurchaseState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is PurchaseState.Cancelled -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Purchase cancelled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        adRepository.purchaseAdRemoval()
                    }
                },
                enabled = price != null && purchaseState !is PurchaseState.Processing
            ) {
                if (purchaseState is PurchaseState.Processing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Purchase")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            adRepository.restorePurchase()
                            isLoading = false
                        }
                    }
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Restore")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun RemoveAdsBenefit(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Success dialog shown after successful purchase.
 */
@Composable
fun AdsRemovedSuccessDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Thank You!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Ads have been successfully removed. Thank you for supporting KindVisit!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Continue")
            }
        }
    )
}
