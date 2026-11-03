package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.HolidayPackage
import com.example.data.model.PackageHighlightBadge
import com.example.data.model.PackageInclusionItem
import com.example.data.model.PackageItineraryDay
import com.example.data.model.PackagePaymentMode
import com.example.data.model.PackageRouteWaypoint
import com.example.data.model.PaymentCardInput
import com.example.data.model.SavedTravelerProfile
import com.example.domain.pricing.DisplayCurrency
import com.example.ui.components.inclusionIconFor
import kotlin.math.round

private val GoldAccent = Color(0xFFE5C158)
private val DeepNavy = Color(0xFF08142C)
private val EmeraldBadge = Color(0xFF10B981)

/**
 * 1. PAINEL DE GESTÃO MANUAL (ADMIN CMS DIALOG):
 * Permite cadastrar e gerir pacotes de agências parceiras/particulares com:
 * - Título da Experiência, Destino, IATA, Duração (ex: 7 Noites), Preço por Pessoa,
 * - Taxa de Comissão do Aguiafly (% + € fixo), Sinal de Reserva (%), Vagas Disponíveis,
 * - Galeria de Fotos HD, Itinerário Detalhado Dia a Dia e Ícones de Inclusão (Voo, Alojamento, Refeições, Guias, Transfers).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminCmsCreatePackageDialog(
    existingAdminPackages: List<HolidayPackage>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onDeleteExistingPackage: (HolidayPackage) -> Unit,
    onCreatePackage: (
        title: String,
        destinationCity: String,
        destinationIata: String,
        durationNights: Int,
        netPricePerPersonEur: Double,
        aguiaflyCommissionPercent: Double,
        aguiaflyFixedFeeEur: Double,
        depositPercentage: Int,
        availableSpots: Int,
        partnerAgencyName: String,
        badgeType: PackageHighlightBadge,
        selectedInclusions: Set<PackageInclusionItem>,
        galleryPhotoUrls: List<String>,
        itineraryDays: List<PackageItineraryDay>,
        routeWaypoints: List<PackageRouteWaypoint>
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var destinationCity by remember { mutableStateOf("") }
    var destinationIata by remember { mutableStateOf("MLE") }
    var durationNightsText by remember { mutableStateOf("7") }
    var netPriceText by remember { mutableStateOf("1450") }
    var commissionPercentText by remember { mutableStateOf("12.0") }
    var fixedFeeText by remember { mutableStateOf("35.0") }
    var depositPercentText by remember { mutableStateOf("25") }
    var availableSpotsText by remember { mutableStateOf("10") }
    var partnerAgencyName by remember { mutableStateOf("Agência Parceira Oficial AguiaFly") }
    var selectedBadge by remember { mutableStateOf(PackageHighlightBadge.TUDO_INCLUIDO) }
    var selectedInclusions by remember {
        mutableStateOf(
            setOf(
                PackageInclusionItem.FLIGHT,
                PackageInclusionItem.HOTEL,
                PackageInclusionItem.MEALS,
                PackageInclusionItem.GUIDE,
                PackageInclusionItem.TRANSFERS
            )
        )
    }

    var photoUrl1 by remember {
        mutableStateOf("https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=85")
    }
    var photoUrl2 by remember {
        mutableStateOf("https://images.unsplash.com/photo-1573843981267-be1999ff37cd?auto=format&fit=crop&w=1200&q=85")
    }
    var photoUrl3 by remember {
        mutableStateOf("https://images.unsplash.com/photo-1540202404-a2f29016b523?auto=format&fit=crop&w=1200&q=85")
    }

    val itineraryDays = remember {
        mutableStateListOf(
            PackageItineraryDay(
                dayNumber = 1,
                title = "Voo Internacional & Recepção VIP no Destino",
                locationName = "Aeroporto Internacional → Resort 5★",
                description = "Embarque prioritário, recepção exclusiva à chegada com transfer privado e jantar de boas-vindas.",
                includedMealsLabel = "Jantar de Boas-Vindas Incluído",
                highlightTag = "Transfer & Check-in VIP"
            ),
            PackageItineraryDay(
                dayNumber = 2,
                title = "Tour Guiado Exclusivo & Experiência Gastronómica",
                locationName = "Centro Histórico & Costa Panorâmica",
                description = "Dia completo acompanhado por guia especialista privado com ingressos sem fila e almoço panorâmico.",
                includedMealsLabel = "Pequeno-almoço & Almoço",
                highlightTag = "Guia Especialista"
            ),
            PackageItineraryDay(
                dayNumber = 3,
                title = "Passeio de Barco / Natureza & Spa",
                locationName = "Baía Exclusiva",
                description = "Travessia marítima privada, paragem para mergulho e tarde livre no Spa do resort.",
                includedMealsLabel = "All-Inclusive Premium",
                highlightTag = "Passeio Privado"
            )
        )
    }

    var newDayTitle by remember { mutableStateOf("") }
    var newDayDescription by remember { mutableStateOf("") }

    val netVal = netPriceText.toDoubleOrNull() ?: 1450.0
    val commPctVal = commissionPercentText.toDoubleOrNull() ?: 12.0
    val fixedVal = fixedFeeText.toDoubleOrNull() ?: 35.0
    val previewFinalPrice = HolidayPackage.calculateFinalPricePerPerson(netVal, commPctVal, fixedVal)
    val previewCommissionEur = round((previewFinalPrice - netVal).coerceAtLeast(0.0) * 100.0) / 100.0

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GoldAccent,
        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
        focusedLabelColor = GoldAccent,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = DeepNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PAINEL ADMIN CMS • GESTÃO DE PACOTES",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Cadastrar Pacote de Agência / Experiência",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar CMS", tint = Color.White)
                    }
                }

                // Atalhos rápidos de preenchimento para facilitar testes/cadastros de agências
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            title = "Capadócia em Balão VIP & Istambul Imperial 5★"
                            destinationCity = "Istambul & Capadócia, Turquia"
                            destinationIata = "IST"
                            durationNightsText = "7"
                            netPriceText = "1190"
                            commissionPercentText = "14.0"
                            fixedFeeText = "30.0"
                            availableSpotsText = "6"
                            partnerAgencyName = "Anatolia Luxury Cave Suites (Parceiro CMS)"
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Text("Modelo: Capadócia 7N", color = GoldAccent, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            title = "Fernando de Noronha Eco-Luxury & Catamarã Privado"
                            destinationCity = "Fernando de Noronha, Brasil"
                            destinationIata = "REC"
                            durationNightsText = "6"
                            netPriceText = "1580"
                            commissionPercentText = "12.5"
                            fixedFeeText = "35.0"
                            availableSpotsText = "4"
                            partnerAgencyName = "Noronha Emerald Pousadas VIP (CMS)"
                        },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Text("Modelo: Noronha 6N", color = GoldAccent, fontSize = 11.sp)
                    }
                }

                // Campos Principais
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título da Experiência / Pacote") },
                    placeholder = { Text("Ex: Maldivas Overwater 7 Noites All-Inclusive") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cms_input_title"),
                    colors = textFieldColors,
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = destinationCity,
                        onValueChange = { destinationCity = it },
                        label = { Text("Destino (Cidade & País)") },
                        modifier = Modifier
                            .weight(1.6f)
                            .testTag("cms_input_destination"),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = destinationIata,
                        onValueChange = { destinationIata = it.uppercase().take(3) },
                        label = { Text("IATA (3 letras)") },
                        modifier = Modifier.weight(0.8f),
                        colors = textFieldColors,
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = durationNightsText,
                        onValueChange = { durationNightsText = it.filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("Duração (Noites)") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = availableSpotsText,
                        onValueChange = { availableSpotsText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Vagas Disponíveis") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = depositPercentText,
                        onValueChange = { depositPercentText = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Sinal Reserva %") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors,
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = partnerAgencyName,
                    onValueChange = { partnerAgencyName = it },
                    label = { Text("Agência Parceira / Operador Particular") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                // Estrutura Financeira e Taxa de Comissão do Aguiafly
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102246)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PRECIFICAÇÃO & TAXA DE COMISSÃO AGUIAFLY",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = netPriceText,
                                onValueChange = { netPriceText = it },
                                label = { Text("Custo Neto €") },
                                modifier = Modifier.weight(1f),
                                colors = textFieldColors,
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = commissionPercentText,
                                onValueChange = { commissionPercentText = it },
                                label = { Text("Comissão %") },
                                modifier = Modifier.weight(1f),
                                colors = textFieldColors,
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fixedFeeText,
                                onValueChange = { fixedFeeText = it },
                                label = { Text("Taxa Fixa €") },
                                modifier = Modifier.weight(1f),
                                colors = textFieldColors,
                                singleLine = true
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Margem AguiaFly: +€${String.format(java.util.Locale.US, "%.2f", previewCommissionEur)}/pessoa",
                                color = EmeraldBadge,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Preço Final: €${String.format(java.util.Locale.US, "%.2f", previewFinalPrice)}",
                                color = GoldAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Badges de Destaque
                Text(
                    text = "Badge de Destaque na Vitrine:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PackageHighlightBadge.entries.forEach { badge ->
                        FilterChip(
                            selected = selectedBadge == badge,
                            onClick = { selectedBadge = badge },
                            label = { Text(badge.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = DeepNavy
                            )
                        )
                    }
                }

                // Ícones de Inclusão (Voo, Alojamento, Refeições, Guias, Transfers)
                Text(
                    text = "Ícones de Inclusão no Pacote:",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PackageInclusionItem.entries.forEach { item ->
                        val isSelected = selectedInclusions.contains(item)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedInclusions = if (isSelected) {
                                    selectedInclusions - item
                                } else {
                                    selectedInclusions + item
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = inclusionIconFor(item),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = DeepNavy,
                                selectedLeadingIconColor = DeepNavy
                            )
                        )
                    }
                }

                // Galeria de Fotos HD
                Text(
                    text = "Galeria de Fotos HD (Carrossel):",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = photoUrl1,
                    onValueChange = { photoUrl1 = it },
                    label = { Text("Foto Principal HD (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
                OutlinedTextField(
                    value = photoUrl2,
                    onValueChange = { photoUrl2 = it },
                    label = { Text("Foto Secundária 2 HD (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )
                OutlinedTextField(
                    value = photoUrl3,
                    onValueChange = { photoUrl3 = it },
                    label = { Text("Foto Secundária 3 HD (URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    singleLine = true
                )

                // Itinerário Dia a Dia
                Text(
                    text = "Itinerário Detalhado Dia a Dia (${itineraryDays.size} dias configurados):",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                itineraryDays.forEachIndexed { idx, day ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF12254A)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dia ${day.dayNumber}: ${day.title}",
                                    color = GoldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = day.description,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                            }
                            if (itineraryDays.size > 1) {
                                IconButton(onClick = { itineraryDays.removeAt(idx) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Remover dia",
                                        tint = Color(0xFFF87171)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newDayTitle,
                        onValueChange = { newDayTitle = it },
                        label = { Text("Novo Dia: Título") },
                        modifier = Modifier.weight(1f),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newDayDescription,
                        onValueChange = { newDayDescription = it },
                        label = { Text("Descrição do Dia") },
                        modifier = Modifier.weight(1.2f),
                        colors = textFieldColors,
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newDayTitle.isNotBlank()) {
                                val nextNum = itineraryDays.size + 1
                                itineraryDays.add(
                                    PackageItineraryDay(
                                        dayNumber = nextNum,
                                        title = newDayTitle.trim(),
                                        locationName = destinationCity.ifBlank { "Destino" },
                                        description = newDayDescription.trim().ifBlank { "Atividades guiadas e tempo livre no destino." }
                                    )
                                )
                                newDayTitle = ""
                                newDayDescription = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Adicionar dia", tint = GoldAccent)
                    }
                }

                // Lista de pacotes já cadastrados pelo Admin no Firestore
                if (existingAdminPackages.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Text(
                        text = "Pacotes Manuais Cadastrados no Firestore (${existingAdminPackages.size}):",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    existingAdminPackages.forEach { adminPkg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF102040)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${adminPkg.title} (${adminPkg.packageCode})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${adminPkg.availableSpots}/${adminPkg.totalSpots} vagas • €${adminPkg.finalPricePerPersonEur}",
                                        color = EmeraldBadge,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { onDeleteExistingPackage(adminPkg) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Apagar pacote",
                                        tint = Color(0xFFF87171)
                                    )
                                }
                            }
                        }
                    }
                }

                // Botão Publicar no Catálogo
                Button(
                    onClick = {
                        onCreatePackage(
                            title,
                            destinationCity,
                            destinationIata,
                            durationNightsText.toIntOrNull() ?: 7,
                            netVal,
                            commPctVal,
                            fixedVal,
                            depositPercentText.toIntOrNull() ?: 25,
                            availableSpotsText.toIntOrNull() ?: 10,
                            partnerAgencyName,
                            selectedBadge,
                            selectedInclusions,
                            listOf(photoUrl1, photoUrl2, photoUrl3),
                            itineraryDays.toList(),
                            emptyList()
                        )
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("cms_submit_package_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = DeepNavy
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = DeepNavy,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "PUBLICAR PACOTE NO CATÁLOGO AGUIAFLY",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. CHECKOUT E RESERVA DIRETA DE PACOTES VIA STRIPE:
 * - Escolha entre Compra Instantânea (100%) ou Sinal de Reserva (ex: 25%) cobrado via Stripe
 * - Escolha da quantidade de passageiros/vagas (respeitando o inventário disponível)
 * - Transparência financeira total (Tarifa Neta do Parceiro + Comissão AguiaFly)
 * - Atualização imediata do inventário e emissão do código de confirmação da API / Agência Parceira
 */
@Composable
fun PackageDirectStripeCheckoutDialog(
    pkg: HolidayPackage,
    selectedCurrency: DisplayCurrency,
    savedTravelerProfile: SavedTravelerProfile?,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirmStripeBooking: (
        guestsCount: Int,
        paymentMode: PackagePaymentMode,
        travelerFullName: String,
        travelerEmail: String,
        travelerPhone: String,
        cardInput: PaymentCardInput
    ) -> Unit
) {
    var guestsCount by remember { mutableIntStateOf(2.coerceAtMost(pkg.availableSpots.coerceAtLeast(1))) }
    var paymentMode by remember { mutableStateOf(PackagePaymentMode.DEPOSIT_SIGNAL) }

    val defaultName = remember(savedTravelerProfile) {
        if (savedTravelerProfile != null && savedTravelerProfile.firstName.isNotBlank()) {
            "${savedTravelerProfile.firstName} ${savedTravelerProfile.lastName}".trim()
        } else {
            "Rafael Rodrigues"
        }
    }
    var travelerName by remember { mutableStateOf(defaultName) }
    var travelerEmail by remember {
        mutableStateOf(savedTravelerProfile?.email?.ifBlank { "cliente@aguiafly.com" } ?: "cliente@aguiafly.com")
    }
    var travelerPhone by remember {
        mutableStateOf(savedTravelerProfile?.phone?.ifBlank { "+351912345678" } ?: "+351912345678")
    }

    var cardNumber by remember { mutableStateOf("4242 4242 4242 4242") }
    var cardExpiry by remember { mutableStateOf("08/29") }
    var cardCvc by remember { mutableStateOf("842") }

    val totalNetPartnerEur = round(pkg.netPricePerPersonEur * guestsCount * 100.0) / 100.0
    val totalCommissionEur = round(pkg.commissionAmountPerPersonEur * guestsCount * 100.0) / 100.0
    val totalGrossEur = round(pkg.finalPricePerPersonEur * guestsCount * 100.0) / 100.0

    val chargeNowEur = when (paymentMode) {
        PackagePaymentMode.FULL_PAYMENT -> totalGrossEur
        PackagePaymentMode.DEPOSIT_SIGNAL -> round(totalGrossEur * (pkg.depositPercentage / 100.0) * 100.0) / 100.0
    }
    val remainingBalanceEur = round((totalGrossEur - chargeNowEur).coerceAtLeast(0.0) * 100.0) / 100.0

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = GoldAccent,
        unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
        focusedLabelColor = GoldAccent,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 18.dp),
            shape = RoundedCornerShape(24.dp),
            color = DeepNavy,
            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CHECKOUT DIRETO STRIPE • PACOTES & EXPERIÊNCIAS",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = pkg.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${pkg.destinationCity} • ${pkg.durationLabel} • ${pkg.sourceEnum.displayLabel}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar checkout", tint = Color.White)
                    }
                }

                // Seletor de Número de Viajantes / Vagas
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102246))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Viajantes / Vagas a Reservar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Inventário atual: ${pkg.availableSpots} vagas disponíveis",
                                color = EmeraldBadge,
                                fontSize = 11.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { if (guestsCount > 1) guestsCount-- },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("-", color = GoldAccent, fontWeight = FontWeight.Black)
                            }
                            Text(
                                text = "$guestsCount pax",
                                color = GoldAccent,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                            OutlinedButton(
                                onClick = { if (guestsCount < pkg.availableSpots) guestsCount++ },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text("+", color = GoldAccent, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                // Modalidade de Pagamento Stripe: Sinal de Reserva vs Compra Instantânea 100%
                Text(
                    text = "Escolha a Modalidade de Pagamento Stripe:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isSignal = paymentMode == PackagePaymentMode.DEPOSIT_SIGNAL
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { paymentMode = PackagePaymentMode.DEPOSIT_SIGNAL },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSignal) GoldAccent else Color(0xFF112347),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Sinal de Reserva (${pkg.depositPercentage}%)",
                                color = if (isSignal) DeepNavy else GoldAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Pagar agora: ${selectedCurrency.format(round(totalGrossEur * (pkg.depositPercentage / 100.0) * 100.0) / 100.0)}",
                                color = if (isSignal) DeepNavy else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Bloqueia as vagas na hora",
                                color = if (isSignal) DeepNavy.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    val isFull = paymentMode == PackagePaymentMode.FULL_PAYMENT
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { paymentMode = PackagePaymentMode.FULL_PAYMENT },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isFull) GoldAccent else Color(0xFF112347),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Compra Instantânea (100%)",
                                color = if (isFull) DeepNavy else GoldAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Total: ${selectedCurrency.format(totalGrossEur)}",
                                color = if (isFull) DeepNavy else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Voucher 100% quitado",
                                color = if (isFull) DeepNavy.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.65f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Resumo Financeiro Transparente
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1936)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DEMONSTRATIVO FINANCEIRO TRANSPARENTE",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        FinancialRowItem(
                            label = "Tarifa Base Parceiro (${pkg.partnerAgencyName} x$guestsCount)",
                            value = selectedCurrency.format(totalNetPartnerEur)
                        )
                        FinancialRowItem(
                            label = "Taxa de Intermediação & Garantia AguiaFly (${pkg.aguiaflyCommissionPercent}% + €${pkg.aguiaflyFixedFeeEur})",
                            value = selectedCurrency.format(totalCommissionEur)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                        FinancialRowItem(
                            label = "Valor Total do Pacote ($guestsCount viajantes)",
                            value = selectedCurrency.format(totalGrossEur),
                            isBold = true
                        )
                        FinancialRowItem(
                            label = if (paymentMode == PackagePaymentMode.DEPOSIT_SIGNAL) {
                                "Cobrança Stripe Agora (Sinal ${pkg.depositPercentage}%)"
                            } else {
                                "Cobrança Stripe Agora (100% Quitado)"
                            },
                            value = selectedCurrency.format(chargeNowEur),
                            highlightColor = GoldAccent,
                            isBold = true
                        )
                        if (remainingBalanceEur > 0.0) {
                            FinancialRowItem(
                                label = "Saldo Restante (a pagar até 15 dias antes do embarque)",
                                value = selectedCurrency.format(remainingBalanceEur),
                                highlightColor = EmeraldBadge
                            )
                        }
                    }
                }

                // Dados do Titular da Reserva
                OutlinedTextField(
                    value = travelerName,
                    onValueChange = { travelerName = it },
                    label = { Text("Nome Completo do Viajante Principal") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = travelerEmail,
                        onValueChange = { travelerEmail = it },
                        label = { Text("E-mail para Voucher") },
                        modifier = Modifier.weight(1.3f),
                        colors = fieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = travelerPhone,
                        onValueChange = { travelerPhone = it },
                        label = { Text("WhatsApp / Telefone") },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                }

                // Cartão de Crédito (Stripe PCI-DSS)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Pagamento Seguro Stripe PCI-DSS:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Número do Cartão de Crédito") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = cardExpiry,
                        onValueChange = { cardExpiry = it },
                        label = { Text("Validade (MM/AA)") },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cardCvc,
                        onValueChange = { cardCvc = it },
                        label = { Text("CVC") },
                        modifier = Modifier.weight(1f),
                        colors = fieldColors,
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        onConfirmStripeBooking(
                            guestsCount,
                            paymentMode,
                            travelerName,
                            travelerEmail,
                            travelerPhone,
                            PaymentCardInput(
                                cardNumber = cardNumber,
                                cardholderName = travelerName,
                                expiryMonthYear = cardExpiry,
                                cvc = cardCvc
                            )
                        )
                    },
                    enabled = !isProcessing && pkg.availableSpots >= guestsCount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_package_stripe_checkout_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = DeepNavy
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = DeepNavy,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PAGAR ${selectedCurrency.format(chargeNowEur)} & CONFIRMAR RESERVA",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = EmeraldBadge,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Inventário atualizado em tempo real • Confirmação imediata no parceiro",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialRowItem(
    label: String,
    value: String,
    highlightColor: Color = Color.White,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = if (isBold) 0.95f else 0.72f),
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            color = highlightColor,
            fontSize = if (isBold) 14.sp else 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}
