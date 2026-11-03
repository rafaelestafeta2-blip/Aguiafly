package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyFareSlot
import com.example.data.model.MarkupConfig
import com.example.data.model.PassengerComposition
import com.example.data.model.TripType
import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.pricing.DisplayCurrency
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveFareCalendarSection(
    departureDateIso: String,
    returnDateIso: String,
    tripType: TripType,
    lowestCurrentFareEur: Double,
    onSelectDepartureDate: (String) -> Unit,
    onSelectReturnDate: (String) -> Unit,
    originIata: String = "LIS",
    destinationIata: String = "CDG",
    cabinClass: String = "economy",
    passengers: PassengerComposition = PassengerComposition(),
    markupConfig: MarkupConfig = MarkupConfig(),
    selectedCurrency: DisplayCurrency = DisplayCurrency.EUR,
    modifier: Modifier = Modifier
) {
    var showDepartureDatePicker by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }
    // true = Janela Flexível +/- 3 dias (7 dias da semana); false = Menor Preço do Mês (30 dias)
    var flexiblePlusMinus3DaysMode by rememberSaveable { mutableStateOf(true) }

    // Mantém a data âncora da tira estável quando o utilizador clica num dia dentro da própria tira,
    // evitando que os cartões mudem de posição ou recalculem preços ao clicar!
    var anchorDateIso by rememberSaveable { mutableStateOf(departureDateIso) }

    LaunchedEffect(departureDateIso) {
        val currentAnchor = runCatching { LocalDate.parse(anchorDateIso.take(10)) }.getOrNull()
        val picked = runCatching { LocalDate.parse(departureDateIso.take(10)) }.getOrNull()
        if (currentAnchor == null || picked == null) {
            anchorDateIso = departureDateIso
        } else {
            val diffDays = abs(ChronoUnit.DAYS.between(currentAnchor, picked))
            val maxRange = if (flexiblePlusMinus3DaysMode) 3L else 26L
            if (diffDays > maxRange) {
                anchorDateIso = departureDateIso
            }
        }
    }

    val dailySlots: List<DailyFareSlot> = remember(
        originIata,
        destinationIata,
        anchorDateIso,
        cabinClass,
        tripType,
        passengers,
        markupConfig,
        flexiblePlusMinus3DaysMode
    ) {
        AirportAndLoyaltyCatalog.generateRouteFareCalendar(
            originIata = originIata,
            destinationIata = destinationIata,
            anchorDateIso = anchorDateIso,
            cabinClass = cabinClass,
            tripMultiplier = tripType.multiplier,
            passengerMultiplier = passengers.fareMultiplier,
            markupConfig = markupConfig,
            flexibleModePlusMinus3Days = flexiblePlusMinus3DaysMode,
            totalDaysInMonthMode = 30
        )
    }

    val bestSlotInPeriod = remember(dailySlots) {
        dailySlots.firstOrNull { it.isLowestOfPeriod } ?: dailySlots.minByOrNull { it.estimatedMinEur }
    }

    val gold = MaterialTheme.colorScheme.primary
    val emerald = Color(0xFF10B981)

    if (showDepartureDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(departureDateIso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDepartureDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                            anchorDateIso = picked
                            onSelectDepartureDate(picked)
                        }
                        showDepartureDatePicker = false
                    }
                ) {
                    Text("Confirmar Ida", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepartureDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showReturnDatePicker) {
        val initialMillis = runCatching {
            LocalDate.parse(returnDateIso).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showReturnDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onSelectReturnDate(picked.toString())
                        }
                        showReturnDatePicker = false
                    }
                ) {
                    Text("Confirmar Volta", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Seletores de Data de Ida e Volta
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, gold.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { showDepartureDatePicker = true }
                    .testTag("departure_date_input")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Selecionar data de partida",
                        tint = gold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Data de Ida (±3 dias NDC)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = departureDateIso,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (tripType == TripType.ROUND_TRIP) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .clickable { showReturnDatePicker = true }
                        .testTag("return_date_input")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = "Selecionar data de regresso",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Data de Volta",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = returnDateIso,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Cabeçalho do Calendário de Tarifas + Alternador: "±3 Dias Flexíveis" vs "Menor Preço do Mês"
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendário de Tarifas Flexíveis (${selectedCurrency.code})",
                    style = MaterialTheme.typography.bodySmall,
                    color = gold,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Toque num dia para atualizar voos",
                    style = MaterialTheme.typography.labelSmall,
                    color = emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = flexiblePlusMinus3DaysMode,
                    onClick = { flexiblePlusMinus3DaysMode = true },
                    label = {
                        Text(
                            text = "±3 Dias Flexíveis (Semana)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = gold,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("flexible_3days_mode_chip")
                )

                FilterChip(
                    selected = !flexiblePlusMinus3DaysMode,
                    onClick = {
                        flexiblePlusMinus3DaysMode = false
                        // Calcula o dia mais barato do mês e seleciona automaticamente!
                        val monthlySlots = AirportAndLoyaltyCatalog.generateRouteFareCalendar(
                            originIata = originIata,
                            destinationIata = destinationIata,
                            anchorDateIso = anchorDateIso,
                            cabinClass = cabinClass,
                            tripMultiplier = tripType.multiplier,
                            passengerMultiplier = passengers.fareMultiplier,
                            markupConfig = markupConfig,
                            flexibleModePlusMinus3Days = false,
                            totalDaysInMonthMode = 30
                        )
                        val cheapestInMonth = monthlySlots.firstOrNull { it.isLowestOfPeriod }
                            ?: monthlySlots.minByOrNull { it.estimatedMinEur }
                        if (cheapestInMonth != null) {
                            onSelectDepartureDate(cheapestInMonth.isoDate)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Menor Preço do Mês (30 Dias)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = emerald,
                        selectedLabelColor = Color(0xFF04130D)
                    ),
                    modifier = Modifier.testTag("lowest_month_mode_chip")
                )
            }
        }

        // Carrossel Horizontal de Datas com menor preço disponível de cada dia e badge "Melhor Tarifa"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            dailySlots.forEach { slot ->
                val isSelected = slot.isoDate == departureDateIso
                val isBest = slot.isLowestOfPeriod
                val borderColor = when {
                    isSelected -> gold
                    isBest -> emerald
                    slot.isPromoDay -> emerald.copy(alpha = 0.65f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                }
                val bgColor = when {
                    isSelected -> gold.copy(alpha = 0.24f)
                    isBest -> emerald.copy(alpha = 0.16f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isSelected || isBest) 1.8.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onSelectDepartureDate(slot.isoDate)
                        }
                        .testTag("fare_calendar_day_${slot.isoDate}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // Badge Visual "Melhor Tarifa" no dia mais barato (ou "Selecionado" no dia ativo)
                        if (isBest) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(emerald)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "★ MELHOR TARIFA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF04130D)
                                )
                            }
                        } else if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(gold)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✓ SELECIONADO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0A1426)
                                )
                            }
                        } else if (slot.isPromoDay) {
                            Text(
                                text = "TARIFA BAIXA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = emerald
                            )
                        } else {
                            Spacer(modifier = Modifier.height(13.dp))
                        }

                        Text(
                            text = "${slot.dayOfWeekShort} • ${slot.dayMonthFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) gold else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected || isBest) FontWeight.ExtraBold else FontWeight.Medium
                        )

                        Text(
                            text = selectedCurrency.format(slot.estimatedMinEur),
                            style = MaterialTheme.typography.labelLarge,
                            color = when {
                                isSelected -> gold
                                isBest || slot.isPromoDay -> emerald
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Barra Inteligente de "Melhor Tarifa" encontrada na rota com botão de 1 toque
        if (bestSlotInPeriod != null) {
            val isAlreadyOnBestDay = bestSlotInPeriod.isoDate == departureDateIso
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAlreadyOnBestDay) {
                    emerald.copy(alpha = 0.16f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        1.dp,
                        if (isAlreadyOnBestDay) emerald else gold.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onSelectDepartureDate(bestSlotInPeriod.isoDate)
                    }
                    .testTag("apply_best_fare_day_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isAlreadyOnBestDay) Icons.Default.CheckCircle else Icons.Default.LocalOffer,
                            contentDescription = null,
                            tint = if (isAlreadyOnBestDay) emerald else gold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isAlreadyOnBestDay) {
                                "★ Melhor Tarifa ativa: ${bestSlotInPeriod.dayOfWeekShort}, ${bestSlotInPeriod.dayMonthFormatted} por ${selectedCurrency.format(bestSlotInPeriod.estimatedMinEur)}"
                            } else {
                                "★ Melhor Tarifa em $originIata ➔ $destinationIata: ${bestSlotInPeriod.dayOfWeekShort}, ${bestSlotInPeriod.dayMonthFormatted} por ${selectedCurrency.format(bestSlotInPeriod.estimatedMinEur)}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isAlreadyOnBestDay) {
                        Text(
                            text = "Ver Voos ➔",
                            style = MaterialTheme.typography.labelMedium,
                            color = emerald,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
