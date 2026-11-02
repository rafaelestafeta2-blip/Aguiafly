package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PassengerComposition
import com.example.data.model.TripType
import com.example.data.model.VipClubStatus
import com.example.domain.pricing.MarkupEngine

@Composable
fun VipMemberHeaderCard(
    vipStatus: VipClubStatus,
    modifier: Modifier = Modifier
) {
    val gold = MaterialTheme.colorScheme.primary
    val sapphireTop = MaterialTheme.colorScheme.surfaceVariant
    val sapphireBottom = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        sapphireTop,
                        sapphireBottom,
                        gold.copy(alpha = 0.16f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(gold.copy(alpha = 0.8f), gold.copy(alpha = 0.25f), gold.copy(alpha = 0.7f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
            .testTag("vip_member_header_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(gold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "VIP Club",
                            tint = gold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = vipStatus.tierName,
                            style = MaterialTheme.typography.titleMedium,
                            color = gold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = vipStatus.perksSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = gold.copy(alpha = 0.18f),
                    modifier = Modifier.border(1.dp, gold.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = "${vipStatus.totalPoints} PTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = gold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = gold.copy(alpha = 0.22f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cashback Disponível: ${MarkupEngine.formatEur(vipStatus.cashbackBalanceEur)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Esta rota acumula +${vipStatus.nextFlightPointsEstimate} milhas VIP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TripAndPassengerSelectorBar(
    tripType: TripType,
    passengers: PassengerComposition,
    onSelectTripType: (TripType) -> Unit,
    onUpdatePassengers: (PassengerComposition) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandPassengers by remember { mutableStateOf(false) }
    val gold = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripType.entries.forEach { option ->
                    val selected = tripType == option
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectTripType(option) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (option == TripType.ROUND_TRIP) Icons.Default.SyncAlt else Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            Text(
                                text = option.label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = gold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("trip_type_chip_${option.name.lowercase()}")
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, gold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable { expandPassengers = !expandPassengers }
                    .testTag("passenger_selector_toggle")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = "Passageiros",
                        tint = gold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = passengers.summaryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        AnimatedVisibility(visible = expandPassengers) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .testTag("passenger_selector_panel")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PassengerCounterRow(
                        title = "Adultos (12+ anos)",
                        subtitle = "Tarifa integral NDC",
                        count = passengers.adults,
                        minCount = 1,
                        maxCount = 6,
                        onChange = { onUpdatePassengers(passengers.copy(adults = it)) }
                    )
                    PassengerCounterRow(
                        title = "Crianças (2 a 11 anos)",
                        subtitle = "Tarifa reduzida 75% com assento",
                        count = passengers.children,
                        minCount = 0,
                        maxCount = 4,
                        onChange = { onUpdatePassengers(passengers.copy(children = it)) }
                    )
                    PassengerCounterRow(
                        title = "Bebés (< 2 anos)",
                        subtitle = "Taxa reduzida 15% ao colo",
                        count = passengers.infants,
                        minCount = 0,
                        maxCount = passengers.adults,
                        onChange = { onUpdatePassengers(passengers.copy(infants = it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PassengerCounterRow(
    title: String,
    subtitle: String,
    count: Int,
    minCount: Int,
    maxCount: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (count > minCount) onChange(count - 1) },
                enabled = count > minCount,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Diminuir")
            }
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
                onClick = { if (count < maxCount) onChange(count + 1) },
                enabled = count < maxCount,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aumentar")
            }
        }
    }
}
