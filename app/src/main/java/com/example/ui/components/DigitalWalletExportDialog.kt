package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IssuedTicket
import com.example.domain.checkin.AutoCheckInManager
import com.example.domain.export.TicketPdfExporter

@Composable
fun DigitalWalletExportDialog(
    ticket: IssuedTicket,
    onConfirmWalletSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val boarding = remember(ticket) {
        AutoCheckInManager.parseTicketCheckInState(ticket, forceImmediateCheckIn = true)
    }
    var walletStatusBanner by remember {
        mutableStateOf("✅ Cartão de Embarque sincronizado e salvo na Google Wallet!")
    }
    val emerald = Color(0xFF10B981)

    // Sincroniza e adiciona automaticamente o bilhete à Google Wallet logo ao abrir
    LaunchedEffect(ticket.bookingReference) {
        onConfirmWalletSync()
        val res = TicketPdfExporter.addAndExportToGoogleWallet(
            context = context,
            ticket = ticket,
            launchExternalWalletIfInstalled = false
        )
        res.onSuccess {
            walletStatusBanner =
                "✅ Adicionado à Google Wallet! Passe PNR ${ticket.bookingReference} ativo e salvo em Downloads."
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(28.dp))
                .border(
                    1.5.dp,
                    emerald.copy(alpha = 0.75f),
                    RoundedCornerShape(28.dp)
                )
                .testTag("digital_wallet_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Foto de Capa Panorâmica do Destino (Estilo Google Wallet Pass)
                DestinationCoverHeader(
                    destinationIata = ticket.destinationIata,
                    destinationCity = ticket.destinationCity,
                    bookingReference = ticket.bookingReference,
                    cabinClass = ticket.cabinClass,
                    bannerHeight = 195.dp
                )

                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-18).dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Selo Verde de Confirmação Google Wallet Ativa
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = emerald.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, emerald),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = emerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = walletStatusBanner,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        WalletDetailBlock(
                            label = "Passageiro & Número do Voo",
                            value = "${ticket.passengerName} • ${ticket.flightNumber} (${ticket.airlineName})"
                        )
                        WalletDetailBlock(
                            label = "Localizador PNR & Bilhete IATA",
                            value = "PNR ${ticket.bookingReference} • ETKT ${boarding.eTicketNumber}"
                        )
                        WalletDetailBlock(
                            label = "Partida, Terminal, Portão & Assento",
                            value = "${ticket.departureIso.take(10)} às ${ticket.departureIso.substringAfter("T").take(5)} • ${boarding.terminal} • Portão ${boarding.gate} • Assento ${boarding.assignedSeat}"
                        )

                        IataBarcodeAndQrView(payload = boarding.cleanBcbpPayload)

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                onConfirmWalletSync()
                                TicketPdfExporter.addAndExportToGoogleWallet(
                                    context = context,
                                    ticket = ticket,
                                    launchExternalWalletIfInstalled = true
                                )
                                walletStatusBanner =
                                    "✅ Passe ${ticket.bookingReference} aberto e sincronizado na Google Wallet!"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_google_apple_wallet_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = emerald,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Abrir na Google Wallet / Visualizar Passe",
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val res = TicketPdfExporter.saveTicketPdfToDownloads(
                                        context = context,
                                        ticket = ticket,
                                        openExternalViewerIfAvailable = true
                                    )
                                    res.onSuccess { path ->
                                        walletStatusBanner = "✅ Passe salvo em $path!"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("download_wallet_pass_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Baixar PDF", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    TicketPdfExporter.exportAndShareTicketPdf(context, ticket)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_pkpass_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Enviar Passe", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("close_wallet_dialog_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Concluído • Voltar ao Bilhete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletDetailBlock(
    label: String,
    value: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}
