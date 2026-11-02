package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ANCILLARY_SERVICE_MARGIN_EUR
import com.example.data.model.MarkupConfig
import com.example.data.model.OfferPriceBreakdown
import com.example.data.model.SelectedAncillaries
import com.example.domain.pricing.MarkupEngine

@Composable
fun CheckoutFinancialSummaryCard(
    breakdown: OfferPriceBreakdown,
    markupConfig: MarkupConfig,
    ancillaries: SelectedAncillaries,
    modifier: Modifier = Modifier
) {
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary
    val hasAnyAncillary = breakdown.totalAncillariesEur > 0.0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF153263),
                        Color(0xFF0E254B)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(colors = listOf(champagneGold, aeroCyan)),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("checkout_financial_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumo Financeiro Transparente (EUR €)",
                    style = MaterialTheme.typography.titleMedium,
                    color = champagneGold,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = emerald.copy(alpha = 0.18f),
                    modifier = Modifier.border(1.dp, emerald.copy(alpha = 0.6f), RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = emerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sem Custos Ocultos",
                            style = MaterialTheme.typography.labelLarge,
                            color = emerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Preço Base da Viagem (Tarifa Neta Duffel)
            SummaryPriceRow(
                label = "Preço Base da Viagem (Tarifa Neta Companhia Aérea):",
                amount = MarkupEngine.formatEur(breakdown.netFareEur),
                highlight = false
            )

            // 2. Taxa de Gestão & Emissão Unificada
            SummaryPriceRow(
                label = "Taxa de Gestão e Emissão Unificada:",
                amount = "+${MarkupEngine.formatEur(breakdown.totalMarkupEur)}",
                highlight = true,
                accentColor = aeroCyan
            )

            Text(
                text = "Inclui processamento seguro do pagamento e emissão imediata do bilhete eletrónico IATA NDC sem custos ocultos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SummaryPriceRow(
                label = "Subtotal da Passagem (Preço Base + Taxa de Gestão):",
                amount = MarkupEngine.formatEur(breakdown.displayFareBeforeAncillariesEur),
                highlight = true,
                accentColor = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
            )

            // 3. Detalhamento Transparente de Serviços Opcionais (Ancillaries) com Margem de €2.00
            if (ancillaries.isPaidSeatSelected) {
                SummaryPriceRow(
                    label = "Seleção de Assento Premium (${ancillaries.seatDesignator} • Base ${MarkupEngine.formatEur(breakdown.seatBaseCostEur)} + Taxa Serviço ${MarkupEngine.formatEur(breakdown.seatServiceMarginEur)}):",
                    amount = "+${MarkupEngine.formatEur(breakdown.seatFeeEur)}",
                    highlight = false
                )
            } else {
                SummaryPriceRow(
                    label = "Assento Automático / Gratuito (Atribuído no Check-in):",
                    amount = "€0.00",
                    highlight = true,
                    accentColor = emerald
                )
            }

            if (ancillaries.includeExtraBaggage23Kg) {
                SummaryPriceRow(
                    label = "Bagagem Despachada 23kg (Base ${MarkupEngine.formatEur(breakdown.baggageBaseCostEur)} + Taxa Serviço ${MarkupEngine.formatEur(breakdown.baggageServiceMarginEur)}):",
                    amount = "+${MarkupEngine.formatEur(breakdown.baggageFeeEur)}",
                    highlight = false
                )
            }

            if (ancillaries.includePriorityLounge) {
                SummaryPriceRow(
                    label = "Embarque Prioritário & Fast Track (Opcional):",
                    amount = "+${MarkupEngine.formatEur(breakdown.priorityBoardingFeeEur)}",
                    highlight = false
                )
            }

            if (!hasAnyAncillary) {
                Text(
                    text = "Nenhum serviço opcional contratado • Margem de serviço sobre adicionais (${MarkupEngine.formatEur(ANCILLARY_SERVICE_MARGIN_EUR)}/item) não aplicada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = emerald
                )
            } else if (breakdown.ancillaryServiceMarginTotalEur > 0.0) {
                Text(
                    text = "Inclui margem de serviço transparente de ${MarkupEngine.formatEur(ANCILLARY_SERVICE_MARGIN_EUR)} por bagagem despachada ou assento premium selecionado (Total margem adicionais: +${MarkupEngine.formatEur(breakdown.ancillaryServiceMarginTotalEur)}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = aeroCyan
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = champagneGold.copy(alpha = 0.45f)
            )

            // 4. Valor Total Final a Pagar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Valor Total Final a Pagar:",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (!hasAnyAncillary) {
                            "Preço Base + Taxa de Gestão Unificada (Zero custos extras de emissão)"
                        } else {
                            "Preço Base + Taxa de Gestão Unificada + Opcionais contratados"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = MarkupEngine.formatEur(breakdown.grandTotalEur),
                    style = MaterialTheme.typography.headlineMedium,
                    color = champagneGold,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun SummaryPriceRow(
    label: String,
    amount: String,
    highlight: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge,
            color = if (highlight) accentColor else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
