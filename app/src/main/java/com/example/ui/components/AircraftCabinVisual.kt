package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import com.example.data.model.AUTO_FREE_SEAT
import com.example.data.model.AircraftTechnicalSpec
import com.example.data.model.SeatOption
import com.example.domain.aircraft.AircraftSpecsCatalog

@Composable
fun AircraftBlueprintBanner(
    spec: AircraftTechnicalSpec,
    highlightedSeat: String = AUTO_FREE_SEAT,
    modifier: Modifier = Modifier
) {
    AircraftXRayVisualizer(
        spec = spec,
        highlightedSeat = highlightedSeat,
        modifier = modifier
    )
}

@Composable
fun InteractiveSeatMapGrid(
    seats: List<SeatOption>,
    selectedSeat: String,
    onSelectSeat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = seats.groupBy { it.row }
    val cyan = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.secondary
    val isAutoFree = selectedSeat.isBlank() ||
        selectedSeat.equals(AUTO_FREE_SEAT, ignoreCase = true) ||
        selectedSeat.startsWith("AUTO", ignoreCase = true)
    val isSelectedWindow = !isAutoFree && AircraftSpecsCatalog.isWindowSeat(selectedSeat)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                cyan.copy(alpha = 0.35f),
                RoundedCornerShape(18.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Escolha de Assento (Opcional)",
            style = MaterialTheme.typography.titleMedium,
            color = cyan
        )

        // Opção Padrão: Assento Automático / Gratuito (€0.00)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isAutoFree) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isAutoFree) 1.5.dp else 1.dp,
                    color = if (isAutoFree) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onSelectSeat(AUTO_FREE_SEAT) }
                .testTag("seat_auto_free_option")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isAutoFree,
                        onClick = { onSelectSeat(AUTO_FREE_SEAT) }
                    )
                    Column {
                        Text(
                            text = "Assento Automático / Gratuito (Padrão)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Atribuído sem custo pela companhia aérea no check-in",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "GRÁTIS (€0.00)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Status do assento atual
        Text(
            text = when {
                isAutoFree -> "Padrão ativo: Assento Automático Gratuito (€0.00). Ou toque abaixo para marcar um assento específico:"
                isSelectedWindow -> "Assento pago selecionado: $selectedSeat • JANELA (Vista Panorâmica)"
                else -> "Assento pago selecionado: $selectedSeat • CORREDOR (Acesso Rápido)"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isAutoFree) MaterialTheme.colorScheme.onSurfaceVariant else cyan,
            fontWeight = FontWeight.SemiBold
        )

        // Guia Superior de Posição: JANELA ESQUERDA | CORREDOR | JANELA DIREITA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🪟 JANELA ESQ",
                style = MaterialTheme.typography.labelLarge,
                color = cyan
            )
            Text(
                text = "🚶 CORREDOR",
                style = MaterialTheme.typography.labelLarge,
                color = amber
            )
            Text(
                text = "🪟 JANELA DIR",
                style = MaterialTheme.typography.labelLarge,
                color = cyan
            )
        }

        rows.forEach { (rowNumber, rowSeats) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.8f))
                )

                Text(
                    text = rowNumber.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(24.dp)
                )

                rowSeats.forEachIndexed { index, seat ->
                    val isSelected = !isAutoFree && seat.designator == selectedSeat
                    val bgColor = when {
                        isSelected -> cyan
                        !seat.isAvailable -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val txtColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        !seat.isAvailable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .border(
                                width = 1.dp,
                                color = when {
                                    isSelected -> Color.White
                                    seat.isWindow -> cyan.copy(alpha = 0.55f)
                                    else -> amber.copy(alpha = 0.35f)
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = seat.isAvailable) {
                                onSelectSeat(seat.designator)
                            }
                            .testTag("seat_button_${seat.designator}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = seat.designator,
                                style = MaterialTheme.typography.labelLarge,
                                color = txtColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (seat.isWindow) "JAN" else "COR",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) txtColor else if (seat.isWindow) cyan else amber
                            )
                        }
                    }

                    if (index == (rowSeats.size / 2) - 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = amber.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "🚶",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(26.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.8f))
                )
            }
        }
    }
}
