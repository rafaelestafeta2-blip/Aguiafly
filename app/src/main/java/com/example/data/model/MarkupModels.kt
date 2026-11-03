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
    val isLiveDuffelOffer: Boolean
) {
    val isDirectFlight: Boolean
        get() = stopsCount == 0

    val stopsBadgeLabel: String
        get() = when {
            stopsCount <= 0 -> "Direto"
            stopsCount == 1 -> if (stopoverHubsSummary.isNotBlank()) "1 Escala ($stopoverHubsSummary)" else "1 Escala"
            else -> if (stopoverHubsSummary.isNotBlank()) "$stopsCount Escalas ($stopoverHubsSummary)" else "$stopsCount Escalas"
        }

    val connectionTimeBadgeLabel: String
        get() {
            if (stopsCount <= 0 || totalConnectionMinutes <= 0) {
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
}
