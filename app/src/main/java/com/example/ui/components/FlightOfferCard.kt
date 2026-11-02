package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.ConnectingAirports
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BodyType
import com.example.data.model.EnrichedFlightOffer
import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.pricing.MarkupEngine

@Composable
fun FlightOfferCard(
    offer: EnrichedFlightOffer,
    onSelectOffer: (EnrichedFlightOffer) -> Unit,
    onInspectAircraft: (EnrichedFlightOffer) -> Unit,
    modifier: Modifier = Modifier
) {
    val isWidebody = offer.aircraftSpec.bodyType == BodyType.WIDEBODY
    val isDirect = offer.isDirectFlight
    val emerald = MaterialTheme.colorScheme.tertiary
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val badgeColor = if (isDirect) emerald else champagneGold

    val airlineIdentity = remember(offer.airlineIata, offer.cabinClass, isWidebody) {
        AirportAndLoyaltyCatalog.resolveAirlineIdentity(
            airlineIata = offer.airlineIata,
            cabinClass = offer.cabinClass,
            isWidebody = isWidebody
        )
    }
    val emblemAccent = Color(airlineIdentity.badgeAccentHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        champagneGold.copy(alpha = 0.75f),
                        badgeColor.copy(alpha = 0.4f),
                        champagneGold.copy(alpha = 0.65f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable { onSelectOffer(offer) }
            .testTag("flight_offer_card_${offer.flightNumber}"),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        emblemAccent.copy(alpha = 0.32f),
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .border(1.dp, emblemAccent.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = airlineIdentity.emblemMonogram,
                            style = MaterialTheme.typography.labelLarge,
                            color = champagneGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = offer.airlineName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = champagneGold.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = airlineIdentity.allianceName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = champagneGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${offer.flightNumber} • ${offer.cabinClass.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = null,
                                tint = emerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "-${airlineIdentity.co2ReductionPercent}% CO₂",
                                style = MaterialTheme.typography.labelSmall,
                                color = emerald,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MarkupEngine.formatEur(offer.pricing.displayFareBeforeAncillariesEur),
                        style = MaterialTheme.typography.titleLarge,
                        color = champagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Base ${MarkupEngine.formatEur(offer.netFareEur)} + Taxa Gestão ${MarkupEngine.formatEur(offer.pricing.totalMarkupEur)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = aeroCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = offer.departureTimeFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${offer.originIata} • ${offer.originCity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Tempo total: ${offer.durationFormatted}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Visual Route Line with Stopover Markers
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(1.5.dp)
                                .background(badgeColor.copy(alpha = 0.6f))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        if (isDirect) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = "Voo Direto",
                                tint = emerald,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(offer.stopsCount.coerceAtMost(3)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(champagneGold)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(1.5.dp)
                                .background(badgeColor.copy(alpha = 0.6f))
                        )
                    }

                    // Prominent Badge indicating "Direto" or "X Escala(s)"
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.16f),
                        modifier = Modifier
                            .border(1.dp, badgeColor.copy(alpha = 0.65f), RoundedCornerShape(50))
                            .testTag("flight_stops_badge_${offer.flightNumber}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isDirect) Icons.Default.FlightTakeoff else Icons.Default.ConnectingAirports,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = offer.stopsBadgeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = badgeColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Total Connection Time label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("flight_connection_time_${offer.flightNumber}")
                    ) {
                        if (!isDirect) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = aeroCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = offer.connectionTimeBadgeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDirect) emerald else aeroCyan,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = offer.arrivalTimeFormatted,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${offer.destinationIata} • ${offer.destinationCity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onInspectAircraft(offer) }
                        .testTag("aircraft_spec_badge_${offer.flightNumber}"),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AirlineSeatFlat,
                            contentDescription = "Aeronave",
                            tint = champagneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${offer.aircraftSpec.fullName} • ${offer.aircraftSpec.cabinLayoutBusiness}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi a bordo",
                        tint = aeroCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Luggage,
                        contentDescription = "Bagagem incluída",
                        tint = champagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                airlineIdentity.cabinAmenities.forEach { amenity ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                        modifier = Modifier.border(
                            1.dp,
                            champagneGold.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            text = "✦ $amenity",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${offer.includedBaggageSummary} • ${offer.cancellationPolicySummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
