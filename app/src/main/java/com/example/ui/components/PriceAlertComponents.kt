package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PriceAlertSubscription
import com.example.domain.pricing.MarkupEngine

@Composable
fun CreatePriceAlertActionButton(
    originIata: String,
    destinationIata: String,
    activeAlertCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val champagneGold = MaterialTheme.colorScheme.primary

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("create_price_alert_button"),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.2.dp,
            brush = Brush.horizontalGradient(listOf(aeroCyan, champagneGold))
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = champagneGold
        )
    ) {
        Icon(
            imageVector = if (activeAlertCount > 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
            contentDescription = "Criar Alerta de Preço",
            tint = aeroCyan,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Criar Alerta de Preço ($originIata ➔ $destinationIata)",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ActivePriceAlertsBannerCard(
    alerts: List<PriceAlertSubscription>,
    onTriggerPromoSweepNow: () -> Unit,
    onSelectAlertRoute: (PriceAlertSubscription) -> Unit,
    onDeleteAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF132D59), Color(0xFF0C1F40))
                )
            )
            .border(1.dp, aeroCyan.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .testTag("active_price_alerts_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = champagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Radar de Promoções & Push FCM (${alerts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = champagneGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monitorização automática de tarifas em tempo real",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = aeroCyan.copy(alpha = 0.18f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, aeroCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .clickable { onTriggerPromoSweepNow() }
                        .testTag("run_promo_cron_now_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Simular varredura cron",
                            tint = aeroCyan,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Varredura Push",
                            style = MaterialTheme.typography.labelMedium,
                            color = aeroCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            alerts.forEachIndexed { idx, alert ->
                if (idx > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectAlertRoute(alert) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${alert.originIata} ➔ ${alert.destinationIata}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• ${alert.cabinClass.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = aeroCyan
                            )
                        }
                        Text(
                            text = "Meta Push: ≤ ${MarkupEngine.formatEur(alert.targetMaxPriceEur)} • Atual: ${MarkupEngine.formatEur(alert.lastObservedPriceEur)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (alert.promoTriggered) emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (alert.promoTriggered) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    IconButton(
                        onClick = { onDeleteAlert(alert.id) },
                        modifier = Modifier.testTag("delete_price_alert_${alert.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover alerta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
