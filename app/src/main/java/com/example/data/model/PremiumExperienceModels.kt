package com.example.data.model

import kotlin.math.roundToInt

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
    val isPromoDay: Boolean
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
            val cashback = kotlin.math.round((basePoints * 0.015) * 100.0) / 100.0

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
