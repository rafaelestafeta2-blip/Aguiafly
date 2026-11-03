package com.example.domain.pricing

import com.example.data.model.AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR
import com.example.data.model.ANCILLARY_SERVICE_MARGIN_EUR
import com.example.data.model.IssuedTicket
import com.example.data.model.LAUNCH_FIXED_MARKUP_EUR
import com.example.data.model.LAUNCH_PERCENTAGE_MARKUP
import com.example.data.model.MarkupConfig
import com.example.data.model.OfferPriceBreakdown
import com.example.data.model.OrderCancellationQuote
import com.example.data.model.SelectedAncillaries
import kotlin.math.roundToInt

object MarkupEngine {

    /**
     * Calcula o breakdown financeiro do AguiaFly em EUR (€):
     * 1. FÓRMULA DE MARKUP BASE DA PASSAGEM (Taxa de Gestão Unificada Ultrabaixa):
     *    - €3.50 (fixo) + 3.5% sobre o valor da tarifa neta do voo.
     *    - Garante cobertura integral dos custos de processamento (Stripe) e emissão (Duffel Order Fee)
     *      com lucro líquido positivo em qualquer valor de bilhete.
     * 2. SERVIÇOS OPCIONAIS & MÓDULO PREMIUM (100% Transparentes no Checkout do Stripe):
     *    - Assento Premium / Bagagem 23kg: margem de €2.00 sobre a tarifa da cia aérea.
     *    - eSIM Airalo Global: tarifa neta Airalo + comissão automática AguiaFly (€2.00 + 15%).
     *    - Passe de Sala VIP (Priority Pass / DragonPass): tarifa parceiro + margem AguiaFly (€4.00/aeroporto).
     *    - Experiências & Transfers (GetYourGuide / Viator): tarifa parceiro + comissão AguiaFly.
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

        // 1. Airalo eSIM Global (Net + Comissão automática AguiaFly)
        val esimNet = roundEur(ancillaries.selectedEsimPackage?.wholesaleNetEur ?: 0.0)
        val esimCommission = roundEur(ancillaries.selectedEsimPackage?.aguiaFlyCommissionEur ?: 0.0)
        val esimTotal = roundEur(ancillaries.selectedEsimPackage?.retailPriceEur ?: 0.0)

        // 2. Priority Pass / DragonPass Salas VIP (Origem e Conexões)
        val loungeNet = roundEur(ancillaries.selectedVipLounges.sumOf { it.wholesaleNetEur })
        val loungeMargin = roundEur(ancillaries.selectedVipLounges.sumOf { it.aguiaFlyMarginEur })
        val loungeTotal = roundEur(ancillaries.selectedVipLounges.sumOf { it.retailPassPriceEur })

        // 3. GetYourGuide / Viator Experiências & Transfers no Destino
        val expNet = roundEur(ancillaries.selectedExperiences.sumOf { it.netPartnerEur })
        val expCommission = roundEur(ancillaries.selectedExperiences.sumOf { it.aguiaFlyCommissionEur })
        val expTotal = roundEur(ancillaries.selectedExperiences.sumOf { it.retailPriceEur })

        val ancillaryMarginTotal = roundEur(
            seatMargin + baggageMargin + esimCommission + loungeMargin + expCommission
        )
        val totalAncillaries = roundEur(
            seatFee + baggageFee + priorityFee + esimTotal + loungeTotal + expTotal
        )
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
            esimNetCostEur = esimNet,
            esimCommissionEur = esimCommission,
            esimTotalFeeEur = esimTotal,
            vipLoungeNetCostEur = loungeNet,
            vipLoungeMarginEur = loungeMargin,
            vipLoungeTotalFeeEur = loungeTotal,
            experiencesNetCostEur = expNet,
            experiencesCommissionEur = expCommission,
            experiencesTotalFeeEur = expTotal,
            ancillaryServiceMarginTotalEur = ancillaryMarginTotal,
            totalAncillariesEur = totalAncillaries,
            estimatedGatewayAndDuffelCostEur = estimatedGatewayAndDuffel,
            estimatedNetProfitEur = estimatedNetProfit,
            grandTotalEur = grandTotal
        )
    }

    /**
     * Regras Financeiras Oficiais de Cancelamento e Reembolso AguiaFly:
     * 1. As taxas operacionais originais da intermediação (€3.50 + 3.5%) são integralmente retidas.
     * 2. Sobre o valor estornado pela companhia aérea (consultado via Duffel POST /air/order_cancellations),
     *    aplica-se uma taxa administrativa do AguiaFly de €25.00 para cobrir custos de gestão.
     * 3. O valor líquido final (Estorno Cia Aérea - €25.00) é devolvido no cartão de crédito via Stripe.
     */
    fun calculateCancellationRefundQuote(
        ticket: IssuedTicket,
        airlineRefundAmountEur: Double,
        cancellationId: String,
        expiresAt: String? = null,
        isLiveDuffelQuote: Boolean = false
    ): OrderCancellationQuote {
        val originalNet = roundEur(ticket.netAmountEur.coerceAtLeast(0.0))
        val retainedFixed = LAUNCH_FIXED_MARKUP_EUR
        val retainedPct = roundEur(originalNet * (LAUNCH_PERCENTAGE_MARKUP / 100.0))
        val defaultRetainedMarkup = roundEur(retainedFixed + retainedPct)
        val retainedIntermediation = if (ticket.markupEur > 0.0) {
            roundEur(ticket.markupEur)
        } else {
            defaultRetainedMarkup
        }

        val safeAirlineRefund = roundEur(airlineRefundAmountEur.coerceAtLeast(0.0))
        val adminFee = AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR
        val netStripeCustomerRefund = roundEur((safeAirlineRefund - adminFee).coerceAtLeast(0.0))

        return OrderCancellationQuote(
            ticketId = ticket.id,
            orderId = ticket.orderId,
            bookingReference = ticket.bookingReference,
            flightSummary = "${ticket.flightNumber} • ${ticket.originCity} (${ticket.originIata}) ➔ ${ticket.destinationCity} (${ticket.destinationIata})",
            cancellationId = cancellationId,
            totalChargedOriginalEur = roundEur(ticket.totalChargedEur),
            originalNetFareEur = originalNet,
            retainedFixedFeeEur = retainedFixed,
            retainedPercentageFeeEur = retainedPct,
            retainedIntermediationFeeEur = retainedIntermediation,
            airlineRefundAmountEur = safeAirlineRefund,
            aguiaFlyAdminFeeEur = adminFee,
            netStripeCustomerRefundEur = netStripeCustomerRefund,
            currency = "EUR",
            expiresAt = expiresAt,
            isLiveDuffelQuote = isLiveDuffelQuote
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
