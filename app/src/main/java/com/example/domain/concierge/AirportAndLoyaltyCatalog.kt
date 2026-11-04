package com.example.domain.concierge

import com.example.data.model.AirlineBrandIdentity
import com.example.data.model.AirportConciergeGuide
import com.example.data.model.DailyFareSlot
import com.example.data.model.MarkupConfig
import com.example.domain.pricing.MarkupEngine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

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

    private val liveCalibratedRouteBaseEcoMap = java.util.concurrent.ConcurrentHashMap<String, Double>()

    /**
     * Registra a tarifa base neta (Economy) obtida em tempo real da API (Duffel) para o par de aeroportos,
     * garantindo que o Calendário Mensal e os resultados de busca mostrem exatamente os mesmos valores.
     */
    fun recordLiveRouteBaseEcoNet(originIata: String, destinationIata: String, baseEcoNetEur: Double) {
        val key = "${originIata.trim().uppercase()}_${destinationIata.trim().uppercase()}"
        if (baseEcoNetEur > 10.0) {
            liveCalibratedRouteBaseEcoMap[key] = round(baseEcoNetEur * 100.0) / 100.0
        }
    }

    /**
     * Retorna a tarifa base neta (Economy) de referência para qualquer par de aeroportos,
     * priorizando o valor real calibrado pela API para a rota e garantindo consistência 100% exata
     * entre o Calendário de Tarifas e a lista de voos.
     */
    fun resolveRouteBaseNetEcoEur(originIata: String, destinationIata: String): Double {
        val orig = originIata.trim().uppercase()
        val dest = destinationIata.trim().uppercase()
        if (orig == dest) return 48.0
        val key = "${orig}_${dest}"
        liveCalibratedRouteBaseEcoMap[key]?.let { return it }

        val brazilAirports = setOf("GRU", "GIG", "VCP", "CGH", "BSB", "CNF", "SSA", "REC", "FOR", "POA", "CWB", "BEL", "MAO")
        val longHaulIntl = setOf("JFK", "MIA", "MCO", "LAX", "DXB", "EZE", "SCL", "BOG", "MEX")
        val origInBrazil = orig in brazilAirports
        val destInBrazil = dest in brazilAirports

        val pair = setOf(orig, dest)
        return when {
            pair == setOf("IBZ", "BCN") || pair == setOf("IBZ", "PMI") || pair == setOf("IBZ", "VLC") -> 29.0
            pair == setOf("IBZ", "MAD") || pair == setOf("LIS", "MAD") || pair == setOf("OPO", "MAD") -> 36.0
            pair == setOf("LIS", "OPO") || pair == setOf("BCN", "MAD") -> 34.0
            // Voo doméstico dentro do Brasil (ex: GRU -> FOR, GIG -> GRU)
            origInBrazil && destInBrazil -> 64.0
            // Voo intercontinental (ex: Europa <-> Brasil ou EUA/Dubai)
            origInBrazil || destInBrazil || orig in longHaulIntl || dest in longHaulIntl -> 240.0
            else -> 48.0
        }
    }

    fun cabinClassMultiplier(cabinClass: String): Double {
        return when (cabinClass.lowercase()) {
            "first" -> 3.0
            "business" -> 1.95
            "premium_economy" -> 1.35
            else -> 1.0
        }
    }

    /**
     * Multiplicador determinístico de tarifa por data (Dia da Semana + Sazonalidade da Rota).
     * Usado tanto na busca de voos quanto no Calendário Flexível (+/- 3 dias e Menor Preço do Mês)
     * para que ao clicar num dia do calendário os voos exibam exatamente o mesmo valor anunciado.
     */
    fun calculateDateFareMultiplier(
        originIata: String,
        destinationIata: String,
        dateIso: String
    ): Double {
        val date = runCatching { LocalDate.parse(dateIso.trim().take(10)) }
            .getOrDefault(LocalDate.of(2026, 11, 15))
        val dow = date.dayOfWeek.value
        val routeHash = abs((originIata.uppercase() + destinationIata.uppercase()).hashCode())
        // Define qual terça ou quarta do mês tem a "Super Melhor Tarifa do Mês" para esta rota
        val bestDayOfMonthTarget = ((routeHash % 24) + 3)

        val dowFactor = when (dow) {
            2 -> 0.82 // Terça-feira: dia clássico mais barato na aviação
            3 -> 0.85 // Quarta-feira: tarifas promocionais NDC
            4 -> 0.92 // Quinta-feira
            6 -> 0.95 // Sábado
            1 -> 1.00 // Segunda-feira: tarifa padrão
            5 -> 1.14 // Sexta-feira: alta procura fim de semana
            7 -> 1.18 // Domingo: retorno de fim de semana
            else -> 1.00
        }

        val dayDelta = abs(date.dayOfMonth - bestDayOfMonthTarget)
        val monthSpecialFactor = when {
            dayDelta == 0 -> -0.08 // Dia mais barato absoluto do mês ("Melhor Tarifa")
            dayDelta <= 2 -> -0.04
            (date.dayOfMonth + routeHash) % 7 == 0 -> 0.05
            else -> ((date.dayOfMonth % 5) - 2) * 0.015
        }

        return (dowFactor + monthSpecialFactor).coerceIn(0.74, 1.30)
    }

    /**
     * Calcula o menor preço final de exibição (em EUR) para uma rota e data específicas,
     * exatamente igual ao cálculo do calendário e da lista de voos.
     */
    fun computeExactDisplayLowestFareEur(
        originIata: String,
        destinationIata: String,
        dateIso: String,
        cabinClass: String = "economy",
        tripMultiplier: Double = 1.0,
        passengerMultiplier: Double = 1.0,
        markupConfig: MarkupConfig = MarkupConfig()
    ): Double {
        val baseRouteEcoNet = resolveRouteBaseNetEcoEur(originIata, destinationIata)
        val cabinMult = cabinClassMultiplier(cabinClass)
        val paxTripFactor = (tripMultiplier * passengerMultiplier).coerceAtLeast(1.0)
        val dateMult = calculateDateFareMultiplier(originIata, destinationIata, dateIso)
        val singlePaxNet = round(baseRouteEcoNet * cabinMult * dateMult * 100.0) / 100.0
        val totalAdjustedNet = round(singlePaxNet * paxTripFactor * 100.0) / 100.0
        return MarkupEngine.calculateBreakdown(totalAdjustedNet, markupConfig).displayFareBeforeAncillariesEur
    }

    /**
     * Gera o Calendário de Tarifas Flexíveis (+/- 3 dias = 7 dias da semana ou 30 dias do mês)
     * destacando automaticamente com a badge "Melhor Tarifa" (`isLowestOfPeriod = true`) o dia mais barato.
     */
    fun generateRouteFareCalendar(
        originIata: String,
        destinationIata: String,
        anchorDateIso: String,
        cabinClass: String = "economy",
        tripMultiplier: Double = 1.0,
        passengerMultiplier: Double = 1.0,
        markupConfig: MarkupConfig = MarkupConfig(),
        flexibleModePlusMinus3Days: Boolean = true,
        totalDaysInMonthMode: Int = 30
    ): List<DailyFareSlot> {
        val centerDate = runCatching { LocalDate.parse(anchorDateIso.trim().take(10)) }
            .getOrDefault(LocalDate.of(2026, 11, 15))

        val dates: List<LocalDate> = if (flexibleModePlusMinus3Days) {
            (-3L..3L).map { offset -> centerDate.plusDays(offset) }
        } else {
            val start = centerDate.minusDays(3)
            (0L until totalDaysInMonthMode.toLong()).map { offset -> start.plusDays(offset) }
        }

        val rawSlots = dates.map { date ->
            val iso = date.toString()
            val dow = date.dayOfWeek.value
            val displayEur = computeExactDisplayLowestFareEur(
                originIata = originIata,
                destinationIata = destinationIata,
                dateIso = iso,
                cabinClass = cabinClass,
                tripMultiplier = tripMultiplier,
                passengerMultiplier = passengerMultiplier,
                markupConfig = markupConfig
            )
            Triple(date, dow, displayEur)
        }

        val minFareInPeriod = rawSlots.minOfOrNull { it.third } ?: 0.0
        val maxFareInPeriod = rawSlots.maxOfOrNull { it.third } ?: minFareInPeriod

        return rawSlots.map { (date, dow, displayEur) ->
            val isAbsoluteLowest = abs(displayEur - minFareInPeriod) < 0.02
            val isPromo = isAbsoluteLowest || displayEur <= (minFareInPeriod * 1.08)
            val savingsPct = if (maxFareInPeriod > displayEur && maxFareInPeriod > 0.0) {
                (((maxFareInPeriod - displayEur) / maxFareInPeriod) * 100.0).roundToInt().coerceIn(5, 45)
            } else {
                0
            }

            DailyFareSlot(
                isoDate = date.toString(),
                dayOfWeekShort = dayOfWeekNames[dow] ?: "Dia",
                dayMonthFormatted = date.format(shortMonthFormatter).replace(".", ""),
                estimatedMinEur = displayEur,
                isPromoDay = isPromo,
                isLowestOfPeriod = isAbsoluteLowest,
                savingsPercent = savingsPct
            )
        }
    }

    fun generateFareCalendarStrip(
        selectedDateIso: String,
        baseLowestEur: Double,
        totalDays: Int = 7
    ): List<DailyFareSlot> {
        return generateRouteFareCalendar(
            originIata = "LIS",
            destinationIata = "CDG",
            anchorDateIso = selectedDateIso,
            flexibleModePlusMinus3Days = (totalDays <= 7),
            totalDaysInMonthMode = totalDays
        )
    }

    /**
     * Gera todos os dias (1..N) de um mês específico (`year`, `month`) usando exatamente a mesma
     * fórmula e tarifa base calibrada da rota (`computeExactDisplayLowestFareEur`), garantindo que
     * ao clicar em qualquer dia no calendário, o voo mais barato da pesquisa tenha 100% o mesmo valor!
     */
    fun generateFullMonthFareCalendar(
        year: Int,
        month: Int,
        originIata: String,
        destinationIata: String,
        cabinClass: String = "economy",
        tripMultiplier: Double = 1.0,
        passengerMultiplier: Double = 1.0,
        markupConfig: MarkupConfig = MarkupConfig(),
        anchorDateIso: String? = null,
        referenceLowestFareEur: Double? = null
    ): List<DailyFareSlot> {
        val firstDay = LocalDate.of(year, month, 1)
        val lengthOfMonth = firstDay.lengthOfMonth()
        val dates = (0 until lengthOfMonth).map { firstDay.plusDays(it.toLong()) }

        val rawSlots = dates.map { date ->
            val iso = date.toString()
            val dow = date.dayOfWeek.value
            val displayEur = computeExactDisplayLowestFareEur(
                originIata = originIata,
                destinationIata = destinationIata,
                dateIso = iso,
                cabinClass = cabinClass,
                tripMultiplier = tripMultiplier,
                passengerMultiplier = passengerMultiplier,
                markupConfig = markupConfig
            )
            Triple(date, dow, displayEur)
        }

        val minFareInMonth = rawSlots.minOfOrNull { it.third } ?: 0.0
        val maxFareInMonth = rawSlots.maxOfOrNull { it.third } ?: minFareInMonth

        return rawSlots.map { (date, dow, displayEur) ->
            val isAbsoluteLowest = abs(displayEur - minFareInMonth) < 0.02
            val isCheapDay = isAbsoluteLowest || displayEur <= (minFareInMonth * 1.06)
            val savingsPct = if (maxFareInMonth > displayEur && maxFareInMonth > 0.0) {
                (((maxFareInMonth - displayEur) / maxFareInMonth) * 100.0).roundToInt().coerceIn(5, 45)
            } else {
                0
            }

            DailyFareSlot(
                isoDate = date.toString(),
                dayOfWeekShort = dayOfWeekNames[dow] ?: "Dia",
                dayMonthFormatted = date.format(shortMonthFormatter).replace(".", ""),
                estimatedMinEur = displayEur,
                isPromoDay = isCheapDay,
                isLowestOfPeriod = isAbsoluteLowest,
                savingsPercent = savingsPct
            )
        }
    }

    fun resolveAirlineDisplayName(airlineIata: String): String {
        return when (airlineIata.trim().uppercase()) {
            "TP" -> "TAP Portugal"
            "G3" -> "Gol"
            "LA", "JJ" -> "LATAM Airlines"
            "AD" -> "Azul Linhas Aéreas"
            "UX" -> "Air Europa"
            "IB" -> "Iberia"
            "I2" -> "Iberia Express"
            "VY" -> "Vueling Airlines"
            "FR" -> "Ryanair"
            "U2" -> "easyJet"
            "AF" -> "Air France"
            "KL" -> "KLM Royal Dutch"
            "LH" -> "Lufthansa"
            "LX" -> "SWISS"
            "BA" -> "British Airways"
            "EK" -> "Emirates"
            "QR" -> "Qatar Airways"
            else -> "Companhia $airlineIata"
        }
    }

    fun resolveAirlineIdentity(
        airlineIata: String,
        cabinClass: String = "economy",
        isWidebody: Boolean = false
    ): AirlineBrandIdentity {
        val code = airlineIata.trim().uppercase()
        val (alliance, hexColor, co2Saving) = when (code) {
            "TP" -> Triple("Star Alliance", 0xFF10B981, 21)
            "G3" -> Triple("Parceiro Gol Smiles", 0xFFF97316, 18)
            "AD" -> Triple("Azul TudoAzul", 0xFF0EA5E9, 19)
            "UX" -> Triple("SkyTeam", 0xFF0284C7, 18)
            "VY" -> Triple("IAG Partner", 0xFFEAB308, 20)
            "FR" -> Triple("Low-Cost Europe", 0xFF1D4ED8, 22)
            "U2" -> Triple("Europe Direct", 0xFFF97316, 20)
            "AF" -> Triple("SkyTeam", 0xFF38BDF8, 19)
            "KL" -> Triple("SkyTeam", 0xFF0EA5E9, 18)
            "LH" -> Triple("Star Alliance", 0xFFE5B84B, 17)
            "LX" -> Triple("Star Alliance", 0xFFF43F5E, 20)
            "IB", "I2" -> Triple("oneworld", 0xFFF59E0B, 19)
            "BA" -> Triple("oneworld", 0xFF60A5FA, 16)
            "LA" -> Triple("Parceiro Global", 0xFFA855F7, 18)
            "EK" -> Triple("Emirates Skywards", 0xFFD97706, 17)
            else -> Triple("IATA NDC / GDS Partner", 0xFFE5B84B, 15)
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
                add("Bagagem de Mão Incluída")
                add("Tarifa Prime NDC Garantida")
            }
            add("Wi-Fi a Bordo")
            add("USB-C & Tomada AC")
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
