package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ANCILLARY_SERVICE_MARGIN_EUR
import com.example.data.model.EnrichedFlightOffer
import com.example.data.model.FrequentFlyerSelection
import com.example.data.model.MarkupConfig
import com.example.data.model.PassengerDocumentInfo
import com.example.data.model.PaymentCardInput
import com.example.data.model.SavedTravelerProfile
import com.example.data.model.SelectedAncillaries
import com.example.domain.pricing.MarkupEngine
import com.example.ui.components.CheckoutFinancialSummaryCard
import com.example.ui.components.FrequentFlyerCheckoutCard
import com.example.ui.components.InteractiveCreditCardSection
import com.example.ui.components.PassengerDocumentSection
import com.example.ui.components.SavedTravelerVaultCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun CheckoutScreen(
    offer: EnrichedFlightOffer,
    ancillaries: SelectedAncillaries,
    markupConfig: MarkupConfig,
    isIssuing: Boolean,
    isStripeLive: Boolean,
    savedTravelerProfile: SavedTravelerProfile? = null,
    onSaveTravelerVaultNow: (SavedTravelerProfile) -> Unit = {},
    onDeleteTravelerVault: () -> Unit = {},
    onSelectAutoFreeSeat: () -> Unit,
    onClearAllAncillaries: () -> Unit,
    onToggleExtraBag: (Boolean) -> Unit,
    onTogglePriorityLounge: (Boolean) -> Unit,
    onInspectAircraftAndSeats: () -> Unit,
    onSelectAiraloEsim: (com.example.data.model.AiraloEsimPackage?) -> Unit = {},
    onToggleVipLoungePass: (com.example.data.model.VipLoungeOffer) -> Unit = {},
    onToggleDestinationExperience: (com.example.data.model.DestinationExperienceOffer) -> Unit = {},
    onPayAndIssueTicket: (
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        documentInfo: PassengerDocumentInfo,
        cardInput: PaymentCardInput,
        saveToVault: Boolean,
        frequentFlyerProgram: String,
        frequentFlyerNumber: String
    ) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        val hadFlagSecure = window?.attributes?.flags?.let {
            (it and WindowManager.LayoutParams.FLAG_SECURE) != 0
        } ?: false
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!hadFlagSecure) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val authUser = remember { Firebase.auth.currentUser }
    val nameParts = remember(authUser) {
        (authUser?.displayName ?: "").trim().split(" ").filter { it.isNotBlank() }
    }

    var firstName by remember { mutableStateOf(nameParts.firstOrNull() ?: "") }
    var lastName by remember {
        mutableStateOf(if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else "")
    }
    var email by remember { mutableStateOf(authUser?.email ?: "") }
    var phone by remember { mutableStateOf(authUser?.phoneNumber ?: "") }
    var documentInfo by remember {
        mutableStateOf(
            PassengerDocumentInfo(
                documentNumber = "",
                birthDate = "",
                expiryDate = "",
                autoCheckInEnabled = true
            )
        )
    }
    var ffSelection by remember { mutableStateOf(FrequentFlyerSelection()) }
    var saveToVaultOnCheckout by remember { mutableStateOf(true) }
    var hasAppliedVaultProfile by remember { mutableStateOf(false) }

    // Auto-preenchimento a partir do Cofre de Viajante salvo no Firestore
    LaunchedEffect(savedTravelerProfile) {
        val vault = savedTravelerProfile
        if (vault != null && vault.isPopulated && !hasAppliedVaultProfile) {
            hasAppliedVaultProfile = true
            firstName = vault.firstName
            lastName = vault.lastName
            if (vault.email.isNotBlank()) email = vault.email
            phone = vault.phone
            documentInfo = PassengerDocumentInfo(
                documentNumber = vault.documentNumber,
                birthDate = vault.birthDate,
                expiryDate = vault.expiryDate,
                autoCheckInEnabled = vault.autoCheckInEnabled
            )
            if (vault.frequentFlyerNumber.isNotBlank()) {
                ffSelection = ffSelection.copy(memberNumber = vault.frequentFlyerNumber)
            }
        }
    }

    var cardInput by remember {
        mutableStateOf(
            PaymentCardInput(
                cardholderName = authUser?.displayName?.uppercase() ?: ""
            )
        )
    }

    val breakdown = remember(offer, ancillaries, markupConfig) {
        MarkupEngine.calculateBreakdown(offer.netFareEur, markupConfig, ancillaries)
    }
    val isPaidSeat = ancillaries.isPaidSeatSelected
    val hasAnyAncillary = breakdown.totalAncillariesEur > 0.0
    val seatNetBase = (markupConfig.seatSelectionFeeEur - ANCILLARY_SERVICE_MARGIN_EUR).coerceAtLeast(0.0)
    val bagNetBase = (markupConfig.extraBaggageFeeEur - ANCILLARY_SERVICE_MARGIN_EUR).coerceAtLeast(0.0)

    val passportExpiryAssessment = remember(documentInfo.expiryDate, offer.departureIso) {
        com.example.domain.security.PassportSecurityValidator.validateDocumentExpiry(
            expiryDateIso = documentInfo.expiryDate,
            referenceFlightDateIso = offer.departureIso.take(10)
        )
    }

    val canSaveTravelerVault = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        email.isNotBlank() &&
        phone.trim().length >= 5 &&
        documentInfo.documentNumber.trim().length >= 4 &&
        passportExpiryAssessment.canProceedToIssueTicket

    val isFormReadyToPay = !isIssuing &&
        canSaveTravelerVault &&
        cardInput.isCompleteAndValid

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column {
                        Text(
                            text = "Checkout & Emissão de Passagem",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${offer.airlineName} ${offer.flightNumber} • ${offer.originIata} → ${offer.destinationIata}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Banner de Sessão Blindada (FLAG_SECURE + Luhn + Idempotência)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF065F46).copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("checkout_security_shield_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sessão Blindada PCI-DSS • Print/Gravação de Tela Bloqueados (FLAG_SECURE) • Anti-Duplicidade Ativo",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                }
            }
        }

        // 1. Seleção de Assento (Padrão: Automático / Gratuito €0.00 vs Assento Premium com €2.00 de margem)
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "1. Escolha de Assento (100% Opcional)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (!isPaidSeat) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (!isPaidSeat) 1.5.dp else 1.dp,
                                color = if (!isPaidSeat) MaterialTheme.colorScheme.tertiary else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectAutoFreeSeat() }
                            .testTag("checkout_auto_free_seat_radio")
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
                                    selected = !isPaidSeat,
                                    onClick = onSelectAutoFreeSeat
                                )
                                Column {
                                    Text(
                                        text = "Assento Automático / Gratuito (Padrão)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "A companhia aérea atribui o assento gratuitamente no check-in",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "€0.00",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isPaidSeat) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isPaidSeat) 1.5.dp else 1.dp,
                                color = if (isPaidSeat) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onInspectAircraftAndSeats() }
                            .testTag("open_aircraft_seatmap_button")
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
                                    selected = isPaidSeat,
                                    onClick = onInspectAircraftAndSeats
                                )
                                Column {
                                    Text(
                                        text = if (isPaidSeat) {
                                            "Assento Premium: ${ancillaries.seatDesignator} (${offer.aircraftSpec.fullName})"
                                        } else {
                                            "Escolher Assento Premium (Janela / Corredor)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tarifa cia ${MarkupEngine.formatEur(seatNetBase)} + Margem serviço transparente ${MarkupEngine.formatEur(ANCILLARY_SERVICE_MARGIN_EUR)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = "+${MarkupEngine.formatEur(markupConfig.seatSelectionFeeEur)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. Serviços Opcionais (Ancillaries)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (hasAnyAncillary) {
                            TextButton(
                                onClick = onClearAllAncillaries,
                                modifier = Modifier.testTag("clear_all_ancillaries_button")
                            ) {
                                Text("Desmarcar Tudo")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = ancillaries.includeExtraBaggage23Kg,
                                onCheckedChange = onToggleExtraBag,
                                modifier = Modifier.testTag("extra_bag_checkbox")
                            )
                            Column {
                                Text(
                                    text = "Bagagem Despachada 23kg (Opcional)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Tarifa cia ${MarkupEngine.formatEur(bagNetBase)} + Margem serviço transparente ${MarkupEngine.formatEur(ANCILLARY_SERVICE_MARGIN_EUR)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "+${MarkupEngine.formatEur(markupConfig.extraBaggageFeeEur)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = ancillaries.includePriorityLounge,
                                onCheckedChange = onTogglePriorityLounge,
                                modifier = Modifier.testTag("priority_lounge_checkbox")
                            )
                            Text(
                                text = "Embarque Prioritário & Fast Track (Opcional)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "+${MarkupEngine.formatEur(markupConfig.priorityBoardingFeeEur)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 2.1 INTERNET GLOBAL (Airalo eSIM Integration) com markup automático AguiaFly
        item {
            com.example.ui.components.AiraloEsimSelectorCard(
                destinationIata = offer.destinationIata,
                destinationCity = offer.destinationCity,
                selectedPackage = ancillaries.selectedEsimPackage,
                onSelectPackage = onSelectAiraloEsim,
                isPostBookingConfirmation = false
            )
        }

        // 2.2 ACESSO A SALAS VIP (Priority Pass / DragonPass API) para origem e conexões da rota
        item {
            com.example.ui.components.RouteVipLoungePassCard(
                originIata = offer.originIata,
                destinationIata = offer.destinationIata,
                stopoverHubsSummary = offer.stopoverHubsSummary,
                selectedLounges = ancillaries.selectedVipLounges,
                onToggleLoungePass = onToggleVipLoungePass,
                isConfirmedTripView = false
            )
        }

        // 2.3 EXPERIÊNCIAS & TRANSFERS NO DESTINO (GetYourGuide / Viator API)
        item {
            com.example.ui.components.DestinationExperiencesSectionCard(
                destinationIata = offer.destinationIata,
                destinationCity = offer.destinationCity,
                selectedExperiences = ancillaries.selectedExperiences,
                onToggleExperience = onToggleDestinationExperience,
                isPostPurchaseConfirmation = false
            )
        }

        // 2.5 Cofre de Viajante Salvo (Auto-Preenchimento para Próximas Compras)
        item {
            SavedTravelerVaultCard(
                savedProfile = savedTravelerProfile,
                saveToVaultOnCheckout = saveToVaultOnCheckout,
                onToggleSaveToVault = { saveToVaultOnCheckout = it },
                canSaveNow = canSaveTravelerVault,
                onSaveNowToVault = {
                    onSaveTravelerVaultNow(
                        SavedTravelerProfile(
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            phone = phone,
                            documentNumber = documentInfo.documentNumber,
                            birthDate = documentInfo.birthDate,
                            expiryDate = documentInfo.expiryDate,
                            frequentFlyerProgram = ffSelection.program.displayName,
                            frequentFlyerNumber = ffSelection.memberNumber,
                            autoCheckInEnabled = documentInfo.autoCheckInEnabled
                        )
                    )
                },
                onDeleteVaultProfile = {
                    onDeleteTravelerVault()
                    hasAppliedVaultProfile = false
                }
            )
        }

        // 3. Dados Reais do Passageiro
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "3. Dados do Passageiro",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("Nome (conforme documento)") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("passenger_first_name_input")
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Sobrenome") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("passenger_last_name_input")
                        )
                    }
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail para envio do E-Ticket") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passenger_email_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone Internacional (ex: +5511999999999 ou +351...)") },
                        placeholder = { Text("+5511999999999") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("passenger_phone_input")
                    )
                }
            }
        }

        // 3.5 Documento de Identificação Real (Passaporte / Cartão de Cidadão / RG) & Check-in Automático
        item {
            PassengerDocumentSection(
                documentInfo = documentInfo,
                onUpdateDocumentInfo = { documentInfo = it },
                flightDepartureDateIso = offer.departureIso.take(10)
            )
        }

        // 3.8 Programa de Fidelidade & Passageiro Frequente (Milhas + AguiaFly Royal Club)
        item {
            FrequentFlyerCheckoutCard(
                selection = ffSelection,
                totalFareEur = breakdown.grandTotalEur,
                cabinClass = offer.cabinClass,
                onUpdateSelection = { ffSelection = it }
            )
        }

        // 4. Formulário Interativo de Cartão de Crédito com Detecção Automática de Bandeira
        item {
            InteractiveCreditCardSection(
                cardInput = cardInput,
                onUpdateCardInput = { cardInput = it }
            )
        }

        // 5. Resumo Financeiro Transparente (Preço Base + Taxa de Gestão Unificada €3.50 + 3.5% + Opcionais)
        item {
            CheckoutFinancialSummaryCard(
                breakdown = breakdown,
                markupConfig = markupConfig,
                ancillaries = ancillaries
            )
        }

        // 5.5 Selo de Pagamento Seguro Criptografado
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, Color(0xFF635BFF).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF635BFF),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Pagamento Criptografado PCI-DSS Nível 1",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Seus dados de cartão são protegidos por criptografia bancária de ponta a ponta e emissão imediata de E-Ticket.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isFormReadyToPay && !isIssuing) {
                    Text(
                        text = "Preencha telefone, documento oficial e os dados válidos do cartão para habilitar a emissão.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Button(
                    onClick = {
                        onPayAndIssueTicket(
                            firstName,
                            lastName,
                            email,
                            phone,
                            documentInfo,
                            cardInput,
                            saveToVaultOnCheckout,
                            ffSelection.program.displayName,
                            ffSelection.memberNumber
                        )
                    },
                    enabled = isFormReadyToPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("confirm_stripe_payment_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isIssuing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pagar ${MarkupEngine.formatEur(breakdown.grandTotalEur)} (${cardInput.brand.displayName}) & Emitir E-Ticket",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
