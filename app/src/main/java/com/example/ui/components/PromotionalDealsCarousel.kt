package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.data.model.PromotionalDealsCatalog
import com.example.data.model.TravelPromotionDeal
import com.example.domain.pricing.DisplayCurrency
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PromotionalDealsCarousel(
    currentOriginIata: String,
    dealsList: List<TravelPromotionDeal> = PromotionalDealsCatalog.GLOBAL_DEALS,
    isRefreshingFares: Boolean = false,
    isDuffelLive: Boolean = false,
    selectedCurrency: DisplayCurrency = DisplayCurrency.EUR,
    onRefreshFares: () -> Unit = {},
    onSelectDealToBook: (TravelPromotionDeal) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = PromotionalDealsCatalog.CATEGORIES
    var selectedCategory by remember { mutableStateOf("Todas") }
    var hourBucket by remember { mutableStateOf(PromotionalDealsCatalog.currentHourBucket()) }

    val originPlace = remember(currentOriginIata) {
        PlaceAutocompleteCatalog.resolveByIata(currentOriginIata)
    }

    // Atualização automática a cada 1 hora (3.600.000 ms) para rodar novas ofertas globais
    LaunchedEffect(originPlace.iataCode) {
        while (true) {
            delay(3_600_000L)
            hourBucket = PromotionalDealsCatalog.currentHourBucket()
            onRefreshFares()
        }
    }

    val deals = remember(selectedCategory, originPlace.iataCode, dealsList, hourBucket) {
        val adaptedLiveMap = dealsList
            .filter { !it.destinationIata.equals(originPlace.iataCode, ignoreCase = true) }
            .associateBy({ it.destinationIata.uppercase() }) { rawDeal ->
                PromotionalDealsCatalog.adaptDealToOrigin(
                    deal = rawDeal,
                    currentOrigin = originPlace.iataCode,
                    hourBucket = hourBucket
                )
            }

        val catalogRotated = PromotionalDealsCatalog.getFilteredDeals(
            category = selectedCategory,
            currentOrigin = originPlace.iataCode,
            hourBucket = hourBucket
        )

        catalogRotated.map { catDeal ->
            adaptedLiveMap[catDeal.destinationIata.uppercase()] ?: catDeal
        }
    }

    val pagerState = rememberPagerState(pageCount = { deals.size })
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    val scope = rememberCoroutineScope()

    // Auto-scroll contínuo a cada 4.5 segundos (pausa quando o utilizador arrasta ou interage)
    LaunchedEffect(pagerState, isDragged, deals.size) {
        if (!isDragged && deals.isNotEmpty()) {
            while (true) {
                delay(4500)
                if (!isDragged && deals.isNotEmpty()) {
                    val next = (pagerState.currentPage + 1) % deals.size
                    pagerState.animateScrollToPage(next, animationSpec = tween(700))
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("promotional_deals_carousel_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabeçalho da Seção
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFF9800).copy(alpha = 0.18f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Promoções",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Promoções Saindo de ${originPlace.cityName} (${originPlace.iataCode})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "🔄 Atualizado a cada 1h • ${deals.size} destinos globais verificados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botão de Refresh para buscar os preços mais baixos
            IconButton(
                onClick = {
                    hourBucket = PromotionalDealsCatalog.currentHourBucket() + 1L
                    onRefreshFares()
                },
                enabled = !isRefreshingFares,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("refresh_lowest_fares_button")
            ) {
                if (isRefreshingFares) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Atualizar tarifas",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Filtros Rápidos de Categoria (Todas, Europa, Brasil, Américas, Ásia & Dubai, África & Oceania, Praias)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val selected = selectedCategory == cat
                FilterChip(
                    selected = selected,
                    onClick = {
                        selectedCategory = cat
                        scope.launch { pagerState.scrollToPage(0) }
                    },
                    label = {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        // Carrossel Horizontal de Banners Chamativos
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 2.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(248.dp)
                .testTag("promotional_deals_horizontal_pager")
        ) { page ->
            val deal = deals.getOrNull(page)
            if (deal != null) {
                PromotionalDealBannerCard(
                    deal = deal,
                    selectedCurrency = selectedCurrency,
                    onClickToBook = { onSelectDealToBook(deal) }
                )
            }
        }

        // Indicador de Posição (Dots do Carrossel)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            deals.indices.forEach { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 22.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
fun PromotionalDealBannerCard(
    deal: TravelPromotionDeal,
    selectedCurrency: DisplayCurrency = DisplayCurrency.EUR,
    onClickToBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFFFD700)
    val deepNavy = Color(0xFF03132B)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = deepNavy,
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                listOf(
                    gold.copy(alpha = 0.85f),
                    Color(0xFF635BFF).copy(alpha = 0.5f),
                    gold.copy(alpha = 0.3f)
                )
            )
        ),
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClickToBook() }
            .testTag("deal_banner_${deal.destinationIata}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Foto de Alta Resolução do Destino / País
            AsyncImage(
                model = deal.photoUrl,
                contentDescription = "${deal.destinationCity}, ${deal.destinationCountry}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Gradiente Escuro de Alto Contraste (Imperial Sapphire)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.30f),
                                Color(0xFF020E22).copy(alpha = 0.65f),
                                Color(0xFF020E22).copy(alpha = 0.95f)
                            )
                        )
                    )
            )

            // Conteúdo do Banner
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topo do Banner: Badges de Desconto & Voo Direto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de Promoção Chamativo
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF3D00), Color(0xFFFF9100))
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = deal.highlightBadge,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Badge de Cia Aérea / Voo Direto
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.55f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.4f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${deal.airlineName} • ${if (deal.isDirectFlight) "Direto" else "Conexão"}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Parte Inferior do Banner: Destino, Rota, Preço Promocional e Botão de Compra
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${deal.destinationCity}, ${deal.destinationCountry}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Saindo de ${deal.originCity} (${deal.originIata}) • ${deal.departureDateIso}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = deal.tagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = gold.copy(alpha = 0.95f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Bloco de Preços sem quebra de linha
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = selectedCurrency.format(deal.originalFareEur),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f),
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = selectedCurrency.format(deal.displayTotalEur),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = gold,
                                fontSize = 22.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "Tarifa Final (${selectedCurrency.code})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 9.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Botão de Ação em linha única sem corte de texto
                    Button(
                        onClick = onClickToBook,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gold,
                            contentColor = Color(0xFF03132B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("buy_deal_button_${deal.destinationIata}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aproveitar por ${selectedCurrency.format(deal.displayTotalEur)} ➔",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
