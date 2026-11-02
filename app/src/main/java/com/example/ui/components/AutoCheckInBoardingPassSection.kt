package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BoardingPassCardData
import com.example.data.model.CheckInWindowStatus

@Composable
fun AutoCheckInBoardingPassSection(
    boardingData: BoardingPassCardData,
    onTriggerInstantCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.secondary
    val emerald = MaterialTheme.colorScheme.tertiary
    val isReady = boardingData.status == CheckInWindowStatus.BOARDING_PASS_READY

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isReady) emerald.copy(alpha = 0.12f) else cyan.copy(alpha = 0.10f)
            )
            .border(
                width = 1.2.dp,
                color = if (isReady) emerald.copy(alpha = 0.6f) else cyan.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
            .testTag("auto_checkin_boarding_pass_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Cabeçalho do Status do Check-in Automático
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isReady) Icons.Default.CheckCircle else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isReady) emerald else amber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isReady) {
                            "CARTÃO DE EMBARQUE PRONTO (CHECK-IN CONCLUÍDO)"
                        } else {
                            "CHECK-IN AUTOMÁTICO AGENDADO (${boardingData.windowHoursBeforeDeparture}H ANTES)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isReady) emerald else amber,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isReady) {
                            "Boarding Pass oficial emitido e pronto para leitura no aeroporto"
                        } else {
                            "Abertura da janela: ${boardingData.windowOpensFormatted}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 2. Documento APIS vinculado ao Check-in Automático
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Badge,
                contentDescription = null,
                tint = cyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "APIS Validado: ${boardingData.documentSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        // 3. Se o Cartão de Embarque já foi gerado, exibe Portão, Embarque, Grupo, Assento e QR Code Pronto!
        if (isReady) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BoardingMetricPill(
                    label = "PORTÃO",
                    value = "${boardingData.gate} (${boardingData.terminal})",
                    accent = emerald,
                    modifier = Modifier.weight(1f)
                )
                BoardingMetricPill(
                    label = "EMBARQUE",
                    value = boardingData.boardingTimeFormatted,
                    accent = cyan,
                    modifier = Modifier.weight(1f)
                )
                BoardingMetricPill(
                    label = "ASSENTO",
                    value = boardingData.assignedSeat,
                    accent = amber,
                    modifier = Modifier.weight(1f)
                )
                BoardingMetricPill(
                    label = "GRUPO",
                    value = boardingData.boardingGroup.substringBefore(" "),
                    accent = cyan,
                    modifier = Modifier.weight(1f)
                )
            }

            if (boardingData.wasAutoAssignedAtCheckIn) {
                Text(
                    text = "Assento ${boardingData.assignedSeat} atribuído gratuitamente pela companhia aérea no Check-in Automático!",
                    style = MaterialTheme.typography.bodySmall,
                    color = emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // QR Code 2D + Barcode 1D IATA BCBP do Cartão de Embarque
            IataBarcodeAndQrView(payload = boardingData.cleanBcbpPayload)
        } else {
            // Caso o voo seja no futuro (>48h), exibe o QR provisório e permite antecipar o Check-in Automático com 1 toque
            IataBarcodeAndQrView(payload = boardingData.cleanBcbpPayload)

            Button(
                onClick = onTriggerInstantCheckIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("trigger_instant_checkin_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = amber,
                    contentColor = Color(0xFF040D1A)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Emitir Cartão de Embarque Antecipado",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BoardingMetricPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
