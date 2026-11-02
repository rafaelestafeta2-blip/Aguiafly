package com.example.domain.concierge

import com.example.data.model.AirlineBrandIdentity
import com.example.data.model.AirportConciergeGuide
import com.example.data.model.DailyFareSlot
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

object AirportAndLoyaltyCatalog {

    private val shortMonthFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale("pt", "PT"))
    private val dayOfWeekNames = mapOf(
        1 to "Seg",
        2 to "Ter",
        3 to "Qua",
        4 to "Qui",
        5 to "Sex",
        6 to "Sáb",
        7 to "Dom"
    )

    fun generateFareCalendarStrip(
        selectedDateIso: String,
        baseLowestEur: Double,
        totalDays: Int = 14
    ): List<DailyFareSlot> {
        val centerDate = runCatching { LocalDate.parse(selectedDateIso.trim()) }
            .getOrDefault(LocalDate.of(2026, 11, 15))
        val startDate = centerDate.minusDays(3)
        val anchorFare = baseLowestEur.coerceAtLeast(120.0)

        return (0 until totalDays).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val dow = date.dayOfWeek.value
            val dayFactor = when (dow) {
                2, 3 -> 0.88 // Terça e Quarta com tarifas promocionais NDC
                4 -> 0.93
                5, 7 -> 1.12 // Sexta e Domingo alta procura
                6 -> 0.96
                else -> 1.0
            }
            val variation = ((date.dayOfMonth % 5) - 2) * 0.025
            val computedFare = round(anchorFare * (dayFactor + variation) * 100.0) / 100.0
            val isPromo = (dow == 2 || dow == 3 || computedFare <= anchorFare * 0.91)

            DailyFareSlot(
                isoDate = date.toString(),
                dayOfWeekShort = dayOfWeekNames[dow] ?: "Dia",
                dayMonthFormatted = date.format(shortMonthFormatter).replace(".", ""),
                estimatedMinEur = computedFare,
                isPromoDay = isPromo
            )
        }
    }

    fun resolveAirlineIdentity(
        airlineIata: String,
        cabinClass: String,
        isWidebody: Boolean
    ): AirlineBrandIdentity {
        val code = airlineIata.trim().uppercase()
        val (alliance, hexColor, co2Saving) = when (code) {
            "TP" -> Triple("Star Alliance", 0xFF10B981, 21)
            "AF" -> Triple("SkyTeam", 0xFF38BDF8, 19)
            "KL" -> Triple("SkyTeam", 0xFF0EA5E9, 18)
            "LH" -> Triple("Star Alliance", 0xFFE5B84B, 17)
            "LX" -> Triple("Star Alliance", 0xFFF43F5E, 20)
            "IB" -> Triple("oneworld", 0xFFF59E0B, 19)
            "BA" -> Triple("oneworld", 0xFF60A5FA, 16)
            "LA" -> Triple("Parceiro Global", 0xFFA855F7, 18)
            else -> Triple("IATA NDC Partner", 0xFFE5B84B, 15)
        }

        val amenities = buildList {
            if (cabinClass in listOf("business", "first")) {
                add(if (isWidebody) "Suíte Lie-Flat 180°" else "Assento Executivo Bloqueado")
                add("Menu Chef & Champagne")
                add("Fast Track & VIP Lounge")
            } else if (cabinClass == "premium_economy") {
                add("Reclinação Ampliada 38\"")
                add("Refeição Quente Premium")
                add("Embarque Prioritário")
            } else {
                add("Bagagem NDC Garantida")
                add("Snack & Bebidas Cortesia")
            }
            add("Wi-Fi Alta Velocidade")
            add("USB-C & AC 110V")
        }

        return AirlineBrandIdentity(
            airlineIata = code,
            emblemMonogram = code.take(2),
            allianceName = alliance,
            badgeAccentHex = hexColor,
            co2ReductionPercent = co2Saving,
            cabinAmenities = amenities
        )
    }

    fun resolveConciergeGuide(
        originIata: String,
        destinationIata: String,
        flightNumber: String
    ): AirportConciergeGuide {
        val orig = originIata.trim().uppercase()
        val dest = destinationIata.trim().uppercase()
        val seed = abs((orig + dest + flightNumber).hashCode())
        val gateNum = (seed % 38) + 4
        val gateLetter = listOf("A", "B", "S", "E")[seed % 4]
        val beltNum = (seed % 11) + 2

        val (terminal, loungeName, loungeDetails) = when (orig) {
            "LIS" -> Triple(
                "Terminal 1 • Área Schengen / Internacional",
                "TAP Premium Lounge Tejo & Atlântico",
                "Piso 6 após Controle de Segurança • 05:00 às 23:30 • Duche & Bar de Vinhos"
            )
            "OPO" -> Triple(
                "Terminal Principal • Piso 3",
                "ANA Lounge Porto Francisco Sá Carneiro",
                "Entre Portões 31 e 32 • 05:00 às 23:00 • Vista Pista Panorâmica"
            )
            "GRU" -> Triple(
                "Terminal 3 • Embarque Internacional",
                "Espaço Banco Safra & Star Alliance Lounge GRU",
                "Mezanino Piso Superior Duty Free • Aberto 24h • Buffet Executivo & Suítes"
            )
            "GIG" -> Triple(
                "Terminal 2 • Píer Sul Internacional",
                "Plaza Premium & Star Alliance Lounge Galeão",
                "Piso 3 Área Restrita Internacional • 06:00 às 23:00"
            )
            "CDG" -> Triple(
                "Terminal 2E • Hall L / K",
                "Salon Air France Business & La Première",
                "Mezanino Central Hall L • 05:30 às 23:30 • Spa Clarins & Alta Gastronomia"
            )
            "MAD" -> Triple(
                "Terminal 4 Satélite (T4S)",
                "Iberia Premium Lounge Velázquez",
                "Centro do Duty Free T4S • Aberto 24h • Adega Espanhola & Salas de Descanso"
            )
            "LHR" -> Triple(
                "Terminal 5 • Concourse A/B",
                "Galleries First & Concorde Lounge",
                "South Security Piso 3 • 05:00 às 22:30 • Champagne Bar & Terraço"
            )
            "FRA" -> Triple(
                "Terminal 1 • Concourse Z",
                "Lufthansa Senator & Panorama Lounge",
                "Em frente ao Portão Z50 • 05:00 às 22:00 • Workpods & Buffet Bávaro"
            )
            "DXB" -> Triple(
                "Terminal 3 • Concourse A",
                "First & Business Class Concourse Lounge",
                "Piso Dedicado com Embarque Direto • Aberto 24h • Cigar Bar & Áreas Quiet"
            )
            else -> Triple(
                "Terminal Internacional 1",
                "AguiaFly Executive Partner Lounge ($orig)",
                "Área de Embarque VIP após Fast Track • 05:30 às 23:00 • Wi-Fi & Buffet"
            )
        }

        val (weatherText, tempC) = when (dest) {
            "LIS", "OPO", "FAO", "FNC" -> "Céu limpo e brisa atlântica" to 22
            "MAD", "BCN", "IBZ", "PMI", "AGP" -> "Ensolarado mediterrânico" to 24
            "GRU", "CGH", "VCP", "GIG", "BSB" -> "Tropical ameno com sol" to 27
            "CDG", "ORY", "AMS", "BRU" -> "Parcialmente nublado elegante" to 16
            "LHR", "LGW", "DUB" -> "Brisa fresca europeia" to 15
            "FCO", "MXP", "VCE", "ATH" -> "Céu aberto ensolarado" to 23
            "ZRH", "GVA", "FRA", "MUC", "VIE" -> "Clima alpino cristalino" to 14
            "DXB", "MIA" -> "Ensolarado quente premium" to 30
            else -> "Tempo agradável para aterragem" to 21
        }

        return AirportConciergeGuide(
            originIata = orig,
            destinationIata = dest,
            terminalLabel = terminal,
            gateSuggestion = "$gateLetter$gateNum",
            baggageBeltSuggestion = "Tapete $beltNum (Prioridade VIP)",
            vipLoungeName = loungeName,
            loungeLocationAndHours = loungeDetails,
            fastTrackAvailable = true,
            destinationWeatherSummary = weatherText,
            destinationTempCelsius = tempC
        )
    }
}
