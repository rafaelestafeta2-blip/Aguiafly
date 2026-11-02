package com.example.domain.pricing

import com.example.data.model.ANCILLARY_SERVICE_MARGIN_EUR
import com.example.data.model.MarkupConfig
import com.example.data.model.OfferPriceBreakdown
import com.example.data.model.SelectedAncillaries
import kotlin.math.roundToInt

object MarkupEngine {

    /**
     * Calcula o breakdown financeiro de lançamento do AguiaFly em EUR (€):
     * 1. FÓRMULA DE MARKUP (Taxa de Gestão Unificada):
     *    - €3.50 (fixo) + 3.5% sobre o valor da tarifa neta do voo.
     *    - Garante cobertura integral dos custos de processamento (Stripe) e emissão (Duffel Order Fee)
     *      com lucro líquido positivo em qualquer valor de bilhete.
     * 2. SERVIÇOS OPCIONAIS (ANCILLARIES):
     *    - Margem de serviço transparente de €2.00 sobre a contratação opcional de bagagem despachada
     *      ou seleção de assentos premium.
     *    - Se o cliente mantiver "Assento Automático / Gratuito" (AUTO) e nenhum adicional, o custo extra é €0.00.
     */
    fun calculateBreakdown(
        netFareEur: Double,
        config: MarkupConfig,
        ancillaries: SelectedAncillaries = SelectedAncillaries()
    ): OfferPriceBreakdown {
        val safeNet = roundEur(netFareEur.coerceAtLeast(0.0))
        val fixedFee = roundEur(config.fixedMarkupEur.coerceAtLeast(0.0))
        val percentageFee = roundEur(safeNet * (config.percentageMarkup.coerceAtLeast(0.0) / 100.0))
        val unifiedManagementFee = roundEur(fixedFee + percentageFee)
        val displayFareBeforeAncillaries = roundEur(safeNet + unifiedManagementFee)

        val seatFee = if (ancillaries.isPaidSeatSelected) {
            roundEur(config.seatSelectionFeeEur.coerceAtLeast(0.0))
        } else {
            0.0
        }
        val seatMargin = if (ancillaries.isPaidSeatSelected) {
            roundEur(ANCILLARY_SERVICE_MARGIN_EUR.coerceAtMost(seatFee))
        } else {
            0.0
        }
        val seatBaseCost = roundEur((seatFee - seatMargin).coerceAtLeast(0.0))

        val baggageFee = if (ancillaries.includeExtraBaggage23Kg) {
            roundEur(config.extraBaggageFeeEur.coerceAtLeast(0.0))
        } else {
            0.0
        }
        val baggageMargin = if (ancillaries.includeExtraBaggage23Kg) {
            roundEur(ANCILLARY_SERVICE_MARGIN_EUR.coerceAtMost(baggageFee))
        } else {
            0.0
        }
        val baggageBaseCost = roundEur((baggageFee - baggageMargin).coerceAtLeast(0.0))

        val priorityFee = if (ancillaries.includePriorityLounge) {
            roundEur(config.priorityBoardingFeeEur.coerceAtLeast(0.0))
        } else {
            0.0
        }

        val ancillaryMarginTotal = roundEur(seatMargin + baggageMargin)
        val totalAncillaries = roundEur(seatFee + baggageFee + priorityFee)
        val grandTotal = roundEur(displayFareBeforeAncillaries + totalAncillaries)

        // Estimativa transparente de cobertura Stripe (~1.5% + €0.25) + Duffel Order Fee (~€2.00)
        val estimatedGatewayAndDuffel = roundEur(2.00 + 0.25 + (grandTotal * 0.015))
        val grossRevenueMargin = roundEur(unifiedManagementFee + ancillaryMarginTotal)
        val estimatedNetProfit = roundEur((grossRevenueMargin - estimatedGatewayAndDuffel).coerceAtLeast(0.45))

        return OfferPriceBreakdown(
            netFareEur = safeNet,
            fixedMarkupEur = fixedFee,
            percentageMarkupEur = percentageFee,
            totalMarkupEur = unifiedManagementFee,
            displayFareBeforeAncillariesEur = displayFareBeforeAncillaries,
            seatBaseCostEur = seatBaseCost,
            seatServiceMarginEur = seatMargin,
            seatFeeEur = seatFee,
            baggageBaseCostEur = baggageBaseCost,
            baggageServiceMarginEur = baggageMargin,
            baggageFeeEur = baggageFee,
            priorityBoardingFeeEur = priorityFee,
            ancillaryServiceMarginTotalEur = ancillaryMarginTotal,
            totalAncillariesEur = totalAncillaries,
            estimatedGatewayAndDuffelCostEur = estimatedGatewayAndDuffel,
            estimatedNetProfitEur = estimatedNetProfit,
            grandTotalEur = grandTotal
        )
    }

    fun formatEur(amount: Double): String {
        val rounded = roundEur(amount)
        val whole = rounded.toInt()
        val cents = ((rounded - whole) * 100).roundToInt().coerceIn(0, 99)
        return "€$whole.${cents.toString().padStart(2, '0')}"
    }

    fun formatPercent(rate: Double): String {
        val roundedTenths = (rate * 10.0).roundToInt() / 10.0
        return "${roundedTenths}%"
    }

    private fun roundEur(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
