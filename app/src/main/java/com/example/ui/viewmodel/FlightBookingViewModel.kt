package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.BodyType
import com.example.data.model.CloudPassportVisualProfile
import com.example.data.model.EnrichedFlightOffer
import com.example.data.model.FrequentFlyerSelection
import com.example.data.model.IssuedTicket
import com.example.data.model.LocalProfileVisualState
import com.example.data.model.MarkupConfig
import com.example.data.model.OrderCancellationConfirmation
import com.example.data.model.OrderCancellationQuote
import com.example.data.model.PassengerComposition
import com.example.data.model.PassengerDocumentInfo
import com.example.data.model.PaymentCardInput
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.data.model.PlanespottersPhoto
import com.example.data.model.PriceAlertSubscription
import com.example.data.model.ProfilePassportStorage
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class FlightFilterOption(val label: String) {
    ALL("Todos"),
    LOWEST_OF_MONTH("★ Menor Preço do Mês"),
    DIRECT_ONLY("Voos Diretos"),
    WITH_STOPS("Com Escalas"),
    LOWEST_PRICE("Menor Tarifa €"),
    FASTEST("Mais Rápido"),
    WIDEBODY_ONLY("Apenas Widebody")
}

data class PendingCheckoutParams(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val documentInfo: PassengerDocumentInfo,
    val cardInput: PaymentCardInput,
    val saveToTravelerVault: Boolean,
    val frequentFlyerProgram: String,
    val frequentFlyerNumber: String,
    val onOrderIssued: (IssuedTicket) -> Unit
)

data class OfferPriceChangedState(
    val updatedOffer: EnrichedFlightOffer,
    val previousGrandTotalEur: Double,
    val newGrandTotalEur: Double,
    val diffEur: Double,
    val currency: String,
    val message: String,
    val pendingParams: PendingCheckoutParams
)

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

    private val _cabinClass = MutableStateFlow("economy")
    val cabinClass: StateFlow<String> = _cabinClass.asStateFlow()

    private val _activeFilter = MutableStateFlow(FlightFilterOption.ALL)
    val activeFilter: StateFlow<FlightFilterOption> = _activeFilter.asStateFlow()

    private val _rawOffers = MutableStateFlow<List<EnrichedFlightOffer>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    private var searchJob: Job? = null

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

    private val _seatMap = MutableStateFlow<List<com.example.data.model.SeatOption>>(emptyList())
    val seatMap: StateFlow<List<com.example.data.model.SeatOption>> = _seatMap.asStateFlow()

    private val _isLoadingSeatMap = MutableStateFlow(false)
    val isLoadingSeatMap: StateFlow<Boolean> = _isLoadingSeatMap.asStateFlow()

    private val _seatConflictError = MutableStateFlow<String?>(null)
    val seatConflictError: StateFlow<String?> = _seatConflictError.asStateFlow()

    private val _priceChangeState = MutableStateFlow<OfferPriceChangedState?>(null)
    val priceChangeState: StateFlow<OfferPriceChangedState?> = _priceChangeState.asStateFlow()

    private var seatMapJob: kotlinx.coroutines.Job? = null

    fun clearSeatConflictError() {
        _seatConflictError.value = null
    }

    fun dismissPriceChangeDialog() {
        _priceChangeState.value = null
    }

    fun acceptUpdatedPriceAndProceed() {
        val state = _priceChangeState.value ?: return
        _priceChangeState.value = null

        // 4. Limpeza e Reset do Ciclo de Vida da Oferta
        _selectedOffer.value = state.updatedOffer
        _rawOffers.value = _rawOffers.value.map {
            if (it.offerId == state.updatedOffer.offerId ||
                (it.flightNumber == state.updatedOffer.flightNumber && it.departureIso == state.updatedOffer.departureIso)
            ) {
                state.updatedOffer
            } else {
                it
            }
        }

        val params = state.pendingParams
        completeStripeCheckoutAndIssueOrder(
            firstName = params.firstName,
            lastName = params.lastName,
            email = params.email,
            phone = params.phone,
            documentInfo = params.documentInfo,
            cardInput = params.cardInput,
            saveToTravelerVault = params.saveToTravelerVault,
            frequentFlyerProgram = params.frequentFlyerProgram,
            frequentFlyerNumber = params.frequentFlyerNumber,
            onOrderIssued = params.onOrderIssued
        )
    }

    fun loadSeatMapForOffer(offer: EnrichedFlightOffer) {
        seatMapJob?.cancel()
        _seatMap.value = emptyList()
        _isLoadingSeatMap.value = true
        seatMapJob = viewModelScope.launch {
            try {
                val liveSeats = flightRepository.fetchSeatMap(offer)
                _seatMap.value = liveSeats
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("FlightBookingVM", "Erro ao carregar seat map para offer ${offer.offerId}", e)
                val seedKey = "${offer.offerId}_${offer.flightNumber}_${offer.originIata}_${offer.destinationIata}_${offer.departureTimeFormatted}"
                _seatMap.value = com.example.domain.aircraft.AircraftSpecsCatalog.generateSeatMapForSpec(offer.aircraftSpec, offer.cabinClass, seedKey)
            } finally {
                _isLoadingSeatMap.value = false
            }
        }
    }

    val isDuffelLive: Boolean = flightRepository.isDuffelConfigured()
    val isStripeLive: Boolean = flightRepository.isStripeConfigured()
    val isTestModeActive: Boolean = flightRepository.isTestModeActive()

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

    val cloudPassportVisualProfile: StateFlow<CloudPassportVisualProfile?> =
        travelerProfileRepository.observePassportVisualProfile(currentUserId)
            .catch { e ->
                Log.w("FlightBookingVM", "Error observing passport visual profile", e)
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
        }.sortedBy { it.pricing.displayFareBeforeAncillariesEur }

        when (filter) {
            FlightFilterOption.ALL -> repriced
            FlightFilterOption.LOWEST_OF_MONTH -> repriced
            FlightFilterOption.DIRECT_ONLY -> repriced.filter { it.isDirectFlight }
            FlightFilterOption.WITH_STOPS -> repriced.filter { !it.isDirectFlight }
            FlightFilterOption.WIDEBODY_ONLY -> repriced.filter { it.aircraftSpec.bodyType == BodyType.WIDEBODY }
            FlightFilterOption.LOWEST_PRICE -> repriced
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

    fun syncPassportVisualStateToCloud(context: Context, state: LocalProfileVisualState) {
        viewModelScope.launch {
            try {
                val appCtx = context.applicationContext
                val payload = withContext(Dispatchers.IO) {
                    ProfilePassportStorage.buildCloudVisualProfilePayload(
                        context = appCtx,
                        userId = currentUserId,
                        state = state
                    )
                }
                travelerProfileRepository.savePassportVisualProfile(payload)
            } catch (e: Exception) {
                Log.w("FlightBookingVM", "Failed to sync passport visual state to Firestore", e)
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
        val previousDate = _departureDate.value
        _originIata.value = origin.uppercase()
        _destinationIata.value = destination.uppercase()
        _departureDate.value = date
        _cabinClass.value = cabin
        if (previousOrigin != _originIata.value) {
            refreshPromotionalDeals()
        }
        if (previousDate != date) {
            val formattedDate = com.example.data.model.CountryPassportCatalog.formatStampDate(date)
            _statusBanner.value = "📅 Data selecionada: $formattedDate (${_originIata.value} ➔ ${_destinationIata.value}) • Tarifas atualizadas!"
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
        if (filter == FlightFilterOption.LOWEST_OF_MONTH) {
            val monthlySlots = com.example.domain.concierge.AirportAndLoyaltyCatalog.generateRouteFareCalendar(
                originIata = _originIata.value,
                destinationIata = _destinationIata.value,
                anchorDateIso = _departureDate.value,
                cabinClass = _cabinClass.value,
                tripMultiplier = _tripType.value.multiplier,
                passengerMultiplier = _passengerComposition.value.fareMultiplier,
                markupConfig = markupConfig.value,
                flexibleModePlusMinus3Days = false,
                totalDaysInMonthMode = 30
            )
            val cheapestDay = monthlySlots.firstOrNull { it.isLowestOfPeriod }
                ?: monthlySlots.minByOrNull { it.estimatedMinEur }
            if (cheapestDay != null) {
                _departureDate.value = cheapestDay.isoDate
                executeSearch()
                val formattedPrice = _selectedCurrency.value.format(cheapestDay.estimatedMinEur)
                _statusBanner.value =
                    "★ Menor Preço do Mês selecionado: ${cheapestDay.dayOfWeekShort}, ${cheapestDay.dayMonthFormatted} por $formattedPrice!"
            }
        }
    }

    fun executeSearch() {
        searchJob?.cancel()
        val reqOrigin = _originIata.value
        val reqDest = _destinationIata.value
        val reqDate = _departureDate.value
        val reqReturnDate = _returnDate.value
        val reqTripType = _tripType.value
        val reqPax = _passengerComposition.value
        val reqCabin = _cabinClass.value
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            try {
                val results = flightRepository.searchFlights(
                    originIata = reqOrigin,
                    destinationIata = reqDest,
                    departureDate = reqDate,
                    cabinClass = reqCabin,
                    markupConfig = markupConfig.value,
                    returnDate = reqReturnDate,
                    tripType = reqTripType,
                    passengerComposition = reqPax
                )
                _rawOffers.value = results

                // Verifica automaticamente se a busca na API encontrou tarifa promocional abaixo da meta dos alertas ativos
                val cheapestOffer = results.minByOrNull { it.pricing.displayFareBeforeAncillariesEur }
                if (cheapestOffer != null) {
                    val priceEur = cheapestOffer.pricing.displayFareBeforeAncillariesEur
                    priceAlerts.value.forEach { alert ->
                        if (alert.originIata.equals(reqOrigin, ignoreCase = true) &&
                            alert.destinationIata.equals(reqDest, ignoreCase = true) &&
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
                    val isNewDrop = !alert.promoTriggered || (observedEur < alert.lastObservedPriceEur - 1.0)
                    if (alert.id.isNotBlank()) {
                        priceAlertRepository.updateObservedPriceAndPromoState(
                            alertId = alert.id,
                            observedPriceEur = observedEur,
                            promoTriggered = true
                        )
                    }
                    if (isNewDrop) {
                        onPromoFound(alert, observedEur, airline)
                    }
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

    private val _ticketEsimSelections = MutableStateFlow<Map<String, com.example.data.model.AiraloEsimPackage>>(emptyMap())
    val ticketEsimSelections: StateFlow<Map<String, com.example.data.model.AiraloEsimPackage>> = _ticketEsimSelections.asStateFlow()

    private val _ticketLoungeSelections = MutableStateFlow<Map<String, List<com.example.data.model.VipLoungeOffer>>>(emptyMap())
    val ticketLoungeSelections: StateFlow<Map<String, List<com.example.data.model.VipLoungeOffer>>> = _ticketLoungeSelections.asStateFlow()

    private val _ticketExperienceSelections = MutableStateFlow<Map<String, List<com.example.data.model.DestinationExperienceOffer>>>(emptyMap())
    val ticketExperienceSelections: StateFlow<Map<String, List<com.example.data.model.DestinationExperienceOffer>>> = _ticketExperienceSelections.asStateFlow()

    fun selectOffer(offer: EnrichedFlightOffer, resetAncillaries: Boolean = false) {
        // Limpa imediatamente o mapa de assentos e redefine a seleção para o novo voo
        _seatMap.value = emptyList()
        val currentAnc = _selectedAncillaries.value
        val baseAncillaries = if (resetAncillaries) {
            SelectedAncillaries(
                selectedVipLounges = currentAnc.selectedVipLounges,
                seatDesignator = "AUTO",
                selectedSeatServiceId = null
            )
        } else {
            currentAnc.copy(
                seatDesignator = "AUTO",
                selectedSeatServiceId = null
            )
        }
        _selectedAncillaries.value = baseAncillaries

        val updatedPricing = MarkupEngine.calculateBreakdown(
            offer.netFareEur,
            markupConfig.value,
            baseAncillaries
        )
        _selectedOffer.value = offer.copy(pricing = updatedPricing)
        loadSeatMapForOffer(offer)
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
        _tripType.value = TripType.ONE_WAY
        _passengerComposition.value = PassengerComposition()

        // Seleciona imediatamente a oferta com o valor EXATO exibido no carrossel ou na notificação Push
        val exactPromoOffer = flightRepository.buildOfferFromDeal(deal, markupConfig.value)
        selectOffer(exactPromoOffer, resetAncillaries = true)

        val formattedPrice = _selectedCurrency.value.format(deal.displayTotalEur)
        _statusBanner.value = "🔥 Promoção ${deal.originCity} ➔ ${deal.destinationCity} selecionada ($formattedPrice)!"
        onNavigateToCheckout()

        // Atualiza a lista da rota em segundo plano mantendo a oferta promocional no topo
        viewModelScope.launch {
            try {
                val liveRouteOffers = flightRepository.searchFlights(
                    originIata = deal.originIata,
                    destinationIata = deal.destinationIata,
                    departureDate = deal.departureDateIso,
                    cabinClass = "economy",
                    markupConfig = markupConfig.value
                )
                val merged = (listOf(exactPromoOffer) + liveRouteOffers.filter { it.offerId != exactPromoOffer.offerId })
                _rawOffers.value = merged
            } catch (_: Exception) {
                _rawOffers.value = listOf(exactPromoOffer)
            }
        }
    }

    /**
     * Chamado quando o utilizador toca numa Notificação Push de promoção para abrir
     * diretamente a página de compra (Checkout) com aquela promoção exata selecionada.
     */
    fun selectPromotionalDealFromPushIntent(
        promoDealId: String?,
        promoOrigin: String?,
        promoDestination: String?,
        onNavigateToCheckout: () -> Unit
    ) {
        val currentDeals = _promotionalDeals.value
        val catalogDeals = com.example.data.model.PromotionalDealsCatalog.getFilteredDeals(
            category = "Todas",
            currentOrigin = promoOrigin?.ifBlank { _originIata.value } ?: _originIata.value
        )
        val allCandidates = currentDeals + catalogDeals

        val matchedDeal = allCandidates.firstOrNull {
            !promoDealId.isNullOrBlank() && it.id.equals(promoDealId, ignoreCase = true)
        } ?: allCandidates.firstOrNull {
            !promoDestination.isNullOrBlank() && it.destinationIata.equals(promoDestination, ignoreCase = true)
        } ?: allCandidates.firstOrNull()

        if (matchedDeal != null) {
            val resolvedOrigin = promoOrigin?.takeIf { it.isNotBlank() } ?: matchedDeal.originIata
            val resolvedOriginCity = PlaceAutocompleteCatalog.resolveByIata(resolvedOrigin).cityName
            val finalDeal = matchedDeal.copy(
                originIata = resolvedOrigin,
                originCity = resolvedOriginCity
            )
            selectPromotionalDeal(finalDeal, onNavigateToCheckout)
        }
    }

    fun updateSeat(seatDesignator: String) {
        val foundServiceId = _seatMap.value.firstOrNull { it.designator.equals(seatDesignator, ignoreCase = true) }?.serviceId
        _selectedAncillaries.value = _selectedAncillaries.value.copy(
            seatDesignator = seatDesignator,
            selectedSeatServiceId = foundServiceId
        )
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

    fun selectAiraloEsimPackage(pkg: com.example.data.model.AiraloEsimPackage?) {
        _selectedAncillaries.value = _selectedAncillaries.value.copy(selectedEsimPackage = pkg)
        if (pkg != null) {
            _statusBanner.value =
                "📶 eSIM Airalo ${pkg.planTitle} adicionado (+${MarkupEngine.formatEur(pkg.retailPriceEur)} incl. comissão AguiaFly)"
        }
    }

    fun toggleVipLoungePass(lounge: com.example.data.model.VipLoungeOffer) {
        val current = _selectedAncillaries.value.selectedVipLounges
        val exists = current.any { it.loungeId == lounge.loungeId }
        val updated = if (exists) {
            current.filterNot { it.loungeId == lounge.loungeId }
        } else {
            current + lounge
        }
        _selectedAncillaries.value = _selectedAncillaries.value.copy(selectedVipLounges = updated)
        if (!exists) {
            _statusBanner.value =
                "🥂 Passe Sala VIP ${lounge.airportIata} (${lounge.loungeName}) adicionado (+${MarkupEngine.formatEur(lounge.retailPassPriceEur)})"
        }
    }

    fun toggleDestinationExperience(experience: com.example.data.model.DestinationExperienceOffer) {
        val current = _selectedAncillaries.value.selectedExperiences
        val exists = current.any { it.experienceId == experience.experienceId }
        val updated = if (exists) {
            current.filterNot { it.experienceId == experience.experienceId }
        } else {
            current + experience
        }
        _selectedAncillaries.value = _selectedAncillaries.value.copy(selectedExperiences = updated)
        if (!exists) {
            _statusBanner.value =
                "🎟️ ${experience.title} adicionado ao Checkout (+${MarkupEngine.formatEur(experience.retailPriceEur)})"
        }
    }

    fun selectTicketPostBookingEsim(ticket: IssuedTicket, pkg: com.example.data.model.AiraloEsimPackage?) {
        val key = ticket.bookingReference.ifBlank { ticket.id }
        if (pkg == null) {
            _ticketEsimSelections.value = _ticketEsimSelections.value - key
        } else {
            _ticketEsimSelections.value = _ticketEsimSelections.value + (key to pkg)
            _statusBanner.value =
                "📶 eSIM Airalo ${pkg.planTitle} ativado para a reserva $key (${MarkupEngine.formatEur(pkg.retailPriceEur)})!"
        }
    }

    fun toggleTicketPostBookingLounge(ticket: IssuedTicket, lounge: com.example.data.model.VipLoungeOffer) {
        val key = ticket.bookingReference.ifBlank { ticket.id }
        val current = _ticketLoungeSelections.value[key].orEmpty()
        val exists = current.any { it.loungeId == lounge.loungeId }
        val updated = if (exists) {
            current.filterNot { it.loungeId == lounge.loungeId }
        } else {
            current + lounge
        }
        _ticketLoungeSelections.value = _ticketLoungeSelections.value + (key to updated)
        if (!exists) {
            _statusBanner.value =
                "🥂 Passe Sala VIP ${lounge.loungeName} (${lounge.airportIata}) emitido para a reserva $key!"
        }
    }

    fun toggleTicketPostBookingExperience(ticket: IssuedTicket, experience: com.example.data.model.DestinationExperienceOffer) {
        val key = ticket.bookingReference.ifBlank { ticket.id }
        val current = _ticketExperienceSelections.value[key].orEmpty()
        val exists = current.any { it.experienceId == experience.experienceId }
        val updated = if (exists) {
            current.filterNot { it.experienceId == experience.experienceId }
        } else {
            current + experience
        }
        _ticketExperienceSelections.value = _ticketExperienceSelections.value + (key to updated)
        if (!exists) {
            _statusBanner.value =
                "✨ Experiência '${experience.title}' confirmada em ${ticket.destinationCity} para a reserva $key!"
        }
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
        val activeAncillaries = _selectedAncillaries.value
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
                    ancillaries = activeAncillaries,
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
                    val ticketKey = finalTicket.bookingReference.ifBlank { finalTicket.id }
                    activeAncillaries.selectedEsimPackage?.let { esim ->
                        _ticketEsimSelections.value = _ticketEsimSelections.value + (ticketKey to esim)
                    }
                    if (activeAncillaries.selectedVipLounges.isNotEmpty()) {
                        _ticketLoungeSelections.value = _ticketLoungeSelections.value + (ticketKey to activeAncillaries.selectedVipLounges)
                    }
                    if (activeAncillaries.selectedExperiences.isNotEmpty()) {
                        _ticketExperienceSelections.value = _ticketExperienceSelections.value + (ticketKey to activeAncillaries.selectedExperiences)
                    }
                    val checkInMsg = if (documentInfo.autoCheckInEnabled) {
                        "Check-in Automático agendado!"
                    } else {
                        "Emitido com sucesso!"
                    }
                    _statusBanner.value = "E-Ticket ${finalTicket.bookingReference} • $checkInMsg"
                    onOrderIssued(finalTicket)
                }.onFailure { err ->
                    if (err is com.example.data.repository.OfferPriceChangedException) {
                        _priceChangeState.value = OfferPriceChangedState(
                            updatedOffer = err.updatedOffer,
                            previousGrandTotalEur = err.previousGrandTotalEur,
                            newGrandTotalEur = err.newGrandTotalEur,
                            diffEur = err.diffEur,
                            currency = err.currency,
                            message = err.message,
                            pendingParams = PendingCheckoutParams(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                phone = phone,
                                documentInfo = documentInfo,
                                cardInput = cardInput,
                                saveToTravelerVault = saveToTravelerVault,
                                frequentFlyerProgram = frequentFlyerProgram,
                                frequentFlyerNumber = frequentFlyerNumber,
                                onOrderIssued = onOrderIssued
                            )
                        )
                    } else if (err is com.example.data.repository.SeatNotAvailableException) {
                        _seatConflictError.value = err.message
                        _selectedAncillaries.value = _selectedAncillaries.value.copy(
                            seatDesignator = com.example.data.model.AUTO_FREE_SEAT,
                            selectedSeatServiceId = null
                        )
                        loadSeatMapForOffer(offer)
                    }
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

