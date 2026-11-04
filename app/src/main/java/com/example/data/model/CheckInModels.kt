package com.example.data.model

enum class TravelDocType(val label: String, val shortCode: String) {
    CITIZEN_CARD("Cartão de Cidadão (UE)", "CC"),
    PASSPORT("Passaporte Internacional", "PASS")
}

data class IssuingCountryOption(
    val isoCode: String,
    val label: String
)

val PASSPORT_ISSUING_COUNTRIES = listOf(
    IssuingCountryOption("PT", "🇵🇹 Portugal (PT)"),
    IssuingCountryOption("BR", "🇧🇷 Brasil (BR)"),
    IssuingCountryOption("ES", "🇪🇸 Espanha (ES)"),
    IssuingCountryOption("FR", "🇫🇷 França (FR)"),
    IssuingCountryOption("IT", "🇮🇹 Itália (IT)"),
    IssuingCountryOption("DE", "🇩🇪 Alemanha (DE)"),
    IssuingCountryOption("GB", "🇬🇧 Reino Unido (GB)"),
    IssuingCountryOption("US", "🇺🇸 Estados Unidos (US)"),
    IssuingCountryOption("CH", "🇨🇭 Suíça (CH)"),
    IssuingCountryOption("AO", "🇦🇴 Angola (AO)")
)

/**
 * Dados do documento de identificação (APIS) recolhidos durante a compra
 * para emissão IATA/GDS (`identity_documents`) e agendamento do Check-in Automático.
 */
data class PassengerDocumentInfo(
    val docType: TravelDocType = TravelDocType.CITIZEN_CARD,
    val documentNumber: String = "",
    val birthDate: String = "1992-05-15",
    val expiryDate: String = "2031-08-20",
    val issuingCountryCode: String = "PT",
    val gender: String = "m",
    val taxIdNifCpf: String = "",
    val autoCheckInEnabled: Boolean = true
)

enum class CheckInWindowStatus {
    BOARDING_PASS_READY,
    SCHEDULED_WAITING_WINDOW,
    MANUAL_ONLY
}

data class BoardingPassCardData(
    val status: CheckInWindowStatus,
    val windowHoursBeforeDeparture: Int,
    val windowOpensFormatted: String,
    val assignedSeat: String,
    val wasAutoAssignedAtCheckIn: Boolean,
    val gate: String,
    val terminal: String,
    val boardingGroup: String,
    val boardingTimeFormatted: String,
    val sequenceNumber: String,
    val eTicketNumber: String,
    val documentTypeLabel: String,
    val documentNumber: String,
    val taxIdNifCpf: String,
    val baggageAllowanceSummary: String,
    val farePolicySummary: String,
    val documentSummary: String,
    val cleanBcbpPayload: String
)
