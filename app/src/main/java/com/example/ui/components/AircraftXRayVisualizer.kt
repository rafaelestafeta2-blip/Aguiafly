package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatReclineExtra
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AircraftTechnicalSpec
import com.example.data.model.BodyType
import com.example.domain.aircraft.AircraftSpecsCatalog

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AircraftXRayVisualizer(
    spec: AircraftTechnicalSpec,
    highlightedSeat: String = "AUTO",
    modifier: Modifier = Modifier
) {
    var showInteriorCabin by remember { mutableStateOf(false) }
    var inspectedSeat by remember(highlightedSeat) { mutableStateOf(highlightedSeat) }

    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary
    val isAutoSeat = inspectedSeat.isBlank() ||
        inspectedSeat.equals("AUTO", ignoreCase = true) ||
        inspectedSeat.startsWith("AUTO", ignoreCase = true)
    val isWindow = !isAutoSeat && AircraftSpecsCatalog.isWindowSeat(inspectedSeat)
    val seatDescription = if (isAutoSeat) {
        "Atribuído gratuitamente pela companhia no check-in (€0.00) • Toque num banco acima para simular Janela/Corredor"
    } else {
        AircraftSpecsCatalog.describeSeatPosition(inspectedSeat, spec)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF132D59),
                        Color(0xFF0E2347)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = if (showInteriorCabin) aeroCyan else champagneGold.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
            .testTag("aircraft_xray_visualizer"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showInteriorCabin) {
                        "MAPA INTERNO • JANELA vs CORREDOR"
                    } else {
                        "FOTO REAL DA AERONAVE • ICAO ${spec.icaoCode}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = champagneGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${spec.fullName} (${spec.bodyType.label})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = champagneGold.copy(alpha = 0.16f),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = champagneGold,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { showInteriorCabin = !showInteriorCabin }
                    .testTag("toggle_aircraft_interior_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showInteriorCabin) Icons.Default.Flight else Icons.Default.AirlineSeatReclineExtra,
                        contentDescription = null,
                        tint = champagneGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showInteriorCabin) "Ver Foto do Avião" else "Abrir Bancos",
                        style = MaterialTheme.typography.labelLarge,
                        color = champagneGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        AnimatedContent(
            targetState = showInteriorCabin,
            transitionSpec = {
                (fadeIn(tween(500)) + scaleIn(initialScale = 0.94f, animationSpec = tween(500)))
                    .togetherWith(fadeOut(tween(350)))
            },
            label = "aircraftPhotoToCabinTransition"
        ) { isInterior ->
            if (!isInterior) {
                RealAircraftExteriorCard(
                    spec = spec,
                    inspectedSeat = if (isAutoSeat) "Automático (Grátis)" else inspectedSeat,
                    isWindow = isWindow,
                    onOpenCabin = { showInteriorCabin = true }
                )
            } else {
                InteractiveCabinCutawayView(
                    spec = spec,
                    inspectedSeat = inspectedSeat,
                    onSelectSeatToInspect = { inspectedSeat = it },
                    onBackToExterior = { showInteriorCabin = false }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                isAutoSeat -> emerald.copy(alpha = 0.16f)
                isWindow -> champagneGold.copy(alpha = 0.16f)
                else -> aeroCyan.copy(alpha = 0.16f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    when {
                        isAutoSeat -> emerald.copy(alpha = 0.65f)
                        isWindow -> champagneGold.copy(alpha = 0.65f)
                        else -> aeroCyan.copy(alpha = 0.65f)
                    },
                    RoundedCornerShape(12.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = when {
                        isAutoSeat -> emerald
                        isWindow -> champagneGold
                        else -> aeroCyan
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = when {
                            isAutoSeat -> "ASSENTO AUTOMÁTICO / GRATUITO (€0.00)"
                            isWindow -> "ASSENTO $inspectedSeat • VOCÊ VAI SENTAR NA JANELA"
                            else -> "ASSENTO $inspectedSeat • VOCÊ VAI SENTAR NO CORREDOR"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = when {
                            isAutoSeat -> emerald
                            isWindow -> champagneGold
                            else -> aeroCyan
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = seatDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun RealAircraftExteriorCard(
    spec: AircraftTechnicalSpec,
    inspectedSeat: String,
    isWindow: Boolean,
    onOpenCabin: () -> Unit
) {
    val champagneGold = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(215.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpenCabin() }
    ) {
        AsyncImage(
            model = spec.exteriorPhotoUrl,
            contentDescription = "Foto real ${spec.fullName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x66081730),
                            Color(0x1A081730),
                            Color(0xD9081730)
                        )
                    )
                )
        )

        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xD90B1F3F),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .border(1.dp, champagneGold.copy(alpha = 0.7f), RoundedCornerShape(50))
        ) {
            Text(
                text = "${spec.manufacturer} • Reg ${spec.sampleRegistration} • ${spec.cruiseSpeedMach}",
                style = MaterialTheme.typography.bodySmall,
                color = champagneGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = spec.fullName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isWindow) {
                        "Assento $inspectedSeat: Lado da Janela"
                    } else {
                        "Assento $inspectedSeat"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(champagneGold)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color(0xFF07152B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Toque para ver Bancos & Janelas",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF07152B),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveCabinCutawayView(
    spec: AircraftTechnicalSpec,
    inspectedSeat: String,
    onSelectSeatToInspect: (String) -> Unit,
    onBackToExterior: () -> Unit
) {
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val columns = if (spec.bodyType == BodyType.WIDEBODY) {
        listOf("A", "D", "G", "K")
    } else {
        listOf("A", "C", "D", "F")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B1F3F))
            .border(1.dp, aeroCyan.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF153263))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🪟 JANELA ESQ (A)",
                style = MaterialTheme.typography.labelLarge,
                color = champagneGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "🚶 CORREDOR CENTRAL",
                style = MaterialTheme.typography.labelLarge,
                color = aeroCyan,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "🪟 JANELA DIR (${columns.last()})",
                style = MaterialTheme.typography.labelLarge,
                color = champagneGold,
                fontWeight = FontWeight.Bold
            )
        }

        for (rowNum in 1..4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WindowWallIndicator(isLeft = true)

                columns.forEachIndexed { index, col ->
                    val seatCode = "$rowNum$col"
                    val isSelected = seatCode.equals(inspectedSeat, ignoreCase = true)
                    val isSeatWindow = col == "A" || col == "K" || col == "F"

                    Box(
                        modifier = Modifier
                            .width(56.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                when {
                                    isSelected -> champagneGold
                                    else -> Color(0xFF163566)
                                }
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) champagneGold else if (isSeatWindow) champagneGold.copy(alpha = 0.6f) else aeroCyan.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectSeatToInspect(seatCode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = seatCode,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Color(0xFF07152B) else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSeatWindow) "JANELA" else "CORREDOR",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) Color(0xFF07152B) else if (isSeatWindow) champagneGold else aeroCyan
                            )
                        }
                    }

                    if ((spec.bodyType == BodyType.WIDEBODY && (index == 0 || index == 2)) ||
                        (spec.bodyType == BodyType.NARROWBODY && index == 1)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(26.dp)
                        ) {
                            Text(
                                text = "┆",
                                style = MaterialTheme.typography.bodySmall,
                                color = aeroCyan,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "CORR",
                                style = MaterialTheme.typography.bodySmall,
                                color = aeroCyan
                            )
                        }
                    }
                }

                WindowWallIndicator(isLeft = false)
            }
        }

        Text(
            text = "Dica: Toque em qualquer banco acima para conferir se é Janela ou Corredor, ou clique em 'Ver Foto do Avião' para voltar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onBackToExterior() }
        )
    }
}

@Composable
private fun WindowWallIndicator(isLeft: Boolean) {
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isLeft) Spacer(modifier = Modifier.width(3.dp))
        Box(
            modifier = Modifier
                .width(7.dp)
                .height(24.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(aeroCyan, champagneGold)
                    )
                )
                .border(1.dp, champagneGold, CircleShape)
        )
        if (isLeft) Spacer(modifier = Modifier.width(3.dp))
    }
}
