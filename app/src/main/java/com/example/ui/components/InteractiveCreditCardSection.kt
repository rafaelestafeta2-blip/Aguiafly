package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentCardInput

@Composable
fun InteractiveCreditCardSection(
    cardInput: PaymentCardInput,
    onUpdateCardInput: (PaymentCardInput) -> Unit,
    modifier: Modifier = Modifier
) {
    val gold = Color(0xFFFFD54F)
    val deepSapphire = Color(0xFF04142E)
    val royalSapphire = Color(0xFF0A2652)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_credit_card_section")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Cartão de Crédito • Checkout Seguro Stripe",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Detecção automática de bandeira e validação Luhn PCI-DSS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (cardInput.isCompleteAndValid) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Cartão válido",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Cartão Metálico Visual Interativo (Imperial Sapphire & Gold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(deepSapphire, royalSapphire, Color(0xFF10356C))
                        )
                    )
                    .padding(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AGUIAFLY • DIRECT PAY",
                            style = MaterialTheme.typography.labelMedium,
                            color = gold,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(cardInput.brand.badgeColorHex),
                            border = BorderStroke(1.dp, gold.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = cardInput.brand.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = cardInput.formattedCardNumberPreview,
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "TITULAR DO CARTÃO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                            Text(
                                text = cardInput.cardholderName.ifBlank { "NOME NO CARTÃO" }.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = gold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "VALIDADE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp
                            )
                            Text(
                                text = cardInput.expiryMonthYear.ifBlank { "MM/AA" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Campos Reais de Entrada do Cartão
            OutlinedTextField(
                value = cardInput.cardNumber,
                onValueChange = { raw ->
                    val digits = raw.filter { it.isDigit() }.take(16)
                    val formatted = digits.chunked(4).joinToString(" ")
                    onUpdateCardInput(cardInput.copy(cardNumber = formatted))
                },
                label = { Text("Número do Cartão de Crédito") },
                placeholder = { Text("0000 0000 0000 0000") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_number_input")
            )

            OutlinedTextField(
                value = cardInput.cardholderName,
                onValueChange = { onUpdateCardInput(cardInput.copy(cardholderName = it)) },
                label = { Text("Nome Impresso no Cartão") },
                placeholder = { Text("Ex: RAFAEL RODRIGUES") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_holder_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = cardInput.expiryMonthYear,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        val formatted = if (digits.length > 2) {
                            "${digits.take(2)}/${digits.drop(2)}"
                        } else {
                            digits
                        }
                        onUpdateCardInput(cardInput.copy(expiryMonthYear = formatted))
                    },
                    label = { Text("Validade (MM/AA)") },
                    placeholder = { Text("08/29") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_expiry_input")
                )

                OutlinedTextField(
                    value = cardInput.cvc,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(4)
                        onUpdateCardInput(cardInput.copy(cvc = digits))
                    },
                    label = { Text("CVC / CVV") },
                    placeholder = { Text("123") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_cvc_input")
                )
            }

            if (cardInput.cleanDigits.length >= 13) {
                val luhnOk = cardInput.isCardNumberValid
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (luhnOk) Color(0xFF065F46).copy(alpha = 0.25f) else Color(0xFF991B1B).copy(alpha = 0.25f),
                    border = BorderStroke(
                        1.dp,
                        if (luhnOk) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFFEF4444).copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (luhnOk) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (luhnOk) Color(0xFF10B981) else Color(0xFFF87171),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (luhnOk) {
                                "✓ Algoritmo de Luhn (Mod-10) Aprovado • ${cardInput.brand.displayName}"
                            } else {
                                "⚠ Dígito verificador Luhn inválido — confira a numeração do cartão"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (luhnOk) Color(0xFF34D399) else Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            cardInput.validationMessage?.let { warning ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = warning,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
