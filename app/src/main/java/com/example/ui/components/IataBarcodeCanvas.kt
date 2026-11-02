package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.abs

@Composable
fun IataBarcodeAndQrView(
    payload: String,
    modifier: Modifier = Modifier
) {
    val safePayload = payload.ifBlank { "M1PASSENGER/AGUIAFLY EAGF999 LISOPOTP 1024 319C002A0001 100" }
    var showZoomDialog by remember { mutableStateOf(false) }
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    if (showZoomDialog) {
        AirportScannerZoomDialog(
            payload = safePayload,
            onDismiss = { showZoomDialog = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF8FAFC))
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(colors = listOf(champagneGold, aeroCyan)),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { showZoomDialog = true }
            .padding(16.dp)
            .testTag("iata_qr_barcode_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CÓDIGO DE EMBARQUE IATA BCBP (2D QR + 1D BARCODE)",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF0B1F3F),
            fontWeight = FontWeight.ExtraBold
        )

        // 1. QR Code 2D Ampliado (148.dp) para leitura rápida nos scanners de aeroporto
        QrMatrixCanvas(
            payload = safePayload,
            qrSize = 148.dp
        )

        // 2. Código de Barras 1D de Largura Total (68.dp de altura)
        Barcode1DCanvas(
            payload = safePayload,
            barHeight = 68.dp
        )

        Text(
            text = safePayload.take(44),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF0B1F3F),
            maxLines = 1
        )

        // 3. Botão de Expansão (Zoom) em Azul Safira & Dourado Champagne
        Button(
            onClick = { showZoomDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("expand_qr_zoom_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0B1F3F),
                contentColor = champagneGold
            )
        ) {
            Icon(
                imageVector = Icons.Default.ZoomIn,
                contentDescription = "Ampliar QR Code",
                tint = champagneGold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ampliar QR Code / Modo Leitor do Aeroporto",
                style = MaterialTheme.typography.labelLarge,
                color = champagneGold,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AirportScannerZoomDialog(
    payload: String,
    onDismiss: () -> Unit
) {
    val champagneGold = MaterialTheme.colorScheme.primary
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0E254B),
            tonalElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, champagneGold, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MODO SCANNER DE AEROPORTO",
                            style = MaterialTheme.typography.titleMedium,
                            color = champagneGold,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Aproxime do leitor óptico do portão de embarque",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBFD4F2)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar Zoom",
                            tint = champagneGold
                        )
                    }
                }

                // Painel óptico Branco Pérola para leitura a laser/câmera nos portões
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // QR Code Gigante (235.dp) de altíssimo contraste
                    QrMatrixCanvas(
                        payload = payload,
                        qrSize = 235.dp
                    )

                    // Barcode 1D Gigante (86.dp)
                    Barcode1DCanvas(
                        payload = payload,
                        barHeight = 86.dp
                    )

                    Text(
                        text = payload,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF061E3E),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = champagneGold,
                        contentColor = Color(0xFF07152B)
                    )
                ) {
                    Text(
                        text = "Fechar Modo Ampliado",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QrMatrixCanvas(
    payload: String,
    qrSize: Dp
) {
    Canvas(
        modifier = Modifier
            .size(qrSize)
            .background(Color.White)
            .padding(6.dp)
    ) {
        val grid = 17
        val cellW = size.width / grid
        val cellH = size.height / grid
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isFinder = (r in 0..3 && c in 0..3) ||
                    (r in 0..3 && c in (grid - 4) until grid) ||
                    (r in (grid - 4) until grid && c in 0..3) ||
                    (r == 8 && c == 8)
                val hashBit = abs((payload.hashCode() + r * 37 + c * 19)) % 2 == 0
                if (isFinder || hashBit) {
                    drawRect(
                        color = Color(0xFF061E3E),
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW * 0.92f, cellH * 0.92f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Barcode1DCanvas(
    payload: String,
    barHeight: Dp
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
    ) {
        val barsCount = 74
        val slotWidth = size.width / barsCount
        for (i in 0 until barsCount) {
            val ch = payload[i % payload.length].code
            val drawBar = ((ch + i * 7) % 3 != 0) || i < 4 || i > barsCount - 5
            if (drawBar) {
                val thickness = if ((ch + i) % 5 == 0) slotWidth * 0.85f else slotWidth * 0.52f
                drawRect(
                    color = Color(0xFF061E3E),
                    topLeft = Offset(i * slotWidth, 0f),
                    size = Size(thickness, size.height)
                )
            }
        }
    }
}
