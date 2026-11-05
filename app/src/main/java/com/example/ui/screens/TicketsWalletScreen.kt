package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BoardingPassCardData
import com.example.data.model.IssuedTicket
import com.example.data.model.OrderCancellationConfirmation
import com.example.data.model.OrderCancellationQuote
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.domain.export.TicketPdfExporter
import com.example.domain.pricing.MarkupEngine
import com.example.ui.components.CancelledTicketRefundReceiptBanner
import com.example.ui.components.DestinationCoverHeader
import com.example.ui.components.DigitalWalletExportDialog
import com.example.ui.components.OrderCancellationRefundDialog
import com.example.ui.components.PdfTicketPreviewDialog
import com.example.ui.components.TicketAircraftSpecsSection

@Composable
fun TicketsWalletScreen(
    tickets: List<IssuedTicket>,
    onToggleGoogleWallet: (IssuedTicket) -> Unit,
    onRequestVipSupportForTicket: (IssuedTicket) -> Unit = {},
    activeCancellationTicket: IssuedTicket? = null,
    activeCancellationQuote: OrderCancellationQuote? = null,
    isLoadingCancellationQuote: Boolean = false,
    isConfirmingCancellation: Boolean = false,
    confirmedCancellations: Map<String, OrderCancellationConfirmation> = emptyMap(),
    onRequestCancellationQuote: (IssuedTicket) -> Unit = {},
    onConfirmCancellationRefund: () -> Unit = {},
    onDismissCancellationDialog: () -> Unit = {},
    ticketEsimSelections: Map<String, com.example.data.model.AiraloEsimPackage> = emptyMap(),
    ticketLoungeSelections: Map<String, List<com.example.data.model.VipLoungeOffer>> = emptyMap(),
    ticketExperienceSelections: Map<String, List<com.example.data.model.DestinationExperienceOffer>> = emptyMap(),
    onSelectTicketEsim: (IssuedTicket, com.example.data.model.AiraloEsimPackage?) -> Unit = { _, _ -> },
    onToggleTicketLounge: (IssuedTicket, com.example.data.model.VipLoungeOffer) -> Unit = { _, _ -> },
    onToggleTicketExperience: (IssuedTicket, com.example.data.model.DestinationExperienceOffer) -> Unit = { _, _ -> }
) {
    var walletDialogTicket by remember { mutableStateOf<IssuedTicket?>(null) }
    var pdfPreviewTicket by remember { mutableStateOf<IssuedTicket?>(null) }
    var selectedTicketKey by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedFullTicket = remember(tickets, selectedTicketKey) {
        if (selectedTicketKey == null) null
        else tickets.firstOrNull { it.id.ifBlank { it.orderId } == selectedTicketKey }
    }

    if (selectedFullTicket != null) {
        BackHandler {
            selectedTicketKey = null
        }
    }

    walletDialogTicket?.let { activeTicket ->
        DigitalWalletExportDialog(
            ticket = activeTicket,
            onConfirmWalletSync = {
                onToggleGoogleWallet(activeTicket)
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

    activeCancellationTicket?.let { cancellingTicket ->
        OrderCancellationRefundDialog(
            ticket = cancellingTicket,
            quote = activeCancellationQuote,
            isLoadingQuote = isLoadingCancellationQuote,
            isConfirmingCancellation = isConfirmingCancellation,
            onConfirmCancellation = onConfirmCancellationRefund,
            onDismiss = onDismissCancellationDialog
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (selectedFullTicket != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedTicketKey = null }
                        .testTag("back_to_tickets_list_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar para lista de bilhetes",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Voltar para Minhas Viagens (${tickets.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Visualizando Bilhete Completo • PNR ${selectedFullTicket.bookingReference}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${selectedFullTicket.originIata} ➔ ${selectedFullTicket.destinationIata}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            item(key = "full_${selectedFullTicket.id.ifBlank { selectedFullTicket.orderId }}") {
                val ticketKey = selectedFullTicket.id.ifBlank { selectedFullTicket.orderId }
                val bookingRefKey = selectedFullTicket.bookingReference.ifBlank { ticketKey }
                BoardingPassCard(
                    ticket = selectedFullTicket,
                    cancellationConfirmation = confirmedCancellations[ticketKey],
                    selectedEsim = ticketEsimSelections[bookingRefKey],
                    selectedLounges = ticketLoungeSelections[bookingRefKey].orEmpty(),
                    selectedExperiences = ticketExperienceSelections[bookingRefKey].orEmpty(),
                    onSelectEsim = { pkg -> onSelectTicketEsim(selectedFullTicket, pkg) },
                    onToggleLounge = { lounge -> onToggleTicketLounge(selectedFullTicket, lounge) },
                    onToggleExperience = { exp -> onToggleTicketExperience(selectedFullTicket, exp) },
                    onOpenWalletDialog = {
                        onToggleGoogleWallet(selectedFullTicket)
                        walletDialogTicket = selectedFullTicket
                    },
                    onOpenPdfPreview = { pdfPreviewTicket = selectedFullTicket },
                    onRequestVipSupport = { onRequestVipSupportForTicket(selectedFullTicket) },
                    onRequestCancellation = { onRequestCancellationQuote(selectedFullTicket) }
                )
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Minhas Viagens & Carteira Digital",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Toque em qualquer bilhete para abrir o Cartão de Embarque completo, QR Code, Radar e serviços VIP",
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
                                text = "Sua carteira exibe exclusivamente passagens aéreas reais emitidas após confirmação de pagamento Stripe e reserva IATA NDC. Escolha um voo na aba Voos para emitir seu primeiro E-Ticket.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(tickets, key = { it.id.ifBlank { it.orderId } }) { ticket ->
                    val ticketKey = ticket.id.ifBlank { ticket.orderId }
                    CompactTicketCoverCard(
                        ticket = ticket,
                        cancellationConfirmation = confirmedCancellations[ticketKey],
                        onOpenFullTicket = { selectedTicketKey = ticketKey },
                        onQuickDownloadPdf = { pdfPreviewTicket = ticket },
                        onQuickAddGoogleWallet = {
                            onToggleGoogleWallet(ticket)
                            walletDialogTicket = ticket
                        }
                    )
                }
            }
        }
    }
}

/**
 * Cartão de Bilhete Compacto e Limpo:
 * Exibe a foto de capa da cidade/país de destino (`DestinationCoverHeader` garantido para todas as cidades,
 * incluindo Campinas / São Paulo - VCP) + resumo essencial da rota, data, assento e PNR.
 * Ao clicar, abre o bilhete completo (`BoardingPassCard`) com QR Code e todos os detalhes.
 */
@Composable
private fun CompactTicketCoverCard(
    ticket: IssuedTicket,
    cancellationConfirmation: OrderCancellationConfirmation?,
    onOpenFullTicket: () -> Unit,
    onQuickDownloadPdf: () -> Unit = {},
    onQuickAddGoogleWallet: () -> Unit = {}
) {
    val context = LocalContext.current
    val isCancelled = ticket.isCancelled || cancellationConfirmation != null
    val resolvedDestCity = remember(ticket.destinationIata, ticket.destinationCity) {
        ticket.destinationCity.ifBlank {
            PlaceAutocompleteCatalog.resolveByIata(ticket.destinationIata).cityName
        }
    }
    val resolvedOriginCity = remember(ticket.originIata, ticket.originCity) {
        ticket.originCity.ifBlank {
            PlaceAutocompleteCatalog.resolveByIata(ticket.originIata).cityName
        }
    }
    val depDate = ticket.departureIso.take(10)
    val depTime = ticket.departureIso.drop(11).take(5).ifBlank { "09:30" }
    val arrTime = ticket.arrivalIso.drop(11).take(5).ifBlank { "12:15" }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(22.dp)
            )
            .clickable { onOpenFullTicket() }
            .testTag("compact_ticket_card_${ticket.bookingReference}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Foto de Capa Real da Cidade / País de Destino
            DestinationCoverHeader(
                destinationIata = ticket.destinationIata,
                destinationCity = resolvedDestCity,
                bookingReference = ticket.bookingReference,
                cabinClass = ticket.cabinClass,
                bannerHeight = 168.dp
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Linha 1: Rota + Horários + Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightTakeoff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$resolvedOriginCity (${ticket.originIata}) ➔ $resolvedDestCity (${ticket.destinationIata})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "📅 $depDate • 🕒 $depTime ➔ $arrTime • ${ticket.airlineName} (${ticket.flightNumber})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isCancelled) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                        } else {
                            Color(0xFF10B981).copy(alpha = 0.16f)
                        }
                    ) {
                        Text(
                            text = if (isCancelled) "Cancelado" else "Confirmado",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCancelled) MaterialTheme.colorScheme.error else Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Linha 2: Passageiro, Assento e Valor Pago
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👤 ${ticket.passengerName} • 💺 Assento ${ticket.seatDesignator}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = MarkupEngine.formatEur(ticket.totalChargedEur),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Atalhos Diretos de 1 Toque: Baixar PDF e Adicionar à Google Wallet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            TicketPdfExporter.saveTicketPdfToDownloads(context, ticket)
                            onQuickDownloadPdf()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("compact_download_pdf_button_${ticket.bookingReference}"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Baixar PDF",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Baixar PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            TicketPdfExporter.addAndExportToGoogleWallet(context, ticket)
                            onQuickAddGoogleWallet()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("compact_google_wallet_button_${ticket.bookingReference}"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ticket.addedToGoogleWallet) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (ticket.addedToGoogleWallet) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    ) {
                        Icon(
                            imageVector = if (ticket.addedToGoogleWallet) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.AccountBalanceWallet
                            },
                            contentDescription = "Google Wallet",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (ticket.addedToGoogleWallet) "Na Wallet ✓" else "Google Wallet",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Linha 3: Botão Limpo para Abrir o Bilhete Completo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_full_ticket_button_${ticket.bookingReference}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Toque para abrir Bilhete Completo & QR Code",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Abrir bilhete",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardingPassCard(
    ticket: IssuedTicket,
    cancellationConfirmation: OrderCancellationConfirmation?,
    selectedEsim: com.example.data.model.AiraloEsimPackage?,
    selectedLounges: List<com.example.data.model.VipLoungeOffer>,
    selectedExperiences: List<com.example.data.model.DestinationExperienceOffer>,
    onSelectEsim: (com.example.data.model.AiraloEsimPackage?) -> Unit,
    onToggleLounge: (com.example.data.model.VipLoungeOffer) -> Unit,
    onToggleExperience: (com.example.data.model.DestinationExperienceOffer) -> Unit,
    onOpenWalletDialog: () -> Unit,
    onOpenPdfPreview: () -> Unit,
    onRequestVipSupport: () -> Unit,
    onRequestCancellation: () -> Unit
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

                    // 3.6 ACESSO A SALAS VIP (Priority Pass / DragonPass API) para Origem e Conexões
                    com.example.ui.components.RouteVipLoungePassCard(
                        originIata = ticket.originIata,
                        destinationIata = ticket.destinationIata,
                        selectedLounges = selectedLounges,
                        onToggleLoungePass = onToggleLounge,
                        isConfirmedTripView = true,
                        bookingReference = ticket.bookingReference
                    )

                    // 3.7 INTERNET GLOBAL 5G (Airalo eSIM Integration) na Confirmação da Reserva
                    com.example.ui.components.AiraloEsimSelectorCard(
                        destinationIata = ticket.destinationIata,
                        destinationCity = ticket.destinationCity,
                        selectedPackage = selectedEsim,
                        onSelectPackage = onSelectEsim,
                        isPostBookingConfirmation = true,
                        bookingReference = ticket.bookingReference
                    )

                    // 3.8 EXPERIÊNCIAS NO DESTINO ("O que fazer em [Cidade de Destino]" - GetYourGuide / Viator)
                    com.example.ui.components.DestinationExperiencesSectionCard(
                        destinationIata = ticket.destinationIata,
                        destinationCity = ticket.destinationCity,
                        selectedExperiences = selectedExperiences,
                        onToggleExperience = onToggleExperience,
                        isPostPurchaseConfirmation = true,
                        bookingReference = ticket.bookingReference
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
                            onClick = {
                                TicketPdfExporter.saveTicketPdfToDownloads(context, ticket)
                                onOpenPdfPreview()
                            },
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
                                text = "Baixar E-Ticket PDF",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = { TicketPdfExporter.exportAndShareTicketPdf(context, ticket) },
                            modifier = Modifier
                                .weight(0.8f)
                                .height(50.dp)
                                .testTag("share_text_receipt_button_${ticket.bookingReference}"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartilhar PDF",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enviar PDF",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 5. Botão de Exportação Apple Wallet / Google Wallet
                    Button(
                        onClick = {
                            TicketPdfExporter.addAndExportToGoogleWallet(context, ticket)
                            onOpenWalletDialog()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_wallet_button_${ticket.bookingReference}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (ticket.addedToGoogleWallet) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            contentColor = if (ticket.addedToGoogleWallet) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
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
                                "Adicionado à Google Wallet ✓ (Toque para Abrir Passe)"
                            } else {
                                "Adicionar à Google Wallet / Apple Wallet"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 6. Gestão de Cancelamentos & Reembolsos (Duffel POST /air/order_cancellations + Estorno Stripe)
                    val isTicketCancelled = ticket.isCancelled || cancellationConfirmation != null
                    if (isTicketCancelled) {
                        CancelledTicketRefundReceiptBanner(
                            ticket = ticket,
                            confirmation = cancellationConfirmation
                        )
                    } else {
                        OutlinedButton(
                            onClick = onRequestCancellation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("request_cancellation_refund_button_${ticket.bookingReference}"),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.75f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CurrencyExchange,
                                contentDescription = "Solicitar Cancelamento e Reembolso",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Solicitar Cancelamento / Reembolso",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
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
