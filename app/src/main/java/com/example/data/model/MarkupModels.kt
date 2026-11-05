package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val AUTO_FREE_SEAT = "AUTO"
const val LAUNCH_FIXED_MARKUP_EUR = 3.50
const val LAUNCH_PERCENTAGE_MARKUP = 3.5
const val ANCILLARY_SERVICE_MARGIN_EUR = 2.00

/**
 * Configuração da Fórmula de Lançamento AguiaFly:
 * - Taxa Dinâmica Unificada: €3.50 (fixo) + 3.5% sobre a tarifa neta do voo.
 *   Garante cobertura integral dos custos Stripe + Duffel Order Fee com lucro líquido positivo.
 * - Serviços Opcionais (Ancillaries & Módulo Premium):
 *   - Bagagem despachada (23kg) ou seleção de assentos premium: margem de €2.00.
 *   - Airalo eSIM Global: tarifa neta Airalo + comissão automática AguiaFly (€2.00 + 15%).
 *   - Salas VIP Priority Pass / DragonPass: tarifa parceiro + margem AguiaFly (€4.00/passe).
 *   - Passeios & Transfers GetYourGuide / Viator: tarifa parceiro + comissão transparente AguiaFly.
 */
@IgnoreExtraProperties
data class MarkupConfig(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fixedMarkupEur: Double = LAUNCH_FIXED_MARKUP_EUR,
    val percentageMarkup: Double = LAUNCH_PERCENTAGE_MARKUP,
    val seatSelectionFeeEur: Double = 14.0, // €12.00 tarifa cia aérea + €2.00 margem de serviço AguiaFly
    val extraBaggageFeeEur: Double = 22.0, // €20.00 tarifa cia aérea + €2.00 margem de serviço AguiaFly
    val priorityBoardingFeeEur: Double = 16.0,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

/**
 * Todos os adicionais (ancillaries e serviços premium) vêm desmarcados por padrão e o assento padrão
 * é "AUTO" (Assento Automático / Gratuito atribuído pela companhia no check-in, custo €0.00).
 */
data class SelectedAncillaries(
    val seatDesignator: String = AUTO_FREE_SEAT,
    val selectedSeatServiceId: String? = null,
    val includeExtraBaggage23Kg: Boolean = false,
    val includePriorityLounge: Boolean = false,
    val selectedEsimPackage: AiraloEsimPackage? = null,
    val selectedVipLounges: List<VipLoungeOffer> = emptyList(),
    val selectedExperiences: List<DestinationExperienceOffer> = emptyList()
) {
    val isPaidSeatSelected: Boolean
        get() = seatDesignator.isNotBlank() &&
            !seatDesignator.equals(AUTO_FREE_SEAT, ignoreCase = true) &&
            !seatDesignator.startsWith("AUTO", ignoreCase = true)

    fun isLoungeSelected(loungeId: String): Boolean =
        selectedVipLounges.any { it.loungeId == loungeId }

    fun isExperienceSelected(experienceId: String): Boolean =
        selectedExperiences.any { it.experienceId == experienceId }
}

data class OfferPriceBreakdown(
    val netFareEur: Double,
    val fixedMarkupEur: Double,
    val percentageMarkupEur: Double,
    val totalMarkupEur: Double, // Taxa de Gestão & Emissão Unificada (€3.50 + 3.5%)
    val displayFareBeforeAncillariesEur: Double,
    val seatBaseCostEur: Double = 0.0,
    val seatServiceMarginEur: Double = 0.0, // €2.00 quando assento premium é selecionado
    val seatFeeEur: Double,
    val baggageBaseCostEur: Double = 0.0,
    val baggageServiceMarginEur: Double = 0.0, // €2.00 quando bagagem despachada é selecionada
    val baggageFeeEur: Double,
    val priorityBoardingFeeEur: Double,
    val esimNetCostEur: Double = 0.0,
    val esimCommissionEur: Double = 0.0,
    val esimTotalFeeEur: Double = 0.0,
    val vipLoungeNetCostEur: Double = 0.0,
    val vipLoungeMarginEur: Double = 0.0,
    val vipLoungeTotalFeeEur: Double = 0.0,
    val experiencesNetCostEur: Double = 0.0,
    val experiencesCommissionEur: Double = 0.0,
    val experiencesTotalFeeEur: Double = 0.0,
    val ancillaryServiceMarginTotalEur: Double = 0.0,
    val totalAncillariesEur: Double,
    val estimatedGatewayAndDuffelCostEur: Double = 0.0,
    val estimatedNetProfitEur: Double = 0.0,
    val grandTotalEur: Double
)

data class FlightSegmentLeg(
    val originIata: String,
    val originCity: String,
    val originAirportName: String,
    val destinationIata: String,
    val destinationCity: String,
    val destinationAirportName: String,
    val departureTimeFormatted: String,
    val arrivalTimeFormatted: String,
    val departureDateFormatted: String,
    val arrivalDateFormatted: String,
    val airlineIata: String,
    val airlineName: String,
    val flightNumber: String,
    val aircraftIataCode: String,
    val durationFormatted: String,
    val layoverMinutesAfterSegment: Int = 0,
    val cabinClass: String = "economy",
    val carryOnBaggageText: String = "Não Incluída",
    val checkedBaggageText: String = "Não Incluída",
    val airportChangeAfterSegment: Boolean = false,
    val nextDepartureAirportIata: String = "",
    val isCriticalLayover: Boolean = false,
    val criticalLayoverAlertMessage: String? = null
)

data class EnrichedFlightOffer(
    val offerId: String,
    val passengerId: String,
    val airlineName: String,
    val airlineIata: String,
    val flightNumber: String,
    val originIata: String,
    val originCity: String,
    val destinationIata: String,
    val destinationCity: String,
    val departureTimeFormatted: String,
    val arrivalTimeFormatted: String,
    val departureIso: String,
    val arrivalIso: String,
    val durationFormatted: String,
    val durationMinutes: Int,
    val stopsCount: Int = 0,
    val stopoverHubsSummary: String = "",
    val totalConnectionMinutes: Int = 0,
    val cabinClass: String,
    val netFareEur: Double,
    val pricing: OfferPriceBreakdown,
    val aircraftSpec: AircraftTechnicalSpec,
    val includedBaggageSummary: String,
    val cancellationPolicySummary: String,
    val isLiveDuffelOffer: Boolean,
    val segmentLegs: List<FlightSegmentLeg> = emptyList(),
    val airportChange: Boolean = false,
    val hasCriticalLayoverAlert: Boolean = false,
    val criticalLayoverAlertSummary: String? = null,
    val carryOnBaggageAllowance: String = "Não Incluída",
    val checkedBaggageAllowance: String = "Não Incluída",
    val isRoundTrip: Boolean = false,
    val returnDateIso: String = "",
    val returnDepartureTimeFormatted: String = "",
    val returnArrivalTimeFormatted: String = "",
    val returnFlightNumber: String = "",
    val returnDurationFormatted: String = ""
) {
    /**
     * Número efetivo de escalas: se houver mais de 1 segmento na lista `segmentLegs`,
     * garante obrigatoriamente pelo menos `segmentLegs.size - 1` escalas,
     * independentemente de qualquer flag externa.
     */
    val effectiveStopsCount: Int
        get() = when {
            segmentLegs.size > 1 -> maxOf(stopsCount, segmentLegs.size - 1)
            !isDirectFlight -> maxOf(stopsCount, 1)
            else -> 0
        }

    /**
     * Filtro Estrito de Voos Diretos (`non_stop`):
     * Uma rota SÓ pode ser classificada e exibida como 'Direta' / 'Directo' se:
     * 1. `slice.segments` tem exatamente tamanho 1 (`length === 1`).
     * 2. O aeroporto de partida desse segmento (`origin.iata_code`) é EXATAMENTE a origem pesquisada
     *    E o aeroporto de chegada (`destination.iata_code`) é EXATAMENTE o destino final pesquisado.
     * 3. A rota e a companhia aérea possuem operação direta comercial real autorizada.
     * Caso contrário, a oferta é estritamente reclassificada como 'Com Escala'.
     */
    val isDirectFlight: Boolean
        get() = if (segmentLegs.isNotEmpty()) {
            val first = segmentLegs.first()
            val origMatch = first.originIata.equals(originIata, ignoreCase = true) ||
                com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent(first.originIata, originIata)
            val destMatch = first.destinationIata.equals(destinationIata, ignoreCase = true) ||
                com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent(first.destinationIata, destinationIata)
            val isIntercontinental = com.example.domain.concierge.AirportAndLoyaltyCatalog.isIntercontinentalRoute(originIata, destinationIata)
            val canAirportDirect = com.example.domain.concierge.AirportAndLoyaltyCatalog.canAirportHaveDirectLongHaul(originIata) &&
                com.example.domain.concierge.AirportAndLoyaltyCatalog.canAirportHaveDirectLongHaul(destinationIata)

            segmentLegs.size == 1 &&
                stopsCount == 0 &&
                origMatch &&
                destMatch &&
                com.example.domain.concierge.AirportAndLoyaltyCatalog.hasDirectCommercialFlights(originIata, destinationIata) &&
                com.example.domain.concierge.AirportAndLoyaltyCatalog.canAirlineOperateDirect(airlineIata, originIata, destinationIata) &&
                (!isIntercontinental || canAirportDirect)
        } else {
            val isIntercontinental = com.example.domain.concierge.AirportAndLoyaltyCatalog.isIntercontinentalRoute(originIata, destinationIata)
            val canAirportDirect = com.example.domain.concierge.AirportAndLoyaltyCatalog.canAirportHaveDirectLongHaul(originIata) &&
                com.example.domain.concierge.AirportAndLoyaltyCatalog.canAirportHaveDirectLongHaul(destinationIata)

            stopsCount == 0 &&
                com.example.domain.concierge.AirportAndLoyaltyCatalog.hasDirectCommercialFlights(originIata, destinationIata) &&
                com.example.domain.concierge.AirportAndLoyaltyCatalog.canAirlineOperateDirect(airlineIata, originIata, destinationIata) &&
                (!isIntercontinental || canAirportDirect)
        }

    val nonStop: Boolean
        get() = isDirectFlight

    val flightTypeClassification: String
        get() = if (isDirectFlight) "Direto" else "Com Escala"

    val stopsBadgeLabel: String
        get() = when {
            isDirectFlight -> "Directo"
            effectiveStopsCount == 1 -> if (stopoverHubsSummary.isNotBlank()) "1 Escala ($stopoverHubsSummary)" else "1 Escala"
            else -> if (stopoverHubsSummary.isNotBlank()) "$effectiveStopsCount Escalas ($stopoverHubsSummary)" else "$effectiveStopsCount Escalas"
        }

    val connectionTimeBadgeLabel: String
        get() {
            if (isDirectFlight || totalConnectionMinutes <= 0) {
                return "Voo Direto • Sem Conexão"
            }
            val hours = totalConnectionMinutes / 60
            val minutes = totalConnectionMinutes % 60
            val formatted = when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes.toString().padStart(2, '0')}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
            return "Conexão total: $formatted"
        }

    /**
     * Retorna a lista detalhada de trechos (pernas do voo) e escalas para exibição no "Resumo da sua viagem".
     * Se o voo tiver escalas (stopsCount > 0), retorna cada trecho individualmente com horários, aeroporto de
     * conexão e tempo de espera na escala.
     */
    fun resolvedSegmentLegs(defaultDateLabel: String): List<FlightSegmentLeg> {
        if (segmentLegs.isNotEmpty()) {
            return segmentLegs
        }
        val origPlace = PlaceAutocompleteCatalog.resolveByIata(originIata)
        val destPlace = PlaceAutocompleteCatalog.resolveByIata(destinationIata)

        if (stopsCount <= 0) {
            return listOf(
                FlightSegmentLeg(
                    originIata = originIata,
                    originCity = originCity.ifBlank { origPlace.cityName },
                    originAirportName = origPlace.airportName,
                    destinationIata = destinationIata,
                    destinationCity = destinationCity.ifBlank { destPlace.cityName },
                    destinationAirportName = destPlace.airportName,
                    departureTimeFormatted = departureTimeFormatted,
                    arrivalTimeFormatted = arrivalTimeFormatted,
                    departureDateFormatted = defaultDateLabel,
                    arrivalDateFormatted = defaultDateLabel,
                    airlineIata = airlineIata,
                    airlineName = airlineName,
                    flightNumber = flightNumber,
                    aircraftIataCode = aircraftSpec.iataCode,
                    durationFormatted = durationFormatted,
                    layoverMinutesAfterSegment = 0
                )
            )
        }

        // Extrai códigos IATA reais dos aeroportos de conexão (ex: "via MAD (Madrid)" -> ["MAD"])
        val ignored = setOf("VIA", "POR", "COM", "SEM", "VOO", "AND", "THE")
        val hubIatas = stopoverHubsSummary
            .replace("(", " ")
            .replace(")", " ")
            .split(",", "•", "/", " ", "-")
            .map { it.trim().uppercase() }
            .filter {
                it.length == 3 &&
                    it.all { ch -> ch.isLetter() } &&
                    it !in ignored &&
                    it != originIata.uppercase() &&
                    it != destinationIata.uppercase()
            }
            .distinct()
            .ifEmpty {
                listOf(if (originIata.equals("MAD", true) || destinationIata.equals("MAD", true)) "LIS" else "MAD")
            }
            .take(stopsCount.coerceAtLeast(1))

        val totalLayover = if (totalConnectionMinutes > 0) totalConnectionMinutes else (hubIatas.size * 80)
        val layoverPerStop = (totalLayover / hubIatas.size.coerceAtLeast(1)).coerceAtLeast(45)
        val totalFlightMinutes = (durationMinutes - totalLayover).coerceAtLeast(60)
        val legsCount = hubIatas.size + 1
        val flightMinsPerLeg = (totalFlightMinutes / legsCount).coerceAtLeast(40)

        val allPoints = listOf(originIata.uppercase()) + hubIatas + listOf(destinationIata.uppercase())
        val baseDepMins = parseHhMmToMinutes(departureTimeFormatted)
        var currentClockMins = baseDepMins
        val baseFlightDigits = flightNumber.filter { it.isDigit() }.toIntOrNull() ?: 1024

        return (0 until legsCount).map { legIdx ->
            val fromIata = allPoints[legIdx]
            val toIata = allPoints[legIdx + 1]
            val fromPlace = PlaceAutocompleteCatalog.resolveByIata(fromIata)
            val toPlace = PlaceAutocompleteCatalog.resolveByIata(toIata)

            val legDepFormatted = formatMinutesToHhMm(currentClockMins)
            val legFlightMins = if (legIdx == legsCount - 1) {
                (totalFlightMinutes - (flightMinsPerLeg * (legsCount - 1))).coerceAtLeast(40)
            } else {
                flightMinsPerLeg
            }
            currentClockMins += legFlightMins
            val legArrFormatted = if (legIdx == legsCount - 1) {
                arrivalTimeFormatted
            } else {
                formatMinutesToHhMm(currentClockMins)
            }
            val layoverAfter = if (legIdx < legsCount - 1) layoverPerStop else 0
            currentClockMins += layoverAfter

            val legHours = legFlightMins / 60
            val legRemMins = legFlightMins % 60
            val legDurationLabel = if (legHours > 0 && legRemMins > 0) {
                "${legHours} h ${legRemMins} min"
            } else if (legHours > 0) {
                "${legHours} h"
            } else {
                "${legRemMins} min"
            }

            FlightSegmentLeg(
                originIata = fromIata,
                originCity = if (legIdx == 0 && originCity.isNotBlank()) originCity else fromPlace.cityName,
                originAirportName = fromPlace.airportName,
                destinationIata = toIata,
                destinationCity = if (legIdx == legsCount - 1 && destinationCity.isNotBlank()) destinationCity else toPlace.cityName,
                destinationAirportName = toPlace.airportName,
                departureTimeFormatted = if (legIdx == 0) departureTimeFormatted else legDepFormatted,
                arrivalTimeFormatted = legArrFormatted,
                departureDateFormatted = defaultDateLabel,
                arrivalDateFormatted = defaultDateLabel,
                airlineIata = airlineIata,
                airlineName = airlineName,
                flightNumber = if (legIdx == 0) flightNumber else "${airlineIata}${baseFlightDigits + legIdx * 12}",
                aircraftIataCode = aircraftSpec.iataCode,
                durationFormatted = legDurationLabel,
                layoverMinutesAfterSegment = layoverAfter
            )
        }
    }

    private fun parseHhMmToMinutes(hhMm: String): Int {
        val parts = hhMm.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (h * 60 + m)
    }

    private fun formatMinutesToHhMm(totalMins: Int): String {
        val norm = ((totalMins % 1440) + 1440) % 1440
        val h = norm / 60
        val m = norm % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
    }
}

