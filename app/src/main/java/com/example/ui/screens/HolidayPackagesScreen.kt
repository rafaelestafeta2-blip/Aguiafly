package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.HolidayPackage
import com.example.data.model.PackageBookingRecord
import com.example.data.model.PackageHighlightBadge
import com.example.data.model.PackageInclusionItem
import com.example.data.model.PackageItineraryDay
import com.example.data.model.PackagePaymentMode
import com.example.data.model.PackageRouteWaypoint
import com.example.data.model.PaymentCardInput
import com.example.data.model.SavedTravelerProfile
import com.example.data.remote.HolidayPackageAggregatorCatalog
import com.example.domain.pricing.DisplayCurrency
import com.example.ui.components.HolidayPackageUltraCard
import com.example.ui.components.InclusionMiniChip
import com.example.ui.components.InteractivePackageRouteMapCard
import com.example.ui.components.ItineraryAccordionSection
import com.example.ui.components.PackageBadgePill
import com.example.ui.components.SourceConnectorPill
import com.example.ui.viewmodel.PackageCatalogFilter

private val GoldAccent = com.example.ui.theme.ChampagneGold
private val DeepNavy = com.example.ui.theme.RoyalSapphireNavy
private val EmeraldBadge = com.example.ui.theme.EmeraldSuccess

/**
 * Aba "Pacotes & Experiências" do AguiaFly:
 * - Unifica os resultados das APIs de Agregadores (GetYourGuide / Viator / Civitatis)
 *   e os pacotes cadastrados manualmente no Painel Admin CMS
 * - Design moderno, dinâmico e de alto impacto visual (estilo Airbnb Luxe / Instagram)
 * - Acesso direto ao Admin Dashboard (CMS), Detalhe com Mapa Interativo + Accordion de Itinerário
 *   e Checkout Direto / Sinal de Reserva via Stripe.
 */
@Composable
fun HolidayPackagesScreen(
    packages: List<HolidayPackage>,
    adminCmsPackages: List<HolidayPackage>,
    userPackageBookings: List<PackageBookingRecord>,
    activeFilter: PackageCatalogFilter,
    searchQuery: String,
    selectedCurrency: DisplayCurrency,
    savedTravelerProfile: SavedTravelerProfile?,
    selectedPackageForDetail: HolidayPackage?,
    selectedPackageForCheckout: HolidayPackage?,
    showAdminCmsDialog: Boolean,
    isSavingCmsPackage: Boolean,
    isProcessingBooking: Boolean,
    onSelectFilter: (PackageCatalogFilter) -> Unit,
    onUpdateSearchQuery: (String) -> Unit,
    onOpenPackageDetail: (HolidayPackage) -> Unit,
    onClosePackageDetail: () -> Unit,
    onOpenPackageCheckout: (HolidayPackage) -> Unit,
    onClosePackageCheckout: () -> Unit,
    onSetShowAdminCmsDialog: (Boolean) -> Unit,
    onCreateCmsPackage: (
        title: String,
        destinationCity: String,
        destinationIata: String,
        durationNights: Int,
        netPricePerPersonEur: Double,
        aguiaflyCommissionPercent: Double,
        aguiaflyFixedFeeEur: Double,
        depositPercentage: Int,
        availableSpots: Int,
        partnerAgencyName: String,
        badgeType: PackageHighlightBadge,
        selectedInclusions: Set<PackageInclusionItem>,
        galleryPhotoUrls: List<String>,
        itineraryDays: List<PackageItineraryDay>,
        routeWaypoints: List<PackageRouteWaypoint>
    ) -> Unit,
    onDeleteCmsPackage: (HolidayPackage) -> Unit,
    onConfirmStripeBooking: (
        pkg: HolidayPackage,
        guestsCount: Int,
        paymentMode: PackagePaymentMode,
        travelerFullName: String,
        travelerEmail: String,
        travelerPhone: String,
        cardInput: PaymentCardInput
    ) -> Unit,
    onBackToFlights: () -> Unit
) {
    BackHandler {
        when {
            selectedPackageForCheckout != null -> onClosePackageCheckout()
            selectedPackageForDetail != null -> onClosePackageDetail()
            showAdminCmsDialog -> onSetShowAdminCmsDialog(false)
            else -> onBackToFlights()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("holiday_packages_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header Executivo + Botão Admin Dashboard (CMS) + Status Conectores API
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AGUIAFLY LUXURY CURATION • CMS + API",
                                color = GoldAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Pacotes de Férias & Experiências",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Agências Parceiras (Admin CMS) + GetYourGuide • Viator • Civitatis",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { onSetShowAdminCmsDialog(true) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = DeepNavy
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("open_admin_cms_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddBusiness,
                                contentDescription = "Admin CMS",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin CMS",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Status dos Conectores API Unificados
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = EmeraldBadge,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Motor Unificado Ativo: Admin CMS (${adminCmsPackages.size + 3}) + APIs GetYourGuide / Viator / Civitatis",
                            color = EmeraldBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Barra de Busca Instantânea por Destino, Agência ou Experiência
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onUpdateSearchQuery,
                        placeholder = {
                            Text(
                                "Buscar pacote, ilha, país ou agência (ex: Maldivas, Capri, Suíça, Paris)...",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = GoldAccent)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { onUpdateSearchQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpar", tint = Color.White)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("packages_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.22f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedPlaceholderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedPlaceholderColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // 2. Filtros Dinâmicos (Todos, Exclusivos Agências CMS, APIs, Tudo Incluído, Últimas Vagas)
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PackageCatalogFilter.entries) { filterOption ->
                    val selected = activeFilter == filterOption
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filterOption) },
                        label = {
                            Text(
                                text = filterOption.label,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldAccent,
                            selectedLabelColor = DeepNavy,
                            containerColor = Color(0xFF102244),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Secção de Reservas de Pacotes Confirmadas do Utilizador (se existirem)
        if (userPackageBookings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B253A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldBadge.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ConfirmationNumber,
                                contentDescription = null,
                                tint = EmeraldBadge,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Minhas Reservas de Pacotes & Experiências (${userPackageBookings.size})",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        userPackageBookings.take(3).forEach { bkg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DeepNavy.copy(alpha = 0.75f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${bkg.packageTitle} • ${bkg.guestsCount} pax",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Código AguiaFly: ${bkg.bookingCode} • Parceiro: ${bkg.partnerConfirmationCode}",
                                            color = GoldAccent,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = selectedCurrency.format(bkg.amountChargedNowEur),
                                            color = EmeraldBadge,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = if (bkg.status == "DEPOSIT_PAID") "Sinal Pago" else "100% Confirmado",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Lista Unificada de Pacotes & Experiências Ultra-Premium
        items(packages, key = { it.id }) { pkg ->
            HolidayPackageUltraCard(
                pkg = pkg,
                selectedCurrency = selectedCurrency,
                onOpenDetails = onOpenPackageDetail,
                onDirectReserve = onOpenPackageCheckout
            )
        }
    }

    // Modal de Detalhes Completos do Pacote: Carrossel HD + Mapa Interativo do Roteiro + Accordion Dia a Dia
    selectedPackageForDetail?.let { detailPkg ->
        HolidayPackageFullDetailDialog(
            pkg = detailPkg,
            selectedCurrency = selectedCurrency,
            onDismiss = onClosePackageDetail,
            onProceedToReserve = {
                onClosePackageDetail()
                onOpenPackageCheckout(detailPkg)
            }
        )
    }

    // Modal de Checkout Direto / Sinal de Reserva via Stripe
    selectedPackageForCheckout?.let { checkoutPkg ->
        PackageDirectStripeCheckoutDialog(
            pkg = checkoutPkg,
            selectedCurrency = selectedCurrency,
            savedTravelerProfile = savedTravelerProfile,
            isProcessing = isProcessingBooking,
            onDismiss = onClosePackageCheckout,
            onConfirmStripeBooking = { guests, mode, name, email, phone, card ->
                onConfirmStripeBooking(checkoutPkg, guests, mode, name, email, phone, card)
            }
        )
    }

    // Modal do Painel de Gestão Manual (Admin CMS)
    if (showAdminCmsDialog) {
        AdminCmsCreatePackageDialog(
            existingAdminPackages = adminCmsPackages,
            isSaving = isSavingCmsPackage,
            onDismiss = { onSetShowAdminCmsDialog(false) },
            onDeleteExistingPackage = onDeleteCmsPackage,
            onCreatePackage = onCreateCmsPackage
        )
    }
}

/**
 * Tela/Modal de Detalhes Completos de um Pacote de Férias ou Experiência Exclusiva:
 * - Carrossel de Fotos HD em tela ampla
 * - Badges destacadas + Conector de origem (CMS ou API)
 * - Ícones de Inclusão detalhados
 * - Mapa Interativo do Roteiro (Canvas com etapas GPS clicáveis)
 * - Aba Sanfonada (Accordion) com Itinerário Dia a Dia
 * - CTA para Reserva Instantânea ou Sinal via Stripe
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HolidayPackageFullDetailDialog(
    pkg: HolidayPackage,
    selectedCurrency: DisplayCurrency,
    onDismiss: () -> Unit,
    onProceedToReserve: () -> Unit
) {
    val photos = remember(pkg.galleryPhotosJson) {
        pkg.galleryPhotos.ifEmpty {
            listOf("https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=85")
        }
    }
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val inclusions = remember(pkg.inclusionsCsv) { pkg.inclusions }
    val itineraryDays = remember(pkg.itineraryDaysJson) { pkg.itineraryDays }
    val waypoints = remember(pkg.routeWaypointsJson) { pkg.routeWaypoints }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DeepNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero Carrossel HD
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = photos[page],
                            contentDescription = pkg.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Transparent,
                                        DeepNavy
                                    )
                                )
                            )
                    )

                    // Botão Voltar + Badges
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DeepNavy.copy(alpha = 0.78f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PackageBadgePill(badge = pkg.effectiveBadge)
                            SourceConnectorPill(source = pkg.sourceEnum)
                        }
                    }

                    // Título sobre o Hero
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(16.dp)
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = pkg.title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Operador Verificado: ${pkg.partnerAgencyName} • Cód: ${pkg.packageCode}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Resumo de Inclusões
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Serviços Incluídos nesta Experiência:",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            inclusions.forEach { item ->
                                InclusionMiniChip(item = item)
                            }
                        }
                    }

                    // Mapa Interativo do Roteiro
                    InteractivePackageRouteMapCard(waypoints = waypoints)

                    // Aba Sanfonada (Accordion) do Itinerário Dia a Dia
                    ItineraryAccordionSection(itineraryDays = itineraryDays)

                    // Barra Fixa de Reserva Direta ou Sinal via Stripe
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF102348)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
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
                                Column {
                                    Text(
                                        text = "${selectedCurrency.format(pkg.finalPricePerPersonEur)} / pessoa",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "Sinal de Reserva (${pkg.depositPercentage}%): ${selectedCurrency.format(pkg.depositPerPersonEur)}",
                                        color = EmeraldBadge,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GoldAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "${pkg.availableSpots} vagas restantes",
                                        color = GoldAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = onProceedToReserve,
                                enabled = pkg.availableSpots > 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("detail_proceed_to_checkout_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GoldAccent,
                                    contentColor = DeepNavy
                                )
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RESERVAR AGORA OU PAGAR SINAL VIA STRIPE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
