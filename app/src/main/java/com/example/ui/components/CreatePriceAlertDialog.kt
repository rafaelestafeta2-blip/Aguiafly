package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.pricing.MarkupEngine
import kotlin.math.roundToInt

@Composable
fun CreatePriceAlertDialog(
    initialOriginIata: String,
    initialDestinationIata: String,
    initialCabinClass: String,
    currentLowestPriceEur: Double,
    onDismiss: () -> Unit,
    onConfirmCreateAlert: (
        originIata: String,
        destinationIata: String,
        cabinClass: String,
        targetMaxPriceEur: Double,
        baselinePriceEur: Double
    ) -> Unit
) {
    var origin by remember(initialOriginIata) { mutableStateOf(initialOriginIata.uppercase().take(3)) }
    var destination by remember(initialDestinationIata) { mutableStateOf(initialDestinationIata.uppercase().take(3)) }
    var cabin by remember(initialCabinClass) { mutableStateOf(initialCabinClass) }

    val suggestedTarget = remember(currentLowestPriceEur) {
        ((currentLowestPriceEur * 0.88).roundToInt().coerceAtLeast(35)).toString()
    }
    var targetPriceInput by remember(suggestedTarget) { mutableStateOf(suggestedTarget) }

    val cabins = listOf(
        "economy" to "Economy",
        "premium_economy" to "Premium Eco",
        "business" to "Business",
        "first" to "First"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Criar Alerta de Preço (Push FCM)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Escolha a rota que deseja monitorizar. Assim que a nossa rotina automática encontrar voos promocionais abaixo da sua meta, enviaremos uma Notificação Push ao seu telemóvel.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it.uppercase().take(3) },
                        label = { Text("Origem (IATA)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alert_origin_input")
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it.uppercase().take(3) },
                        label = { Text("Destino (IATA)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alert_destination_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    cabins.forEach { (code, label) ->
                        FilterChip(
                            selected = cabin == code,
                            onClick = { cabin = code },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text(
                    text = "Menor tarifa atual nesta rota: ${MarkupEngine.formatEur(currentLowestPriceEur)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = targetPriceInput,
                    onValueChange = { targetPriceInput = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("Preço Alvo Promocional em EUR (€)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_target_price_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedTarget = targetPriceInput.toDoubleOrNull()?.coerceAtLeast(15.0)
                        ?: (currentLowestPriceEur * 0.88)
                    if (origin.length == 3 && destination.length == 3) {
                        onConfirmCreateAlert(
                            origin,
                            destination,
                            cabin,
                            parsedTarget,
                            currentLowestPriceEur.coerceAtLeast(parsedTarget)
                        )
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("confirm_create_price_alert_button")
            ) {
                Text("Ativar Alerta Push", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
