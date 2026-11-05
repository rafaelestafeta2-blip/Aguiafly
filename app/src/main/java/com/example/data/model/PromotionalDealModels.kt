package com.example.data.model

import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.pricing.MarkupEngine
import java.time.LocalDate
import kotlin.math.round

data class TravelPromotionDeal(
    val id: String,
    val destinationCity: String,
    val destinationCountry: String,
    val destinationIata: String,
    val originCity: String = "Lisboa",
    val originIata: String = "LIS",
    val airlineName: String,
    val airlineIata: String,
    val departureDateIso: String,
    val returnDateIso: String? = null,
    val originalFareEur: Double,
    val netFareEur: Double,
    val displayTotalEur: Double,
    val discountPercentage: Int,
    val highlightBadge: String,
    val imageUrl: String,
    val category: String, // "Europa", "Brasil", "Américas", "Ásia & Dubai", "África & Oceania", "Praias"
    val isDirectFlight: Boolean = true,
    val seatsRemaining: Int = 4,
    val isLiveApiPrice: Boolean = false,
    val matchedOffer: EnrichedFlightOffer? = null
) {
    val discountPercent: Int
        get() = discountPercentage

    val formattedDisplayPrice: String
        get() = MarkupEngine.formatEur(displayTotalEur)

    val formattedOriginalPrice: String
        get() = MarkupEngine.formatEur(originalFareEur)

    val photoUrl: String
        get() = imageUrl.ifBlank {
            com.example.domain.destination.DestinationCoverCatalog
                .resolveDestination(destinationIata, destinationCity)
                .coverImageUrl
        }

    val tagline: String
        get() {
            val destMeta = com.example.domain.destination.DestinationCoverCatalog
                .resolveDestination(destinationIata, destinationCity)
            val flightMode = if (isDirectFlight) "Voo Direto" else "Conexão Rápida"
            return "${destMeta.tagline.substringBefore("•").trim()} • $flightMode via $airlineName"
        }
}

object PromotionalDealsCatalog {

    private fun dynamicPromoDate(daysAhead: Long): String =
        LocalDate.now().plusDays(daysAhead).toString()

    fun currentHourBucket(): Long = System.currentTimeMillis() / 3_600_000L

    val CATEGORIES = listOf(
        "Todas",
        "Europa",
        "Brasil",
        "Américas",
        "Ásia & Dubai",
        "África & Oceania",
        "Praias"
    )

    val GLOBAL_DEALS: List<TravelPromotionDeal>
        get() = listOf(
            // ==================== EUROPA ====================
            TravelPromotionDeal(
                id = "promo_lis_cdg",
                destinationCity = "Paris",
                destinationCountry = "França",
                destinationIata = "CDG",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(14),
                originalFareEur = 129.0,
                netFareEur = 44.0,
                displayTotalEur = 49.04,
                discountPercentage = 62,
                highlightBadge = "🔥 FLASH SALE -62%",
                imageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_fco",
                destinationCity = "Roma",
                destinationCountry = "Itália",
                destinationIata = "FCO",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(18),
                originalFareEur = 145.0,
                netFareEur = 52.0,
                displayTotalEur = 57.32,
                discountPercentage = 60,
                highlightBadge = "⚡ TARIFA ESPECIAL",
                imageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 5
            ),
            TravelPromotionDeal(
                id = "promo_lis_lhr",
                destinationCity = "Londres",
                destinationCountry = "Reino Unido",
                destinationIata = "LHR",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "British Airways",
                airlineIata = "BA",
                departureDateIso = dynamicPromoDate(12),
                originalFareEur = 139.0,
                netFareEur = 48.0,
                displayTotalEur = 53.18,
                discountPercentage = 61,
                highlightBadge = "🇬🇧 ESCAPADINHA REAL",
                imageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_mad",
                destinationCity = "Madrid",
                destinationCountry = "Espanha",
                destinationIata = "MAD",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Iberia",
                airlineIata = "IB",
                departureDateIso = dynamicPromoDate(9),
                originalFareEur = 89.0,
                netFareEur = 26.0,
                displayTotalEur = 30.41,
                discountPercentage = 65,
                highlightBadge = "💥 SUPER OFERTA €30",
                imageUrl = "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 6
            ),
            TravelPromotionDeal(
                id = "promo_lis_bcn",
                destinationCity = "Barcelona",
                destinationCountry = "Espanha",
                destinationIata = "BCN",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Vueling",
                airlineIata = "VY",
                departureDateIso = dynamicPromoDate(15),
                originalFareEur = 109.0,
                netFareEur = 34.0,
                displayTotalEur = 38.69,
                discountPercentage = 64,
                highlightBadge = "✨ MEDITERRÂNEO",
                imageUrl = "https://images.unsplash.com/photo-1583422409516-2895a77efded?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 5
            ),
            TravelPromotionDeal(
                id = "promo_lis_ams",
                destinationCity = "Amsterdão",
                destinationCountry = "Países Baixos",
                destinationIata = "AMS",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "KLM Royal Dutch",
                airlineIata = "KL",
                departureDateIso = dynamicPromoDate(20),
                originalFareEur = 165.0,
                netFareEur = 64.0,
                displayTotalEur = 69.74,
                discountPercentage = 58,
                highlightBadge = "🌷 CANAIS & CULTURA",
                imageUrl = "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_zrh",
                destinationCity = "Zurique",
                destinationCountry = "Suíça",
                destinationIata = "ZRH",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "SWISS International",
                airlineIata = "LX",
                departureDateIso = dynamicPromoDate(22),
                originalFareEur = 185.0,
                netFareEur = 72.0,
                displayTotalEur = 78.02,
                discountPercentage = 57,
                highlightBadge = "🏔️ ALPES SUÍÇOS",
                imageUrl = "https://images.unsplash.com/photo-1515488764276-beab7607c1e6?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_ath",
                destinationCity = "Atenas",
                destinationCountry = "Grécia",
                destinationIata = "ATH",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Aegean Airlines",
                airlineIata = "A3",
                departureDateIso = dynamicPromoDate(25),
                originalFareEur = 199.0,
                netFareEur = 79.0,
                displayTotalEur = 85.27,
                discountPercentage = 56,
                highlightBadge = "🏛️ GRÉCIA CLÁSSICA",
                imageUrl = "https://images.unsplash.com/photo-1603565816030-6b389eeb23cb?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_ist",
                destinationCity = "Istambul",
                destinationCountry = "Turquia",
                destinationIata = "IST",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Turkish Airlines",
                airlineIata = "TK",
                departureDateIso = dynamicPromoDate(27),
                originalFareEur = 235.0,
                netFareEur = 95.0,
                displayTotalEur = 101.83,
                discountPercentage = 55,
                highlightBadge = "🕌 BÓSFORO PREMIUM",
                imageUrl = "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?auto=format&fit=crop&w=900&q=80",
                category = "Europa",
                isDirectFlight = true,
                seatsRemaining = 4
            ),

            // ==================== BRASIL ====================
            TravelPromotionDeal(
                id = "promo_lis_gru",
                destinationCity = "São Paulo",
                destinationCountry = "Brasil",
                destinationIata = "GRU",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(28),
                originalFareEur = 689.0,
                netFareEur = 359.0,
                displayTotalEur = 375.07,
                discountPercentage = 45,
                highlightBadge = "🇧🇷 OFERTA BRASIL",
                imageUrl = "https://images.unsplash.com/photo-1543059080-f9b1272213d5?auto=format&fit=crop&w=900&q=80",
                category = "Brasil",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_vcp",
                destinationCity = "Campinas / São Paulo",
                destinationCountry = "Brasil",
                destinationIata = "VCP",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Azul Linhas Aéreas",
                airlineIata = "AD",
                departureDateIso = dynamicPromoDate(30),
                originalFareEur = 669.0,
                netFareEur = 345.0,
                displayTotalEur = 360.58,
                discountPercentage = 46,
                highlightBadge = "🇧🇷 HUB AZUL VIRACOPOS",
                imageUrl = "https://images.unsplash.com/photo-1578002573559-689b0abc4148?auto=format&fit=crop&w=900&q=80",
                category = "Brasil",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_gig",
                destinationCity = "Rio de Janeiro",
                destinationCountry = "Brasil",
                destinationIata = "GIG",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(32),
                originalFareEur = 720.0,
                netFareEur = 369.0,
                displayTotalEur = 385.42,
                discountPercentage = 46,
                highlightBadge = "🌴 CIDADE MARAVILHOSA",
                imageUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=900&q=80",
                category = "Brasil",
                isDirectFlight = true,
                seatsRemaining = 2
            ),
            TravelPromotionDeal(
                id = "promo_lis_ssa",
                destinationCity = "Salvador",
                destinationCountry = "Brasil",
                destinationIata = "SSA",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(34),
                originalFareEur = 650.0,
                netFareEur = 335.0,
                displayTotalEur = 350.23,
                discountPercentage = 46,
                highlightBadge = "☀️ NORDESTE BAHIA",
                imageUrl = "https://images.unsplash.com/photo-1555881400-74d7acaacd8b?auto=format&fit=crop&w=900&q=80",
                category = "Brasil",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_for",
                destinationCity = "Fortaleza",
                destinationCountry = "Brasil",
                destinationIata = "FOR",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(36),
                originalFareEur = 620.0,
                netFareEur = 319.0,
                displayTotalEur = 333.67,
                discountPercentage = 46,
                highlightBadge = "🏖️ CEARÁ & JERI",
                imageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=900&q=80",
                category = "Brasil",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_rec",
                destinationCity = "Recife",
                destinationCountry = "Brasil",
                destinationIata = "REC",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(38),
                originalFareEur = 635.0,
                netFareEur = 329.0,
                displayTotalEur = 344.02,
                discountPercentage = 46,
                highlightBadge = "🌊 PORTO DE GALINHAS",
                imageUrl = "https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f?auto=format&fit=crop&w=900&q=80",
                category = "Brasil",
                isDirectFlight = true,
                seatsRemaining = 3
            ),

            // ==================== AMÉRICAS ====================
            TravelPromotionDeal(
                id = "promo_lis_jfk",
                destinationCity = "Nova Iorque",
                destinationCountry = "Estados Unidos",
                destinationIata = "JFK",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(26),
                originalFareEur = 590.0,
                netFareEur = 289.0,
                displayTotalEur = 302.62,
                discountPercentage = 48,
                highlightBadge = "🗽 MANHATTAN DEAL",
                imageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=900&q=80",
                category = "Américas",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_mia",
                destinationCity = "Miami",
                destinationCountry = "Estados Unidos",
                destinationIata = "MIA",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(29),
                originalFareEur = 640.0,
                netFareEur = 319.0,
                displayTotalEur = 333.67,
                discountPercentage = 47,
                highlightBadge = "🌴 SOUTH BEACH VIP",
                imageUrl = "https://images.unsplash.com/photo-1535498730771-e735b998cd64?auto=format&fit=crop&w=900&q=80",
                category = "Américas",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_mco",
                destinationCity = "Orlando",
                destinationCountry = "Estados Unidos",
                destinationIata = "MCO",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "British Airways",
                airlineIata = "BA",
                departureDateIso = dynamicPromoDate(33),
                originalFareEur = 680.0,
                netFareEur = 349.0,
                displayTotalEur = 364.72,
                discountPercentage = 46,
                highlightBadge = "🎢 DISNEY & UNIVERSAL",
                imageUrl = "https://images.unsplash.com/photo-1575089776834-8be34696ffb9?auto=format&fit=crop&w=900&q=80",
                category = "Américas",
                isDirectFlight = false,
                seatsRemaining = 5
            ),
            TravelPromotionDeal(
                id = "promo_lis_eze",
                destinationCity = "Buenos Aires",
                destinationCountry = "Argentina",
                destinationIata = "EZE",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Iberia",
                airlineIata = "IB",
                departureDateIso = dynamicPromoDate(37),
                originalFareEur = 760.0,
                netFareEur = 415.0,
                displayTotalEur = 433.03,
                discountPercentage = 43,
                highlightBadge = "🇦🇷 TANGO & PATAGÓNIA",
                imageUrl = "https://images.unsplash.com/photo-1589909202802-8f4aadce1849?auto=format&fit=crop&w=900&q=80",
                category = "Américas",
                isDirectFlight = false,
                seatsRemaining = 3
            ),

            // ==================== ÁSIA & DUBAI ====================
            TravelPromotionDeal(
                id = "promo_lis_dxb",
                destinationCity = "Dubai",
                destinationCountry = "Emirados Árabes",
                destinationIata = "DXB",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Emirates",
                airlineIata = "EK",
                departureDateIso = dynamicPromoDate(25),
                originalFareEur = 690.0,
                netFareEur = 349.0,
                displayTotalEur = 364.72,
                discountPercentage = 47,
                highlightBadge = "✨ LUXO EMIRATES",
                imageUrl = "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=900&q=80",
                category = "Ásia & Dubai",
                isDirectFlight = true,
                seatsRemaining = 2
            ),
            TravelPromotionDeal(
                id = "promo_lis_doh",
                destinationCity = "Doha",
                destinationCountry = "Catar",
                destinationIata = "DOH",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Qatar Airways",
                airlineIata = "QR",
                departureDateIso = dynamicPromoDate(28),
                originalFareEur = 670.0,
                netFareEur = 339.0,
                displayTotalEur = 354.37,
                discountPercentage = 47,
                highlightBadge = "🏆 5-STAR SKYTRAX",
                imageUrl = "https://images.unsplash.com/photo-1578895101408-1a36b834405b?auto=format&fit=crop&w=900&q=80",
                category = "Ásia & Dubai",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_hnd",
                destinationCity = "Tóquio",
                destinationCountry = "Japão",
                destinationIata = "HND",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Lufthansa",
                airlineIata = "LH",
                departureDateIso = dynamicPromoDate(40),
                originalFareEur = 990.0,
                netFareEur = 529.0,
                displayTotalEur = 551.02,
                discountPercentage = 44,
                highlightBadge = "🇯🇵 JAPÃO IMPERIAL",
                imageUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=900&q=80",
                category = "Ásia & Dubai",
                isDirectFlight = false,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_bkk",
                destinationCity = "Banguecoque",
                destinationCountry = "Tailândia",
                destinationIata = "BKK",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Qatar Airways",
                airlineIata = "QR",
                departureDateIso = dynamicPromoDate(42),
                originalFareEur = 880.0,
                netFareEur = 469.0,
                displayTotalEur = 488.92,
                discountPercentage = 44,
                highlightBadge = "🇹🇭 TAILÂNDIA EXÓTICA",
                imageUrl = "https://images.unsplash.com/photo-1508009603885-50cf7c579365?auto=format&fit=crop&w=900&q=80",
                category = "Ásia & Dubai",
                isDirectFlight = false,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_sin",
                destinationCity = "Singapura",
                destinationCountry = "Singapura",
                destinationIata = "SIN",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Emirates",
                airlineIata = "EK",
                departureDateIso = dynamicPromoDate(44),
                originalFareEur = 920.0,
                netFareEur = 489.0,
                displayTotalEur = 509.62,
                discountPercentage = 45,
                highlightBadge = "🦁 MARINA BAY LUXURY",
                imageUrl = "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=900&q=80",
                category = "Ásia & Dubai",
                isDirectFlight = false,
                seatsRemaining = 3
            ),

            // ==================== ÁFRICA & OCEANIA ====================
            TravelPromotionDeal(
                id = "promo_lis_rak",
                destinationCity = "Marraquexe",
                destinationCountry = "Marrocos",
                destinationIata = "RAK",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(13),
                originalFareEur = 149.0,
                netFareEur = 49.0,
                displayTotalEur = 54.22,
                discountPercentage = 64,
                highlightBadge = "🇲🇦 MEDINA & RIADS",
                imageUrl = "https://images.unsplash.com/photo-1539020140153-e479b8c22e70?auto=format&fit=crop&w=900&q=80",
                category = "África & Oceania",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_cpt",
                destinationCity = "Cidade do Cabo",
                destinationCountry = "África do Sul",
                destinationIata = "CPT",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "British Airways",
                airlineIata = "BA",
                departureDateIso = dynamicPromoDate(39),
                originalFareEur = 840.0,
                netFareEur = 449.0,
                displayTotalEur = 468.22,
                discountPercentage = 44,
                highlightBadge = "🇿🇦 SAFARI & TABLE MTN",
                imageUrl = "https://images.unsplash.com/photo-1580060839134-75a5edca2e99?auto=format&fit=crop&w=900&q=80",
                category = "África & Oceania",
                isDirectFlight = false,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_syd",
                destinationCity = "Sydney",
                destinationCountry = "Austrália",
                destinationIata = "SYD",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Emirates",
                airlineIata = "EK",
                departureDateIso = dynamicPromoDate(48),
                originalFareEur = 1390.0,
                netFareEur = 749.0,
                displayTotalEur = 778.72,
                discountPercentage = 44,
                highlightBadge = "🇦🇺 OPERA HOUSE & BONDI",
                imageUrl = "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=900&q=80",
                category = "África & Oceania",
                isDirectFlight = false,
                seatsRemaining = 2
            ),

            // ==================== PRAIAS & ILHAS ====================
            TravelPromotionDeal(
                id = "promo_lis_ibz",
                destinationCity = "Ibiza",
                destinationCountry = "Espanha",
                destinationIata = "IBZ",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Vueling",
                airlineIata = "VY",
                departureDateIso = dynamicPromoDate(16),
                originalFareEur = 139.0,
                netFareEur = 45.0,
                displayTotalEur = 50.08,
                discountPercentage = 64,
                highlightBadge = "🏖️ VERÃO BALEARES",
                imageUrl = "https://images.unsplash.com/photo-1563784462041-5f97ac9523dd?auto=format&fit=crop&w=900&q=80",
                category = "Praias",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_pmi",
                destinationCity = "Palma de Maiorca",
                destinationCountry = "Espanha",
                destinationIata = "PMI",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Vueling",
                airlineIata = "VY",
                departureDateIso = dynamicPromoDate(19),
                originalFareEur = 129.0,
                netFareEur = 39.0,
                displayTotalEur = 43.87,
                discountPercentage = 66,
                highlightBadge = "🌊 CALAS TURQUESAS",
                imageUrl = "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=900&q=80",
                category = "Praias",
                isDirectFlight = true,
                seatsRemaining = 4
            ),
            TravelPromotionDeal(
                id = "promo_lis_cun",
                destinationCity = "Cancún",
                destinationCountry = "México",
                destinationIata = "CUN",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "TAP Air Portugal",
                airlineIata = "TP",
                departureDateIso = dynamicPromoDate(35),
                originalFareEur = 740.0,
                netFareEur = 379.0,
                displayTotalEur = 395.77,
                discountPercentage = 47,
                highlightBadge = "🇲🇽 CARIBE & TULUM",
                imageUrl = "https://images.unsplash.com/photo-1552074284-5e88ef1aef18?auto=format&fit=crop&w=900&q=80",
                category = "Praias",
                isDirectFlight = true,
                seatsRemaining = 3
            ),
            TravelPromotionDeal(
                id = "promo_lis_mle",
                destinationCity = "Maldivas",
                destinationCountry = "Maldivas",
                destinationIata = "MLE",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Qatar Airways",
                airlineIata = "QR",
                departureDateIso = dynamicPromoDate(41),
                originalFareEur = 980.0,
                netFareEur = 519.0,
                displayTotalEur = 540.67,
                discountPercentage = 45,
                highlightBadge = "🏝️ PARAÍSO NO ÍNDICO",
                imageUrl = "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=900&q=80",
                category = "Praias",
                isDirectFlight = false,
                seatsRemaining = 2
            ),
            TravelPromotionDeal(
                id = "promo_lis_dps",
                destinationCity = "Bali",
                destinationCountry = "Indonésia",
                destinationIata = "DPS",
                originCity = "Lisboa",
                originIata = "LIS",
                airlineName = "Emirates",
                airlineIata = "EK",
                departureDateIso = dynamicPromoDate(45),
                originalFareEur = 960.0,
                netFareEur = 499.0,
                displayTotalEur = 519.97,
                discountPercentage = 46,
                highlightBadge = "🌺 ILHA DOS DEUSES",
                imageUrl = "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=900&q=80",
                category = "Praias",
                isDirectFlight = false,
                seatsRemaining = 3
            )
        )

    /**
     * Adapta qualquer promoção global para o aeroporto de origem atualmente selecionado pelo utilizador:
     * 1. Atualiza `originIata` e `originCity` com precisão.
     * 2. Verifica se a rota `originIata ➔ destinationIata` possui voo direto comercial real.
     *    Nunca exibe "Voo Direto" se a origem (ex: Ibiza - IBZ) exigir conexão para chegar ao destino!
     * 3. Seleciona uma companhia aérea real compatível com a rota.
     * 4. Invalida `matchedOffer` caso a oferta guardada pertença a outra origem (ex: LIS quando a origem atual é IBZ),
     *    garantindo que ao clicar no card de promoção o utilizador abra EXATAMENTE o voo saindo da origem exibida.
     */
    fun adaptDealToOrigin(
        deal: TravelPromotionDeal,
        currentOrigin: String,
        markupConfig: MarkupConfig = MarkupConfig(),
        hourBucket: Long = currentHourBucket()
    ): TravelPromotionDeal {
        val originPlace = PlaceAutocompleteCatalog.resolveByIata(currentOrigin)
        val origIata = originPlace.iataCode.uppercase().trim()
        val destIata = deal.destinationIata.uppercase().trim()

        val routeHasDirect = AirportAndLoyaltyCatalog.hasDirectCommercialFlights(origIata, destIata)
        val isIntercontinental = AirportAndLoyaltyCatalog.isIntercontinentalRoute(origIata, destIata)
        val canAirportsHaveDirectLongHaul = AirportAndLoyaltyCatalog.canAirportHaveDirectLongHaul(origIata) &&
            AirportAndLoyaltyCatalog.canAirportHaveDirectLongHaul(destIata)

        val resolvedCarrier: Pair<String, String> = resolveRealisticCarrierForRoute(
            originIata = origIata,
            destinationIata = destIata,
            preferredAirlineIata = deal.airlineIata,
            preferredAirlineName = deal.airlineName,
            routeHasDirect = routeHasDirect && (!isIntercontinental || canAirportsHaveDirectLongHaul)
        )

        val finalIsDirect = routeHasDirect &&
            (!isIntercontinental || canAirportsHaveDirectLongHaul) &&
            AirportAndLoyaltyCatalog.canAirlineOperateDirect(resolvedCarrier.first, origIata, destIata)

        // Se o voo mudou de origem ou de preço base, recalcula a tarifa promocional coerente com a rota real
        val validMatchedOffer = deal.matchedOffer?.takeIf {
            it.originIata.equals(origIata, ignoreCase = true) &&
                it.destinationIata.equals(destIata, ignoreCase = true)
        }

        val finalNetEur: Double
        val finalDisplayEur: Double
        val finalOriginalEur: Double
        val finalDiscountPct: Int

        if (validMatchedOffer != null) {
            finalNetEur = validMatchedOffer.netFareEur
            finalDisplayEur = validMatchedOffer.pricing.displayFareBeforeAncillariesEur
            finalOriginalEur = round(finalDisplayEur * 1.52)
            finalDiscountPct = (((finalOriginalEur - finalDisplayEur) / finalOriginalEur) * 100.0).toInt().coerceIn(30, 68)
        } else if (origIata == deal.originIata.uppercase()) {
            // Pequena variação horária (-3% a +2%) para manter ofertas frescas a cada hora
            val hourlyShift = (((hourBucket + deal.id.hashCode().toLong()) % 5L) - 2L) * 1.0
            finalNetEur = (deal.netFareEur + hourlyShift).coerceAtLeast(19.0)
            finalDisplayEur = MarkupEngine.calculateBreakdown(finalNetEur, markupConfig).displayFareBeforeAncillariesEur
            finalOriginalEur = maxOf(deal.originalFareEur, round(finalDisplayEur * 1.55))
            finalDiscountPct = (((finalOriginalEur - finalDisplayEur) / finalOriginalEur) * 100.0).toInt().coerceIn(32, 68)
        } else {
            val baseRouteEco = AirportAndLoyaltyCatalog.resolveRouteBaseNetEcoEur(origIata, destIata)
            val hourlyFactor = 0.78 + (((hourBucket + deal.id.hashCode().toLong()).let { kotlin.math.abs(it) } % 7L) * 0.015)
            finalNetEur = round(baseRouteEco * hourlyFactor).coerceAtLeast(22.0)
            finalDisplayEur = MarkupEngine.calculateBreakdown(finalNetEur, markupConfig).displayFareBeforeAncillariesEur
            finalOriginalEur = round(finalDisplayEur * 1.58)
            finalDiscountPct = (((finalOriginalEur - finalDisplayEur) / finalOriginalEur) * 100.0).toInt().coerceIn(32, 66)
        }

        val cleanBadge = if (!finalIsDirect && deal.highlightBadge.contains("DIRETO", ignoreCase = true)) {
            "⚡ 1 ESCALA RÁPIDA"
        } else {
            deal.highlightBadge
        }

        return deal.copy(
            originIata = origIata,
            originCity = originPlace.cityName,
            airlineIata = resolvedCarrier.first,
            airlineName = resolvedCarrier.second,
            isDirectFlight = finalIsDirect,
            netFareEur = finalNetEur,
            displayTotalEur = finalDisplayEur,
            originalFareEur = finalOriginalEur,
            discountPercentage = finalDiscountPct,
            highlightBadge = cleanBadge,
            isLiveApiPrice = validMatchedOffer != null,
            matchedOffer = validMatchedOffer
        )
    }

    private fun resolveRealisticCarrierForRoute(
        originIata: String,
        destinationIata: String,
        preferredAirlineIata: String,
        preferredAirlineName: String,
        routeHasDirect: Boolean
    ): Pair<String, String> {
        val orig = originIata.uppercase()
        val dest = destinationIata.uppercase()

        if (routeHasDirect && AirportAndLoyaltyCatalog.canAirlineOperateDirect(preferredAirlineIata, orig, dest)) {
            return preferredAirlineIata to preferredAirlineName
        }

        val candidates = listOf(
            "TP" to "TAP Air Portugal",
            "IB" to "Iberia",
            "VY" to "Vueling",
            "UX" to "Air Europa",
            "AD" to "Azul Linhas Aéreas",
            "LA" to "LATAM Airlines",
            "BA" to "British Airways",
            "AF" to "Air France",
            "KL" to "KLM Royal Dutch",
            "LH" to "Lufthansa",
            "LX" to "SWISS International",
            "EK" to "Emirates",
            "QR" to "Qatar Airways",
            "TK" to "Turkish Airlines",
            "FR" to "Ryanair",
            "U2" to "easyJet"
        )

        if (routeHasDirect) {
            candidates.firstOrNull { (code, _) ->
                AirportAndLoyaltyCatalog.canAirlineOperateDirect(code, orig, dest)
            }?.let { return it }
        }

        val spanishAirports = setOf("MAD", "BCN", "IBZ", "PMI", "AGP", "VLC", "ALC", "SVQ", "BIO", "TFS", "LPA")
        val portugueseAirports = setOf("LIS", "OPO", "FAO", "FNC", "PDL")
        val brazilianAirports = setOf("GRU", "VCP", "CGH", "GIG", "SDU", "BSB", "CNF", "SSA", "REC", "FOR", "POA", "CWB", "FLN")

        return when {
            dest in setOf("DXB", "SYD", "DPS", "SIN") -> "EK" to "Emirates"
            dest in setOf("DOH", "MLE", "BKK") -> "QR" to "Qatar Airways"
            dest == "IST" -> "TK" to "Turkish Airlines"
            dest == "HND" || dest == "NRT" -> "LH" to "Lufthansa"
            dest == "VCP" && orig in portugueseAirports -> "AD" to "Azul Linhas Aéreas"
            dest in brazilianAirports && orig in portugueseAirports -> "TP" to "TAP Air Portugal"
            dest in brazilianAirports && orig in spanishAirports -> "IB" to "Iberia"
            orig in spanishAirports && dest in spanishAirports -> "VY" to "Vueling"
            orig in spanishAirports -> "IB" to "Iberia"
            orig in portugueseAirports -> "TP" to "TAP Air Portugal"
            else -> preferredAirlineIata to preferredAirlineName
        }
    }

    /**
     * Retorna as promoções globais filtradas por categoria, adaptadas à origem atual
     * e rotacionadas automaticamente a cada 1 hora (`hourBucket`) para que o carrossel
     * traga sempre novidades de todos os continentes.
     */
    fun getFilteredDeals(
        category: String,
        currentOrigin: String = "LIS",
        hourBucket: Long = currentHourBucket(),
        markupConfig: MarkupConfig = MarkupConfig()
    ): List<TravelPromotionDeal> {
        val originPlace = PlaceAutocompleteCatalog.resolveByIata(currentOrigin)
        val available = GLOBAL_DEALS
            .filter { !it.destinationIata.equals(originPlace.iataCode, ignoreCase = true) }
            .map { deal ->
                adaptDealToOrigin(
                    deal = deal,
                    currentOrigin = originPlace.iataCode,
                    markupConfig = markupConfig,
                    hourBucket = hourBucket
                )
            }

        val filtered = if (category.equals("Todas", ignoreCase = true)) {
            available
        } else {
            available.filter { it.category.equals(category, ignoreCase = true) }
        }

        if (filtered.size <= 1) return filtered

        // Rotaciona a ordem das ofertas a cada 1 hora garantindo variedade constante no topo do carrossel
        return filtered.sortedBy { deal ->
            val hash = kotlin.math.abs((deal.id.hashCode().toLong() * 31L) xor (hourBucket * 1315423911L))
            hash % 10_000L
        }
    }
}
