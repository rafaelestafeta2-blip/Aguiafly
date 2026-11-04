package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BodyType
import com.example.data.model.EnrichedFlightOffer
import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.pricing.MarkupEngine

/**
 * Ícone Oficial da Companhia Aérea (estilo eDreams):
 * Renderiza o emblema circular com as cores oficiais de cada companhia (TAP Portugal TP, Gol G3,
 * LATAM, Azul, Iberia, Vueling, Air France, Lufthansa, British Airways, etc.).
 */
@Composable
fun AirlineLogoBadge(
    airlineIata: String,
    airlineName: String = "",
    size: Dp = 30.dp,
    modifier: Modifier = Modifier
) {
    val code = airlineIata.trim().uppercase()
    Surface(
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier
            .size(size)
            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (code) {
                "TP" -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF84CC16), fontWeight = FontWeight.Black)) {
                                append("T")
                            }
                            withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Black)) {
                                append("A")
                            }
                            withStyle(SpanStyle(color = Color(0xFF15803D), fontWeight = FontWeight.Black)) {
                                append("P")
                            }
                        },
                        fontSize = (size.value * 0.34f).sp,
                        letterSpacing = (-0.8).sp,
                        maxLines = 1
                    )
                }

                "G3" -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFF97316), fontWeight = FontWeight.Black)) {
                                append("G")
                            }
                            withStyle(SpanStyle(color = Color(0xFFFB923C), fontWeight = FontWeight.Black)) {
                                append("O")
                            }
                            withStyle(SpanStyle(color = Color(0xFF64748B), fontWeight = FontWeight.Black)) {
                                append("L")
                            }
                        },
                        fontSize = (size.value * 0.33f).sp,
                        letterSpacing = (-0.6).sp,
                        maxLines = 1
                    )
                }

                "LA", "JJ" -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFE11D48), fontWeight = FontWeight.Black)) {
                                append("L")
                            }
                            withStyle(SpanStyle(color = Color(0xFF1E1B4B), fontWeight = FontWeight.Black)) {
                                append("A")
                            }
                        },
                        fontSize = (size.value * 0.38f).sp,
                        maxLines = 1
                    )
                }

                "AD" -> {
                    Text(
                        text = "AZUL",
                        color = Color(0xFF0284C7),
                        fontSize = (size.value * 0.26f).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }

                "IB", "I2" -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFFDC2626), fontWeight = FontWeight.Black)) {
                                append("I")
                            }
                            withStyle(SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Black)) {
                                append("B")
                            }
                        },
                        fontSize = (size.value * 0.38f).sp,
                        maxLines = 1
                    )
                }

                "VY" -> {
                    Box(
                        modifier = Modifier
                            .size(size * 0.82f)
                            .clip(CircleShape)
                            .background(Color(0xFFFACC15)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VY",
                            color = Color(0xFF1E293B),
                            fontSize = (size.value * 0.34f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                "AF" -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Black)) {
                                append("A")
                            }
                            withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Black)) {
                                append("F")
                            }
                        },
                        fontSize = (size.value * 0.38f).sp,
                        maxLines = 1
                    )
                }

                "LH" -> {
                    Box(
                        modifier = Modifier
                            .size(size * 0.82f)
                            .clip(CircleShape)
                            .background(Color(0xFF1E3A8A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LH",
                            color = Color(0xFFFBBF24),
                            fontSize = (size.value * 0.33f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                "UX" -> {
                    Text(
                        text = "UX",
                        color = Color(0xFF0284C7),
                        fontSize = (size.value * 0.36f).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }

                "KL" -> {
                    Text(
                        text = "KLM",
                        color = Color(0xFF00A1DE),
                        fontSize = (size.value * 0.28f).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }

                "BA" -> {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF1E3A8A), fontWeight = FontWeight.Black)) {
                                append("B")
                            }
                            withStyle(SpanStyle(color = Color(0xFFDC2626), fontWeight = FontWeight.Black)) {
                                append("A")
                            }
                        },
                        fontSize = (size.value * 0.38f).sp,
                        maxLines = 1
                    )
                }

                "FR" -> {
                    Box(
                        modifier = Modifier
                            .size(size * 0.82f)
                            .clip(CircleShape)
                            .background(Color(0xFF073590)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "FR",
                            color = Color(0xFFF1C933),
                            fontSize = (size.value * 0.33f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                "U2" -> {
                    Box(
                        modifier = Modifier
                            .size(size * 0.82f)
                            .clip(CircleShape)
                            .background(Color(0xFFFF6600)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "U2",
                            color = Color.White,
                            fontSize = (size.value * 0.33f).sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                else -> {
                    val fallbackText = code.take(2).ifBlank { airlineName.take(2).uppercase() }
                    Text(
                        text = fallbackText,
                        color = Color(0xFF005AA3),
                        fontSize = (size.value * 0.36f).sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

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
    val geniusGreen = Color(0xFF159A74)
    val edreamsBlue = Color(0xFF005AA3)

    val airlineIdentity = remember(offer.airlineIata, offer.cabinClass, isWidebody) {
        AirportAndLoyaltyCatalog.resolveAirlineIdentity(
            airlineIata = offer.airlineIata,
            cabinClass = offer.cabinClass,
            isWidebody = isWidebody
        )
    }

    val displayPriceEur = offer.pricing.displayFareBeforeAncillariesEur

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isBestFareOffer) 2.dp else 1.dp,
                color = if (isBestFareOffer) geniusGreen else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelectOffer(offer) }
            .testTag("flight_offer_card_${offer.flightNumber}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Faixa Superior Verde "Melhor Tarifa Disponível" apenas para o voo mais barato da busca real
            if (isBestFareOffer) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(geniusGreen)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Melhor Oferta",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "★ Menor tarifa da busca",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.92f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Corpo Principal dividido em 2 Colunas (Esquerda: Companhia + Horários | Direita: Preço Real)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // COLUNA ESQUERDA: Ícone da Companhia + Nome + Horários (16:55 --- 7 h 50 min / Directo ✈ ---> 21:45)
                Column(
                    modifier = Modifier
                        .weight(1.38f)
                        .padding(end = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Linha do Ícone da Companhia Aérea + Nome da Companhia
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AirlineLogoBadge(
                            airlineIata = offer.airlineIata,
                            airlineName = offer.airlineName,
                            size = 28.dp
                        )
                        Text(
                            text = offer.airlineName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF475569),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Linha de Horários: 16:55 (LIS) ➔ 7 h 50 min / Directo ✈ ➔ 21:45 (FOR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Partida
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = offer.departureTimeFormatted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = offer.originIata,
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Centro: Duração + Linha com Avião + "Directo" em verde
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = offer.durationFormatted,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFCBD5E1))
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.5.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                                if (!isDirect) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF59E0B))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(1.5.dp)
                                            .background(Color(0xFFCBD5E1))
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.AirplanemodeActive,
                                    contentDescription = null,
                                    tint = edreamsBlue,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .rotate(90f)
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = if (isDirect) "Directo" else offer.stopsBadgeLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDirect) geniusGreen else Color(0xFFD97706),
                                maxLines = 1,
                                modifier = Modifier.testTag("flight_stops_badge_${offer.flightNumber}")
                            )

                            if (!isDirect && offer.stopoverHubsSummary.isNotBlank()) {
                                Text(
                                    text = offer.stopoverHubsSummary,
                                    fontSize = 9.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("flight_connection_time_${offer.flightNumber}")
                                )
                            } else {
                                Box(modifier = Modifier.testTag("flight_connection_time_${offer.flightNumber}"))
                            }
                        }

                        // Chegada
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = offer.arrivalTimeFormatted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = offer.destinationIata,
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Chip da aeronave + bagagem real
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onInspectAircraft(offer) }
                                .testTag("aircraft_spec_badge_${offer.flightNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flight,
                                    contentDescription = "Aeronave",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${offer.flightNumber} • ${offer.aircraftSpec.iataCode}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Luggage,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Mão: ${offer.carryOnBaggageAllowance} • Desp.: ${offer.checkedBaggageAllowance}",
                                fontSize = 9.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (offer.airportChange || offer.hasCriticalLayoverAlert) {
                        val alertLabel = buildList {
                            if (offer.airportChange) add("⚠️ Troca de aeroporto na conexão")
                            if (!offer.criticalLayoverAlertSummary.isNullOrBlank()) add(offer.criticalLayoverAlertSummary)
                        }.joinToString(" • ")

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                                .testTag("flight_critical_alert_${offer.flightNumber}")
                        ) {
                            Text(
                                text = alertLabel,
                                fontSize = 10.sp,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // DIVISÓRIA VERTICAL TRACEJADA
                Canvas(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .padding(vertical = 4.dp)
                ) {
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }

                // COLUNA DIREITA: Preço Real + Detalhamento Transparente (Sem tags falsas de urgência)
                Column(
                    modifier = Modifier
                        .weight(0.92f)
                        .padding(start = 10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = selectedCurrency.format(displayPriceEur),
                        fontSize = 22.sp,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = "Tarifa NDC",
                        fontSize = 12.sp,
                        color = edreamsBlue,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "por passageiro\nBase ${MarkupEngine.formatEur(offer.netFareEur)}\n(${airlineIdentity.allianceName})",
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
