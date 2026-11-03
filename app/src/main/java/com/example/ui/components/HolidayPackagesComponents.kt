package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.HolidayPackage
import com.example.data.model.PackageHighlightBadge
import com.example.data.model.PackageInclusionItem
import com.example.data.model.PackageItineraryDay
import com.example.data.model.PackageRouteWaypoint
import com.example.data.model.PackageSourceType
import com.example.domain.pricing.DisplayCurrency
import java.util.Locale

private val GoldAccent = Color(0xFFE5C158)
private val DeepNavy = Color(0xFF08142C)
private val EmeraldBadge = Color(0xFF10B981)
private val UrgentCoral = Color(0xFFF97316)
private val RoyalPurple = Color(0xFF8B5CF6)

/**
 * Card Ultra-Premium estilo Airbnb Luxe / Instagram para cada Pacote de Férias ou Experiência Exclusiva:
 * - Carrossel HorizontalPager de fotos HD com indicadores
 * - Badges em destaque ("★ Tudo Incluído", "✦ Exclusivo Aguiafly", "🔥 Últimas Vagas")
 * - Origem da integração (Admin CMS vs GetYourGuide / Viator / Civitatis API)
 * - Ícones de Inclusão (Voo, Alojamento, Refeições, Guias, Transfers)
 * - Preço por pessoa, valor de Sinal de Reserva e Vagas Restantes em tempo real
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HolidayPackageUltraCard(
    pkg: HolidayPackage,
    selectedCurrency: DisplayCurrency = DisplayCurrency.EUR,
    onOpenDetails: (HolidayPackage) -> Unit,
    onDirectReserve: (HolidayPackage) -> Unit,
    modifier: Modifier = Modifier
) {
    val photos = remember(pkg.galleryPhotosJson) {
        pkg.galleryPhotos.ifEmpty {
            listOf("https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=85")
        }
    }
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val badge = pkg.effectiveBadge
    val inclusions = remember(pkg.inclusionsCsv) { pkg.inclusions }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDetails(pkg) }
            .testTag("holiday_package_card_${pkg.id}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E1E3C)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Carrossel de Fotos HD (estilo Airbnb/Instagram)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = photos[page],
                        contentDescription = "${pkg.title} foto ${page + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Gradiente escuro para leitura perfeita das badges e título
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent,
                                    DeepNavy.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Topo: Badge Principal + Conector API / CMS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PackageBadgePill(badge = badge)
                    SourceConnectorPill(source = pkg.sourceEnum)
                }

                // Base da foto: Destino, Duração e Paginação de Fotos
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${pkg.destinationCity} (${pkg.destinationIata}) • ${pkg.durationLabel}",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = pkg.title,
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg.partnerAgencyName,
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        // Indicadores estilo Instagram/Airbnb
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(photos.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(if (isSelected) 18.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) GoldAccent else Color.White.copy(alpha = 0.45f)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Ícones de Inclusão + Inventário de Vagas + Preço & CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PackageInclusionItem.entries.forEach { item ->
                        val isIncluded = inclusions.contains(item)
                        if (isIncluded) {
                            InclusionMiniChip(item = item)
                        }
                    }
                }

                // Linha Financeira + Inventário de Vagas Restantes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PREÇO FINAL POR PESSOA",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedCurrency.format(pkg.finalPricePerPersonEur),
                                color = GoldAccent,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "/ pessoa",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = "Ou Sinal de Reserva (${pkg.depositPercentage}%): ${selectedCurrency.format(pkg.depositPerPersonEur)}",
                            color = EmeraldBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Contador de Vagas Disponíveis em Tempo Real
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (pkg.availableSpots <= 4) {
                            UrgentCoral.copy(alpha = 0.18f)
                        } else {
                            Color.White.copy(alpha = 0.08f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (pkg.availableSpots <= 4) UrgentCoral else GoldAccent.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (pkg.availableSpots > 0) "${pkg.availableSpots} VAGAS" else "ESGOTADO",
                                color = if (pkg.availableSpots <= 4) UrgentCoral else GoldAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "de ${pkg.totalSpots} lugares",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Botões de Ação: Ver Roteiro & Mapa Interativo | Reservar via Stripe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenDetails(pkg) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_package_details_${pkg.id}"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Roteiro & Mapa",
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = { onDirectReserve(pkg) },
                        enabled = pkg.availableSpots > 0,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(48.dp)
                            .testTag("btn_package_reserve_${pkg.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = DeepNavy
                        )
                    ) {
                        Text(
                            text = if (pkg.availableSpots > 0) "Reservar Pacote" else "Sem Vagas",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PackageBadgePill(badge: PackageHighlightBadge) {
    val (bgColor, textColor) = when (badge) {
        PackageHighlightBadge.TUDO_INCLUIDO -> EmeraldBadge to Color(0xFF042F2E)
        PackageHighlightBadge.EXCLUSIVO_AGUIAFLY -> GoldAccent to DeepNavy
        PackageHighlightBadge.ULTIMAS_VAGAS -> UrgentCoral to Color.White
        PackageHighlightBadge.PARCEIRO_OFICIAL -> RoyalPurple to Color.White
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor
    ) {
        Text(
            text = badge.label.uppercase(Locale.getDefault()),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun SourceConnectorPill(source: PackageSourceType) {
    Surface(
        shape = RoundedCornerShape(50),
        color = DeepNavy.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = null,
                tint = GoldAccent,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = source.badgeShortLabel,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun InclusionMiniChip(item: PackageInclusionItem) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF142952),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = inclusionIconFor(item),
                contentDescription = item.label,
                tint = GoldAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = item.label,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun inclusionIconFor(item: PackageInclusionItem): ImageVector = when (item) {
    PackageInclusionItem.FLIGHT -> Icons.Default.FlightTakeoff
    PackageInclusionItem.HOTEL -> Icons.Default.Apartment
    PackageInclusionItem.MEALS -> Icons.Default.LocalDining
    PackageInclusionItem.GUIDE -> Icons.Default.RecordVoiceOver
    PackageInclusionItem.TRANSFERS -> Icons.Default.DirectionsCar
}

/**
 * Mapa Interativo do Roteiro do Pacote:
 * Renderiza a trajetória geográfica entre todas as paragens do pacote e permite tocar
 * nos pinos ou nos chips para inspecionar as coordenadas e destaques de cada etapa.
 */
@Composable
fun InteractivePackageRouteMapCard(
    waypoints: List<PackageRouteWaypoint>,
    modifier: Modifier = Modifier
) {
    val safePoints = remember(waypoints) {
        waypoints.ifEmpty {
            listOf(
                PackageRouteWaypoint(1, "Chegada Internacional", "Recepção Aeroporto VIP", 38.7756, -9.1354),
                PackageRouteWaypoint(2, "Resort Principal 5★", "Check-in & Experiências", 38.7223, -9.1393),
                PackageRouteWaypoint(3, "Roteiro Guiado", "Passeio Exclusivo", 38.7975, -9.3906)
            )
        }
    }
    var selectedIdx by remember { mutableIntStateOf(0) }
    val activePoint = safePoints.getOrElse(selectedIdx) { safePoints.first() }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF091834)),
        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Mapa Interativo do Roteiro",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Toque nas etapas para explorar as paragens geográficas",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = GoldAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "${safePoints.size} Etapas GPS",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Canvas do Mapa Náutico/Aeronáutico Interativo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF061024), Color(0xFF0D234A))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grelha cartográfica subtil
                    val gridCols = 6
                    val gridRows = 4
                    for (c in 1 until gridCols) {
                        val x = w * (c.toFloat() / gridCols)
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(x, 0f),
                            end = Offset(x, h),
                            strokeWidth = 1f
                        )
                    }
                    for (r in 1 until gridRows) {
                        val y = h * (r.toFloat() / gridRows)
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    val count = safePoints.size
                    val nodeOffsets = safePoints.mapIndexed { idx, _ ->
                        val fractionX = if (count <= 1) 0.5f else 0.12f + (0.76f * idx.toFloat() / (count - 1))
                        val waveY = if (idx % 2 == 0) 0.62f else 0.34f
                        Offset(w * fractionX, h * waveY)
                    }

                    // Curva suave ligando todas as paragens do pacote
                    if (nodeOffsets.size >= 2) {
                        val routePath = Path().apply {
                            moveTo(nodeOffsets.first().x, nodeOffsets.first().y)
                            for (i in 0 until nodeOffsets.size - 1) {
                                val curr = nodeOffsets[i]
                                val next = nodeOffsets[i + 1]
                                val ctrlX = (curr.x + next.x) / 2f
                                val ctrlY = if (i % 2 == 0) minOf(curr.y, next.y) - 28f else maxOf(curr.y, next.y) + 28f
                                quadraticTo(ctrlX, ctrlY, next.x, next.y)
                            }
                        }
                        drawPath(
                            path = routePath,
                            color = GoldAccent.copy(alpha = 0.85f),
                            style = Stroke(
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                    }

                    // Desenha os pinos de cada etapa
                    nodeOffsets.forEachIndexed { idx, pos ->
                        val isSelected = idx == selectedIdx
                        if (isSelected) {
                            drawCircle(
                                color = GoldAccent.copy(alpha = 0.28f),
                                radius = 20.dp.toPx(),
                                center = pos
                            )
                        }
                        drawCircle(
                            color = if (isSelected) GoldAccent else Color(0xFF38BDF8),
                            radius = if (isSelected) 9.dp.toPx() else 6.5.dp.toPx(),
                            center = pos
                        )
                        drawCircle(
                            color = DeepNavy,
                            radius = 3.dp.toPx(),
                            center = pos
                        )
                    }
                }

                // Legenda da etapa selecionada no mapa
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = DeepNavy.copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Parada ${activePoint.orderIndex}: ${activePoint.label}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "${activePoint.subtitle} • GPS ${
                                    String.format(Locale.US, "%.4f, %.4f", activePoint.latitude, activePoint.longitude)
                                }",
                                color = GoldAccent,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Seletor interativo das etapas do roteiro
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                safePoints.forEachIndexed { index, wp ->
                    val isSelected = index == selectedIdx
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIdx = index },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) GoldAccent else Color(0xFF13274F),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GoldAccent else Color.White.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Etapa ${wp.orderIndex}",
                                color = if (isSelected) DeepNavy else GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = wp.label,
                                color = if (isSelected) DeepNavy else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Aba Sanfonada (Accordion) para o Itinerário Detalhado Dia a Dia.
 */
@Composable
fun ItineraryAccordionSection(
    itineraryDays: List<PackageItineraryDay>,
    modifier: Modifier = Modifier
) {
    var expandedDayNumber by remember { mutableIntStateOf(itineraryDays.firstOrNull()?.dayNumber ?: 1) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Itinerário Detalhado Dia a Dia (Accordion)",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )

        itineraryDays.forEach { dayItem ->
            val isExpanded = expandedDayNumber == dayItem.dayNumber
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expandedDayNumber = if (isExpanded) -1 else dayItem.dayNumber
                    }
                    .testTag("accordion_day_${dayItem.dayNumber}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) Color(0xFF142A54) else Color(0xFF0C1B38)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isExpanded) GoldAccent else Color.White.copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isExpanded) GoldAccent else Color(0xFF1E3A6D)
                            ) {
                                Text(
                                    text = "DIA ${dayItem.dayNumber}",
                                    color = if (isExpanded) DeepNavy else GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dayItem.title,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (dayItem.locationName.isNotBlank()) {
                                    Text(
                                        text = dayItem.locationName,
                                        color = GoldAccent,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Recolher dia" else "Expandir dia",
                            tint = GoldAccent
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = dayItem.description,
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldBadge.copy(alpha = 0.16f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldBadge,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = dayItem.includedMealsLabel,
                                            color = EmeraldBadge,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = dayItem.highlightTag,
                                        color = GoldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
