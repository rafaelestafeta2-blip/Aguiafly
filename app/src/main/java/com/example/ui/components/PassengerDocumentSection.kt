package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.PASSPORT_ISSUING_COUNTRIES
import com.example.data.model.PassengerDocumentInfo
import com.example.data.model.TravelDocType
import com.example.domain.security.PassportSecurityValidator
import com.example.domain.security.PassportValidityStatus

@Composable
fun PassengerDocumentSection(
    documentInfo: PassengerDocumentInfo,
    onUpdateDocumentInfo: (PassengerDocumentInfo) -> Unit,
    flightDepartureDateIso: String? = null,
    modifier: Modifier = Modifier
) {
    val cyan = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.secondary
    var maskDocumentNumber by remember { mutableStateOf(false) }

    val expiryAssessment = remember(documentInfo.expiryDate, flightDepartureDateIso) {
        PassportSecurityValidator.validateDocumentExpiry(
            expiryDateIso = documentInfo.expiryDate,
            referenceFlightDateIso = flightDepartureDateIso
        )
    }

    val birthDateAssessment = remember(documentInfo.birthDate, flightDepartureDateIso) {
        PassportSecurityValidator.validateBirthDate(
            birthDateIso = documentInfo.birthDate,
            referenceFlightDateIso = flightDepartureDateIso
        )
    }

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
                        text = "Exigido pelas companhias aéreas (APIS) • Validação ICAO de 6 meses ativa",
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

            // Seletor de Género IATA (M / F) exigido pela Duffel API
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Género IATA:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf("m" to "Masculino (MR)", "f" to "Feminino (MS)").forEach { (code, label) ->
                    FilterChip(
                        selected = documentInfo.gender.equals(code, ignoreCase = true),
                        onClick = { onUpdateDocumentInfo(documentInfo.copy(gender = code)) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("passenger_gender_chip_$code")
                    )
                }
            }

            // Seletor de País Emissor do Documento (issuing_country_code ISO-3166-1 alpha-2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "País Emissor do Passaporte / Documento (ISO):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PASSPORT_ISSUING_COUNTRIES.forEach { country ->
                        val isSelected = documentInfo.issuingCountryCode.equals(country.isoCode, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onUpdateDocumentInfo(documentInfo.copy(issuingCountryCode = country.isoCode))
                            },
                            label = { Text(country.label) },
                            modifier = Modifier.testTag("issuing_country_chip_${country.isoCode}")
                        )
                    }
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
                visualTransformation = if (maskDocumentNumber) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                trailingIcon = {
                    IconButton(
                        onClick = { maskDocumentNumber = !maskDocumentNumber },
                        modifier = Modifier.testTag("toggle_passport_mask_checkout_button")
                    ) {
                        Icon(
                            imageVector = if (maskDocumentNumber) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (maskDocumentNumber) "Revelar documento" else "Mascarar documento",
                            tint = cyan
                        )
                    }
                },
                supportingText = if (documentInfo.documentNumber.isNotBlank()) {
                    {
                        val maskedPreview = PassportSecurityValidator.maskDocumentNumber(documentInfo.documentNumber)
                        Text("Exibição protegida no bilhete/cofre: $maskedPreview • Emissor: ${documentInfo.issuingCountryCode}")
                    }
                } else null,
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
                    isError = documentInfo.birthDate.isNotBlank() && !birthDateAssessment.canProceedToIssueTicket,
                    supportingText = {
                        Text(
                            text = birthDateAssessment.badgeTitle,
                            color = if (birthDateAssessment.canProceedToIssueTicket) {
                                Color(0xFF10B981)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    },
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
                    isError = documentInfo.expiryDate.isNotBlank() && !expiryAssessment.canProceedToIssueTicket,
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("passenger_doc_expiry_input")
                )
            }

            // Diagnóstico de Validade Internacional do Passaporte (Regra ICAO 6 Meses)
            if (documentInfo.expiryDate.isNotBlank()) {
                val (badgeBg, badgeBorder, badgeText) = when (expiryAssessment.status) {
                    PassportValidityStatus.VALID -> Triple(
                        Color(0xFF065F46).copy(alpha = 0.25f),
                        Color(0xFF10B981).copy(alpha = 0.6f),
                        Color(0xFF34D399)
                    )
                    PassportValidityStatus.EXPIRING_WITHIN_6_MONTHS -> Triple(
                        Color(0xFF78350F).copy(alpha = 0.30f),
                        Color(0xFFF59E0B).copy(alpha = 0.65f),
                        Color(0xFFFBBF24)
                    )
                    PassportValidityStatus.EXPIRED,
                    PassportValidityStatus.INVALID_DATE_FORMAT,
                    PassportValidityStatus.EMPTY -> Triple(
                        Color(0xFF7F1D1D).copy(alpha = 0.30f),
                        Color(0xFFEF4444).copy(alpha = 0.70f),
                        Color(0xFFFCA5A5)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("passport_expiry_status_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (expiryAssessment.status == PassportValidityStatus.VALID) {
                                Icons.Default.CheckCircle
                            } else {
                                Icons.Default.Warning
                            },
                            contentDescription = null,
                            tint = badgeText,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = expiryAssessment.badgeTitle,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeText
                            )
                            Text(
                                text = expiryAssessment.helperMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
