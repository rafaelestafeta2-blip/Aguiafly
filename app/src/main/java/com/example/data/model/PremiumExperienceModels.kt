package com.example.data.model

import kotlin.math.round
import kotlin.math.roundToInt

const val AIRALO_ESIM_FIXED_MARKUP_EUR = 2.00
const val AIRALO_ESIM_PERCENTAGE_MARKUP = 15.0
const val VIP_LOUNGE_AGUIAFLY_MARGIN_EUR = 4.00
const val EXPERIENCE_PARTNER_MARKUP_PERCENT = 12.0

enum class TripType(val label: String, val multiplier: Double) {
    ONE_WAY("Só de Ida", 1.0),
    ROUND_TRIP("Ida e Volta", 1.85)
}

data class PassengerComposition(
    val adults: Int = 1,
    val children: Int = 0,
    val infants: Int = 0
) {
    val totalPassengers: Int
        get() = (adults + children + infants).coerceAtLeast(1)

    /**
     * Adultos = 100%, Crianças (2-11 anos) = 75%, Bebés (<2 anos no colo) = 15% da tarifa base.
     */
    val fareMultiplier: Double
        get() = (adults * 1.0) + (children * 0.75) + (infants * 0.15)

    val summaryLabel: String
        get() {
            val parts = mutableListOf<String>()
            parts.add("$adults Adt")
            if (children > 0) parts.add("$children Chd")
            if (infants > 0) parts.add("$infants Inf")
            return parts.joinToString(" • ")
        }
}

data class DailyFareSlot(
    val isoDate: String,
    val dayOfWeekShort: String,
    val dayMonthFormatted: String,
    val estimatedMinEur: Double,
    val isPromoDay: Boolean,
    val isLowestOfPeriod: Boolean = false,
    val savingsPercent: Int = 0
)

enum class FrequentFlyerProgram(
    val code: String,
    val displayName: String,
    val alliance: String
) {
    NONE("NONE", "Apenas AguiaFly Royal Club", "AguiaFly Private"),
    TAP_MILES_AND_GO("TP", "TAP Miles&Go", "Star Alliance"),
    AF_KLM_FLYING_BLUE("AF", "Air France / KLM Flying Blue", "SkyTeam"),
    IBERIA_PLUS("IB", "Iberia Plus Avios", "oneworld"),
    LUFTHANSA_MILES_AND_MORE("LH", "Lufthansa Miles & More", "Star Alliance"),
    BA_EXECUTIVE_CLUB("BA", "British Airways Executive Club", "oneworld"),
    LATAM_PASS("LA", "LATAM Pass", "Parceiro Global")
}

data class FrequentFlyerSelection(
    val program: FrequentFlyerProgram = FrequentFlyerProgram.TAP_MILES_AND_GO,
    val memberNumber: String = ""
)

data class VipClubStatus(
    val tierName: String,
    val tierBadgeLabel: String,
    val totalPoints: Int,
    val cashbackBalanceEur: Double,
    val nextFlightPointsEstimate: Int,
    val perksSummary: String
) {
    companion object {
        fun calculateForUser(
            issuedTicketsCount: Int,
            totalHistoricalSpentEur: Double,
            currentOfferDisplayEur: Double,
            cabinClass: String
        ): VipClubStatus {
            val multiplier = if (cabinClass in listOf("business", "first")) 12 else 6
            val basePoints = 4850 + (issuedTicketsCount * 2400) + (totalHistoricalSpentEur * 8).roundToInt()
            val nextFlightPts = (currentOfferDisplayEur * multiplier).roundToInt().coerceAtLeast(950)
            val cashback = round((basePoints * 0.015) * 100.0) / 100.0

            val (tier, badge, perks) = when {
                basePoints >= 10000 || issuedTicketsCount >= 2 -> Triple(
                    "AguiaFly Royal Black",
                    "ROYAL BLACK VIP",
                    "Fast Track Ilimitado • Concierge 24/7 • +12 pts/€ em Executiva"
                )
                basePoints >= 5000 || issuedTicketsCount >= 1 -> Triple(
                    "AguiaFly Sapphire Gold",
                    "SAPPHIRE GOLD",
                    "Prioridade NDC • Acesso Lounge Partner • +10 pts/€"
                )
                else -> Triple(
                    "AguiaFly Imperial Silver",
                    "IMPERIAL MEMBER",
                    "Tarifas Diretas NDC • Acúmulo Duplo de Milhas"
                )
            }

            return VipClubStatus(
                tierName = tier,
                tierBadgeLabel = badge,
                totalPoints = basePoints,
                cashbackBalanceEur = cashback,
                nextFlightPointsEstimate = nextFlightPts,
                perksSummary = perks
            )
        }
    }
}

data class AirlineBrandIdentity(
    val airlineIata: String,
    val emblemMonogram: String,
    val allianceName: String,
    val badgeAccentHex: Long,
    val co2ReductionPercent: Int,
    val cabinAmenities: List<String>
)

data class AirportConciergeGuide(
    val originIata: String,
    val destinationIata: String,
    val terminalLabel: String,
    val gateSuggestion: String,
    val baggageBeltSuggestion: String,
    val vipLoungeName: String,
    val loungeLocationAndHours: String,
    val fastTrackAvailable: Boolean,
    val destinationWeatherSummary: String,
    val destinationTempCelsius: Int
)

/**
 * 1. INTERNET GLOBAL (eSIM Integration - Airalo Partner API):
 * Oferta de dados móveis 5G para o país de destino com comissão/markup automático AguiaFly
 * (€2.00 fixo + 15% sobre a tarifa neta Airalo).
 */
data class AiraloEsimPackage(
    val packageId: String,
    val destinationIata: String,
    val countryName: String,
    val countryIso: String,
    val planTitle: String,
    val localOperatorName: String,
    val dataAllowanceLabel: String,
    val validityDays: Int,
    val networkSpeedLabel: String = "5G / LTE Ultra • Hotspot Liberado",
    val wholesaleNetEur: Double,
    val aguiaFlyCommissionEur: Double = round(((wholesaleNetEur * (AIRALO_ESIM_PERCENTAGE_MARKUP / 100.0)) + AIRALO_ESIM_FIXED_MARKUP_EUR) * 100.0) / 100.0,
    val retailPriceEur: Double = round((wholesaleNetEur + aguiaFlyCommissionEur) * 100.0) / 100.0,
    val isRecommended: Boolean = false
) {
    val lpaActivationCode: String
        get() = "LPA:1\$rsp.airalo.com\$AGUIA-${countryIso.uppercase()}-${packageId.uppercase()}"
}

/**
 * 2. ACESSO A SALAS VIP (Lounge Access - Priority Pass / DragonPass API):
 * Passe opcional para o aeroporto de origem e aeroportos de conexão/escala da rota.
 */
data class VipLoungeOffer(
    val loungeId: String,
    val airportIata: String,
    val airportCity: String,
    val isConnectionHub: Boolean,
    val segmentRoleLabel: String,
    val loungeName: String,
    val terminalLocation: String,
    val partnerNetwork: String = "Priority Pass • DragonPass",
    val openHours: String = "05:00 – 23:30 • Acesso 3h antes do voo",
    val amenitiesSummary: String = "Buffet Quente, Bar Champagne, Wi-Fi 1Gbps, Duchas & Business Suites",
    val wholesaleNetEur: Double,
    val aguiaFlyMarginEur: Double = VIP_LOUNGE_AGUIAFLY_MARGIN_EUR,
    val retailPassPriceEur: Double = round((wholesaleNetEur + aguiaFlyMarginEur) * 100.0) / 100.0
) {
    val voucherCodePrefix: String
        get() = "VIP-${airportIata.uppercase()}-${loungeId.takeLast(4).uppercase()}"
}

/**
 * 3. RASTREAMENTO E PORTÃO EM TEMPO REAL (Flight Status API - FlightAware / Aviationstack):
 * Estados operacionais em tempo real, terminal, portão de embarque e coordenadas da rota.
 */
enum class FlightOperationalState(
    val code: String,
    val badgeLabel: String,
    val shortLabel: String,
    val detailMessagePt: String,
    val accentHex: Long
) {
    ON_TIME(
        code = "ON_TIME",
        badgeLabel = "NO HORÁRIO • ON TIME",
        shortLabel = "No Horário",
        detailMessagePt = "Voo confirmado no horário pelo controle de tráfego aéreo Eurocontrol / ADS-B.",
        accentHex = 0xFF10B981
    ),
    BOARDING(
        code = "BOARDING",
        badgeLabel = "EMBARQUE ABERTO • GATE OPEN",
        shortLabel = "Embarque",
        detailMessagePt = "Portão aberto para embarque prioritário AguiaFly Royal e demais grupos.",
        accentHex = 0xFF38BDF8
    ),
    DELAYED(
        code = "DELAYED",
        badgeLabel = "ATRASADO • +35 MIN",
        shortLabel = "Atrasado",
        detailMessagePt = "Ajuste operacional de slot aéreo (+35 min). Novo horário atualizado em tempo real.",
        accentHex = 0xFFF59E0B
    ),
    CANCELLED(
        code = "CANCELLED",
        badgeLabel = "CANCELADO • PROTEÇÃO ATIVA",
        shortLabel = "Cancelado",
        detailMessagePt = "Voo cancelado pela companhia. Reacomodação gratuita NDC ou reembolso rápido disponível.",
        accentHex = 0xFFEF4444
    )
}

data class GeoWaypoint(
    val iata: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val isHubStopover: Boolean = false
)

data class RealtimeFlightTelemetry(
    val flightNumber: String,
    val operationalState: FlightOperationalState,
    val departureTerminal: String,
    val departureGate: String,
    val arrivalTerminal: String,
    val arrivalGate: String,
    val baggageClaimBelt: String,
    val delayMinutes: Int,
    val scheduledDepartureTime: String,
    val estimatedDepartureTime: String,
    val scheduledArrivalTime: String,
    val estimatedArrivalTime: String,
    val altitudeFeet: Int,
    val groundSpeedKmh: Int,
    val distanceKm: Int,
    val aircraftProgressFraction: Float,
    val telemetrySource: String = "FlightAware AeroAPI • Aviationstack Live",
    val waypoints: List<GeoWaypoint>
)

/**
 * 4. EXPERIÊNCIAS NO DESTINO (Tours & Transfers - GetYourGuide / Viator API):
 * Passeios guiados, ingressos sem fila e transfer privado executivo aeroporto-hotel.
 */
enum class ExperienceCategory(
    val labelPt: String,
    val badgeColorHex: Long
) {
    PRIVATE_TRANSFER("Transfer Privado Executivo", 0xFF38BDF8),
    SKIP_THE_LINE_TICKET("Ingresso Sem Fila (Fast Track)", 0xFFFFD54F),
    VIP_TOUR("Passeio & Experiência VIP", 0xFF10B981)
}

data class DestinationExperienceOffer(
    val experienceId: String,
    val destinationIata: String,
    val destinationCity: String,
    val title: String,
    val subtitle: String,
    val category: ExperienceCategory,
    val providerName: String,
    val durationLabel: String,
    val ratingScore: Double,
    val reviewsCount: Int,
    val netPartnerEur: Double,
    val aguiaFlyCommissionEur: Double = round((netPartnerEur * (EXPERIENCE_PARTNER_MARKUP_PERCENT / 100.0) + 1.50) * 100.0) / 100.0,
    val retailPriceEur: Double = round((netPartnerEur + aguiaFlyCommissionEur) * 100.0) / 100.0,
    val instantConfirmation: Boolean = true
)
