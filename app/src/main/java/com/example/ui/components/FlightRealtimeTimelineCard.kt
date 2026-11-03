package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlightOperationalState
import com.example.data.remote.PremiumEcosystemCatalog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

@Composable
fun FlightRealtimeTimelineCard(
    originIata: String,
    originCity: String,
    destinationIata: String,
    destinationCity: String,
    departureIso: String,
    arrivalIso: String,
    flightNumber: String,
    stopoverHubIata: String = "",
    modifier: Modifier = Modifier
) {
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    var selectedOperationalState by remember(flightNumber) {
        mutableStateOf(FlightOperationalState.ON_TIME)
    }

    val telemetry = remember(
        originIata,
        destinationIata,
        departureIso,
        arrivalIso,
        flightNumber,
        stopoverHubIata,
        selectedOperationalState
    ) {
        PremiumEcosystemCatalog.getRealtimeFlightTelemetry(
            originIata = originIata,
            originCity = originCity,
            destinationIata = destinationIata,
            destinationCity = destinationCity,
            departureIso = departureIso,
            arrivalIso = arrivalIso,
            flightNumber = flightNumber,
            stopoverHubIata = stopoverHubIata,
            overrideState = selectedOperationalState
        )
    }

    val statusAccentColor = Color(telemetry.operationalState.accentHex)

    val depDate = remember(departureIso) { departureIso.take(10) }
    val arrDate = remember(arrivalIso) { arrivalIso.take(10) }
    val durationText = remember(departureIso, arrivalIso) {
        computeDurationFormatted(departureIso, arrivalIso)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "flightRadarTimelineAnim")
    val animatedPlaneFraction by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = if (selectedOperationalState == FlightOperationalState.CANCELLED) 0.16f else 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "planeProgressFraction"
    )
    val radarPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radarPulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E254B),
                        Color(0xFF17376E),
                        Color(0xFF0B1E3D)
                    )
                )
            )
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(statusAccentColor.copy(alpha = 0.85f), champagneGold.copy(alpha = 0.65f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("flight_realtime_timeline_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 0. Cabeçalho de Rastreamento em Tempo Real (FlightAware / Aviationstack ADS-B)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusAccentColor.copy(alpha = radarPulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "RADAR EM TEMPO REAL • ${telemetry.flightNumber}",
                            style = MaterialTheme.typography.labelLarge,
                            color = champagneGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = telemetry.telemetrySource,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusAccentColor.copy(alpha = 0.20f),
                    modifier = Modifier
                        .border(1.dp, statusAccentColor.copy(alpha = 0.85f), RoundedCornerShape(50))
                        .testTag("flight_live_status_badge")
                ) {
                    Text(
                        text = telemetry.operationalState.badgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusAccentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Seletor Interativo de Simulação/Verificação de Status (No Horário, Atrasado, Cancelado, Embarque)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status Live:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlightOperationalState.entries.forEach { stateOption ->
                    val isSelected = selectedOperationalState == stateOption
                    val chipColor = Color(stateOption.accentHex)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) chipColor.copy(alpha = 0.24f) else Color(0xFF0A1A35),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                width = if (isSelected) 1.2.dp else 1.dp,
                                color = if (isSelected) chipColor else chipColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedOperationalState = stateOption }
                            .testTag("flight_status_chip_${stateOption.code.lowercase()}")
                    ) {
                        Text(
                            text = stateOption.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) chipColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 1. Horário de Partida | Duração Total | Horário de Chegada (com atualização de atraso se aplicável)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PARTIDA • $originIata",
                        style = MaterialTheme.typography.labelLarge,
                        color = champagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = telemetry.estimatedDepartureTime,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (telemetry.delayMinutes > 0) statusAccentColor else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (telemetry.delayMinutes > 0) {
                            "Previsto ${telemetry.scheduledDepartureTime} (+${telemetry.delayMinutes}m)"
                        } else {
                            "$originCity ($depDate)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF081730),
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = champagneGold.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(50)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = champagneGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$durationText • ${telemetry.distanceKm} km",
                            style = MaterialTheme.typography.labelLarge,
                            color = champagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CHEGADA • $destinationIata",
                        style = MaterialTheme.typography.labelLarge,
                        color = aeroCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = telemetry.estimatedArrivalTime,
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (telemetry.delayMinutes > 0) statusAccentColor else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (telemetry.delayMinutes > 0) {
                            "Previsto ${telemetry.scheduledArrivalTime} (+${telemetry.delayMinutes}m)"
                        } else {
                            "$destinationCity ($arrDate)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }

            // 2. Painel de Terminal, Portão de Embarque e Esteira em Tempo Real
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GateTelemetryPill(
                    label = "Terminal Partida",
                    value = "Terminal ${telemetry.departureTerminal}",
                    accentColor = champagneGold,
                    modifier = Modifier.weight(1f)
                )
                GateTelemetryPill(
                    label = "Portão Embarque",
                    value = "Portão ${telemetry.departureGate}",
                    accentColor = statusAccentColor,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("realtime_boarding_gate_pill")
                )
                GateTelemetryPill(
                    label = "Chegada & Mala",
                    value = "${telemetry.arrivalTerminal} • ${telemetry.baggageClaimBelt}",
                    accentColor = aeroCyan,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. MAPA DO TRAJETO EM TEMPO REAL (Canvas Radar Aeronáutico com Curva Geodésica)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF061329),
                                Color(0xFF0A2146),
                                Color(0xFF071630)
                            )
                        )
                    )
                    .border(1.dp, aeroCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .testTag("flight_route_trajectory_map")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Malha Cartográfica de Latitude / Longitude (Estilo Radar FlightAware)
                    val gridStepX = w / 6f
                    val gridStepY = h / 4f
                    for (i in 1..5) {
                        drawLine(
                            color = aeroCyan.copy(alpha = 0.10f),
                            start = Offset(i * gridStepX, 0f),
                            end = Offset(i * gridStepX, h),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    for (j in 1..3) {
                        drawLine(
                            color = aeroCyan.copy(alpha = 0.10f),
                            start = Offset(0f, j * gridStepY),
                            end = Offset(w, j * gridStepY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val startPt = Offset(w * 0.12f, h * 0.72f)
                    val controlPt = Offset(w * 0.50f, h * 0.14f)
                    val endPt = Offset(w * 0.88f, h * 0.68f)

                    // Arco completo tracejado (Rota Great-Circle)
                    val fullArcPath = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        quadraticTo(controlPt.x, controlPt.y, endPt.x, endPt.y)
                    }
                    drawPath(
                        path = fullArcPath,
                        color = aeroCyan.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    )

                    // Ponto atual da aeronave na curva quadrática de Bézier
                    val t = animatedPlaneFraction.coerceIn(0.08f, 0.92f)
                    val oneMinusT = 1f - t
                    val planeX = (oneMinusT * oneMinusT * startPt.x) +
                        (2f * oneMinusT * t * controlPt.x) +
                        (t * t * endPt.x)
                    val planeY = (oneMinusT * oneMinusT * startPt.y) +
                        (2f * oneMinusT * t * controlPt.y) +
                        (t * t * endPt.y)

                    // Linha ativa percorrida até a posição da aeronave
                    val activePath = Path().apply {
                        moveTo(startPt.x, startPt.y)
                        val midCtrlX = startPt.x + (controlPt.x - startPt.x) * t
                        val midCtrlY = startPt.y + (controlPt.y - startPt.y) * t
                        quadraticTo(midCtrlX, midCtrlY, planeX, planeY)
                    }
                    drawPath(
                        path = activePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(champagneGold, statusAccentColor, aeroCyan)
                        ),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Marcador de Origem
                    drawCircle(
                        color = champagneGold.copy(alpha = 0.28f),
                        radius = 12.dp.toPx(),
                        center = startPt
                    )
                    drawCircle(
                        color = champagneGold,
                        radius = 5.5.dp.toPx(),
                        center = startPt
                    )

                    // Se houver escala/hub intermediário, desenha o pino de conexão no ápice
                    if (telemetry.waypoints.size > 2) {
                        val hubPt = Offset(
                            x = (0.25f * startPt.x) + (0.5f * controlPt.x) + (0.25f * endPt.x),
                            y = (0.25f * startPt.y) + (0.5f * controlPt.y) + (0.25f * endPt.y)
                        )
                        drawCircle(
                            color = champagneGold,
                            radius = 4.5.dp.toPx(),
                            center = hubPt
                        )
                    }

                    // Marcador de Destino
                    drawCircle(
                        color = aeroCyan.copy(alpha = 0.28f),
                        radius = 12.dp.toPx(),
                        center = endPt
                    )
                    drawCircle(
                        color = aeroCyan,
                        radius = 5.5.dp.toPx(),
                        center = endPt
                    )

                    // Halo de Radar na posição ao vivo da aeronave
                    drawCircle(
                        color = statusAccentColor.copy(alpha = 0.30f * radarPulseAlpha),
                        radius = 18.dp.toPx(),
                        center = Offset(planeX, planeY)
                    )
                    drawCircle(
                        color = statusAccentColor,
                        radius = 7.dp.toPx(),
                        center = Offset(planeX, planeY)
                    )
                }

                // Etiquetas do Mapa do Trajeto (Topo: Altitude & Velocidade | Base: Origem e Destino)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = aeroCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mapa do Trajeto ADS-B",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = aeroCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (selectedOperationalState == FlightOperationalState.CANCELLED) {
                            "Aeronave em Solo • Voo Cancelado"
                        } else {
                            "Alt: ${telemetry.altitudeFeet} ft • Vel: ${telemetry.groundSpeedKmh} km/h"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = champagneGold,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛫 $originIata ($originCity)",
                        style = MaterialTheme.typography.labelSmall,
                        color = champagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        tint = statusAccentColor,
                        modifier = Modifier
                            .size(15.dp)
                            .rotate(90f)
                    )
                    Text(
                        text = "🛬 $destinationIata ($destinationCity)",
                        style = MaterialTheme.typography.labelSmall,
                        color = aeroCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 4. Mensagem de Telemetria Operacional
            Text(
                text = telemetry.operationalState.detailMessagePt,
                style = MaterialTheme.typography.bodySmall,
                color = statusAccentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GateTelemetryPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF091B38),
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

private fun computeDurationFormatted(departureIso: String, arrivalIso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val d1 = parser.parse(departureIso.take(16))
        val d2 = parser.parse(arrivalIso.take(16))
        if (d1 != null && d2 != null) {
            val diffMins = abs((d2.time - d1.time) / 60_000L).toInt().coerceAtLeast(45)
            val h = diffMins / 60
            val m = diffMins % 60
            "${h}h ${m.toString().padStart(2, '0')}m"
        } else {
            "2h 15m"
        }
    } catch (_: Exception) {
        "2h 15m"
    }
}
