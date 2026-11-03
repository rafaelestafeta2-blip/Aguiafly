package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiraloEsimPackage
import com.example.data.model.DestinationExperienceOffer
import com.example.data.model.ExperienceCategory
import com.example.data.model.VipLoungeOffer
import com.example.data.remote.PremiumEcosystemCatalog
import com.example.domain.pricing.MarkupEngine

/**
 * 1. INTERNET GLOBAL (eSIM Integration - Airalo API):
 * Exibido no Checkout e na Confirmação da Reserva ("Minhas Viagens") para comprar/ativar
 * um eSIM de internet 5G para o país de destino com comissão/markup automático AguiaFly.
 */
@Composable
fun AiraloEsimSelectorCard(
    destinationIata: String,
    destinationCity: String,
    selectedPackage: AiraloEsimPackage?,
    onSelectPackage: (AiraloEsimPackage?) -> Unit,
    isPostBookingConfirmation: Boolean = false,
    bookingReference: String = "",
    modifier: Modifier = Modifier
) {
    val packages = remember(destinationIata, destinationCity) {
        PremiumEcosystemCatalog.getAiraloEsimPackagesForDestination(destinationIata, destinationCity)
    }
    val countryName = packages.firstOrNull()?.countryName ?: destinationCity
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E254B),
                        Color(0xFF143264)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(aeroCyan.copy(alpha = 0.75f), champagneGold.copy(alpha = 0.65f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("airalo_esim_selector_card_${destinationIata.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = aeroCyan.copy(alpha = 0.18f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = null,
                                tint = aeroCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Internet Global 5G • eSIM $countryName",
                            style = MaterialTheme.typography.titleMedium,
                            color = champagneGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Airalo Partner API • Ativação Digital Instantânea no Pouso",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = aeroCyan.copy(alpha = 0.16f),
                    modifier = Modifier.border(1.dp, aeroCyan.copy(alpha = 0.55f), RoundedCornerShape(50))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CellTower,
                            contentDescription = null,
                            tint = aeroCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AIRALO eSIM",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = aeroCyan,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (!isPostBookingConfirmation) {
                // Opção padrão no Checkout: Sem eSIM (€0.00)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedPackage == null) {
                        emerald.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (selectedPackage == null) emerald else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectPackage(null) }
                        .testTag("airalo_esim_none_option")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedPackage == null,
                                onClick = { onSelectPackage(null) }
                            )
                            Text(
                                text = "Não adicionar eSIM agora (Posso ativar depois na reserva)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "€0.00",
                            style = MaterialTheme.typography.labelLarge,
                            color = emerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            packages.forEach { pkg ->
                val isSelected = selectedPackage?.packageId == pkg.packageId
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        aeroCyan.copy(alpha = 0.18f)
                    } else {
                        Color(0xFF0A1B38)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) aeroCyan else champagneGold.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (isSelected && isPostBookingConfirmation) {
                                onSelectPackage(null)
                            } else {
                                onSelectPackage(pkg)
                            }
                        }
                        .testTag("airalo_esim_pkg_${pkg.packageId}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectPackage(pkg) }
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = pkg.planTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (pkg.isRecommended) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = champagneGold
                                            ) {
                                                Text(
                                                    text = "MAIS POPULAR",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF07152B),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${pkg.localOperatorName} • Validade ${pkg.validityDays} dias (${pkg.dataAllowanceLabel})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${MarkupEngine.formatEur(pkg.retailPriceEur)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = champagneGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Net ${MarkupEngine.formatEur(pkg.wholesaleNetEur)} + Markup ${MarkupEngine.formatEur(pkg.aguiaFlyCommissionEur)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = aeroCyan
                                )
                            }
                        }

                        if (isSelected && isPostBookingConfirmation) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = emerald.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, emerald.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = emerald,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "eSIM Ativo para Reserva ${bookingReference.ifBlank { "CONFIRMADA" }}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = emerald,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "Perfil LPA: ${pkg.lpaActivationCode}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. ACESSO A SALAS VIP (Lounge Access - Priority Pass / DragonPass API):
 * Exibido na Seleção de Voos, no Checkout e na aba "Minhas Viagens" com a opção
 * "Adicionar Passe de Sala VIP" para os aeroportos da rota (origem e conexões).
 */
@Composable
fun RouteVipLoungePassCard(
    originIata: String,
    destinationIata: String,
    stopoverHubsSummary: String = "",
    selectedLounges: List<VipLoungeOffer>,
    onToggleLoungePass: (VipLoungeOffer) -> Unit,
    isConfirmedTripView: Boolean = false,
    bookingReference: String = "",
    modifier: Modifier = Modifier
) {
    val routeLounges = remember(originIata, destinationIata, stopoverHubsSummary) {
        PremiumEcosystemCatalog.getRouteVipLounges(
            originIata = originIata,
            destinationIata = destinationIata,
            stopoverHubsSummary = stopoverHubsSummary
        )
    }
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF132C58),
                        Color(0xFF0B1E3D)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(champagneGold.copy(alpha = 0.80f), aeroCyan.copy(alpha = 0.50f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("route_vip_lounge_pass_card_${originIata}_${destinationIata}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = champagneGold.copy(alpha = 0.18f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Weekend,
                                contentDescription = null,
                                tint = champagneGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Adicionar Passe de Sala VIP",
                            style = MaterialTheme.typography.titleMedium,
                            color = champagneGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Priority Pass • DragonPass API • Origem ($originIata) & Conexões",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = champagneGold.copy(alpha = 0.16f),
                    modifier = Modifier.border(1.dp, champagneGold.copy(alpha = 0.65f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = "LOUNGE PASS",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = champagneGold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            routeLounges.forEach { lounge ->
                val isSelected = selectedLounges.any { it.loungeId == lounge.loungeId }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        champagneGold.copy(alpha = 0.16f)
                    } else {
                        Color(0xFF081833)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) champagneGold else aeroCyan.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleLoungePass(lounge) }
                        .testTag("vip_lounge_option_${lounge.airportIata.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleLoungePass(lounge) }
                                )
                                Column {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (lounge.isConnectionHub) {
                                            aeroCyan.copy(alpha = 0.20f)
                                        } else {
                                            emerald.copy(alpha = 0.20f)
                                        }
                                    ) {
                                        Text(
                                            text = lounge.segmentRoleLabel.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (lounge.isConnectionHub) aeroCyan else emerald,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${lounge.loungeName} (${lounge.airportCity})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${lounge.terminalLocation} • ${lounge.openHours}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${MarkupEngine.formatEur(lounge.retailPassPriceEur)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = champagneGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Net ${MarkupEngine.formatEur(lounge.wholesaleNetEur)} + Taxa ${MarkupEngine.formatEur(lounge.aguiaFlyMarginEur)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = aeroCyan
                                )
                            }
                        }

                        Text(
                            text = "✦ ${lounge.amenitiesSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = champagneGold.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isSelected && isConfirmedTripView) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = emerald.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, emerald.copy(alpha = 0.55f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = emerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Passe Digital Emitido • Voucher ${lounge.voucherCodePrefix}-${bookingReference.ifBlank { "VIP" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = emerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "QR ATIVO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = champagneGold,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. EXPERIÊNCIAS NO DESTINO (Tours & Transfers - GetYourGuide / Viator API):
 * Secção "O que fazer em [Cidade de Destino]" exibida após a confirmação da compra ("Minhas Viagens")
 * e também disponível no Checkout para adicionar passeios, ingressos sem fila e transfer privado do aeroporto para o hotel.
 */
@Composable
fun DestinationExperiencesSectionCard(
    destinationIata: String,
    destinationCity: String,
    selectedExperiences: List<DestinationExperienceOffer>,
    onToggleExperience: (DestinationExperienceOffer) -> Unit,
    isPostPurchaseConfirmation: Boolean = true,
    bookingReference: String = "",
    modifier: Modifier = Modifier
) {
    val experiences = remember(destinationIata, destinationCity) {
        PremiumEcosystemCatalog.getDestinationExperiences(destinationIata, destinationCity)
    }
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0E254B),
                        Color(0xFF153468)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(emerald.copy(alpha = 0.75f), champagneGold.copy(alpha = 0.70f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("destination_experiences_card_${destinationIata.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = emerald.copy(alpha = 0.18f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                tint = emerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "O que fazer em $destinationCity",
                            style = MaterialTheme.typography.titleMedium,
                            color = champagneGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "GetYourGuide & Viator API • Transfers Privados, Ingressos Sem Fila e Passeios VIP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = champagneGold.copy(alpha = 0.25f))

            experiences.forEach { offer ->
                val isSelected = selectedExperiences.any { it.experienceId == offer.experienceId }
                val badgeAccent = Color(offer.category.badgeColorHex)
                val categoryIcon = when (offer.category) {
                    ExperienceCategory.PRIVATE_TRANSFER -> Icons.Default.DirectionsCar
                    ExperienceCategory.SKIP_THE_LINE_TICKET -> Icons.Default.ConfirmationNumber
                    ExperienceCategory.VIP_TOUR -> Icons.Default.Explore
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) {
                        badgeAccent.copy(alpha = 0.16f)
                    } else {
                        Color(0xFF091B38)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) badgeAccent else badgeAccent.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onToggleExperience(offer) }
                        .testTag("experience_item_${offer.experienceId}")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeAccent.copy(alpha = 0.20f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = categoryIcon,
                                        contentDescription = null,
                                        tint = badgeAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = offer.category.labelPt.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = badgeAccent
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = champagneGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${offer.ratingScore} (${offer.reviewsCount}) • ${offer.providerName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = offer.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = offer.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "⏱ ${offer.durationLabel} • Confirmação Imediata",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = aeroCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "+${MarkupEngine.formatEur(offer.retailPriceEur)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = champagneGold,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Net ${MarkupEngine.formatEur(offer.netPartnerEur)} + Taxa ${MarkupEngine.formatEur(offer.aguiaFlyCommissionEur)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    color = emerald
                                )
                            }
                        }

                        if (isPostPurchaseConfirmation) {
                            if (isSelected) {
                                Button(
                                    onClick = { onToggleExperience(offer) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = emerald,
                                        contentColor = Color(0xFF04142E)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Voucher Confirmado (${bookingReference.ifBlank { "GYG-VIP" }}) • Toque p/ Gerir",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onToggleExperience(offer) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, champagneGold.copy(alpha = 0.7f))
                                ) {
                                    Text(
                                        text = "Adicionar à Minha Viagem em $destinationCity (+${MarkupEngine.formatEur(offer.retailPriceEur)})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = champagneGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
