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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyType
import com.example.data.model.EnrichedFlightOffer
import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.pricing.MarkupEngine

@Composable
fun FlightOfferCard(
    offer: EnrichedFlightOffer,
    onSelectOffer: (EnrichedFlightOffer) -> Unit,
    onInspectAircraft: (EnrichedFlightOffer) -> Unit,
    isBestFareOffer: Boolean = false,
    selectedCurrency: com.example.domain.pricing.DisplayCurrency = com.example.domain.pricing.DisplayCurrency.EUR,
    modifier: Modifier = Modifier
) {
    val isWidebody = offer.aircraftSpec.bodyType == BodyType.WIDEBODY
    val isDirect = offer.isDirectFlight
    val emerald = MaterialTheme.colorScheme.tertiary
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val badgeColor = if (isDirect) emerald else champagneGold
    val formattedDateBadge = remember(offer.departureIso) {
        com.example.data.model.CountryPassportCatalog.formatStampDate(offer.departureIso.take(10))
    }

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
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isBestFareOffer) 1.8.dp else 1.dp,
                brush = Brush.horizontalGradient(
                    colors = if (isBestFareOffer) {
                        listOf(emerald, champagneGold, emerald)
                    } else {
                        listOf(
                            champagneGold.copy(alpha = 0.75f),
                            badgeColor.copy(alpha = 0.4f),
                            champagneGold.copy(alpha = 0.65f)
                        )
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onSelectOffer(offer) }
            .testTag("flight_offer_card_${offer.flightNumber}"),
        shape = RoundedCornerShape(20.dp),
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 0. Faixa de Data do Voo + Destaque "★ MELHOR TARIFA" no voo mais barato
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = champagneGold.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "📅 PARTIDA: $formattedDateBadge",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = champagneGold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                if (isBestFareOffer) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            text = "★ MELHOR TARIFA DISPONÍVEL",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF04130D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 1. Cabeçalho Horizontal: Companhia + Código à esquerda (weight 1f) e Preço Total à direita (sem quebra)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        emblemAccent.copy(alpha = 0.32f),
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .border(1.dp, emblemAccent.copy(alpha = 0.8f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = airlineIdentity.emblemMonogram,
                            style = MaterialTheme.typography.labelLarge,
                            color = champagneGold,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = offer.airlineName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${offer.flightNumber} • ${offer.cabinClass.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = champagneGold.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = airlineIdentity.allianceName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = champagneGold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = selectedCurrency.format(offer.pricing.displayFareBeforeAncillariesEur),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp,
                        color = if (isBestFareOffer) Color(0xFF10B981) else champagneGold,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = emerald,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "-${airlineIdentity.co2ReductionPercent}% CO₂",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = emerald,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 2. Linha Dedicada de Horários (Partida ➔ Duração/Escalas ➔ Chegada) com espaço amplo
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coluna Partida
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = offer.departureTimeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = offer.originIata,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = champagneGold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = offer.originCity,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Coluna Central: Duração + Rota + Badge Direto/Escalas
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = offer.durationFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(1.5.dp)
                                    .background(badgeColor.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (isDirect) {
                                Icon(
                                    imageVector = Icons.Default.FlightTakeoff,
                                    contentDescription = "Voo Direto",
                                    tint = emerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(offer.stopsCount.coerceAtMost(3)) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(champagneGold)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(1.5.dp)
                                    .background(badgeColor.copy(alpha = 0.6f))
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeColor.copy(alpha = 0.16f),
                            modifier = Modifier
                                .border(1.dp, badgeColor.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .testTag("flight_stops_badge_${offer.flightNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDirect) Icons.Default.FlightTakeoff else Icons.Default.ConnectingAirports,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = offer.stopsBadgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("flight_connection_time_${offer.flightNumber}")
                        ) {
                            if (!isDirect) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = aeroCyan,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = offer.connectionTimeBadgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isDirect) emerald else aeroCyan,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Coluna Chegada
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = offer.arrivalTimeFormatted,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = offer.destinationIata,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = champagneGold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = offer.destinationCity,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }

            // 3. Linha de Transparência Tarifária (Base + Taxa de Gestão em linha limpa)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Base ${MarkupEngine.formatEur(offer.netFareEur)} + Taxa Gestão ${MarkupEngine.formatEur(offer.pricing.totalMarkupEur)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = aeroCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wi-Fi a bordo",
                        tint = aeroCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = Icons.Default.Luggage,
                        contentDescription = "Bagagem incluída",
                        tint = champagneGold,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

            // 4. Aeronave & Amenidades Compactas
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
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AirlineSeatFlat,
                            contentDescription = "Aeronave",
                            tint = champagneGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${offer.aircraftSpec.fullName} • ${offer.aircraftSpec.cabinLayoutBusiness}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

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
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Text(
                text = "${offer.includedBaggageSummary} • ${offer.cancellationPolicySummary}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
