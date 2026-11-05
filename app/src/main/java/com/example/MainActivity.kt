package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.LuxuryAuthGateScreen
import com.example.auth.configureAppCheck
import com.example.auth.signOutUser
import com.example.data.repository.FlightRepository
import com.example.data.repository.HolidayPackageFirestoreRepository
import com.example.data.repository.PriceAlertFirestoreRepository
import com.example.data.repository.TicketFirestoreRepository
import com.example.data.repository.TravelerProfileFirestoreRepository
import com.example.notifications.PushNotificationHelper
import com.example.ui.screens.AiConciergeScreen
import com.example.ui.screens.AircraftDetailScreen
import com.example.ui.screens.CheckoutScreen
import com.example.ui.screens.HolidayPackagesScreen
import com.example.ui.screens.ProfilePassportScreen
import com.example.ui.screens.SearchAndResultsScreen
import com.example.ui.screens.TicketsWalletScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AiConciergeViewModel
import com.example.ui.viewmodel.FlightBookingViewModel
import com.example.ui.viewmodel.FlightFilterOption
import com.example.ui.viewmodel.HolidayPackagesViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

enum class MainDestination {
    SEARCH,
    HOLIDAY_PACKAGES,
    AI_CONCIERGE,
    TICKETS_WALLET,
    PROFILE_PASSPORT,
    AIRCRAFT_DETAIL,
    CHECKOUT
}

class MainActivity : ComponentActivity() {
    private var latestIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        latestIntentState = intent
        configureAppCheck(this, intent)
        PushNotificationHelper.ensureNotificationChannel(this)
        PushNotificationHelper.scheduleBackgroundGlobalPromoRadar(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation(
                    incomingIntent = latestIntentState,
                    onConsumePromoIntent = {
                        latestIntentState?.removeExtra("promo_deal_id")
                        latestIntentState?.removeExtra("promo_origin")
                        latestIntentState?.removeExtra("promo_destination")
                        latestIntentState = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        latestIntentState = intent
    }
}

@Composable
fun AppNavigation(
    incomingIntent: Intent? = null,
    onConsumePromoIntent: () -> Unit = {}
) {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val hasIncomingPromo = incomingIntent?.getStringExtra("promo_destination") != null ||
        incomingIntent?.getStringExtra("promo_deal_id") != null
    var showIntroSplash by remember { mutableStateOf(!hasIncomingPromo) }

    LaunchedEffect(hasIncomingPromo) {
        if (hasIncomingPromo) {
            showIntroSplash = false
        }
    }

    val startupPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissões de Localização, Notificações Push e Câmara processadas automaticamente ao abrir o app
    }

    LaunchedEffect(showIntroSplash) {
        if (!showIntroSplash) {
            val permissions = buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                add(Manifest.permission.CAMERA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
            startupPermissionsLauncher.launch(permissions)
        }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val user = currentUser
        if (user == null) {
            LuxuryAuthGateScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        } else {
            AetherAuthenticatedShell(
                currentUserId = user.uid,
                incomingIntent = incomingIntent,
                onConsumePromoIntent = onConsumePromoIntent,
                onReplayIntro = { showIntroSplash = true },
                onSignedOut = { currentUser = null }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showIntroSplash,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()
        ) {
            com.example.ui.components.AguiaFlyEntrySplashOverlay(
                onFinished = { showIntroSplash = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AetherAuthenticatedShell(
    currentUserId: String,
    incomingIntent: Intent? = null,
    onConsumePromoIntent: () -> Unit = {},
    onReplayIntro: () -> Unit = {},
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val bookingViewModel: FlightBookingViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                FlightBookingViewModel(
                    ticketRepository = TicketFirestoreRepository(db),
                    priceAlertRepository = PriceAlertFirestoreRepository(db),
                    travelerProfileRepository = TravelerProfileFirestoreRepository(db),
                    flightRepository = FlightRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )
    val packagesViewModel: HolidayPackagesViewModel = viewModel(
        key = "pkg_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                HolidayPackagesViewModel(
                    repository = HolidayPackageFirestoreRepository(db),
                    flightRepository = FlightRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )
    val aiViewModel: AiConciergeViewModel = viewModel()

    var currentScreen by remember { mutableStateOf(MainDestination.SEARCH) }

    val origin by bookingViewModel.originIata.collectAsStateWithLifecycle()
    val dest by bookingViewModel.destinationIata.collectAsStateWithLifecycle()
    val date by bookingViewModel.departureDate.collectAsStateWithLifecycle()
    val returnDate by bookingViewModel.returnDate.collectAsStateWithLifecycle()
    val tripType by bookingViewModel.tripType.collectAsStateWithLifecycle()
    val passengers by bookingViewModel.passengerComposition.collectAsStateWithLifecycle()
    val cabin by bookingViewModel.cabinClass.collectAsStateWithLifecycle()
    val filter by bookingViewModel.activeFilter.collectAsStateWithLifecycle()
    val markupConfig by bookingViewModel.markupConfig.collectAsStateWithLifecycle()
    val offers by bookingViewModel.displayedOffers.collectAsStateWithLifecycle()
    val priceAlerts by bookingViewModel.priceAlerts.collectAsStateWithLifecycle()
    val isSearching by bookingViewModel.isSearching.collectAsStateWithLifecycle()
    val selectedOffer by bookingViewModel.selectedOffer.collectAsStateWithLifecycle()
    val ancillaries by bookingViewModel.selectedAncillaries.collectAsStateWithLifecycle()
    val aircraftPhotos by bookingViewModel.aircraftPhotos.collectAsStateWithLifecycle()
    val isIssuing by bookingViewModel.isIssuingOrder.collectAsStateWithLifecycle()
    val tickets by bookingViewModel.issuedTickets.collectAsStateWithLifecycle()
    val statusBanner by bookingViewModel.statusBanner.collectAsStateWithLifecycle()
    val promotionalDeals by bookingViewModel.promotionalDeals.collectAsStateWithLifecycle()
    val autoDetectedPromoDeal by bookingViewModel.autoDetectedPromoDeal.collectAsStateWithLifecycle()
    val isRefreshingPromos by bookingViewModel.isRefreshingPromos.collectAsStateWithLifecycle()
    val detectedAirport by bookingViewModel.detectedAirport.collectAsStateWithLifecycle()
    val selectedCurrency by bookingViewModel.selectedCurrency.collectAsStateWithLifecycle()
    val savedTravelerProfile by bookingViewModel.savedTravelerProfile.collectAsStateWithLifecycle()
    val cloudPassportVisualProfile by bookingViewModel.cloudPassportVisualProfile.collectAsStateWithLifecycle()
    val activeCancellationTicket by bookingViewModel.activeCancellationTicket.collectAsStateWithLifecycle()
    val activeCancellationQuote by bookingViewModel.activeCancellationQuote.collectAsStateWithLifecycle()
    val isLoadingCancellationQuote by bookingViewModel.isLoadingCancellationQuote.collectAsStateWithLifecycle()
    val isConfirmingCancellation by bookingViewModel.isConfirmingCancellation.collectAsStateWithLifecycle()
    val confirmedCancellations by bookingViewModel.confirmedCancellations.collectAsStateWithLifecycle()
    val ticketEsimSelections by bookingViewModel.ticketEsimSelections.collectAsStateWithLifecycle()
    val ticketLoungeSelections by bookingViewModel.ticketLoungeSelections.collectAsStateWithLifecycle()
    val ticketExperienceSelections by bookingViewModel.ticketExperienceSelections.collectAsStateWithLifecycle()
    val liveSeatMap by bookingViewModel.seatMap.collectAsStateWithLifecycle()
    val isLoadingSeatMap by bookingViewModel.isLoadingSeatMap.collectAsStateWithLifecycle()
    val seatConflictError by bookingViewModel.seatConflictError.collectAsStateWithLifecycle()
    val priceChangeState by bookingViewModel.priceChangeState.collectAsStateWithLifecycle()

    val unifiedPackages by packagesViewModel.unifiedPackages.collectAsStateWithLifecycle()
    val adminCmsPackages by packagesViewModel.adminCmsPackages.collectAsStateWithLifecycle()
    val userPackageBookings by packagesViewModel.userPackageBookings.collectAsStateWithLifecycle()
    val packageFilter by packagesViewModel.activeFilter.collectAsStateWithLifecycle()
    val packageSearchQuery by packagesViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPackageForDetail by packagesViewModel.selectedPackageForDetail.collectAsStateWithLifecycle()
    val selectedPackageForCheckout by packagesViewModel.selectedPackageForCheckout.collectAsStateWithLifecycle()
    val showAdminCmsSheet by packagesViewModel.showAdminCmsSheet.collectAsStateWithLifecycle()
    val isSavingCmsPackage by packagesViewModel.isSavingCmsPackage.collectAsStateWithLifecycle()
    val isProcessingPackageBooking by packagesViewModel.isProcessingBooking.collectAsStateWithLifecycle()
    val packageStatusMessage by packagesViewModel.statusMessage.collectAsStateWithLifecycle()

    val chatHistory by aiViewModel.chatHistory.collectAsStateWithLifecycle()
    val aiState by aiViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(packageStatusMessage) {
        packageStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            packagesViewModel.clearStatusMessage()
        }
    }

    // Restaura automaticamente do Firestore a foto de perfil, foto de capa e fotos dos carimbos logo ao entrar no app
    LaunchedEffect(cloudPassportVisualProfile, currentUserId) {
        val cloud = cloudPassportVisualProfile
        if (cloud != null) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val local = com.example.data.model.ProfilePassportStorage.loadState(context, currentUserId)
                com.example.data.model.ProfilePassportStorage.mergeAndRestoreCloudVisualProfile(
                    context = context,
                    userId = currentUserId,
                    localState = local,
                    cloud = cloud
                )
            }
        }
    }

    LaunchedEffect(incomingIntent) {
        val promoDealId = incomingIntent?.getStringExtra("promo_deal_id")
        val promoOrigin = incomingIntent?.getStringExtra("promo_origin")
        val promoDest = incomingIntent?.getStringExtra("promo_destination")
        if (!promoDealId.isNullOrBlank() || !promoDest.isNullOrBlank()) {
            bookingViewModel.selectPromotionalDealFromPushIntent(
                promoDealId = promoDealId,
                promoOrigin = promoOrigin,
                promoDestination = promoDest,
                onNavigateToCheckout = {
                    currentScreen = MainDestination.CHECKOUT
                }
            )
            onConsumePromoIntent()
        }
    }

    LaunchedEffect(autoDetectedPromoDeal) {
        autoDetectedPromoDeal?.let { deal ->
            PushNotificationHelper.triggerAutoGlobalPromoRadarIfEligible(
                context = context,
                currentOriginIata = origin,
                deals = promotionalDeals.ifEmpty { listOf(deal) }
            )
            bookingViewModel.consumeAutoDetectedPromoDeal()
        }
    }

    LaunchedEffect(statusBanner) {
        statusBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            bookingViewModel.clearBanner()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    com.example.ui.components.AguiaFlyTopBarLogo(
                        onReplayIntro = onReplayIntro
                    )
                },
                actions = {
                    IconButton(
                        onClick = { currentScreen = MainDestination.PROFILE_PASSPORT },
                        modifier = Modifier.testTag("top_bar_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil e Passaporte Digital",
                            tint = if (currentScreen == MainDestination.PROFILE_PASSPORT) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = { signOutUser(context, onSignedOut, scope) },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sair da conta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentScreen in listOf(
                        MainDestination.SEARCH,
                        MainDestination.AIRCRAFT_DETAIL,
                        MainDestination.CHECKOUT
                    ),
                    onClick = { currentScreen = MainDestination.SEARCH },
                    icon = { Icon(Icons.Default.FlightTakeoff, contentDescription = "Voos NDC") },
                    label = { Text("Voos") },
                    modifier = Modifier.testTag("nav_flights_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == MainDestination.HOLIDAY_PACKAGES,
                    onClick = { currentScreen = MainDestination.HOLIDAY_PACKAGES },
                    icon = { Icon(Icons.Default.BeachAccess, contentDescription = "Pacotes e Experiências") },
                    label = { Text("Pacotes") },
                    modifier = Modifier.testTag("nav_packages_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == MainDestination.AI_CONCIERGE,
                    onClick = { currentScreen = MainDestination.AI_CONCIERGE },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Concierge IA") },
                    label = { Text("Concierge") },
                    modifier = Modifier.testTag("nav_ai_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == MainDestination.TICKETS_WALLET,
                    onClick = { currentScreen = MainDestination.TICKETS_WALLET },
                    icon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = "Minhas Viagens") },
                    label = { Text("Viagens (${tickets.size + userPackageBookings.size})") },
                    modifier = Modifier.testTag("nav_tickets_tab")
                )
                NavigationBarItem(
                    selected = currentScreen == MainDestination.PROFILE_PASSPORT,
                    onClick = { currentScreen = MainDestination.PROFILE_PASSPORT },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Perfil e Passaporte") },
                    label = { Text("Perfil") },
                    modifier = Modifier.testTag("nav_profile_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                MainDestination.HOLIDAY_PACKAGES -> HolidayPackagesScreen(
                    packages = unifiedPackages,
                    adminCmsPackages = adminCmsPackages,
                    userPackageBookings = userPackageBookings,
                    activeFilter = packageFilter,
                    searchQuery = packageSearchQuery,
                    selectedCurrency = selectedCurrency,
                    savedTravelerProfile = savedTravelerProfile,
                    selectedPackageForDetail = selectedPackageForDetail,
                    selectedPackageForCheckout = selectedPackageForCheckout,
                    showAdminCmsDialog = showAdminCmsSheet,
                    isSavingCmsPackage = isSavingCmsPackage,
                    isProcessingBooking = isProcessingPackageBooking,
                    onSelectFilter = packagesViewModel::setFilter,
                    onUpdateSearchQuery = packagesViewModel::updateSearchQuery,
                    onOpenPackageDetail = packagesViewModel::openPackageDetail,
                    onClosePackageDetail = packagesViewModel::closePackageDetail,
                    onOpenPackageCheckout = packagesViewModel::openPackageCheckout,
                    onClosePackageCheckout = packagesViewModel::closePackageCheckout,
                    onSetShowAdminCmsDialog = packagesViewModel::setShowAdminCmsSheet,
                    onCreateCmsPackage = { title, destCity, destIata, nights, netEur, commPct, fixedEur, depPct, spots, agency, badge, incl, photos, days, wps ->
                        packagesViewModel.createManualCmsPackage(
                            title = title,
                            destinationCity = destCity,
                            destinationIata = destIata,
                            durationNights = nights,
                            netPricePerPersonEur = netEur,
                            aguiaflyCommissionPercent = commPct,
                            aguiaflyFixedFeeEur = fixedEur,
                            depositPercentage = depPct,
                            availableSpots = spots,
                            partnerAgencyName = agency,
                            badgeType = badge,
                            selectedInclusions = incl,
                            galleryPhotoUrls = photos,
                            itineraryDays = days,
                            routeWaypoints = wps
                        )
                    },
                    onDeleteCmsPackage = packagesViewModel::deleteManualCmsPackage,
                    onConfirmStripeBooking = { pkg, guests, mode, fullName, email, phone, card ->
                        packagesViewModel.completePackageStripeCheckout(
                            pkg = pkg,
                            guestsCount = guests,
                            paymentMode = mode,
                            travelerFullName = fullName,
                            travelerEmail = email,
                            travelerPhone = phone,
                            cardInput = card
                        )
                    },
                    onBackToFlights = { currentScreen = MainDestination.SEARCH }
                )

                MainDestination.SEARCH -> SearchAndResultsScreen(
                    originIata = origin,
                    destinationIata = dest,
                    departureDate = date,
                    cabinClass = cabin,
                    activeFilter = filter,
                    offers = offers,
                    priceAlerts = priceAlerts,
                    isSearching = isSearching,
                    isDuffelLive = bookingViewModel.isDuffelLive,
                    onUpdateSearchParams = bookingViewModel::updateSearchParams,
                    onExecuteSearch = bookingViewModel::executeSearch,
                    onSelectFilter = bookingViewModel::setFilter,
                    onCreatePriceAlert = { orig, destIata, cab, targetEur, baseEur ->
                        bookingViewModel.createRoutePriceAlert(
                            originIata = orig,
                            destinationIata = destIata,
                            cabinClass = cab,
                            targetMaxPriceEur = targetEur,
                            baselinePriceEur = baseEur
                        )
                    },
                    onDeletePriceAlert = bookingViewModel::deleteRoutePriceAlert,
                    onTriggerPromoCronSweep = {
                        bookingViewModel.runPromotionalFareCronSweep(
                            onPromoFound = { alert, promoPriceEur, airlineName ->
                                PushNotificationHelper.showPromotionalPriceNotification(
                                    context = context,
                                    originIata = alert.originIata,
                                    destinationIata = alert.destinationIata,
                                    cabinClass = alert.cabinClass,
                                    promoPriceEur = promoPriceEur,
                                    baselinePriceEur = alert.baselinePriceEur,
                                    airlineName = airlineName
                                )
                            },
                            onGlobalCountryDealTriggered = { deal ->
                                PushNotificationHelper.showAutomaticGlobalCountryPromoNotification(
                                    context = context,
                                    deal = deal
                                )
                            }
                        )
                    },
                    onSelectOfferForCheckout = { offer ->
                        bookingViewModel.selectOffer(offer, resetAncillaries = true)
                        currentScreen = MainDestination.CHECKOUT
                    },
                    onInspectAircraft = { offer ->
                        bookingViewModel.selectOffer(offer, resetAncillaries = true)
                        currentScreen = MainDestination.AIRCRAFT_DETAIL
                    },
                    onSearchAirportPlaces = bookingViewModel::searchAirportPlaces,
                    returnDate = returnDate,
                    tripType = tripType,
                    passengers = passengers,
                    issuedTicketsCount = tickets.size,
                    promotionalDeals = promotionalDeals,
                    isRefreshingPromos = isRefreshingPromos,
                    onRefreshPromos = bookingViewModel::refreshPromotionalDeals,
                    onSelectTripType = bookingViewModel::updateTripType,
                    onUpdateReturnDate = bookingViewModel::updateReturnDate,
                    onUpdatePassengers = bookingViewModel::updatePassengerComposition,
                    onSelectDealPromotion = { deal ->
                        bookingViewModel.selectPromotionalDeal(deal) {
                            currentScreen = MainDestination.CHECKOUT
                        }
                    },
                    detectedAirport = detectedAirport,
                    selectedCurrency = selectedCurrency,
                    onDetectNearestAirport = bookingViewModel::applyDetectedNearestAirport,
                    onSelectCurrency = bookingViewModel::updateSelectedCurrency,
                    selectedVipLounges = ancillaries.selectedVipLounges,
                    onToggleVipLoungePass = bookingViewModel::toggleVipLoungePass
                )

                MainDestination.AIRCRAFT_DETAIL -> {
                    val offer = selectedOffer ?: offers.firstOrNull()
                    if (offer != null) {
                        AircraftDetailScreen(
                            offer = offer,
                            photos = aircraftPhotos,
                            selectedSeat = ancillaries.seatDesignator,
                            onSelectSeat = bookingViewModel::updateSeat,
                            onProceedToCheckout = { currentScreen = MainDestination.CHECKOUT },
                            onBack = { currentScreen = MainDestination.SEARCH },
                            seatMap = liveSeatMap,
                            isLoadingSeatMap = isLoadingSeatMap
                        )
                    } else {
                        currentScreen = MainDestination.SEARCH
                    }
                }

                MainDestination.CHECKOUT -> {
                    val offer = selectedOffer ?: offers.firstOrNull()
                    if (offer != null) {
                        CheckoutScreen(
                            offer = offer,
                            ancillaries = ancillaries,
                            markupConfig = markupConfig,
                            isIssuing = isIssuing,
                            isStripeLive = bookingViewModel.isStripeLive,
                            isTestMode = bookingViewModel.isTestModeActive,
                            savedTravelerProfile = savedTravelerProfile,
                            onSaveTravelerVaultNow = bookingViewModel::saveTravelerProfileToVault,
                            onDeleteTravelerVault = bookingViewModel::deleteTravelerProfileFromVault,
                            onSelectAutoFreeSeat = {
                                bookingViewModel.updateSeat(com.example.data.model.AUTO_FREE_SEAT)
                            },
                            onClearAllAncillaries = bookingViewModel::clearAllAncillaries,
                            onToggleExtraBag = bookingViewModel::toggleExtraBaggage,
                            onTogglePriorityLounge = bookingViewModel::togglePriorityLounge,
                            onInspectAircraftAndSeats = { currentScreen = MainDestination.AIRCRAFT_DETAIL },
                            onSelectAiraloEsim = bookingViewModel::selectAiraloEsimPackage,
                            onToggleVipLoungePass = bookingViewModel::toggleVipLoungePass,
                            onToggleDestinationExperience = bookingViewModel::toggleDestinationExperience,
                            onPayAndIssueTicket = { fn, ln, em, ph, docInfo, cardInput, saveToVault, ffProg, ffNum ->
                                bookingViewModel.completeStripeCheckoutAndIssueOrder(
                                    firstName = fn,
                                    lastName = ln,
                                    email = em,
                                    phone = ph,
                                    documentInfo = docInfo,
                                    cardInput = cardInput,
                                    saveToTravelerVault = saveToVault,
                                    frequentFlyerProgram = ffProg,
                                    frequentFlyerNumber = ffNum,
                                    onOrderIssued = {
                                        currentScreen = MainDestination.TICKETS_WALLET
                                    }
                                )
                            },
                            onBack = { currentScreen = MainDestination.SEARCH },
                            seatConflictError = seatConflictError,
                            onDismissSeatConflict = bookingViewModel::clearSeatConflictError,
                            onResolveSeatConflict = { currentScreen = MainDestination.AIRCRAFT_DETAIL },
                            priceChangeState = priceChangeState,
                            onAcceptUpdatedPrice = bookingViewModel::acceptUpdatedPriceAndProceed,
                            onDismissPriceChange = bookingViewModel::dismissPriceChangeDialog,
                            onCancelAndReturnToSearch = {
                                bookingViewModel.dismissPriceChangeDialog()
                                currentScreen = MainDestination.SEARCH
                            }
                        )
                    } else {
                        currentScreen = MainDestination.SEARCH
                    }
                }

                MainDestination.AI_CONCIERGE -> AiConciergeScreen(
                    chatHistory = chatHistory,
                    uiState = aiState,
                    onSendPrompt = aiViewModel::askConcierge,
                    onApplyRecommendation = { rec ->
                        bookingViewModel.updateSearchParams(
                            origin = rec.recommendedOriginIata,
                            destination = rec.recommendedDestinationIata,
                            date = rec.recommendedDate,
                            cabin = rec.recommendedCabinClass
                        )
                        if (rec.preferWidebody) {
                            bookingViewModel.setFilter(FlightFilterOption.WIDEBODY_ONLY)
                        } else {
                            bookingViewModel.setFilter(FlightFilterOption.ALL)
                        }
                        bookingViewModel.executeSearch()
                        currentScreen = MainDestination.SEARCH
                    }
                )

                MainDestination.TICKETS_WALLET -> TicketsWalletScreen(
                    tickets = tickets,
                    onToggleGoogleWallet = bookingViewModel::toggleGoogleWalletStatus,
                    onRequestVipSupportForTicket = { ticket ->
                        aiViewModel.askConcierge(
                            "Preciso de assistência Concierge VIP prioritária para minha reserva confirmada PNR ${ticket.bookingReference} no voo ${ticket.flightNumber} (${ticket.airlineName}) de ${ticket.originCity} (${ticket.originIata}) para ${ticket.destinationCity} (${ticket.destinationIata}) em ${ticket.departureIso.take(10)}, cabine ${ticket.cabinClass}."
                        )
                        currentScreen = MainDestination.AI_CONCIERGE
                    },
                    activeCancellationTicket = activeCancellationTicket,
                    activeCancellationQuote = activeCancellationQuote,
                    isLoadingCancellationQuote = isLoadingCancellationQuote,
                    isConfirmingCancellation = isConfirmingCancellation,
                    confirmedCancellations = confirmedCancellations,
                    onRequestCancellationQuote = bookingViewModel::requestTicketCancellationQuote,
                    onConfirmCancellationRefund = bookingViewModel::confirmTicketCancellationAndRefund,
                    onDismissCancellationDialog = bookingViewModel::dismissCancellationDialog,
                    ticketEsimSelections = ticketEsimSelections,
                    ticketLoungeSelections = ticketLoungeSelections,
                    ticketExperienceSelections = ticketExperienceSelections,
                    onSelectTicketEsim = bookingViewModel::selectTicketPostBookingEsim,
                    onToggleTicketLounge = bookingViewModel::toggleTicketPostBookingLounge,
                    onToggleTicketExperience = bookingViewModel::toggleTicketPostBookingExperience
                )

                MainDestination.PROFILE_PASSPORT -> ProfilePassportScreen(
                    currentUserId = currentUserId,
                    issuedTickets = tickets,
                    savedTravelerProfile = savedTravelerProfile,
                    selectedCurrency = selectedCurrency,
                    onSelectCurrency = bookingViewModel::updateSelectedCurrency,
                    onSaveTravelerProfile = bookingViewModel::saveTravelerProfileToVault,
                    onNavigateToFlights = { currentScreen = MainDestination.SEARCH },
                    onBackToHome = { currentScreen = MainDestination.SEARCH },
                    cloudPassportVisualProfile = cloudPassportVisualProfile,
                    onSyncVisualStateToCloud = { updatedVisualState ->
                        bookingViewModel.syncPassportVisualStateToCloud(context, updatedVisualState)
                    }
                )
            }
        }
    }
}
