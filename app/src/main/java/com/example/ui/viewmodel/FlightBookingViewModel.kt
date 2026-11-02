package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.BodyType
import com.example.data.model.EnrichedFlightOffer
import com.example.data.model.FrequentFlyerSelection
import com.example.data.model.IssuedTicket
import com.example.data.model.MarkupConfig
import com.example.data.model.OrderCancellationConfirmation
import com.example.data.model.OrderCancellationQuote
import com.example.data.model.PassengerComposition
import com.example.data.model.PassengerDocumentInfo
import com.example.data.model.PaymentCardInput
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.data.model.PlanespottersPhoto
import com.example.data.model.PriceAlertSubscription
import com.example.data.model.SavedTravelerProfile
import com.example.data.model.SelectedAncillaries
import com.example.data.model.TravelPromotionDeal
import com.example.data.model.TripType
import com.example.data.repository.FlightRepository
import com.example.data.repository.PriceAlertFirestoreRepository
import com.example.data.repository.TicketFirestoreRepository
import com.example.data.repository.TravelerProfileFirestoreRepository
import com.example.domain.aircraft.AircraftSpecsCatalog
import com.example.domain.location.DetectedAirportResult
import com.example.domain.pricing.DisplayCurrency
import com.example.domain.pricing.MarkupEngine
import kotlin.math.round
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FlightFilterOption(val label: String) {
    ALL("Todos"),
    DIRECT_ONLY("Voos Diretos"),
    WITH_STOPS("Com Escalas"),
    LOWEST_PRICE("Menor Tarifa €"),
    FASTEST("Mais Rápido"),
    WIDEBODY_ONLY("Apenas Widebody")
}

class FlightBookingViewModel(
    private val ticketRepository: TicketFirestoreRepository,
    private val priceAlertRepository: PriceAlertFirestoreRepository,
    private val travelerProfileRepository: TravelerProfileFirestoreRepository,
    private val flightRepository: FlightRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _originIata = MutableStateFlow("LIS")
    val originIata: StateFlow<String> = _originIata.asStateFlow()

    private val _destinationIata = MutableStateFlow("CDG")
    val destinationIata: StateFlow<String> = _destinationIata.asStateFlow()

    private val _departureDate = MutableStateFlow("2026-11-15")
    val departureDate: StateFlow<String> = _departureDate.asStateFlow()

    private val _returnDate = MutableStateFlow("2026-11-22")
    val returnDate: StateFlow<String> = _returnDate.asStateFlow()

    private val _tripType = MutableStateFlow(TripType.ONE_WAY)
    val tripType: StateFlow<TripType> = _tripType.asStateFlow()

    private val _passengerComposition = MutableStateFlow(PassengerComposition())
    val passengerComposition: StateFlow<PassengerComposition> = _passengerComposition.asStateFlow()

    private val _frequentFlyerSelection = MutableStateFlow(FrequentFlyerSelection())
    val frequentFlyerSelection: StateFlow<FrequentFlyerSelection> = _frequentFlyerSelection.asStateFlow()

    private val _cabinClass = MutableStateFlow("business")
    val cabinClass: StateFlow<String> = _cabinClass.asStateFlow()

    private val _activeFilter = MutableStateFlow(FlightFilterOption.ALL)
    val activeFilter: StateFlow<FlightFilterOption> = _activeFilter.asStateFlow()

    private val _rawOffers = MutableStateFlow<List<EnrichedFlightOffer>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedOffer = MutableStateFlow<EnrichedFlightOffer?>(null)
    val selectedOffer: StateFlow<EnrichedFlightOffer?> = _selectedOffer.asStateFlow()

    private val _selectedAncillaries = MutableStateFlow(SelectedAncillaries())
    val selectedAncillaries: StateFlow<SelectedAncillaries> = _selectedAncillaries.asStateFlow()

    private val _aircraftPhotos = MutableStateFlow<List<PlanespottersPhoto>>(emptyList())
    val aircraftPhotos: StateFlow<List<PlanespottersPhoto>> = _aircraftPhotos.asStateFlow()

    private val _isIssuingOrder = MutableStateFlow(false)
    val isIssuingOrder: StateFlow<Boolean> = _isIssuingOrder.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _promotionalDeals = MutableStateFlow<List<TravelPromotionDeal>>(
        com.example.data.model.PromotionalDealsCatalog.GLOBAL_DEALS
    )
    val promotionalDeals: StateFlow<List<TravelPromotionDeal>> = _promotionalDeals.asStateFlow()

    private val _isRefreshingPromos = MutableStateFlow(false)
    val isRefreshingPromos: StateFlow<Boolean> = _isRefreshingPromos.asStateFlow()

    private val _detectedAirport = MutableStateFlow<DetectedAirportResult?>(null)
    val detectedAirport: StateFlow<DetectedAirportResult?> = _detectedAirport.asStateFlow()

    private val _selectedCurrency = MutableStateFlow(DisplayCurrency.EUR)
    val selectedCurrency: StateFlow<DisplayCurrency> = _selectedCurrency.asStateFlow()

    private val _activeCancellationTicket = MutableStateFlow<IssuedTicket?>(null)
    val activeCancellationTicket: StateFlow<IssuedTicket?> = _activeCancellationTicket.asStateFlow()

    private val _activeCancellationQuote = MutableStateFlow<OrderCancellationQuote?>(null)
    val activeCancellationQuote: StateFlow<OrderCancellationQuote?> = _activeCancellationQuote.asStateFlow()

    private val _isLoadingCancellationQuote = MutableStateFlow(false)
    val isLoadingCancellationQuote: StateFlow<Boolean> = _isLoadingCancellationQuote.asStateFlow()

    private val _isConfirmingCancellation = MutableStateFlow(false)
    val isConfirmingCancellation: StateFlow<Boolean> = _isConfirmingCancellation.asStateFlow()

    private val _confirmedCancellations = MutableStateFlow<Map<String, OrderCancellationConfirmation>>(emptyMap())
    val confirmedCancellations: StateFlow<Map<String, OrderCancellationConfirmation>> = _confirmedCancellations.asStateFlow()

    val isDuffelLive: Boolean = flightRepository.isDuffelConfigured()
    val isStripeLive: Boolean = flightRepository.isStripeConfigured()

    val markupConfig: StateFlow<MarkupConfig> = ticketRepository.observeMarkupConfig(currentUserId)
        .catch { e ->
            Log.w("FlightBookingVM", "Error observing markup config", e)
            emit(MarkupConfig(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = MarkupConfig(userId = currentUserId)
        )

    val issuedTickets: StateFlow<List<IssuedTicket>> = ticketRepository.observeUserTickets(currentUserId)
        .catch { e ->
            Log.w("FlightBookingVM", "Error observing user tickets", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val priceAlerts: StateFlow<List<PriceAlertSubscription>> = priceAlertRepository.observeUserPriceAlerts(currentUserId)
        .catch { e ->
            Log.w("FlightBookingVM", "Error observing price alerts", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val savedTravelerProfile: StateFlow<SavedTravelerProfile?> = travelerProfileRepository.observeTravelerProfile(currentUserId)
        .catch { e ->
            Log.w("FlightBookingVM", "Error observing traveler profile", e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val displayedOffers: StateFlow<List<EnrichedFlightOffer>> = combine(
        _rawOffers,
        markupConfig,
        _activeFilter,
        _tripType,
        _passengerComposition
    ) { offers, config, filter, trip, pax ->
        val totalFactor = trip.multiplier * pax.fareMultiplier
        val repriced = offers.map { offer ->
            val adjustedNet = round(offer.netFareEur * totalFactor * 100.0) / 100.0
            offer.copy(
                netFareEur = adjustedNet,
                pricing = MarkupEngine.calculateBreakdown(adjustedNet, config)
            )
        }
        when (filter) {
            FlightFilterOption.ALL -> repriced
            FlightFilterOption.DIRECT_ONLY -> repriced.filter { it.isDirectFlight }
            FlightFilterOption.WITH_STOPS -> repriced.filter { !it.isDirectFlight }
            FlightFilterOption.WIDEBODY_ONLY -> repriced.filter { it.aircraftSpec.bodyType == BodyType.WIDEBODY }
            FlightFilterOption.LOWEST_PRICE -> repriced.sortedBy { it.pricing.displayFareBeforeAncillariesEur }
            FlightFilterOption.FASTEST -> repriced.sortedBy { it.durationMinutes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        executeSearch()
        refreshPromotionalDeals()
    }

    fun applyDetectedNearestAirport(result: DetectedAirportResult) {
        _detectedAirport.value = result
        val newOrigin = result.airport.iataCode.uppercase()
        if (_originIata.value != newOrigin) {
            _originIata.value = newOrigin
            if (_destinationIata.value == newOrigin) {
                _destinationIata.value = if (newOrigin == "CDG") "LIS" else "CDG"
            }
            executeSearch()
            refreshPromotionalDeals()
        } else {
            refreshPromotionalDeals()
        }
        _statusBanner.value = "📍 Aeroporto mais próximo: ${result.airport.cityName} (${result.airport.iataCode}) • ${result.detectionSource}"
    }

    fun updateSelectedCurrency(currency: DisplayCurrency) {
        _selectedCurrency.value = currency
    }

    fun saveTravelerProfileToVault(profile: SavedTravelerProfile) {
        viewModelScope.launch {
            val res = travelerProfileRepository.saveTravelerProfile(
                profile.copy(userId = currentUserId)
            )
            res.onSuccess {
                _statusBanner.value = "🔒 Seus dados foram salvos no Cofre de Viajante para próximas compras!"
            }.onFailure { err ->
                _statusBanner.value = "Erro ao salvar Cofre de Viajante: ${err.localizedMessage}"
            }
        }
    }

    fun deleteTravelerProfileFromVault() {
        viewModelScope.launch {
            val res = travelerProfileRepository.deleteTravelerProfile()
            res.onSuccess {
                _statusBanner.value = "Cofre de Viajante limpo com sucesso."
            }
        }
    }

    private val _autoDetectedPromoDeal = MutableStateFlow<TravelPromotionDeal?>(null)
    val autoDetectedPromoDeal: StateFlow<TravelPromotionDeal?> = _autoDetectedPromoDeal.asStateFlow()

    fun consumeAutoDetectedPromoDeal() {
        _autoDetectedPromoDeal.value = null
    }

    fun refreshPromotionalDeals() {
        viewModelScope.launch {
            _isRefreshingPromos.value = true
            try {
                val live = flightRepository.fetchLivePromotionalDeals(_originIata.value, markupConfig.value)
                if (live.isNotEmpty()) {
                    _promotionalDeals.value = live
                    val bestPromo = live.minByOrNull { it.displayTotalEur } ?: live.first()
                    _autoDetectedPromoDeal.value = bestPromo
                }
            } catch (e: Exception) {
                Log.w("FlightBookingVM", "Erro ao atualizar promoções reais", e)
            } finally {
                _isRefreshingPromos.value = false
            }
        }
    }

    fun updateSearchParams(origin: String, destination: String, date: String, cabin: String) {
        val previousOrigin = _originIata.value
        _originIata.value = origin.uppercase()
        _destinationIata.value = destination.uppercase()
        _departureDate.value = date
        _cabinClass.value = cabin
        if (previousOrigin != _originIata.value) {
            refreshPromotionalDeals()
        }
    }

    fun updateTripType(tripType: TripType) {
        _tripType.value = tripType
    }

    fun updateReturnDate(date: String) {
        _returnDate.value = date
    }

    fun updatePassengerComposition(composition: PassengerComposition) {
        _passengerComposition.value = composition
    }

    fun updateFrequentFlyerSelection(selection: FrequentFlyerSelection) {
        _frequentFlyerSelection.value = selection
    }

    suspend fun searchAirportPlaces(query: String): List<AirportPlaceSuggestion> {
        return flightRepository.searchPlacesSuggestions(query)
    }

    fun setFilter(filter: FlightFilterOption) {
        _activeFilter.value = filter
    }

    fun executeSearch() {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = flightRepository.searchFlights(
                    originIata = _originIata.value,
                    destinationIata = _destinationIata.value,
                    departureDate = _departureDate.value,
                    cabinClass = _cabinClass.value,
                    markupConfig = markupConfig.value
                )
                _rawOffers.value = results

                // Verifica automaticamente se a busca na API encontrou tarifa promocional abaixo da meta dos alertas ativos
                val cheapestOffer = results.minByOrNull { it.pricing.displayFareBeforeAncillariesEur }
                if (cheapestOffer != null) {
                    val priceEur = cheapestOffer.pricing.displayFareBeforeAncillariesEur
                    priceAlerts.value.forEach { alert ->
                        if (alert.originIata.equals(_originIata.value, ignoreCase = true) &&
                            alert.destinationIata.equals(_destinationIata.value, ignoreCase = true) &&
                            priceEur <= alert.targetMaxPriceEur &&
                            alert.id.isNotBlank()
                        ) {
                            priceAlertRepository.updateObservedPriceAndPromoState(
                                alertId = alert.id,
                                observedPriceEur = priceEur,
                                promoTriggered = true
                            )
                        }
                    }
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun createRoutePriceAlert(
        originIata: String,
        destinationIata: String,
        cabinClass: String,
        targetMaxPriceEur: Double,
        baselinePriceEur: Double,
        onAlertCreated: (PriceAlertSubscription) -> Unit = {}
    ) {
        viewModelScope.launch {
            val fcmToken = priceAlertRepository.fetchDeviceFcmToken()
            val alert = PriceAlertSubscription(
                userId = currentUserId,
                originIata = originIata.uppercase().take(3),
                destinationIata = destinationIata.uppercase().take(3),
                cabinClass = cabinClass,
                targetMaxPriceEur = targetMaxPriceEur,
                baselinePriceEur = baselinePriceEur,
                lastObservedPriceEur = baselinePriceEur,
                fcmToken = fcmToken,
                isActive = true,
                promoTriggered = false
            )
            val res = priceAlertRepository.createPriceAlert(alert)
            res.onSuccess { id ->
                val saved = alert.copy(id = id)
                _statusBanner.value = "Alerta Push criado para ${saved.originIata} ➔ ${saved.destinationIata} (Meta ≤ ${MarkupEngine.formatEur(saved.targetMaxPriceEur)})"
                onAlertCreated(saved)
            }.onFailure { err ->
                _statusBanner.value = "Erro ao criar alerta: ${err.localizedMessage}"
            }
        }
    }

    fun deleteRoutePriceAlert(alertId: String) {
        if (alertId.isBlank()) return
        viewModelScope.launch {
            priceAlertRepository.deletePriceAlert(alertId)
            _statusBanner.value = "Alerta de preço removido."
        }
    }

    fun runPromotionalFareCronSweep(
        onPromoFound: (alert: PriceAlertSubscription, promoPriceEur: Double, airlineName: String) -> Unit,
        onGlobalCountryDealTriggered: (TravelPromotionDeal) -> Unit = {}
    ) {
        val currentAlerts = priceAlerts.value
        viewModelScope.launch {
            currentAlerts.forEach { alert ->
                val routeOffers = flightRepository.searchFlights(
                    originIata = alert.originIata,
                    destinationIata = alert.destinationIata,
                    departureDate = _departureDate.value,
                    cabinClass = alert.cabinClass,
                    markupConfig = markupConfig.value
                )
                val bestOffer = routeOffers.minByOrNull { it.pricing.displayFareBeforeAncillariesEur }
                val observedEur = bestOffer?.pricing?.displayFareBeforeAncillariesEur ?: alert.lastObservedPriceEur
                val airline = bestOffer?.airlineName ?: "TAP Air Portugal NDC"

                if (observedEur <= alert.targetMaxPriceEur) {
                    if (alert.id.isNotBlank()) {
                        priceAlertRepository.updateObservedPriceAndPromoState(
                            alertId = alert.id,
                            observedPriceEur = observedEur,
                            promoTriggered = true
                        )
                    }
                    onPromoFound(alert, observedEur, airline)
                }
            }

            val liveDeals = try {
                flightRepository.fetchLivePromotionalDeals(_originIata.value, markupConfig.value)
            } catch (_: Exception) {
                emptyList()
            }
            val featuredDeal = liveDeals.minByOrNull { it.displayTotalEur }
                ?: _promotionalDeals.value.minByOrNull { it.displayTotalEur }
            if (featuredDeal != null) {
                onGlobalCountryDealTriggered(featuredDeal)
            }
        }
    }

    fun selectOffer(offer: EnrichedFlightOffer, resetAncillaries: Boolean = false) {
        if (resetAncillaries) {
            _selectedAncillaries.value = SelectedAncillaries()
        }
        val updatedPricing = MarkupEngine.calculateBreakdown(
            offer.netFareEur,
            markupConfig.value,
            _selectedAncillaries.value
        )
        _selectedOffer.value = offer.copy(pricing = updatedPricing)
        viewModelScope.launch {
            _aircraftPhotos.value = flightRepository.fetchAircraftPhotos(
                icaoCode = offer.aircraftSpec.icaoCode,
                registration = offer.aircraftSpec.sampleRegistration
            )
        }
    }

    fun selectPromotionalDeal(deal: TravelPromotionDeal, onNavigateToCheckout: () -> Unit) {
        _originIata.value = deal.originIata
        _destinationIata.value = deal.destinationIata
        _departureDate.value = deal.departureDateIso
        _cabinClass.value = "economy"

        viewModelScope.launch {
            _isSearching.value = true
            try {
                val liveRouteOffers = flightRepository.searchFlights(
                    originIata = deal.originIata,
                    destinationIata = deal.destinationIata,
                    departureDate = deal.departureDateIso,
                    cabinClass = "economy",
                    markupConfig = markupConfig.value
                )
                _rawOffers.value = liveRouteOffers

                val cheapestLiveOffer = liveRouteOffers.minByOrNull { it.pricing.displayFareBeforeAncillariesEur }
                if (cheapestLiveOffer != null) {
                    selectOffer(cheapestLiveOffer, resetAncillaries = true)
                } else {
                    val spec = AircraftSpecsCatalog.resolveSpec("359", null)
                    val resolvedOriginCity = PlaceAutocompleteCatalog.resolveByIata(deal.originIata).cityName
                    val fallbackOffer = EnrichedFlightOffer(
                        offerId = "off_${deal.id}",
                        passengerId = "pas_promo_01",
                        airlineName = deal.airlineName,
                        airlineIata = deal.airlineIata,
                        flightNumber = "${deal.airlineIata}880",
                        originIata = deal.originIata,
                        originCity = resolvedOriginCity,
                        destinationIata = deal.destinationIata,
                        destinationCity = deal.destinationCity,
                        departureTimeFormatted = "09:30",
                        arrivalTimeFormatted = "12:45",
                        departureIso = "${deal.departureDateIso}T09:30:00Z",
                        arrivalIso = "${deal.departureDateIso}T12:45:00Z",
                        durationFormatted = "2h 45m",
                        durationMinutes = 165,
                        stopsCount = if (deal.isDirectFlight) 0 else 1,
                        stopoverHubsSummary = if (deal.isDirectFlight) "" else "via MAD",
                        totalConnectionMinutes = if (deal.isDirectFlight) 0 else 75,
                        cabinClass = "economy",
                        netFareEur = deal.netFareEur,
                        pricing = MarkupEngine.calculateBreakdown(deal.netFareEur, markupConfig.value),
                        aircraftSpec = spec,
                        includedBaggageSummary = "1x 23kg Despachada + 1x Cabine (Tarifa Promocional)",
                        cancellationPolicySummary = "Alteração Flexível • Tarifa Promocional NDC",
                        isLiveDuffelOffer = false
                    )
                    selectOffer(fallbackOffer, resetAncillaries = true)
                }

                val formattedPrice = _selectedCurrency.value.format(deal.displayTotalEur)
                _statusBanner.value = "🔥 Promoção ${deal.originCity} ➔ ${deal.destinationCity} selecionada ($formattedPrice)!"
                onNavigateToCheckout()
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun updateSeat(seatDesignator: String) {
        _selectedAncillaries.value = _selectedAncillaries.value.copy(seatDesignator = seatDesignator)
    }

    fun clearAllAncillaries() {
        _selectedAncillaries.value = SelectedAncillaries()
    }

    fun toggleExtraBaggage(enabled: Boolean) {
        _selectedAncillaries.value = _selectedAncillaries.value.copy(includeExtraBaggage23Kg = enabled)
    }

    fun togglePriorityLounge(enabled: Boolean) {
        _selectedAncillaries.value = _selectedAncillaries.value.copy(includePriorityLounge = enabled)
    }

    fun completeStripeCheckoutAndIssueOrder(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        documentInfo: PassengerDocumentInfo,
        cardInput: PaymentCardInput,
        saveToTravelerVault: Boolean = true,
        frequentFlyerProgram: String = "",
        frequentFlyerNumber: String = "",
        onOrderIssued: (IssuedTicket) -> Unit
    ) {
        val offer = _selectedOffer.value ?: return
        viewModelScope.launch {
            _isIssuingOrder.value = true
            try {
                if (saveToTravelerVault &&
                    firstName.isNotBlank() &&
                    lastName.isNotBlank() &&
                    phone.isNotBlank() &&
                    documentInfo.documentNumber.isNotBlank()
                ) {
                    travelerProfileRepository.saveTravelerProfile(
                        SavedTravelerProfile(
                            userId = currentUserId,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            phone = phone,
                            documentNumber = documentInfo.documentNumber,
                            birthDate = documentInfo.birthDate,
                            expiryDate = documentInfo.expiryDate,
                            frequentFlyerProgram = frequentFlyerProgram,
                            frequentFlyerNumber = frequentFlyerNumber,
                            autoCheckInEnabled = documentInfo.autoCheckInEnabled
                        )
                    )
                }

                val result = flightRepository.processCheckoutAndIssueOrder(
                    offer = offer,
                    ancillaries = _selectedAncillaries.value,
                    markupConfig = markupConfig.value,
                    passengerFirstName = firstName,
                    passengerLastName = lastName,
                    passengerEmail = email,
                    passengerPhone = phone,
                    documentInfo = documentInfo,
                    cardInput = cardInput,
                    userId = currentUserId
                )
                result.onSuccess { ticket ->
                    val saveRes = ticketRepository.createTicket(ticket)
                    val savedId = saveRes.getOrDefault(ticket.id)
                    val finalTicket = ticket.copy(id = savedId)
                    val checkInMsg = if (documentInfo.autoCheckInEnabled) {
                        "Check-in Automático agendado!"
                    } else {
                        "Emitido com sucesso!"
                    }
                    _statusBanner.value = "E-Ticket ${finalTicket.bookingReference} • $checkInMsg"
                    onOrderIssued(finalTicket)
                }.onFailure { err ->
                    _statusBanner.value = "Atenção: ${err.localizedMessage}"
                }
            } finally {
                _isIssuingOrder.value = false
            }
        }
    }

    fun toggleGoogleWalletStatus(ticket: IssuedTicket) {
        if (ticket.id.isBlank()) return
        viewModelScope.launch {
            ticketRepository.markAddedToGoogleWallet(ticket.id, !ticket.addedToGoogleWallet)
        }
    }

    /**
     * Consulta o endpoint de cancelamentos da Duffel API (POST /air/order_cancellations)
     * e exibe o resumo financeiro antes da confirmação final (Valor devolvido pela companhia aérea,
     * Taxa de serviço de €25.00, Taxas operacionais originais retidas €3.50 + 3.5% e Valor líquido final via Stripe).
     */
    fun requestTicketCancellationQuote(ticket: IssuedTicket) {
        _activeCancellationTicket.value = ticket
        _activeCancellationQuote.value = null
        viewModelScope.launch {
            _isLoadingCancellationQuote.value = true
            try {
                val res = flightRepository.requestOrderCancellationQuote(ticket)
                res.onSuccess { quote ->
                    _activeCancellationQuote.value = quote
                }.onFailure { err ->
                    _statusBanner.value = "Erro ao consultar reembolso na Duffel: ${err.localizedMessage}"
                    _activeCancellationTicket.value = null
                }
            } finally {
                _isLoadingCancellationQuote.value = false
            }
        }
    }

    /**
     * Confirma definitivamente o cancelamento da reserva na Duffel API e processa o reembolso
     * líquido no cartão de crédito via Stripe.
     */
    fun confirmTicketCancellationAndRefund() {
        val ticket = _activeCancellationTicket.value ?: return
        val quote = _activeCancellationQuote.value ?: return
        viewModelScope.launch {
            _isConfirmingCancellation.value = true
            try {
                val res = flightRepository.confirmOrderCancellationAndStripeRefund(ticket, quote)
                res.onSuccess { confirmation ->
                    val key = ticket.id.ifBlank { ticket.orderId }
                    _confirmedCancellations.value = _confirmedCancellations.value + (key to confirmation)
                    if (ticket.id.isNotBlank()) {
                        ticketRepository.markTicketCancelled(
                            ticket = ticket,
                            cancellationId = confirmation.cancellationId,
                            netStripeRefundEur = confirmation.netStripeCustomerRefundEur,
                            airlineRefundEur = confirmation.airlineRefundAmountEur
                        )
                    }
                    _activeCancellationTicket.value = null
                    _activeCancellationQuote.value = null
                    _statusBanner.value =
                        "Reserva PNR ${ticket.bookingReference} cancelada! Estorno líquido de ${MarkupEngine.formatEur(confirmation.netStripeCustomerRefundEur)} enviado ao cartão via Stripe."
                }.onFailure { err ->
                    _statusBanner.value = "Falha ao confirmar cancelamento: ${err.localizedMessage}"
                }
            } finally {
                _isConfirmingCancellation.value = false
            }
        }
    }

    fun dismissCancellationDialog() {
        if (_isConfirmingCancellation.value) return
        _activeCancellationTicket.value = null
        _activeCancellationQuote.value = null
    }

    fun clearBanner() {
        _statusBanner.value = null
    }
}

