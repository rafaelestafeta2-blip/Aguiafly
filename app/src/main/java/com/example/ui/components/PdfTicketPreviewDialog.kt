package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IssuedTicket
import com.example.domain.export.TicketPdfExporter

@Composable
fun PdfTicketPreviewDialog(
    ticket: IssuedTicket,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val previewBitmap = remember(ticket) {
        TicketPdfExporter.renderTicketPreviewBitmap(context, ticket)
    }
    var downloadStatusMessage by remember { mutableStateOf<String?>(null) }
    val gold = MaterialTheme.colorScheme.primary
    val emerald = MaterialTheme.colorScheme.tertiary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF040F23),
            tonalElevation = 14.dp,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .border(1.5.dp, gold.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                .testTag("pdf_ticket_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabeçalho do Visualizador
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "E-TICKET & RECIBO FISCAL OFICIAL (PDF)",
                            style = MaterialTheme.typography.titleMedium,
                            color = gold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Reserva PNR ${ticket.bookingReference} • Documento pronto para download ou impressão",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBFD4F2)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_pdf_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar visualização do PDF",
                            tint = gold
                        )
                    }
                }

                // Banner de confirmação quando o usuário baixa o PDF em Downloads
                downloadStatusMessage?.let { statusMsg ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = emerald.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, emerald)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = emerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Folha A4 do E-Ticket Renderizada em Alta Definição
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0A1931))
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Pré-visualização do E-Ticket PDF AguiaFly",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(595f / 842f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                    )
                }

                // Botão Principal: Baixar PDF Direto na Pasta Downloads
                Button(
                    onClick = {
                        val res = TicketPdfExporter.saveTicketPdfToDownloads(context, ticket)
                        downloadStatusMessage = res.fold(
                            onSuccess = { path -> "PDF salvo com sucesso em: $path" },
                            onFailure = { err -> "Aviso ao salvar: ${err.localizedMessage}" }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("download_pdf_to_device_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color(0xFF040F23)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Baixar PDF",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Baixar PDF no Telemóvel (Pasta Downloads)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Botões Secundários: Compartilhar no WhatsApp/E-mail e Imprimir
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { TicketPdfExporter.exportAndShareTicketPdf(context, ticket) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_pdf_from_preview_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.75f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar PDF",
                            tint = gold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp / E-mail",
                            style = MaterialTheme.typography.labelLarge,
                            color = gold,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { TicketPdfExporter.printTicketPdf(context, ticket) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_pdf_from_preview_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.75f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Imprimir Bilhete",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Imprimir A4",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
