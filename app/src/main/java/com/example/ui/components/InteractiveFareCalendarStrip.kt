package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventAvailable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyFareSlot
import com.example.data.model.MarkupConfig
import com.example.data.model.PassengerComposition
import com.example.data.model.TripType
import com.example.domain.concierge.AirportAndLoyaltyCatalog
import com.example.domain.pricing.DisplayCurrency
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

private val PORTUGUESE_MONTHS = mapOf(
    1 to "Janeiro",
    2 to "Fevereiro",
    3 to "Março",
    4 to "Abril",
    5 to "Maio",
    6 to "Junho",
    7 to "Julho",
    8 to "Agosto",
    9 to "Setembro",
    10 to "Outubro",
    11 to "Novembro",
    12 to "Dezembro"
)

/**
 * Seletor Limpo de Datas de Viagem:
 * Mantém a tela principal limpa (sem calendário aberto poluindo a tela).
 * O Calendário Mensal com os Dias Mais Baratos só abre quando o cliente clica no campo de data!
 */
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

    val gold = MaterialTheme.colorScheme.primary
    val emerald = Color(0xFF10B981)

    if (showDepartureDatePicker) {
        EdreamsMonthlyPriceCalendarDialog(
            title = "Quando vai viajar?",
            initialSelectedDateIso = departureDateIso,
            originIata = originIata,
            destinationIata = destinationIata,
            cabinClass = cabinClass,
            tripMultiplier = tripType.multiplier,
            passengerMultiplier = passengers.fareMultiplier,
            markupConfig = markupConfig,
            referenceLowestFareEur = lowestCurrentFareEur,
            selectedCurrency = selectedCurrency,
            onDismiss = { showDepartureDatePicker = false },
            onConfirmDate = { picked ->
                showDepartureDatePicker = false
                onSelectDepartureDate(picked)
            }
        )
    }

    if (showReturnDatePicker) {
        EdreamsMonthlyPriceCalendarDialog(
            title = "Quando vai regressar?",
            initialSelectedDateIso = returnDateIso,
            originIata = destinationIata,
            destinationIata = originIata,
            cabinClass = cabinClass,
            tripMultiplier = 1.0,
            passengerMultiplier = passengers.fareMultiplier,
            markupConfig = markupConfig,
            referenceLowestFareEur = lowestCurrentFareEur,
            selectedCurrency = selectedCurrency,
            onDismiss = { showReturnDatePicker = false },
            onConfirmDate = { picked ->
                showReturnDatePicker = false
                onSelectReturnDate(picked)
            }
        )
    }

    // Apenas os campos de data limpos na tela principal; o calendário só abre ao clicar!
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, gold.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                .clickable { showDepartureDatePicker = true }
                .testTag("departure_date_input")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Selecionar data de partida",
                        tint = gold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Data de Ida (Toque para abrir calendário)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                        Text(
                            text = departureDateIso,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = emerald.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = selectedCurrency.format(lowestCurrentFareEur),
                        style = MaterialTheme.typography.labelSmall,
                        color = emerald,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
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
}

/**
 * Modal de tela cheia idêntico à captura de tela "¿Cuándo?" da eDreams:
 * Abre SOMENTE quando o cliente clica no campo de data.
 * Mostra os meses em sequência vertical, preços sob cada dia sincronizados 100% com a busca da rota,
 * dias mais baratos em verde (#159A74), dia selecionado em azul (#005AA3).
 * Ao tocar em qualquer dia, seleciona e aplica imediatamente a nova data de partida/volta.
 */
@Composable
fun EdreamsMonthlyPriceCalendarDialog(
    title: String,
    initialSelectedDateIso: String,
    originIata: String,
    destinationIata: String,
    cabinClass: String,
    tripMultiplier: Double,
    passengerMultiplier: Double,
    markupConfig: MarkupConfig,
    referenceLowestFareEur: Double,
    selectedCurrency: DisplayCurrency,
    onDismiss: () -> Unit,
    onConfirmDate: (String) -> Unit
) {
    var tempSelectedDateIso by remember(initialSelectedDateIso) { mutableStateOf(initialSelectedDateIso) }
    val baseDate = remember(initialSelectedDateIso) {
        runCatching { LocalDate.parse(initialSelectedDateIso.take(10)) }
            .getOrDefault(LocalDate.of(2026, 11, 15))
    }
    val baseYearMonth = remember(baseDate) { YearMonth.of(baseDate.year, baseDate.monthValue) }
    val monthsToRender = remember(baseYearMonth) {
        (0..5).map { offset -> baseYearMonth.plusMonths(offset.toLong()) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar "¿Cuándo? / Quando vai viajar?"
                Surface(
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(32.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Rota $originIata ➔ $destinationIata • Toque num dia para aplicar",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("close_calendar_dialog_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fechar calendário",
                                tint = Color(0xFF005AA3)
                            )
                        }
                    }
                }

                // Lista rolável com os meses (6 meses a partir do mês atual da pesquisa)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC)),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        EdreamsCalendarLegendRow()
                    }

                    monthsToRender.forEach { ym ->
                        item(key = "${ym.year}-${ym.monthValue}") {
                            val slots = remember(
                                ym,
                                originIata,
                                destinationIata,
                                cabinClass,
                                tripMultiplier,
                                passengerMultiplier,
                                referenceLowestFareEur
                            ) {
                                AirportAndLoyaltyCatalog.generateFullMonthFareCalendar(
                                    year = ym.year,
                                    month = ym.monthValue,
                                    originIata = originIata,
                                    destinationIata = destinationIata,
                                    cabinClass = cabinClass,
                                    tripMultiplier = tripMultiplier,
                                    passengerMultiplier = passengerMultiplier,
                                    markupConfig = markupConfig,
                                    anchorDateIso = initialSelectedDateIso,
                                    referenceLowestFareEur = referenceLowestFareEur
                                )
                            }
                            val mName = PORTUGUESE_MONTHS[ym.monthValue] ?: "Mês"
                            val yrShort = (ym.year % 100).toString().padStart(2, '0')

                            Surface(
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "$mName '$yrShort",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1E293B)
                                    )

                                    EdreamsMonthGridContent(
                                        yearMonth = ym,
                                        slots = slots,
                                        selectedDateIso = tempSelectedDateIso,
                                        selectedCurrency = selectedCurrency,
                                        onSelectDay = { pickedIso ->
                                            tempSelectedDateIso = pickedIso
                                            onConfirmDate(pickedIso)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    EdreamsCalendarLegendRow()
                                }
                            }
                        }
                    }
                }

                // Barra inferior fixa com o botão "Continuar" igual à imagem da eDreams
                Surface(
                    color = Color.White,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Data selecionada ($originIata ➔ $destinationIata)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = tempSelectedDateIso,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Button(
                            onClick = { onConfirmDate(tempSelectedDateIso) },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF005AA3),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .width(165.dp)
                                .testTag("confirm_calendar_date_button")
                        ) {
                            Text(
                                text = "Continuar",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EdreamsCalendarLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            modifier = Modifier.border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp))
        ) {
            Text(
                text = "•••",
                fontSize = 10.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Tarifas da rota em tempo real",
            fontSize = 12.sp,
            color = Color(0xFF334155)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF159A74))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Dias mais baratos",
            fontSize = 12.sp,
            color = Color(0xFF1E293B),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun EdreamsMonthGridContent(
    yearMonth: YearMonth,
    slots: List<DailyFareSlot>,
    selectedDateIso: String,
    selectedCurrency: DisplayCurrency,
    onSelectDay: (String) -> Unit
) {
    val weekDays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    val firstDayOfMonth = remember(yearMonth) { LocalDate.of(yearMonth.year, yearMonth.monthValue, 1) }
    val leadingEmptyCells = (firstDayOfMonth.dayOfWeek.value - 1).coerceIn(0, 6)
    val totalCells = leadingEmptyCells + slots.size
    val rowsCount = (totalCells + 6) / 7

    val edreamsBlue = Color(0xFF005AA3)
    val cheapestGreen = Color(0xFF159A74)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEachIndexed { idx, label ->
                val isWeekend = idx >= 5
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (isWeekend) FontWeight.Bold else FontWeight.Normal,
                    color = if (isWeekend) Color(0xFF1E293B) else Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        for (rowIdx in 0 until rowsCount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (colIdx in 0 until 7) {
                    val cellIndex = rowIdx * 7 + colIdx
                    val slotIndex = cellIndex - leadingEmptyCells
                    val slot = slots.getOrNull(slotIndex)

                    if (slot == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )
                    } else {
                        val dayNumber = slotIndex + 1
                        val isSelected = slot.isoDate == selectedDateIso
                        val isCheapDay = slot.isPromoDay || slot.isLowestOfPeriod
                        val isWeekend = colIdx >= 5
                        val compactPrice = formatCompactCalendarPrice(slot.estimatedMinEur, selectedCurrency)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isSelected -> edreamsBlue
                                        isCheapDay -> cheapestGreen.copy(alpha = 0.10f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onSelectDay(slot.isoDate) }
                                .testTag("month_grid_day_${slot.isoDate}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected || isWeekend || isCheapDay) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        else -> Color(0xFF1E293B)
                                    }
                                )
                                Text(
                                    text = compactPrice,
                                    fontSize = 10.sp,
                                    fontWeight = if (isCheapDay || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> Color.White
                                        isCheapDay -> cheapestGreen
                                        else -> Color(0xFF64748B)
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactCalendarPrice(amountEur: Double, currency: DisplayCurrency): String {
    val converted = (amountEur * currency.rateFromEur).roundToInt()
    return when (currency) {
        DisplayCurrency.EUR -> "$converted €"
        DisplayCurrency.BRL -> "R$$converted"
        DisplayCurrency.USD -> "$$converted"
    }
}
