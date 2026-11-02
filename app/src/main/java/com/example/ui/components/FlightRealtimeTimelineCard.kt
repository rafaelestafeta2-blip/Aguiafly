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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    val depTime = remember(departureIso) { departureIso.substringAfter("T", "10:50").take(5) }
    val arrTime = remember(arrivalIso) { arrivalIso.substringAfter("T", "13:35").take(5) }
    val depDate = remember(departureIso) { departureIso.take(10) }
    val arrDate = remember(arrivalIso) { arrivalIso.take(10) }
    val durationText = remember(departureIso, arrivalIso) {
        computeDurationFormatted(departureIso, arrivalIso)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "flightTimelineAnim")
    val animatedPlaneFraction by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "planeProgressFraction"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF153263),
                        Color(0xFF1D4382)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(champagneGold.copy(alpha = 0.65f), aeroCyan.copy(alpha = 0.55f))
                ),
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("flight_realtime_timeline_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Cabeçalho: Horário de Partida | Tempo Total de Voo | Horário de Chegada
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coluna da Partida (Origem)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "PARTIDA • $originIata",
                        style = MaterialTheme.typography.labelLarge,
                        color = champagneGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = depTime,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$originCity ($depDate)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Selo Central: Tempo Total de Voo em Dourado Champagne & Safira
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0C1E3B),
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
                            text = durationText,
                            style = MaterialTheme.typography.labelLarge,
                            color = champagneGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Coluna da Chegada (Destino)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CHEGADA • $destinationIata",
                        style = MaterialTheme.typography.labelLarge,
                        color = aeroCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = arrTime,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "$destinationCity ($arrDate)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End
                    )
                }
            }

            // 2. Timeline Visual Animada do Trajeto (ex: LIS ➔ ✈️ ➔ OPO)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0B1F3F))
                    .border(1.dp, aeroCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val trackWidth = maxWidth

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    val cy = size.height / 2f
                    val startX = 16.dp.toPx()
                    val endX = size.width - 16.dp.toPx()
                    val planeX = startX + (endX - startX) * animatedPlaneFraction

                    // Linha tracejada total do percurso
                    drawLine(
                        color = aeroCyan.copy(alpha = 0.40f),
                        start = Offset(startX, cy),
                        end = Offset(endX, cy),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )

                    // Trecho percorrido em Gradiente Dourado Champagne -> Azul Ciano
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(champagneGold, aeroCyan)
                        ),
                        start = Offset(startX, cy),
                        end = Offset(planeX, cy),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Ponto de Origem e Ponto de Destino
                    drawCircle(
                        color = champagneGold,
                        radius = 6.dp.toPx(),
                        center = Offset(startX, cy)
                    )
                    drawCircle(
                        color = aeroCyan,
                        radius = 6.dp.toPx(),
                        center = Offset(endX, cy)
                    )
                }

                // Avião Animado percorrendo a rota LIS ➔ OPO
                Box(
                    modifier = Modifier
                        .offset(x = (trackWidth - 36.dp) * animatedPlaneFraction)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(champagneGold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Trajeto $originIata para $destinationIata",
                        tint = Color(0xFF07152B),
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(90f)
                    )
                }
            }

            // 3. Marcos da Linha do Tempo (Origem ➔ Cruzeiro ➔ Destino)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🛫 Decolagem $originIata ($depTime)",
                    style = MaterialTheme.typography.bodySmall,
                    color = champagneGold,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Voo Direto $flightNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "🛬 Pouso $destinationIata ($arrTime)",
                    style = MaterialTheme.typography.bodySmall,
                    color = aeroCyan,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
