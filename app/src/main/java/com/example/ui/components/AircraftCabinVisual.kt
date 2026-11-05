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
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    val rows = seats.groupBy { it.row }.toSortedMap()
    val cyan = MaterialTheme.colorScheme.primary
    val amber = MaterialTheme.colorScheme.secondary
    val occupiedRed = Color(0xFFEF4444)
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Mapa de Assentos (Narrow-Body 6 Colunas)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cyan
                )
                Text(
                    text = "Layout Europeu: A - B - C  |  Corredor  |  D - E - F",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = cyan
                )
            }
        }

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
                isAutoFree -> "Padrão ativo: Assento Automático Gratuito (€0.00). Ou selecione um lugar específico abaixo:"
                isSelectedWindow -> "Assento selecionado: $selectedSeat • JANELA (Vista Panorâmica)"
                else -> "Assento selecionado: $selectedSeat • CORREDOR / MEIO"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isAutoFree) MaterialTheme.colorScheme.onSurfaceVariant else cyan,
            fontWeight = FontWeight.SemiBold
        )

        // LEGENDA VISUAL OBRIGATÓRIA DOS 3 ESTADOS
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Livre",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(cyan)
                    )
                    Text(
                        text = "Selecionado",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyan,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(occupiedRed)
                    )
                    Text(
                        text = "Ocupado (Bloqueado)",
                        style = MaterialTheme.typography.labelSmall,
                        color = occupiedRed
                    )
                }
            }
        }

        if (isLoading && seats.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = cyan
                    )
                    Text(
                        text = "Sincronizando mapa de assentos exclusivo deste voo...",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = cyan
                    )
                    Text(
                        text = "Consultando disponibilidade de lugares em tempo real na API da Duffel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Guia Superior de Colunas: A - B - C | CORREDOR | D - E - F
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Lado Esquerdo: A (Janela), B (Meio), C (Corredor)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = "A (JAN)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = cyan)
                    Text(text = "B (MEI)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "C (COR)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = amber)
                }

                // Corredor Central
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🚶",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Lado Direito: D (Corredor), E (Meio), F (Janela)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(text = "D (COR)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = amber)
                    Text(text = "E (MEI)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "F (JAN)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = cyan)
                }
            }

            // Renderização Linha por Linha da Matriz (6 Colunas A - B - C | D - E - F)
            rows.forEach { (rowNumber, rowSeats) ->
                // Separa os assentos da fileira entre Lado Esquerdo (A, B, C) e Lado Direito (D, E, F)
                val leftSeats = rowSeats.filter { it.column in listOf("A", "B", "C") }
                .sortedBy { it.column }
            val rightSeats = rowSeats.filter { it.column in listOf("D", "E", "F", "G", "K") }
                .sortedBy { it.column }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicador de fuselagem/janela esquerda
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Banco Esquerdo (Colunas A, B, C) com distribuição proporcional weight(1f)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftSeats.forEach { seat ->
                        SeatItemCell(
                            seat = seat,
                            isSelected = !isAutoFree && seat.designator == selectedSeat,
                            onSelectSeat = onSelectSeat,
                            cyanColor = cyan,
                            amberColor = amber,
                            occupiedRedColor = occupiedRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Corredor Central (Número da Fileira + Ícone)
                Column(
                    modifier = Modifier.width(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = rowNumber.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Banco Direito (Colunas D, E, F) com distribuição proporcional weight(1f)
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rightSeats.forEach { seat ->
                        SeatItemCell(
                            seat = seat,
                            isSelected = !isAutoFree && seat.designator == selectedSeat,
                            onSelectSeat = onSelectSeat,
                            cyanColor = cyan,
                            amberColor = amber,
                            occupiedRedColor = occupiedRed,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Indicador de fuselagem/janela direita
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(cyan.copy(alpha = 0.5f))
                )
            }
        }
    }
}
}

@Composable
private fun SeatItemCell(
    seat: SeatOption,
    isSelected: Boolean,
    onSelectSeat: (String) -> Unit,
    cyanColor: Color,
    amberColor: Color,
    occupiedRedColor: Color,
    modifier: Modifier = Modifier
) {
    val isAvailable = seat.isAvailable
    val bgColor = when {
        isSelected -> cyanColor
        !isAvailable -> Color(0xFF1E293B).copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isSelected -> Color.White
        !isAvailable -> occupiedRedColor.copy(alpha = 0.55f)
        seat.isWindow -> cyanColor.copy(alpha = 0.55f)
        else -> amberColor.copy(alpha = 0.4f)
    }
    val txtColor = when {
        isSelected -> Color.White
        !isAvailable -> occupiedRedColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = isAvailable) {
                onSelectSeat(seat.designator)
            }
            .testTag("seat_button_${seat.designator}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = seat.designator,
                style = MaterialTheme.typography.labelSmall,
                color = txtColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    isSelected -> "✓ SEL"
                    !isAvailable -> "✕ OCP"
                    seat.isWindow -> "JAN"
                    seat.column in listOf("C", "D") -> "COR"
                    else -> "MEI"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isSelected -> Color.White
                    !isAvailable -> occupiedRedColor
                    seat.isWindow -> cyanColor
                    seat.column in listOf("C", "D") -> amberColor
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (isSelected || !isAvailable) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
