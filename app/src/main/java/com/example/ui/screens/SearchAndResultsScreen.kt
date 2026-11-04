package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.EUROPEAN_HUBS
import com.example.data.model.EnrichedFlightOffer
import com.example.data.model.PassengerComposition
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.data.model.PriceAlertSubscription
import com.example.data.model.TravelPromotionDeal
import com.example.data.model.TripType
import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.location.DetectedAirportResult
import com.example.domain.location.NearestAirportLocator
import com.example.domain.pricing.DisplayCurrency
import com.example.notifications.PushNotificationHelper
import com.example.ui.components.ActivePriceAlertsBannerCard
import com.example.ui.components.AirportAutocompleteField
import com.example.ui.components.CreatePriceAlertActionButton
import com.example.ui.components.CreatePriceAlertDialog
import com.example.ui.components.FlightOfferCard
import com.example.ui.components.InteractiveFareCalendarSection
import com.example.ui.components.NearestAirportAndCurrencyBar
import com.example.ui.components.PromotionalDealsCarousel
import com.example.ui.components.TripAndPassengerSelectorBar
import com.example.ui.viewmodel.FlightFilterOption

@Composable
fun SearchAndResultsScreen(
    originIata: String,
    destinationIata: String,
    departureDate: String,
    cabinClass: String,
    activeFilter: FlightFilterOption,
    offers: List<EnrichedFlightOffer>,
    priceAlerts: List<PriceAlertSubscription>,
    isSearching: Boolean,
    isDuffelLive: Boolean,
    onUpdateSearchParams: (String, String, String, String) -> Unit,
    onExecuteSearch: () -> Unit,
    onSelectFilter: (FlightFilterOption) -> Unit,
    onCreatePriceAlert: (String, String, String, Double, Double) -> Unit,
    onDeletePriceAlert: (String) -> Unit,
    onTriggerPromoCronSweep: () -> Unit,
    onSelectOfferForCheckout: (EnrichedFlightOffer) -> Unit,
    onInspectAircraft: (EnrichedFlightOffer) -> Unit,
    onSearchAirportPlaces: suspend (String) -> List<AirportPlaceSuggestion> = { query ->
        PlaceAutocompleteCatalog.searchLocalCatalog(query)
    },
    returnDate: String = "2026-11-22",
    tripType: TripType = TripType.ONE_WAY,
    passengers: PassengerComposition = PassengerComposition(),
    issuedTicketsCount: Int = 0,
    promotionalDeals: List<TravelPromotionDeal> = emptyList(),
    isRefreshingPromos: Boolean = false,
    onRefreshPromos: () -> Unit = {},
    onSelectTripType: (TripType) -> Unit = {},
    onUpdateReturnDate: (String) -> Unit = {},
    onUpdatePassengers: (PassengerComposition) -> Unit = {},
    onSelectDealPromotion: (TravelPromotionDeal) -> Unit = {},
    detectedAirport: DetectedAirportResult? = null,
    selectedCurrency: DisplayCurrency = DisplayCurrency.EUR,
    onDetectNearestAirport: (DetectedAirportResult) -> Unit = {},
    onSelectCurrency: (DisplayCurrency) -> Unit = {},
    selectedVipLounges: List<com.example.data.model.VipLoungeOffer> = emptyList(),
    onToggleVipLoungePass: (com.example.data.model.VipLoungeOffer) -> Unit = {}
) {
    val context = LocalContext.current
    var localOrigin by remember { mutableStateOf(originIata) }
    var localDest by remember { mutableStateOf(destinationIata) }
    var localDate by remember { mutableStateOf(departureDate) }
    var localCabin by remember { mutableStateOf(cabinClass) }
    LaunchedEffect(originIata) { localOrigin = originIata }
    LaunchedEffect(destinationIata) { localDest = destinationIata }
    LaunchedEffect(departureDate) { localDate = departureDate }
    LaunchedEffect(cabinClass) { localCabin = cabinClass }
    var showCreateAlertDialog by remember { mutableStateOf(false) }
    var hasAutoDetectedLocation by rememberSaveable { mutableStateOf(false) }
    var hasTriggeredStartupPromoRadar by rememberSaveable { mutableStateOf(false) }
    var isAutoGlobalRadarEnabled by remember {
        mutableStateOf(PushNotificationHelper.isGlobalAutoPromoRadarEnabled(context))
    }

    val unifiedPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        NearestAirportLocator.requestActiveGpsAirportUpdate(context) { result ->
            localOrigin = result.airport.iataCode
            onDetectNearestAirport(result)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && isAutoGlobalRadarEnabled) {
            onTriggerPromoCronSweep()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasAutoDetectedLocation && detectedAirport == null) {
            hasAutoDetectedLocation = true
            val permissionsToRequest = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()

            if (NearestAirportLocator.hasLocationPermission(context)) {
                NearestAirportLocator.requestActiveGpsAirportUpdate(context) { result ->
                    localOrigin = result.airport.iataCode
                    onDetectNearestAirport(result)
                }
            } else {
                val initialFallback = NearestAirportLocator.detectNearestAirport(context)
                localOrigin = initialFallback.airport.iataCode
                onDetectNearestAirport(initialFallback)
            }

            unifiedPermissionsLauncher.launch(permissionsToRequest)
        }
    }

    // Agenda o radar inteligente em segundo plano sem disparar notificações duplicadas na abertura
    LaunchedEffect(isAutoGlobalRadarEnabled) {
        if (!hasTriggeredStartupPromoRadar && isAutoGlobalRadarEnabled) {
            hasTriggeredStartupPromoRadar = true
            PushNotificationHelper.scheduleBackgroundGlobalPromoRadar(context)
        }
    }

    val offersMatchCurrentSelection = remember(offers, localOrigin, localDest, localDate) {
        offers.isNotEmpty() &&
            offers.first().originIata.equals(localOrigin, ignoreCase = true) &&
            offers.first().destinationIata.equals(localDest, ignoreCase = true) &&
            offers.first().departureIso.take(10) == localDate.take(10)
    }

    val lowestCurrentFareEur = remember(
        offers,
        offersMatchCurrentSelection,
        localOrigin,
        localDest,
        localDate,
        localCabin,
        tripType,
        passengers
    ) {
        val exactFormulaFare = AirportAndLoyaltyCatalog.computeExactDisplayLowestFareEur(
            originIata = localOrigin,
            destinationIata = localDest,
            dateIso = localDate,
            cabinClass = localCabin,
            tripMultiplier = tripType.multiplier,
            passengerMultiplier = passengers.fareMultiplier
        )
        if (offersMatchCurrentSelection) {
            offers.minOfOrNull { it.pricing.displayFareBeforeAncillariesEur } ?: exactFormulaFare
        } else {
            exactFormulaFare
        }
    }

    val cabins = listOf(
        "economy" to "Economy",
        "premium_economy" to "Premium Eco",
        "business" to "Business",
        "first" to "First Class"
    )

    if (showCreateAlertDialog) {
        CreatePriceAlertDialog(
            initialOriginIata = localOrigin,
            initialDestinationIata = localDest,
            initialCabinClass = localCabin,
            currentLowestPriceEur = lowestCurrentFareEur,
            onDismiss = { showCreateAlertDialog = false },
            onConfirmCreateAlert = { orig, dest, cab, targetEur, baseEur ->
                showCreateAlertDialog = false
                onCreatePriceAlert(orig, dest, cab, targetEur, baseEur)
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Barra de Localização Automática (Aeroporto Mais Próximo) & Seletor de Moeda Visual
        item {
            NearestAirportAndCurrencyBar(
                detectedAirport = detectedAirport,
                selectedCurrency = selectedCurrency,
                onSelectCurrency = onSelectCurrency
            )
        }

        // Carrossel de Promoções Imperdíveis saindo do aeroporto mais próximo do cliente
        item {
            PromotionalDealsCarousel(
                currentOriginIata = localOrigin,
                dealsList = if (promotionalDeals.isNotEmpty()) promotionalDeals else com.example.data.model.PromotionalDealsCatalog.GLOBAL_DEALS,
                isRefreshingFares = isRefreshingPromos,
                isDuffelLive = isDuffelLive,
                selectedCurrency = selectedCurrency,
                onRefreshFares = onRefreshPromos,
                onSelectDealToBook = { deal ->
                    localOrigin = deal.originIata
                    localDest = deal.destinationIata
                    localDate = deal.departureDateIso
                    onUpdateSearchParams(deal.originIata, deal.destinationIata, deal.departureDateIso, localCabin)
                    onSelectDealPromotion(deal)
                }
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Busca Global de Voos",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Todas as Companhias • Voos Diretos & Conexões (${selectedCurrency.badgeLabel})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TripAndPassengerSelectorBar(
                        tripType = tripType,
                        passengers = passengers,
                        onSelectTripType = onSelectTripType,
                        onUpdatePassengers = onUpdatePassengers
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AirportAutocompleteField(
                            label = "Origem (Aeroporto mais próximo ou escolha outra cidade)",
                            placeholder = "Ex: São Paulo, Lisboa, Porto, Rio, GRU",
                            selectedIata = localOrigin,
                            isOrigin = true,
                            testTag = "origin_iata_input",
                            onQuerySuggestions = onSearchAirportPlaces,
                            onSelectIata = { newOriginIata ->
                                localOrigin = newOriginIata.uppercase()
                                onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                                onExecuteSearch()
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Rota selecionada: $localOrigin ➔ $localDest (${tripType.label} • ${passengers.summaryLabel})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = {
                                    val temp = localOrigin
                                    localOrigin = localDest
                                    localDest = temp
                                    onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                                    onExecuteSearch()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("swap_airports_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Inverter origem e destino",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        AirportAutocompleteField(
                            label = "Destino (Cidade, País ou IATA)",
                            placeholder = "Ex: Paris, Madrid, Lisboa, CDG",
                            selectedIata = localDest,
                            isOrigin = false,
                            testTag = "destination_iata_input",
                            onQuerySuggestions = onSearchAirportPlaces,
                            onSelectIata = { newDestIata ->
                                localDest = newDestIata.uppercase()
                                onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                                onExecuteSearch()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EUROPEAN_HUBS.take(6).forEach { hub ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (localOrigin != hub.iata) {
                                            localDest = hub.iata
                                            onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                                            onExecuteSearch()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "${hub.iata} ${hub.city}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    InteractiveFareCalendarSection(
                        departureDateIso = localDate,
                        returnDateIso = returnDate,
                        tripType = tripType,
                        lowestCurrentFareEur = lowestCurrentFareEur,
                        originIata = localOrigin,
                        destinationIata = localDest,
                        cabinClass = localCabin,
                        passengers = passengers,
                        selectedCurrency = selectedCurrency,
                        onSelectDepartureDate = { pickedDate ->
                            localDate = pickedDate
                            onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                            onExecuteSearch()
                        },
                        onSelectReturnDate = onUpdateReturnDate
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cabins.forEach { (code, label) ->
                            FilterChip(
                                selected = localCabin == code,
                                onClick = {
                                    localCabin = code
                                    onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                                    onExecuteSearch()
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("cabin_chip_$code")
                            )
                        }
                    }

                    Button(
                        onClick = onExecuteSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("search_flights_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Buscar Ofertas NDC (${selectedCurrency.badgeLabel})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    CreatePriceAlertActionButton(
                        originIata = localOrigin,
                        destinationIata = localDest,
                        activeAlertCount = priceAlerts.size,
                        onClick = {
                            showCreateAlertDialog = true
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !PushNotificationHelper.hasNotificationPermission(context)
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }

        if (priceAlerts.isNotEmpty()) {
            item {
                ActivePriceAlertsBannerCard(
                    alerts = priceAlerts,
                    currentOriginIata = localOrigin,
                    isAutoGlobalRadarEnabled = isAutoGlobalRadarEnabled,
                    onToggleAutoGlobalRadar = { enabled ->
                        isAutoGlobalRadarEnabled = enabled
                        PushNotificationHelper.setGlobalAutoPromoRadarEnabled(context, enabled)
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !PushNotificationHelper.hasNotificationPermission(context)
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onTriggerPromoCronSweep()
                            }
                        }
                    },
                    onSelectAlertRoute = { alert ->
                        localOrigin = alert.originIata
                        localDest = alert.destinationIata
                        localCabin = alert.cabinClass
                        onUpdateSearchParams(localOrigin, localDest, localDate, localCabin)
                        onExecuteSearch()
                    },
                    onDeleteAlert = onDeletePriceAlert
                )
            }
        }

        item {
            val origCityName = remember(localOrigin) {
                PlaceAutocompleteCatalog.resolveByIata(localOrigin).cityName
            }
            val destCityName = remember(localDest) {
                PlaceAutocompleteCatalog.resolveByIata(localDest).cityName
            }
            val shortDateLabel = remember(localDate) {
                val parts = localDate.take(10).split("-")
                if (parts.size == 3) "${parts[2]}/${parts[1]}" else localDate
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Barra Superior de Resultados estilo eDreams ("Lisboa - Fortaleza | qua, 30/12 👤 1 | [🔍 Alterar]")
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edreams_results_route_header")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Ícone amarelo/dourado estilo eDreams
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = androidx.compose.ui.graphics.Color(0xFFFACC15),
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Surface(
                                    shape = RoundedCornerShape(3.dp),
                                    color = androidx.compose.ui.graphics.Color(0xFFEAB308),
                                    modifier = Modifier.size(12.dp)
                                ) {}
                            }
                            Column {
                                Text(
                                    text = "$origCityName - $destCityName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = androidx.compose.ui.graphics.Color(0xFF1E293B)
                                )
                                Text(
                                    text = "$shortDateLabel  •  👤 ${passengers.summaryLabel} (${selectedCurrency.format(lowestCurrentFareEur)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier
                                .border(
                                    1.5.dp,
                                    androidx.compose.ui.graphics.Color(0xFF005AA3),
                                    RoundedCornerShape(50)
                                )
                                .clip(RoundedCornerShape(50))
                                .clickable { onExecuteSearch() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFF005AA3),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Alterar",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = androidx.compose.ui.graphics.Color(0xFF005AA3),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Pílulas de Filtro estilo eDreams ("⇅ Melhor", "Escalas", "Horários", "Widebody")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlightFilterOption.entries.forEachIndexed { idx, filter ->
                        val isSelected = activeFilter == filter
                        val pillLabel = when (idx) {
                            0 -> "⇅ Melhor (${offers.size})"
                            1 -> "Escalas (Diretos)"
                            else -> filter.label
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFilter(filter) },
                            shape = RoundedCornerShape(50),
                            label = {
                                Text(
                                    text = pillLabel,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = androidx.compose.ui.graphics.Color.White,
                                labelColor = androidx.compose.ui.graphics.Color(0xFF334155),
                                selectedContainerColor = androidx.compose.ui.graphics.Color(0xFFE0F2FE),
                                selectedLabelColor = androidx.compose.ui.graphics.Color(0xFF005AA3)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                                selectedBorderColor = androidx.compose.ui.graphics.Color(0xFF005AA3),
                                borderWidth = 1.dp,
                                selectedBorderWidth = 1.8.dp
                            ),
                            modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                        )
                    }
                }
            }
        }

        items(offers, key = { it.offerId }) { offer ->
            val isCheapestOffer = offer.offerId == offers.firstOrNull()?.offerId
            AnimatedVisibility(visible = true) {
                FlightOfferCard(
                    offer = offer,
                    isBestFareOffer = isCheapestOffer,
                    selectedCurrency = selectedCurrency,
                    onSelectOffer = onSelectOfferForCheckout,
                    onInspectAircraft = onInspectAircraft
                )
            }
        }
    }
}
