package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.PassengerDocumentInfo
import com.example.data.model.TravelDocType

@Composable
fun PassengerDocumentSection(
    documentInfo: PassengerDocumentInfo,
    onUpdateDocumentInfo: (PassengerDocumentInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.secondary

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = cyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Documento de Viagem & Check-in Automático",
                        style = MaterialTheme.typography.titleMedium,
                        color = cyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Exigido pelas companhias aéreas (APIS) para emitir o Cartão de Embarque",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Seletor: Cartão de Cidadão vs Passaporte
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TravelDocType.entries.forEach { docType ->
                    val selected = documentInfo.docType == docType
                    FilterChip(
                        selected = selected,
                        onClick = {
                            onUpdateDocumentInfo(documentInfo.copy(docType = docType))
                        },
                        label = { Text(docType.label) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("doc_type_chip_${docType.shortCode}")
                    )
                }
            }

            OutlinedTextField(
                value = documentInfo.documentNumber,
                onValueChange = {
                    onUpdateDocumentInfo(documentInfo.copy(documentNumber = it))
                },
                label = {
                    Text(
                        if (documentInfo.docType == TravelDocType.PASSPORT) {
                            "Número do Passaporte (Ex: CA849201)"
                        } else {
                            "Nº Cartão de Cidadão / BI (Ex: 14892041 ZX4)"
                        }
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passenger_doc_number_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = documentInfo.birthDate,
                    onValueChange = {
                        onUpdateDocumentInfo(documentInfo.copy(birthDate = it))
                    },
                    label = { Text("Data de Nascimento") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("passenger_birth_date_input")
                )
                OutlinedTextField(
                    value = documentInfo.expiryDate,
                    onValueChange = {
                        onUpdateDocumentInfo(documentInfo.copy(expiryDate = it))
                    },
                    label = { Text("Validade Documento") },
                    placeholder = { Text("AAAA-MM-DD") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("passenger_doc_expiry_input")
                )
            }

            OutlinedTextField(
                value = documentInfo.taxIdNifCpf,
                onValueChange = {
                    onUpdateDocumentInfo(documentInfo.copy(taxIdNifCpf = it))
                },
                label = { Text("NIF / CPF Fiscal para Recibo & Fatura (Opcional)") },
                placeholder = { Text("Ex: 284910234 (NIF PT) ou 000.000.000-00 (CPF BR)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("passenger_tax_id_input")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Switch de Agendamento do Check-in Automático (24h-48h antes do voo)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cyan.copy(alpha = 0.12f))
                    .border(1.dp, cyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ScheduleSend,
                        contentDescription = null,
                        tint = amber
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Check-in Automático AguiaFly (Grátis)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Agendamos e fazemos o check-in automaticamente assim que a janela da companhia abrir (24h a 48h antes do voo), entregando o Boarding Pass com QR Code em 'Minhas Viagens'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = documentInfo.autoCheckInEnabled,
                    onCheckedChange = {
                        onUpdateDocumentInfo(documentInfo.copy(autoCheckInEnabled = it))
                    },
                    modifier = Modifier.testTag("auto_checkin_switch")
                )
            }
        }
    }
}
