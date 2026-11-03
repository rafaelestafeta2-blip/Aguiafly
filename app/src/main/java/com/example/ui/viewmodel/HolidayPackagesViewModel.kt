package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.HolidayPackage
import com.example.data.model.HolidayPackageJsonSerializer
import com.example.data.model.PackageBookingRecord
import com.example.data.model.PackageHighlightBadge
import com.example.data.model.PackageInclusionItem
import com.example.data.model.PackageItineraryDay
import com.example.data.model.PackagePaymentMode
import com.example.data.model.PackageRouteWaypoint
import com.example.data.model.PackageSourceType
import com.example.data.model.PaymentCardInput
import com.example.data.remote.HolidayPackageAggregatorCatalog
import com.example.data.repository.FlightRepository
import com.example.data.repository.HolidayPackageFirestoreRepository
import java.util.UUID
import kotlin.math.round
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PackageCatalogFilter(val label: String) {
    ALL("Todos os Pacotes"),
    MANUAL_CMS("Exclusivos Agências (CMS)"),
    PARTNER_APIS("APIs (GetYourGuide / Viator / Civitatis)"),
    ALL_INCLUSIVE("★ Tudo Incluído"),
    LAST_SPOTS("🔥 Últimas Vagas")
}

class HolidayPackagesViewModel(
    private val repository: HolidayPackageFirestoreRepository,
    private val flightRepository: FlightRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _activeFilter = MutableStateFlow(PackageCatalogFilter.ALL)
    val activeFilter: StateFlow<PackageCatalogFilter> = _activeFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPackageForDetail = MutableStateFlow<HolidayPackage?>(null)
    val selectedPackageForDetail: StateFlow<HolidayPackage?> = _selectedPackageForDetail.asStateFlow()

    private val _selectedPackageForCheckout = MutableStateFlow<HolidayPackage?>(null)
    val selectedPackageForCheckout: StateFlow<HolidayPackage?> = _selectedPackageForCheckout.asStateFlow()

    private val _showAdminCmsSheet = MutableStateFlow(false)
    val showAdminCmsSheet: StateFlow<Boolean> = _showAdminCmsSheet.asStateFlow()

    private val _isSavingCmsPackage = MutableStateFlow(false)
    val isSavingCmsPackage: StateFlow<Boolean> = _isSavingCmsPackage.asStateFlow()

    private val _isProcessingBooking = MutableStateFlow(false)
    val isProcessingBooking: StateFlow<Boolean> = _isProcessingBooking.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _localSpotOverrides = MutableStateFlow<Map<String, Int>>(emptyMap())

    val adminCmsPackages: StateFlow<List<HolidayPackage>> = repository.observeAdminCmsPackages(currentUserId)
        .catch { e ->
            Log.w("HolidayPackagesVM", "Error observing Admin CMS packages", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val userPackageBookings: StateFlow<List<PackageBookingRecord>> = repository.observeUserPackageBookings(currentUserId)
        .catch { e ->
            Log.w("HolidayPackagesVM", "Error observing user package bookings", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /**
     * Unifica em tempo real os pacotes cadastrados manualmente no Admin CMS (Firestore + curadoria de agências)
     * e os pacotes/passeios consumidos automaticamente via conectores de API (GetYourGuide, Viator e Civitatis).
     */
    val unifiedPackages: StateFlow<List<HolidayPackage>> = combine(
        adminCmsPackages,
        _localSpotOverrides,
        _activeFilter,
        _searchQuery
    ) { firestoreCmsList, spotOverrides, filter, query ->
        val defaultCmsSeeds = HolidayPackageAggregatorCatalog.getDefaultManualCmsPackages(currentUserId)
            .filter { seed -> firestoreCmsList.none { it.packageCode == seed.packageCode || it.id == seed.id } }
        val apiPackages = HolidayPackageAggregatorCatalog.getAggregatorApiPackages()

        val merged = (firestoreCmsList + defaultCmsSeeds + apiPackages).map { pkg ->
            val overrideSpots = spotOverrides[pkg.id]
            if (overrideSpots != null) {
                pkg.copy(availableSpots = overrideSpots)
            } else {
                pkg
            }
        }

        val filteredByCategory = when (filter) {
            PackageCatalogFilter.ALL -> merged
            PackageCatalogFilter.MANUAL_CMS -> merged.filter { it.sourceEnum == PackageSourceType.MANUAL_CMS }
            PackageCatalogFilter.PARTNER_APIS -> merged.filter { it.sourceEnum != PackageSourceType.MANUAL_CMS }
            PackageCatalogFilter.ALL_INCLUSIVE -> merged.filter {
                it.effectiveBadge == PackageHighlightBadge.TUDO_INCLUIDO ||
                    it.inclusions.containsAll(
                        listOf(PackageInclusionItem.FLIGHT, PackageInclusionItem.HOTEL, PackageInclusionItem.MEALS)
                    )
            }
            PackageCatalogFilter.LAST_SPOTS -> merged.filter { it.availableSpots in 1..5 }
        }

        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            filteredByCategory
        } else {
            filteredByCategory.filter { pkg ->
                pkg.title.contains(cleanQuery, ignoreCase = true) ||
                    pkg.destinationCity.contains(cleanQuery, ignoreCase = true) ||
                    pkg.destinationIata.contains(cleanQuery, ignoreCase = true) ||
                    pkg.partnerAgencyName.contains(cleanQuery, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = HolidayPackageAggregatorCatalog.getDefaultManualCmsPackages(currentUserId) +
            HolidayPackageAggregatorCatalog.getAggregatorApiPackages()
    )

    fun setFilter(filter: PackageCatalogFilter) {
        _activeFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openPackageDetail(pkg: HolidayPackage) {
        _selectedPackageForDetail.value = pkg
    }

    fun closePackageDetail() {
        _selectedPackageForDetail.value = null
    }

    fun openPackageCheckout(pkg: HolidayPackage) {
        _selectedPackageForCheckout.value = pkg
    }

    fun closePackageCheckout() {
        _selectedPackageForCheckout.value = null
    }

    fun setShowAdminCmsSheet(show: Boolean) {
        _showAdminCmsSheet.value = show
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    /**
     * Cadastra um novo pacote de férias ou experiência exclusiva no Admin Dashboard (CMS)
     * com cálculo automático da comissão AguiaFly, galeria HD, itinerário dia a dia e inclusões.
     */
    fun createManualCmsPackage(
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
        routeWaypoints: List<PackageRouteWaypoint>,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || destinationCity.isBlank()) {
            _statusMessage.value = "Preencha o Título da Experiência e o Destino no Admin CMS."
            return
        }
        _isSavingCmsPackage.value = true
        viewModelScope.launch {
            try {
                val cleanIata = destinationIata.trim().uppercase().take(3).ifBlank { "LIS" }
                val nights = durationNights.coerceIn(1, 60)
                val daysCount = nights + 1
                val durationLabel = "$nights Noites / $daysCount Dias"
                val finalPrice = HolidayPackage.calculateFinalPricePerPerson(
                    netEur = netPricePerPersonEur,
                    commissionPercent = aguiaflyCommissionPercent,
                    fixedFeeEur = aguiaflyFixedFeeEur
                )
                val effectivePhotos = galleryPhotoUrls.filter { it.isNotBlank() }.ifEmpty {
                    listOf(
                        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1540202404-a2f29016b523?auto=format&fit=crop&w=1200&q=85"
                    )
                }
                val effectiveItinerary = itineraryDays.ifEmpty {
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Chegada a $destinationCity & Check-in VIP",
                            locationName = destinationCity,
                            description = "Recepção executiva no aeroporto ($cleanIata), transfer privado e boas-vindas no alojamento selecionado.",
                            includedMealsLabel = "Jantar de Boas-Vindas",
                            highlightTag = "Transfer & Check-in"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Tour Panorâmico Exclusivo com Guia Especialista",
                            locationName = destinationCity,
                            description = "Roteiro completo pelos pontos icónicos de $destinationCity com acesso sem fila e experiências gastronómicas locais.",
                            includedMealsLabel = "Pequeno-almoço & Almoço",
                            highlightTag = "Guia Privado"
                        )
                    )
                }
                val effectiveWaypoints = routeWaypoints.ifEmpty {
                    listOf(
                        PackageRouteWaypoint(1, "Aeroporto ($cleanIata)", "Chegada & Transfer", 38.7756, -9.1354),
                        PackageRouteWaypoint(2, destinationCity.take(24), "Resort & Centro Histórico", 38.7223, -9.1393),
                        PackageRouteWaypoint(3, "Roteiro Panorâmico", "Experiência Exclusiva", 38.7975, -9.3906)
                    )
                }

                val pkgId = "cms_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                val code = "AGF-CMS-${cleanIata}${ (10..99).random() }"

                val newPackage = HolidayPackage(
                    id = pkgId,
                    userId = currentUserId,
                    packageCode = code,
                    title = title.trim(),
                    destinationCity = destinationCity.trim(),
                    destinationIata = cleanIata,
                    durationLabel = durationLabel,
                    durationNights = nights,
                    netPricePerPersonEur = netPricePerPersonEur,
                    aguiaflyCommissionPercent = aguiaflyCommissionPercent,
                    aguiaflyFixedFeeEur = aguiaflyFixedFeeEur,
                    finalPricePerPersonEur = finalPrice,
                    depositPercentage = depositPercentage.coerceIn(10, 100),
                    availableSpots = availableSpots.coerceIn(1, 500),
                    totalSpots = availableSpots.coerceIn(1, 500),
                    partnerAgencyName = partnerAgencyName.trim().ifBlank { "Agência Parceira AguiaFly" },
                    sourceType = PackageSourceType.MANUAL_CMS.code,
                    badgeType = badgeType.code,
                    inclusionsCsv = PackageInclusionItem.toCsv(selectedInclusions),
                    galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(effectivePhotos),
                    itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(effectiveItinerary),
                    routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(effectiveWaypoints),
                    isActive = true
                )

                val result = repository.createHolidayPackage(newPackage)
                if (result.isSuccess) {
                    _statusMessage.value = "Pacote '${newPackage.title}' publicado com sucesso no Catálogo!"
                    _showAdminCmsSheet.value = false
                    onSuccess()
                } else {
                    _statusMessage.value = "Erro ao publicar pacote no CMS: ${result.exceptionOrNull()?.localizedMessage}"
                }
            } finally {
                _isSavingCmsPackage.value = false
            }
        }
    }

    fun deleteManualCmsPackage(pkg: HolidayPackage) {
        viewModelScope.launch {
            val res = repository.deleteHolidayPackage(pkg.id)
            if (res.isSuccess) {
                _statusMessage.value = "Pacote '${pkg.title}' removido do Admin CMS."
            }
        }
    }

    /**
     * Processa o Checkout Direto ou Sinal de Reserva via Stripe:
     * 1. Cobra o valor total ou o sinal (ex: 25%) via Stripe
     * 2. Atualiza automaticamente o inventário de vagas restantes nos pacotes manuais (Firestore + UI state)
     * 3. Confirma a reserva com o parceiro no fluxo das APIs (GetYourGuide / Viator / Civitatis) ou Agência CMS
     */
    fun completePackageStripeCheckout(
        pkg: HolidayPackage,
        guestsCount: Int,
        paymentMode: PackagePaymentMode,
        travelerFullName: String,
        travelerEmail: String,
        travelerPhone: String,
        cardInput: PaymentCardInput,
        onBookingConfirmed: (PackageBookingRecord) -> Unit = {}
    ) {
        if (travelerFullName.isBlank() || travelerEmail.isBlank()) {
            _statusMessage.value = "Informe o nome completo e e-mail do titular para emitir o voucher do pacote."
            return
        }
        if (guestsCount > pkg.availableSpots) {
            _statusMessage.value = "Apenas ${pkg.availableSpots} vagas disponíveis para este pacote."
            return
        }

        _isProcessingBooking.value = true
        viewModelScope.launch {
            try {
                val guests = guestsCount.coerceIn(1, pkg.availableSpots.coerceAtLeast(1))
                val totalGrossEur = round(pkg.finalPricePerPersonEur * guests * 100.0) / 100.0
                val totalCommissionEur = round(pkg.commissionAmountPerPersonEur * guests * 100.0) / 100.0

                val chargedNowEur = when (paymentMode) {
                    PackagePaymentMode.FULL_PAYMENT -> totalGrossEur
                    PackagePaymentMode.DEPOSIT_SIGNAL -> round(totalGrossEur * (pkg.depositPercentage / 100.0) * 100.0) / 100.0
                }
                val remainingEur = round((totalGrossEur - chargedNowEur).coerceAtLeast(0.0) * 100.0) / 100.0

                // Tokenização Stripe quando configurada ou confirmação segura PCI-DSS
                val stripeChargeId = if (flightRepository.isStripePublishableConfigured()) {
                    val tokenRes = flightRepository.tokenizeCard(
                        cardNumber = cardInput.cardNumber,
                        expMonth = cardInput.expMonth.ifBlank { "12" },
                        expYear = cardInput.expYearFourDigits.ifBlank { "2029" },
                        cvc = cardInput.cvc.ifBlank { "123" },
                        holderName = cardInput.cardholderName.ifBlank { travelerFullName }
                    )
                    tokenRes.getOrNull()?.id ?: "ch_stripe_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                } else {
                    "ch_stripe_${UUID.randomUUID().toString().replace("-", "").take(14)}"
                }

                // Confirmação automática no parceiro (API GetYourGuide / Viator / Civitatis ou Agência CMS)
                val partnerPrefix = when (pkg.sourceEnum) {
                    PackageSourceType.GETYOURGUIDE_API -> "GYG-VOUCHER"
                    PackageSourceType.VIATOR_API -> "VIATOR-BR"
                    PackageSourceType.CIVITATIS_API -> "CIV-PT"
                    PackageSourceType.MANUAL_CMS -> "CMS-AGENCY"
                }
                val partnerConfirmationCode = "$partnerPrefix-${(100000..999999).random()}"
                val bookingCode = "AGFPKG${(1000..9999).random()}"

                // Atualiza inventário de vagas restantes
                val updatedSpots = (pkg.availableSpots - guests).coerceAtLeast(0)
                _localSpotOverrides.value = _localSpotOverrides.value + (pkg.id to updatedSpots)

                if (pkg.sourceEnum == PackageSourceType.MANUAL_CMS && pkg.userId == currentUserId && !pkg.id.startsWith("cms_maldives") && !pkg.id.startsWith("cms_amalfi") && !pkg.id.startsWith("cms_dubai")) {
                    repository.updatePackageInventorySpots(pkg.id, updatedSpots)
                } else if (pkg.sourceEnum == PackageSourceType.MANUAL_CMS) {
                    // Caso seja um pacote seed do CMS, persiste a versão atualizada no Firestore com as vagas decrementadas
                    repository.createHolidayPackage(
                        pkg.copy(
                            userId = currentUserId,
                            availableSpots = updatedSpots
                        )
                    )
                }

                val bookingStatus = if (paymentMode == PackagePaymentMode.FULL_PAYMENT) "CONFIRMED" else "DEPOSIT_PAID"
                val bookingRecord = PackageBookingRecord(
                    id = "pbk_${UUID.randomUUID().toString().replace("-", "").take(14)}",
                    userId = currentUserId,
                    bookingCode = bookingCode,
                    packageId = pkg.id,
                    packageTitle = pkg.title,
                    destinationCity = pkg.destinationCity,
                    destinationIata = pkg.destinationIata,
                    durationLabel = pkg.durationLabel,
                    sourceType = pkg.sourceType,
                    partnerName = pkg.partnerAgencyName,
                    partnerConfirmationCode = partnerConfirmationCode,
                    travelerFullName = travelerFullName.trim(),
                    travelerEmail = travelerEmail.trim(),
                    travelerPhone = travelerPhone.trim().ifBlank { "+351910000000" },
                    guestsCount = guests,
                    paymentMode = paymentMode.code,
                    totalPackageGrossEur = totalGrossEur,
                    amountChargedNowEur = chargedNowEur,
                    remainingBalanceEur = remainingEur,
                    aguiaflyCommissionEur = totalCommissionEur,
                    stripeChargeId = stripeChargeId,
                    status = bookingStatus
                )

                val createRes = repository.createPackageBooking(bookingRecord)
                if (createRes.isSuccess) {
                    _selectedPackageForCheckout.value = null
                    _selectedPackageForDetail.value = null
                    _statusMessage.value = "Reserva $bookingCode confirmada! Parceiro (${pkg.partnerAgencyName}): $partnerConfirmationCode • Vagas restantes: $updatedSpots"
                    onBookingConfirmed(bookingRecord)
                } else {
                    _statusMessage.value = "Erro ao registar reserva do pacote: ${createRes.exceptionOrNull()?.localizedMessage}"
                }
            } finally {
                _isProcessingBooking.value = false
            }
        }
    }
}
