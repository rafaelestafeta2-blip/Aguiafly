package com.example.data.model

import com.example.domain.pricing.MarkupEngine

data class TravelPromotionDeal(
    val id: String,
    val originIata: String = "LIS",
    val originCity: String = "Lisboa",
    val destinationIata: String,
    val destinationCity: String,
    val destinationCountry: String,
    val photoUrl: String,
    val netFareEur: Double,
    val displayTotalEur: Double,
    val originalFareEur: Double,
    val discountPercent: Int,
    val departureDateIso: String,
    val airlineName: String,
    val airlineIata: String,
    val isDirectFlight: Boolean = true,
    val highlightBadge: String, // ex: "🔥 -45% OFF", "⚡ SUPER TARIFA", "💎 EXECUTIVA", "🏖️ PRAIA"
    val category: String, // "Todas", "Europa", "Américas", "Mundo", "Praias"
    val tagline: String
) {
    val formattedDisplayPrice: String
        get() = MarkupEngine.formatEur(displayTotalEur)

    val formattedOriginalPrice: String
        get() = MarkupEngine.formatEur(originalFareEur)

    val formattedSavings: String
        get() = MarkupEngine.formatEur(originalFareEur - displayTotalEur)
}

object PromotionalDealsCatalog {

    val GLOBAL_DEALS: List<TravelPromotionDeal> = listOf(
        TravelPromotionDeal(
            id = "deal_paris_cdg",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "CDG",
            destinationCity = "Paris",
            destinationCountry = "França",
            photoUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 42.0,
            displayTotalEur = 46.97, // €42 + €3.50 + €1.47
            originalFareEur = 95.0,
            discountPercent = 51,
            departureDateIso = "2026-11-18",
            airlineName = "Air France",
            airlineIata = "AF",
            isDirectFlight = true,
            highlightBadge = "🔥 51% OFF • TARIFA RELÂMPAGO",
            category = "Europa",
            tagline = "Primavera em Paris com voo direto e franquia de cabine inclusa"
        ),
        TravelPromotionDeal(
            id = "deal_roma_fco",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "FCO",
            destinationCity = "Roma",
            destinationCountry = "Itália",
            photoUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 49.0,
            displayTotalEur = 54.22,
            originalFareEur = 110.0,
            discountPercent = 51,
            departureDateIso = "2026-11-20",
            airlineName = "TAP Air Portugal",
            airlineIata = "TP",
            isDirectFlight = true,
            highlightBadge = "⚡ MENOR PREÇO HISTÓRICO",
            category = "Europa",
            tagline = "Descubra o Coliseu e a gastronomia italiana ao melhor preço"
        ),
        TravelPromotionDeal(
            id = "deal_rio_gig",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "GIG",
            destinationCity = "Rio de Janeiro",
            destinationCountry = "Brasil",
            photoUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 285.0,
            displayTotalEur = 298.48, // €285 + €3.50 + €9.98
            originalFareEur = 540.0,
            discountPercent = 45,
            departureDateIso = "2026-11-25",
            airlineName = "TAP Air Portugal",
            airlineIata = "TP",
            isDirectFlight = true,
            highlightBadge = "🏖️ PRAIA & SOL • VOO DIRETO",
            category = "Américas",
            tagline = "Voo direto para a Cidade Maravilhosa com bagagem despachada"
        ),
        TravelPromotionDeal(
            id = "deal_madrid_mad",
            originIata = "OPO",
            originCity = "Porto",
            destinationIata = "MAD",
            destinationCity = "Madrid",
            destinationCountry = "Espanha",
            photoUrl = "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 28.0,
            displayTotalEur = 32.48,
            originalFareEur = 68.0,
            discountPercent = 52,
            departureDateIso = "2026-11-16",
            airlineName = "Iberia NDC",
            airlineIata = "IB",
            isDirectFlight = true,
            highlightBadge = "🔥 SUPER ECONÔMICA €32",
            category = "Europa",
            tagline = "Gran Vía, cultura e tapas no centro da capital espanhola"
        ),
        TravelPromotionDeal(
            id = "deal_dubai_dxb",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "DXB",
            destinationCity = "Dubai",
            destinationCountry = "Emirados Árabes",
            photoUrl = "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 320.0,
            displayTotalEur = 334.70,
            originalFareEur = 580.0,
            discountPercent = 42,
            departureDateIso = "2026-11-28",
            airlineName = "Emirates Airline",
            airlineIata = "EK",
            isDirectFlight = true,
            highlightBadge = "💎 LUXO & FUTURO • EMIRATES",
            category = "Mundo",
            tagline = "Experiência de alta tecnologia no Burj Khalifa e desert safari"
        ),
        TravelPromotionDeal(
            id = "deal_londres_lhr",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "LHR",
            destinationCity = "Londres",
            destinationCountry = "Reino Unido",
            photoUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 39.0,
            displayTotalEur = 43.87,
            originalFareEur = 92.0,
            discountPercent = 52,
            departureDateIso = "2026-11-19",
            airlineName = "British Airways",
            airlineIata = "BA",
            isDirectFlight = true,
            highlightBadge = "⚡ OFERTA RELÂMPAGO NDC",
            category = "Europa",
            tagline = "Voo direto para Heathrow no melhor horário da manhã"
        ),
        TravelPromotionDeal(
            id = "deal_tokyo_hnd",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "HND",
            destinationCity = "Tóquio",
            destinationCountry = "Japão",
            photoUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 450.0,
            displayTotalEur = 469.25,
            originalFareEur = 890.0,
            discountPercent = 47,
            departureDateIso = "2026-12-02",
            airlineName = "All Nippon Airways",
            airlineIata = "NH",
            isDirectFlight = false,
            highlightBadge = "🌸 DESTINO DOS SONHOS",
            category = "Mundo",
            tagline = "Cultura milenar, gastronomia e inovação em Shinjuku e Shibuya"
        ),
        TravelPromotionDeal(
            id = "deal_ny_jfk",
            originIata = "LIS",
            originCity = "Lisboa",
            destinationIata = "JFK",
            destinationCity = "Nova Iorque",
            destinationCountry = "Estados Unidos",
            photoUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=80",
            netFareEur = 310.0,
            displayTotalEur = 324.35,
            originalFareEur = 620.0,
            discountPercent = 48,
            departureDateIso = "2026-11-22",
            airlineName = "Delta Air Lines",
            airlineIata = "DL",
            isDirectFlight = true,
            highlightBadge = "🗽 MANHATTAN IMPERDÍVEL",
            category = "Américas",
            tagline = "Voo direto para Nova Iorque com a melhor tarifa da temporada"
        )
    )

    fun getFilteredDeals(category: String, currentOrigin: String = "GRU"): List<TravelPromotionDeal> {
        val originPlace = PlaceAutocompleteCatalog.resolveByIata(currentOrigin)
        val list = if (category.equals("Todas", ignoreCase = true) || category.isBlank()) {
            GLOBAL_DEALS
        } else {
            GLOBAL_DEALS.filter { it.category.equals(category, ignoreCase = true) }
        }
        return list.map { deal ->
            if (deal.destinationIata.equals(originPlace.iataCode, ignoreCase = true)) {
                val fallbackDest = if (originPlace.iataCode == "LIS") {
                    PlaceAutocompleteCatalog.resolveByIata("CDG")
                } else {
                    PlaceAutocompleteCatalog.resolveByIata("LIS")
                }
                deal.copy(
                    originIata = originPlace.iataCode,
                    originCity = originPlace.cityName,
                    destinationIata = fallbackDest.iataCode,
                    destinationCity = fallbackDest.cityName,
                    destinationCountry = fallbackDest.countryName
                )
            } else {
                deal.copy(
                    originIata = originPlace.iataCode,
                    originCity = originPlace.cityName
                )
            }
        }
    }
}
