package com.example.ui.components

import android.content.Intent
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IssuedTicket

@Composable
fun DigitalWalletExportDialog(
    ticket: IssuedTicket,
    onConfirmWalletSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Panoramic Destination Cover Photo (Google Wallet Luxury Style)
                DestinationCoverHeader(
                    destinationIata = ticket.destinationIata,
                    destinationCity = ticket.destinationCity,
                    bookingReference = ticket.bookingReference,
                    cabinClass = ticket.cabinClass,
                    bannerHeight = 210.dp
                )

                // Overlapping Rounded Card Container inspired by the reference screenshot
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
                        WalletDetailBlock(
                            label = "Número do voo",
                            value = "${ticket.flightNumber} • ${ticket.airlineName}"
                        )
                        WalletDetailBlock(
                            label = "Número de confirmação",
                            value = ticket.bookingReference
                        )
                        WalletDetailBlock(
                            label = "Hora de partida agendada",
                            value = "${ticket.departureIso.take(10)} • ${ticket.departureIso.substringAfter("T").take(5)} (${ticket.originIata} → ${ticket.destinationIata})"
                        )
                        WalletDetailBlock(
                            label = "Aeronave & Assento Designado",
                            value = "${ticket.aircraftName} • Assento ${ticket.seatDesignator} (${ticket.cabinClass.uppercase()})"
                        )

                        IataBarcodeAndQrView(payload = ticket.iataBarcodePayload)

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                onConfirmWalletSync()
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_google_apple_wallet_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Confirmar na Google Wallet / Apple Wallet",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_SUBJECT,
                                            "E-Ticket Aether ${ticket.flightNumber} (${ticket.bookingReference})"
                                        )
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "AETHER AVIATION E-TICKET\n" +
                                                "Destino: ${ticket.destinationCity} (${ticket.destinationIata})\n" +
                                                "Número do voo: ${ticket.flightNumber}\n" +
                                                "Número de confirmação: ${ticket.bookingReference}\n" +
                                                "Partida: ${ticket.departureIso}\n" +
                                                "Aeronave: ${ticket.aircraftName} | Assento ${ticket.seatDesignator}\n" +
                                                "IATA BCBP: ${ticket.iataBarcodePayload}"
                                        )
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "Exportar Passe .pkpass / Wallet")
                                    )
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
                                Text("Exportar")
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fechar")
                            }
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}
