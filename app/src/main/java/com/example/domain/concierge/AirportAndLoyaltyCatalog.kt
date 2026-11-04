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
    enum class WorldRegion {
        EUROPE,
        SOUTH_AMERICA,
        NORTH_AMERICA,
        MIDDLE_EAST,
        ASIA,
        AFRICA,
        OCEANIA,
        UNKNOWN
    }

    val METRO_AIRPORT_MAPPING: Map<String, Set<String>> = mapOf(
        "SAO" to setOf("GRU", "CGH", "VCP", "SAO"),
        "RIO" to setOf("GIG", "SDU", "RIO"),
        "BHZ" to setOf("CNF", "PLU", "BHZ"),
        "BUE" to setOf("EZE", "AEP", "BUE"),
        "LON" to setOf("LHR", "LGW", "STN", "LTN", "LCY", "SEN", "LON"),
        "PAR" to setOf("CDG", "ORY", "BVA", "PAR"),
        "NYC" to setOf("JFK", "EWR", "LGA", "NYC"),
        "WAS" to setOf("IAD", "DCA", "BWI", "WAS"),
        "CHI" to setOf("ORD", "MDW", "CHI"),
        "MIA_METRO" to setOf("MIA", "FLL"),
        "ROM" to setOf("FCO", "CIA", "ROM"),
        "MIL" to setOf("MXP", "LIN", "BGY", "MIL"),
        "TYO" to setOf("NRT", "HND", "TYO")
    )

    fun areAirportCodesEquivalent(code1: String?, code2: String?): Boolean {
        if (code1 == null || code2 == null) return false
        val c1 = code1.trim().uppercase()
        val c2 = code2.trim().uppercase()
        if (c1 == c2) return true
        METRO_AIRPORT_MAPPING.values.forEach { group ->
            if (c1 in group && c2 in group) return true
        }
        return false
    }

    fun resolveWorldRegion(iataCode: String): WorldRegion {
        val code = iataCode.trim().uppercase()
        val southAmerica = setOf(
            "SAO", "GRU", "CGH", "VCP", "RIO", "GIG", "SDU", "BSB", "CNF", "BHZ", "PLU",
            "SSA", "REC", "FOR", "POA", "CWB", "BEL", "MAO", "FLN", "NAT", "MCZ", "VIX",
            "CGB", "GYN", "IGU", "EZE", "AEP", "BUE", "SCL", "BOG", "LIM", "MVD", "ASU"
        )
        val northAmerica = setOf(
            "NYC", "JFK", "EWR", "LGA", "MIA", "MCO", "FLL", "BOS", "IAD", "DCA", "BWI", "WAS",
            "ORD", "MDW", "CHI", "LAX", "SFO", "SEA", "ATL", "DFW", "IAH", "LAS", "YUL", "YYZ", "YVR", "MEX", "CUN"
        )
        val middleEast = setOf("DXB", "DOH", "AUH", "RUH", "JED", "IST", "SAW")
        val asia = setOf("NRT", "HND", "TYO", "KIX", "ICN", "SIN", "BKK", "HKG", "PVG", "PEK")
        val africa = setOf("CAI", "CPT", "JNB", "CMN", "RAK", "SID", "BVC", "RAI", "VXE")
        val europe = setOf(
            "LIS", "OPO", "FAO", "FNC", "PDL", "TER", "MAD", "BCN", "IBZ", "PMI", "MAH", "AGP",
            "VLC", "ALC", "SVQ", "BIO", "SCQ", "OVD", "SDR", "TFS", "TFN", "LPA", "ACE", "FUE",
            "CDG", "ORY", "BVA", "PAR", "NCE", "LYS", "MRS", "TLS", "BOD", "LHR", "LGW", "STN",
            "LTN", "LCY", "SEN", "LON", "MAN", "EDI", "BHX", "DUB", "FCO", "CIA", "ROM", "MXP",
            "LIN", "BGY", "MIL", "VCE", "NAP", "BLQ", "CTA", "PMO", "FRA", "MUC", "BER", "HAM",
            "DUS", "STR", "VIE", "ZRH", "GVA", "BSL", "AMS", "BRU", "CRL", "EIN", "CPH", "ARN",
            "OSL", "HEL", "WAW", "PRG", "BUD", "ATH", "SKG"
        )

        return when {
            code in southAmerica -> WorldRegion.SOUTH_AMERICA
            code in northAmerica -> WorldRegion.NORTH_AMERICA
            code in middleEast -> WorldRegion.MIDDLE_EAST
            code in asia -> WorldRegion.ASIA
            code in africa -> WorldRegion.AFRICA
            code in europe -> WorldRegion.EUROPE
            else -> WorldRegion.UNKNOWN
        }
    }

    fun isIntercontinentalRoute(originIata: String, destinationIata: String): Boolean {
        val origRegion = resolveWorldRegion(originIata)
        val destRegion = resolveWorldRegion(destinationIata)
        if (origRegion == WorldRegion.UNKNOWN || destRegion == WorldRegion.UNKNOWN) {
            val nonEuropean = setOf(
                "SAO", "GRU", "CGH", "VCP", "RIO", "GIG", "SDU", "BSB", "CNF", "SSA", "REC", "FOR",
                "JFK", "MIA", "MCO", "DXB", "EZE", "SCL", "BOG", "NYC", "BUE"
            )
            return originIata.uppercase() in nonEuropean || destinationIata.uppercase() in nonEuropean
        }
        return origRegion != destRegion
    }

    /**
     * Duração mínima aceitável (em minutos) para a rota considerando a física aeronáutica e geografia.
     * Europa <-> América do Sul: pelo menos 9h30m (570 min).
     * Europa <-> América do Norte: pelo menos 7h30m (450 min).
     */
    fun getMinimumFlightMinutesForRoute(originIata: String, destinationIata: String): Int {
        val origRegion = resolveWorldRegion(originIata)
        val destRegion = resolveWorldRegion(destinationIata)
        val isEuropeSouthAmerica = (origRegion == WorldRegion.EUROPE && destRegion == WorldRegion.SOUTH_AMERICA) ||
            (origRegion == WorldRegion.SOUTH_AMERICA && destRegion == WorldRegion.EUROPE)
        if (isEuropeSouthAmerica) {
            return 570 // 9 horas e 30 minutos mínimo
        }
        val isEuropeNorthAmerica = (origRegion == WorldRegion.EUROPE && destRegion == WorldRegion.NORTH_AMERICA) ||
            (origRegion == WorldRegion.NORTH_AMERICA && destRegion == WorldRegion.EUROPE)
        if (isEuropeNorthAmerica) {
            return 450 // 7 horas e 30 minutos mínimo
        }
        if (isIntercontinentalRoute(originIata, destinationIata)) {
            return 420 // 7 horas mínimo
        }
        return 35 // Voo intra-Europa
    }

    /**
     * Companhias low-cost europeias estritamente regionais / ponto-a-ponto de curto curso.
     * NUNCA operam rotas intercontinentais (transatlânticas).
     */
    val EUROPEAN_LOW_COST_CARRIERS: Set<String> = setOf(
        "FR", // Ryanair
        "VY", // Vueling
        "U2", // easyJet
        "W6", // Wizz Air
        "TO", // Transavia France
        "HV", // Transavia
        "V7", // Volotea
        "EW", // Eurowings
        "I2"  // Iberia Express
    )

    fun isLowCostRegionalAirline(airlineIata: String): Boolean {
        return airlineIata.trim().uppercase() in EUROPEAN_LOW_COST_CARRIERS
    }

    /**
     * Aeroportos de ilhas ou cidades secundárias sem capacidade ou autorização para voos intercontinentais diretos.
     * Ex: Ibiza (IBZ), Menorca (MAH), Faro (FAO) NÃO possuem voos diretos para América do Sul, EUA ou Ásia.
     */
    val REGIONAL_OUTSTATIONS_NO_LONGHAUL_DIRECT: Set<String> = setOf(
        "IBZ", "PMI", "MAH", "VLC", "ALC", "AGP", "SVQ", "BIO", "FAO", "FNC", "PDL", "ORY", "BVA", "LIN", "BGY", "VCE"
    )

    fun canAirportHaveDirectLongHaul(originIata: String): Boolean {
        return originIata.trim().uppercase() !in REGIONAL_OUTSTATIONS_NO_LONGHAUL_DIRECT
    }

    /**
     * Retorna a tarifa base neta (Economy) de referência para qualquer par de aeroportos,
     * priorizando o valor real calibrado pela API para a rota e garantindo consistência 100% exata
     * entre o Calendário de Tarifas e a lista de voos.
     */
    fun resolveRouteBaseNetEcoEur(originIata: String, destinationIata: String): Double {
        val orig = originIata.trim().uppercase()
        val dest = destinationIata.trim().uppercase()
        if (areAirportCodesEquivalent(orig, dest)) return 48.0
        val key = "${orig}_${dest}"
        liveCalibratedRouteBaseEcoMap[key]?.let { return it }

        val origRegion = resolveWorldRegion(orig)
        val destRegion = resolveWorldRegion(dest)

        val pair = setOf(orig, dest)
        if (pair == setOf("IBZ", "BCN") || pair == setOf("IBZ", "PMI") || pair == setOf("IBZ", "VLC")) return 29.0
        if (pair == setOf("IBZ", "MAD") || pair == setOf("LIS", "MAD") || pair == setOf("OPO", "MAD")) return 36.0
        if (pair == setOf("LIS", "OPO") || pair == setOf("BCN", "MAD")) return 34.0

        // Voo doméstico dentro do Brasil (ex: GRU -> FOR, GIG -> GRU)
        if (origRegion == WorldRegion.SOUTH_AMERICA && destRegion == WorldRegion.SOUTH_AMERICA) {
            return 64.0
        }

        // Rota Europa <-> América do Sul (ex: IBZ ➔ SAO, LIS ➔ GRU, MAD ➔ GIG)
        if ((origRegion == WorldRegion.EUROPE && destRegion == WorldRegion.SOUTH_AMERICA) ||
            (origRegion == WorldRegion.SOUTH_AMERICA && destRegion == WorldRegion.EUROPE)) {
            return 320.0
        }

        // Rota Europa <-> América do Norte (ex: LIS ➔ JFK, MAD ➔ MIA)
        if ((origRegion == WorldRegion.EUROPE && destRegion == WorldRegion.NORTH_AMERICA) ||
            (origRegion == WorldRegion.NORTH_AMERICA && destRegion == WorldRegion.EUROPE)) {
            return 280.0
        }

        // Outras rotas intercontinentais
        if (origRegion != destRegion && origRegion != WorldRegion.UNKNOWN && destRegion != WorldRegion.UNKNOWN) {
            return 290.0
        }

        return 48.0
    }

    /**
     * Pares de aeroportos conhecidos que possuem operações aéreas comerciais regulares diretas (point-to-point).
     * Rotas como Ibiza (IBZ) ➔ Porto (OPO) não possuem operações comerciais regulares diretas por companhias
     * de rede como Air France ou TAP Portugal (que obrigatoriamente conectam através de CDG ou LIS).
     */
    private val KNOWN_DIRECT_ROUTE_PAIRS: Set<Set<String>> = setOf(
        setOf("LIS", "OPO"), setOf("LIS", "FAO"), setOf("LIS", "FNC"), setOf("LIS", "PDL"),
        setOf("LIS", "MAD"), setOf("LIS", "BCN"), setOf("LIS", "CDG"), setOf("LIS", "ORY"),
        setOf("LIS", "LHR"), setOf("LIS", "LGW"), setOf("LIS", "FCO"), setOf("LIS", "MXP"),
        setOf("LIS", "AMS"), setOf("LIS", "BRU"), setOf("LIS", "FRA"), setOf("LIS", "MUC"),
        setOf("LIS", "BER"), setOf("LIS", "ZRH"), setOf("LIS", "GVA"), setOf("LIS", "VIE"),
        setOf("LIS", "DUB"), setOf("LIS", "GRU"), setOf("LIS", "GIG"), setOf("LIS", "BSB"),
        setOf("LIS", "CNF"), setOf("LIS", "SSA"), setOf("LIS", "REC"), setOf("LIS", "FOR"),
        setOf("LIS", "JFK"), setOf("LIS", "MIA"), setOf("LIS", "BOS"), setOf("LIS", "DXB"),
        setOf("OPO", "MAD"), setOf("OPO", "BCN"), setOf("OPO", "CDG"), setOf("OPO", "ORY"),
        setOf("OPO", "LHR"), setOf("OPO", "LGW"), setOf("OPO", "AMS"), setOf("OPO", "BRU"),
        setOf("OPO", "FRA"), setOf("OPO", "MUC"), setOf("OPO", "ZRH"), setOf("OPO", "GVA"),
        setOf("OPO", "GRU"), setOf("OPO", "GIG"),
        setOf("MAD", "BCN"), setOf("MAD", "IBZ"), setOf("MAD", "PMI"), setOf("MAD", "VLC"),
        setOf("MAD", "AGP"), setOf("MAD", "SVQ"), setOf("MAD", "BIO"), setOf("MAD", "CDG"),
        setOf("MAD", "ORY"), setOf("MAD", "LHR"), setOf("MAD", "FCO"), setOf("MAD", "MXP"),
        setOf("MAD", "AMS"), setOf("MAD", "FRA"), setOf("MAD", "MUC"), setOf("MAD", "ZRH"),
        setOf("MAD", "JFK"), setOf("MAD", "MIA"), setOf("MAD", "GRU"), setOf("MAD", "GIG"),
        setOf("BCN", "IBZ"), setOf("BCN", "PMI"), setOf("BCN", "VLC"), setOf("BCN", "AGP"),
        setOf("BCN", "SVQ"), setOf("BCN", "CDG"), setOf("BCN", "ORY"), setOf("BCN", "LHR"),
        setOf("BCN", "LGW"), setOf("BCN", "FCO"), setOf("BCN", "MXP"), setOf("BCN", "AMS"),
        setOf("BCN", "FRA"), setOf("BCN", "MUC"), setOf("BCN", "ZRH"),
        setOf("IBZ", "PMI"), setOf("IBZ", "VLC"), setOf("IBZ", "AGP"),
        setOf("CDG", "LHR"), setOf("CDG", "AMS"), setOf("CDG", "FCO"), setOf("CDG", "FRA"),
        setOf("CDG", "MUC"), setOf("CDG", "JFK"), setOf("CDG", "GRU"),
        setOf("LHR", "JFK"), setOf("LHR", "MIA"), setOf("LHR", "DXB"),
        setOf("GRU", "GIG"), setOf("GRU", "BSB"), setOf("GRU", "CNF"), setOf("GRU", "SSA"),
        setOf("GRU", "REC"), setOf("GRU", "FOR"), setOf("GRU", "CWB"), setOf("GRU", "POA")
    )

    fun hasDirectCommercialFlights(originIata: String, destinationIata: String): Boolean {
        val orig = originIata.trim().uppercase()
        val dest = destinationIata.trim().uppercase()
        if (areAirportCodesEquivalent(orig, dest)) return false
        if (isIntercontinentalRoute(orig, dest)) {
            if (!canAirportHaveDirectLongHaul(orig) || !canAirportHaveDirectLongHaul(dest)) {
                return false
            }
        }
        // Verifica combinação direta ou via equivalência metropolitana (ex: LIS -> SAO mapeia para LIS -> GRU)
        if (setOf(orig, dest) in KNOWN_DIRECT_ROUTE_PAIRS) return true
        val origMetros = METRO_AIRPORT_MAPPING[orig] ?: setOf(orig)
        val destMetros = METRO_AIRPORT_MAPPING[dest] ?: setOf(dest)
        for (o in origMetros) {
            for (d in destMetros) {
                if (setOf(o, d) in KNOWN_DIRECT_ROUTE_PAIRS) return true
            }
        }
        return false
    }

    /**
     * Valida se uma determinada companhia aérea opera voos comerciais sem escalas (diretos)
     * no par de aeroportos pesquisado. Companhias tradicionais de rede (ex: Air France, TAP,
     * Iberia, KLM, Lufthansa) operam em modelo hub-and-spoke e NÃO possuem voos diretos
     * entre aeroportos fora de suas bases (por exemplo: Air France e TAP NUNCA voam direto
     * entre Ibiza IBZ e Porto OPO).
     */
    fun canAirlineOperateDirect(airlineIata: String, originIata: String, destinationIata: String): Boolean {
        val orig = originIata.trim().uppercase()
        val dest = destinationIata.trim().uppercase()
        val carrier = airlineIata.trim().uppercase()

        // 1. Bloqueia low-cost regional em rotas intercontinentais
        if (isIntercontinentalRoute(orig, dest) && isLowCostRegionalAirline(carrier)) {
            return false
        }

        // 2. Se a rota for intercontinental e a origem for outstation regional (ex: Ibiza IBZ)
        if (isIntercontinentalRoute(orig, dest)) {
            if (!canAirportHaveDirectLongHaul(orig) || !canAirportHaveDirectLongHaul(dest)) {
                return false
            }
        }

        if (!hasDirectCommercialFlights(orig, dest)) {
            return false
        }

        return when (carrier) {
            "AF" -> orig in setOf("CDG", "ORY", "PAR") || dest in setOf("CDG", "ORY", "PAR")
            "TP" -> orig in setOf("LIS", "OPO") || dest in setOf("LIS", "OPO")
            "IB", "I2" -> orig in setOf("MAD") || dest in setOf("MAD")
            "UX" -> orig in setOf("MAD") || dest in setOf("MAD")
            "KL" -> orig in setOf("AMS") || dest in setOf("AMS")
            "LH" -> orig in setOf("FRA", "MUC") || dest in setOf("FRA", "MUC")
            "BA" -> orig in setOf("LHR", "LGW", "LON") || dest in setOf("LHR", "LGW", "LON")
            "LX" -> orig in setOf("ZRH", "GVA") || dest in setOf("ZRH", "GVA")
            "G3", "AD", "LA" -> (orig in setOf("GRU", "GIG", "BSB", "CGH", "VCP", "SAO", "RIO") || dest in setOf("GRU", "GIG", "BSB", "CGH", "VCP", "SAO", "RIO"))
            else -> !isLowCostRegionalAirline(carrier) || !isIntercontinentalRoute(orig, dest)
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
