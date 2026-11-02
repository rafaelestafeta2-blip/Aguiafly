package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.BoardingPassCardData
import com.example.data.model.IssuedTicket
import com.example.domain.export.TicketPdfExporter
import com.example.domain.pricing.MarkupEngine
import com.example.ui.components.DestinationCoverHeader
import com.example.ui.components.DigitalWalletExportDialog
import com.example.ui.components.PdfTicketPreviewDialog
import com.example.ui.components.TicketAircraftSpecsSection

@Composable
fun TicketsWalletScreen(
    tickets: List<IssuedTicket>,
    onToggleGoogleWallet: (IssuedTicket) -> Unit,
    onRequestVipSupportForTicket: (IssuedTicket) -> Unit = {}
) {
    var walletDialogTicket by remember { mutableStateOf<IssuedTicket?>(null) }
    var pdfPreviewTicket by remember { mutableStateOf<IssuedTicket?>(null) }

    walletDialogTicket?.let { activeTicket ->
        DigitalWalletExportDialog(
            ticket = activeTicket,
            onConfirmWalletSync = {
                if (activeTicket.id.isNotBlank() && !activeTicket.addedToGoogleWallet) {
                    onToggleGoogleWallet(activeTicket)
                }
            },
            onDismiss = { walletDialogTicket = null }
        )
    }

    pdfPreviewTicket?.let { activePdfTicket ->
        PdfTicketPreviewDialog(
            ticket = activePdfTicket,
            onDismiss = { pdfPreviewTicket = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Minhas Viagens & Carteira Digital",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Bilhetes oficiais emitidos via IATA NDC, E-Ticket Executivo em PDF com Logo & QR Code e integração Wallet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (tickets.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_tickets_wallet_state")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Nenhum Bilhete Emitido Ainda",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Sua carteira exibe exclusivamente passagens aéreas reais emitidas após confirmação de pagamento Stripe e reserva IATA NDC. Escolha um voo na aba Voos NDC para emitir seu primeiro E-Ticket.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(tickets, key = { it.id.ifBlank { it.orderId } }) { ticket ->
                BoardingPassCard(
                    ticket = ticket,
                    onOpenWalletDialog = { walletDialogTicket = ticket },
                    onOpenPdfPreview = { pdfPreviewTicket = ticket },
                    onRequestVipSupport = { onRequestVipSupportForTicket(ticket) }
                )
            }
        }
    }
}

@Composable
private fun BoardingPassCard(
    ticket: IssuedTicket,
    onOpenWalletDialog: () -> Unit,
    onOpenPdfPreview: () -> Unit,
    onRequestVipSupport: () -> Unit
) {
    val context = LocalContext.current
    var showAircraftSpecs by remember { mutableStateOf(false) }
    var forceCheckInNow by remember(ticket.id) { mutableStateOf(false) }
    val boardingData = remember(ticket, forceCheckInNow) {
        com.example.domain.checkin.AutoCheckInManager.parseTicketCheckInState(
            ticket = ticket,
            forceImmediateCheckIn = forceCheckInNow
        )
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(26.dp)
            )
            .testTag("boarding_pass_card_${ticket.bookingReference}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Foto de Capa Panorâmica do Local da Viagem no topo
            DestinationCoverHeader(
                destinationIata = ticket.destinationIata,
                destinationCity = ticket.destinationCity,
                bookingReference = ticket.bookingReference,
                cabinClass = ticket.cabinClass,
                bannerHeight = 225.dp
            )

            // 2. Corpo Arredondado Sobreposto em Branco & Azul AguiaFly
            Surface(
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-18).dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 2.1 Timeline de Voo em Tempo Real (Partida | Duração Total | Chegada + Trajeto Animado)
                    com.example.ui.components.FlightRealtimeTimelineCard(
                        originIata = ticket.originIata,
                        originCity = ticket.originCity,
                        destinationIata = ticket.destinationIata,
                        destinationCity = ticket.destinationCity,
                        departureIso = ticket.departureIso,
                        arrivalIso = ticket.arrivalIso,
                        flightNumber = ticket.flightNumber
                    )

                    // 2.2 Blocos de informação estilo Google Wallet (Branco & Azul)
                    PremiumPassFieldCard(
                        label = "Número do voo & Bilhete IATA",
                        value = "${ticket.flightNumber} • ${ticket.airlineName}",
                        badgeText = "ETKT ${boardingData.eTicketNumber}"
                    )

                    PremiumPassFieldCard(
                        label = "Número de confirmação (PNR)",
                        value = ticket.bookingReference,
                        badgeText = "ASSENTO ${boardingData.assignedSeat}"
                    )

                    PremiumPassFieldCard(
                        label = "Partida, Terminal & Embarque Limite",
                        value = "Partida ${ticket.departureIso.substringAfter("T").take(5)} • Embarque ${boardingData.boardingTimeFormatted} (${boardingData.terminal} • Portão ${boardingData.gate})",
                        badgeText = MarkupEngine.formatEur(ticket.totalChargedEur)
                    )

                    // 2.5 Quadro Completo de Franquia de Bagagem, Política Tarifária & Dados Fiscais
                    BaggageAndFiscalSummaryCard(boardingData = boardingData)

                    // 3. Painel de Check-in Automático + Cartão de Embarque (Boarding Pass) com QR Code Ampliado & Zoom
                    com.example.ui.components.AutoCheckInBoardingPassSection(
                        boardingData = boardingData,
                        onTriggerInstantCheckIn = { forceCheckInNow = true }
                    )

                    // 3.5 Guia Concierge de Salas VIP, Terminal, Portão, Bagagem e Clima no Destino
                    com.example.ui.components.AirportConciergeGuideCard(
                        originIata = ticket.originIata,
                        destinationIata = ticket.destinationIata,
                        destinationCity = ticket.destinationCity,
                        flightNumber = ticket.flightNumber
                    )

                    // Botão Expansível da Ficha Técnica & Fotos da Aeronave
                    OutlinedButton(
                        onClick = { showAircraftSpecs = !showAircraftSpecs },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("toggle_ticket_aircraft_specs_${ticket.bookingReference}"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AirlineSeatFlat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${ticket.aircraftName} • Ficha & Fotos",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showAircraftSpecs) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showAircraftSpecs) {
                        TicketAircraftSpecsSection(
                            aircraftName = ticket.aircraftName,
                            cabinClass = ticket.cabinClass,
                            seatDesignator = ticket.seatDesignator
                        )
                    }

                    // 3.8 Atalhos de Gestão da Reserva & Suporte Concierge VIP Pós-Emissão
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { forceCheckInNow = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manage_reservation_checkin_button_${ticket.bookingReference}"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Gerir Check-in",
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Check-in & Portão",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onRequestVipSupport,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("vip_concierge_support_button_${ticket.bookingReference}"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Suporte VIP da Reserva",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Suporte VIP PNR",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 4. Botões de Comprovante e E-Ticket Executivo (Pré-visualizar/Baixar PDF + Enviar Recibo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenPdfPreview,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("export_pdf_ticket_button_${ticket.bookingReference}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "Ver e Baixar E-Ticket PDF",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "E-Ticket PDF (Logo & QR)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = { TicketPdfExporter.shareTicketAsText(context, ticket) },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(50.dp)
                                .testTag("share_text_receipt_button_${ticket.bookingReference}"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar Recibo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Recibo Texto",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 5. Botão de Exportação Apple Wallet / Google Wallet
                    Button(
                        onClick = onOpenWalletDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_wallet_button_${ticket.bookingReference}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ticket.addedToGoogleWallet) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (ticket.addedToGoogleWallet) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.AccountBalanceWallet
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (ticket.addedToGoogleWallet) {
                                "Salvo na Apple Wallet & Google Wallet (Abrir Passe)"
                            } else {
                                "Exportar para Apple Wallet / Google Wallet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaggageAndFiscalSummaryCard(
    boardingData: BoardingPassCardData
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Luggage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Franquia de Bagagem & Dados Fiscais do Bilhete",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "• Bagagem Incluída: ${boardingData.baggageAllowanceSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "• Política da Tarifa: ${boardingData.farePolicySummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• Identificação & Recibo Fiscal: ${boardingData.documentTypeLabel} ${boardingData.documentNumber} • NIF/CPF: ${boardingData.taxIdNifCpf}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PremiumPassFieldCard(
    label: String,
    value: String,
    badgeText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
