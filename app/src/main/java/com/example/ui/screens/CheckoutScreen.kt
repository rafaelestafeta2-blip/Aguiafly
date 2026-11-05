package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ANCILLARY_SERVICE_MARGIN_EUR
import com.example.data.model.EnrichedFlightOffer
import com.example.data.model.FrequentFlyerSelection
import com.example.data.model.MarkupConfig
import com.example.data.model.PassengerDocumentInfo
import com.example.data.model.PaymentCardInput
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.data.model.SavedTravelerProfile
import com.example.data.model.SelectedAncillaries
import com.example.domain.pricing.MarkupEngine
import com.example.ui.components.AirlineLogoBadge
import com.example.ui.components.CheckoutFinancialSummaryCard
import com.example.ui.components.FrequentFlyerCheckoutCard
import com.example.ui.components.InteractiveCreditCardSection
import com.example.ui.components.PassengerDocumentSection
import com.example.ui.components.SavedTravelerVaultCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class CheckoutWizardStep(val stepNumber: Int, val shortLabel: String, val topBarTitle: String) {
    TRIP_SUMMARY(1, "1. Resumo", "Resumo da sua viagem"),
    EXTRAS_OPTIONS(2, "2. Opções", "Bagagem, Assentos & Extras"),
    PASSENGER_DATA(3, "3. Passageiro", "Dados do Passageiro"),
    PAYMENT_ISSUE(4, "4. Pagamento", "Pagamento & Emissão")
}

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
    onBack: () -> Unit,
    seatConflictError: String? = null,
    onDismissSeatConflict: () -> Unit = {},
    onResolveSeatConflict: () -> Unit = {}
) {
    var currentStep by rememberSaveable { mutableStateOf(CheckoutWizardStep.TRIP_SUMMARY) }
    var freezePriceEnabled by rememberSaveable { mutableStateOf(false) }

    if (!seatConflictError.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = onDismissSeatConflict,
            icon = {
                Icon(
                    imageVector = Icons.Default.EventBusy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Assento Indisponível (Concorrência)",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = seatConflictError,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissSeatConflict()
                        onResolveSeatConflict()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Escolher Outro Assento")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSeatConflict) {
                    Text("Manter Assento Grátis")
                }
            }
        )
    }

    BackHandler {
        when (currentStep) {
            CheckoutWizardStep.TRIP_SUMMARY -> onBack()
            CheckoutWizardStep.EXTRAS_OPTIONS -> currentStep = CheckoutWizardStep.TRIP_SUMMARY
            CheckoutWizardStep.PASSENGER_DATA -> currentStep = CheckoutWizardStep.EXTRAS_OPTIONS
            CheckoutWizardStep.PAYMENT_ISSUE -> currentStep = CheckoutWizardStep.PASSENGER_DATA
        }
    }

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

    val birthDateAssessment = remember(documentInfo.birthDate, offer.departureIso) {
        com.example.domain.security.PassportSecurityValidator.validateBirthDate(
            birthDateIso = documentInfo.birthDate,
            referenceFlightDateIso = offer.departureIso.take(10)
        )
    }

    val canSaveTravelerVault = firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        email.isNotBlank() &&
        phone.trim().length >= 5 &&
        documentInfo.documentNumber.trim().length >= 4 &&
        birthDateAssessment.canProceedToIssueTicket &&
        passportExpiryAssessment.canProceedToIssueTicket

    val isFormReadyToPay = !isIssuing &&
        canSaveTravelerVault &&
        cardInput.isCompleteAndValid

    val edreamsBlue = Color(0xFF005AA3)
    val crossedOutReferenceEur = remember(breakdown.grandTotalEur) {
        (breakdown.grandTotalEur * 1.188).roundToInt() + 0.47
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (currentStep == CheckoutWizardStep.TRIP_SUMMARY) {
                    Color(0xFFF4F7FB)
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
    ) {
        // 1. Cabeçalho Superior Limpo estilo eDreams ("← Resumo da sua viagem" + Indicador de Etapas Clicável)
        Surface(
            color = Color.White,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            when (currentStep) {
                                CheckoutWizardStep.TRIP_SUMMARY -> onBack()
                                CheckoutWizardStep.EXTRAS_OPTIONS -> currentStep = CheckoutWizardStep.TRIP_SUMMARY
                                CheckoutWizardStep.PASSENGER_DATA -> currentStep = CheckoutWizardStep.EXTRAS_OPTIONS
                                CheckoutWizardStep.PAYMENT_ISSUE -> currentStep = CheckoutWizardStep.PASSENGER_DATA
                            }
                        },
                        modifier = Modifier.testTag("checkout_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = edreamsBlue
                        )
                    }
                    Text(
                        text = currentStep.topBarTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                // Barra de Etapas (1. Resumo ➔ 2. Opções ➔ 3. Passageiro ➔ 4. Pagamento)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CheckoutWizardStep.entries.forEach { step ->
                        val isCurrent = step == currentStep
                        val isCompleted = step.stepNumber < currentStep.stepNumber
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                isCurrent -> edreamsBlue
                                isCompleted -> Color(0xFF159A74).copy(alpha = 0.14f)
                                else -> Color(0xFFF1F5F9)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { currentStep = step }
                                .testTag("checkout_step_tab_${step.stepNumber}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF159A74),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Text(
                                    text = step.shortLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isCurrent -> Color.White
                                        isCompleted -> Color(0xFF159A74)
                                        else -> Color(0xFF64748B)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Conteúdo da Etapa Atual (1 página limpa por vez conforme o cliente vai clicando em Continuar/Next)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentStep,
                label = "checkout_wizard_step_transition"
            ) { step ->
                when (step) {
                    CheckoutWizardStep.TRIP_SUMMARY -> {
                        EdreamsTripSummaryStepContent(
                            offer = offer,
                            freezePriceEnabled = freezePriceEnabled,
                            onToggleFreezePrice = { freezePriceEnabled = !freezePriceEnabled },
                            onInspectAircraft = onInspectAircraftAndSeats
                        )
                    }

                    CheckoutWizardStep.EXTRAS_OPTIONS -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                                                text = "2. Bagagem & Embarque Prioritário",
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
                                                        text = "Tarifa cia ${MarkupEngine.formatEur(bagNetBase)} + Margem serviço ${MarkupEngine.formatEur(ANCILLARY_SERVICE_MARGIN_EUR)}",
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

                            item {
                                com.example.ui.components.AiraloEsimSelectorCard(
                                    destinationIata = offer.destinationIata,
                                    destinationCity = offer.destinationCity,
                                    selectedPackage = ancillaries.selectedEsimPackage,
                                    onSelectPackage = onSelectAiraloEsim,
                                    isPostBookingConfirmation = false
                                )
                            }

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

                            item {
                                com.example.ui.components.DestinationExperiencesSectionCard(
                                    destinationIata = offer.destinationIata,
                                    destinationCity = offer.destinationCity,
                                    selectedExperiences = ancillaries.selectedExperiences,
                                    onToggleExperience = onToggleDestinationExperience,
                                    isPostPurchaseConfirmation = false
                                )
                            }
                        }
                    }

                    CheckoutWizardStep.PASSENGER_DATA -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
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
                                            text = "Dados do Passageiro",
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
                                        val iataGivenPreview = com.example.domain.security.PassportSecurityValidator.sanitizeIataGdsName(firstName)
                                        val iataFamilyPreview = com.example.domain.security.PassportSecurityValidator.sanitizeIataGdsName(lastName)
                                        if (iataGivenPreview.isNotBlank() || iataFamilyPreview.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("iata_gds_sanitized_name_badge")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Padrão Estrito IATA/GDS (Sem acentos/cedilhas):",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${iataFamilyPreview.ifBlank { "SOBRENOME" }}/${iataGivenPreview.ifBlank { "NOME" }}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
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

                            item {
                                PassengerDocumentSection(
                                    documentInfo = documentInfo,
                                    onUpdateDocumentInfo = { documentInfo = it },
                                    flightDepartureDateIso = offer.departureIso.take(10)
                                )
                            }

                            item {
                                FrequentFlyerCheckoutCard(
                                    selection = ffSelection,
                                    totalFareEur = breakdown.grandTotalEur,
                                    cabinClass = offer.cabinClass,
                                    onUpdateSelection = { ffSelection = it }
                                )
                            }
                        }
                    }

                    CheckoutWizardStep.PAYMENT_ISSUE -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF065F46).copy(alpha = 0.22f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("checkout_security_shield_banner")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
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
                                                text = "Fluxo Atômico Stripe (capture_method: manual) + Duffel NDC",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6EE7B7)
                                            )
                                        }
                                        Text(
                                            text = "1. Re-valida tarifa/expiração na Duffel (GET /air/offers/:id) • 2. Pré-autoriza cartão na Stripe sem captura imediata • 3. Emite PNR/E-Ticket (POST /air/orders) e captura apenas no sucesso (ou liberta limite na hora via paymentIntents.cancel).",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }
                                }
                            }

                            item {
                                InteractiveCreditCardSection(
                                    cardInput = cardInput,
                                    onUpdateCardInput = { cardInput = it }
                                )
                            }

                            item {
                                CheckoutFinancialSummaryCard(
                                    breakdown = breakdown,
                                    markupConfig = markupConfig,
                                    ancillaries = ancillaries
                                )
                            }

                            item {
                                if (!isFormReadyToPay && !isIssuing) {
                                    Text(
                                        text = "Certifique-se de ter preenchido os dados do passageiro (Etapa 3) e um cartão válido.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Barra Inferior Fixa com Preço Real + Botão "Continuar" / "Emitir"
        Surface(
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format(Locale.GERMANY, "%.2f €", breakdown.grandTotalEur),
                        fontSize = 23.sp,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Preço total por passageiro",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (currentStep != CheckoutWizardStep.PAYMENT_ISSUE) {
                    Button(
                        onClick = {
                            currentStep = when (currentStep) {
                                CheckoutWizardStep.TRIP_SUMMARY -> CheckoutWizardStep.EXTRAS_OPTIONS
                                CheckoutWizardStep.EXTRAS_OPTIONS -> CheckoutWizardStep.PASSENGER_DATA
                                CheckoutWizardStep.PASSENGER_DATA -> CheckoutWizardStep.PAYMENT_ISSUE
                                CheckoutWizardStep.PAYMENT_ISSUE -> CheckoutWizardStep.PAYMENT_ISSUE
                            }
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = edreamsBlue,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .width(180.dp)
                            .testTag("checkout_next_step_button")
                    ) {
                        Text(
                            text = "Continuar",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
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
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = edreamsBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF94A3B8),
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(50.dp)
                            .width(195.dp)
                            .testTag("confirm_stripe_payment_button")
                    ) {
                        if (isIssuing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Confirmar & Emitir",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Etapa 1: "Resumo da sua viagem" — Layout limpo idêntico ao resumo da eDreams (sem banner de congelar preço):
 * - Cartão branco limpo com cabeçalho "Ida" à esquerda e "🕒 7 h 50 min" à direita
 * - Linha do tempo vertical com partida (16:55 • LIS Humberto Delgado, T1 • Lisboa, Portugal),
 *   ícone da companhia aérea (TAP Portugal • TP35), pílulas arredondadas (🕒 7 h 50 min, 💺 Turista, ✈ 339),
 *   chegada (21:45 • FOR Pinto Martins – Fortaleza Internacional • Fortaleza, Brasil) e "Bagagem de mão".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EdreamsTripSummaryStepContent(
    offer: EnrichedFlightOffer,
    freezePriceEnabled: Boolean,
    onToggleFreezePrice: () -> Unit,
    onInspectAircraft: () -> Unit
) {
    val originPlace = remember(offer.originIata) {
        PlaceAutocompleteCatalog.resolveByIata(offer.originIata)
    }
    val destPlace = remember(offer.destinationIata) {
        PlaceAutocompleteCatalog.resolveByIata(offer.destinationIata)
    }

    val formattedShortDate = remember(offer.departureIso) {
        runCatching {
            val d = LocalDate.parse(offer.departureIso.take(10))
            val fmt = DateTimeFormatter.ofPattern("EEE, dd MMM", Locale("pt", "PT"))
            d.format(fmt).replace(".", "")
        }.getOrDefault(offer.departureIso.take(10))
    }

    val cabinLabel = remember(offer.cabinClass) {
        when (offer.cabinClass.lowercase()) {
            "business" -> "Executiva"
            "first" -> "Primeira Classe"
            "premium_economy" -> "Premium Eco"
            else -> "Turista"
        }
    }

    val legs = remember(offer, formattedShortDate) {
        offer.resolvedSegmentLegs(formattedShortDate)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FB)),
        contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Cartão Branco Limpo "Ida" (com suporte completo a Voo Direto e Voos com 1 ou 2 Escalas)
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("edreams_trip_summary_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Cabeçalho "Ida" à esquerda e Duração Total + Badge Direto/Escalas à direita
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Ida",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (offer.isDirectFlight) {
                                    Color(0xFF159A74).copy(alpha = 0.12f)
                                } else {
                                    Color(0xFFF59E0B).copy(alpha = 0.16f)
                                }
                            ) {
                                Text(
                                    text = if (offer.isDirectFlight) {
                                        "Directo"
                                    } else if (offer.effectiveStopsCount == 1) {
                                        "1 escala ${offer.stopoverHubsSummary}".trim()
                                    } else {
                                        "${offer.effectiveStopsCount} escalas ${offer.stopoverHubsSummary}".trim()
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (offer.isDirectFlight) Color(0xFF159A74) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = offer.durationFormatted,
                                fontSize = 14.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Renderiza cada trecho (perna do voo) e as conexões/escalas intermediárias
                    legs.forEachIndexed { legIdx, leg ->
                        val legOrigPlace = remember(leg.originIata) {
                            PlaceAutocompleteCatalog.resolveByIata(leg.originIata)
                        }
                        val legDestPlace = remember(leg.destinationIata) {
                            PlaceAutocompleteCatalog.resolveByIata(leg.destinationIata)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // COLUNA ESQUERDA: Horário e Data de Partida (topo) e Chegada do Trecho (base)
                            Column(
                                modifier = Modifier
                                    .width(78.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = leg.departureTimeFormatted,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = leg.departureDateFormatted,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = leg.arrivalTimeFormatted,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = leg.arrivalDateFormatted,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // COLUNA CENTRAL: Linha vertical com círculo vazado no topo e círculo preenchido na base
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 14.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .border(2.dp, Color(0xFFCBD5E1), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .weight(1f)
                                        .background(Color(0xFFE2E8F0))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (legIdx < legs.lastIndex) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                                        )
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // COLUNA DIREITA: Aeroporto de Partida do Trecho + Companhia e Pílulas + Aeroporto de Chegada do Trecho
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (legs.size > 1) {
                                        Text(
                                            text = "TRECHO ${legIdx + 1} DE ${legs.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF005AA3)
                                        )
                                    }
                                    Text(
                                        text = "${leg.originIata} (${leg.originAirportName}, T1)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${leg.originCity} (${legOrigPlace.countryName})",
                                        fontSize = 14.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                // Bloco Central: Ícone da Companhia + Nome • Voo + Pílulas
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AirlineLogoBadge(
                                            airlineIata = leg.airlineIata,
                                            airlineName = leg.airlineName,
                                            size = 30.dp
                                        )
                                        Text(
                                            text = leg.airlineName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "•",
                                            fontSize = 15.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = leg.flightNumber,
                                            fontSize = 15.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Pílula 1: Duração do Trecho
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Schedule,
                                                    contentDescription = null,
                                                    tint = Color(0xFF475569),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = leg.durationFormatted,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Pílula 2: Cabine do Segmento
                                        val segCabinLabel = when (leg.cabinClass.lowercase()) {
                                            "business" -> "Executiva"
                                            "first" -> "Primeira Classe"
                                            "premium_economy" -> "Premium Economy"
                                            else -> cabinLabel
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AirlineSeatReclineNormal,
                                                    contentDescription = null,
                                                    tint = Color(0xFF475569),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = segCabinLabel,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        // Pílula 3: Código da Aeronave
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFF1F5F9),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable { onInspectAircraft() }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Flight,
                                                    contentDescription = "Inspecionar aeronave",
                                                    tint = Color(0xFF475569),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = leg.aircraftIataCode,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }

                                // Chegada deste Trecho (Aeroporto de Escala ou Destino Final)
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "${leg.destinationIata} (${leg.destinationAirportName})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = "${leg.destinationCity} (${legDestPlace.countryName})",
                                        fontSize = 14.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // Se houver escala após este trecho, exibe o bloco detalhado de conexão estilo eDreams!
                        if (legIdx < legs.lastIndex && leg.layoverMinutesAfterSegment > 0) {
                            val layoverHours = leg.layoverMinutesAfterSegment / 60
                            val layoverMins = leg.layoverMinutesAfterSegment % 60
                            val layoverFormatted = when {
                                layoverHours > 0 && layoverMins > 0 -> "${layoverHours} h ${layoverMins} min"
                                layoverHours > 0 -> "${layoverHours} h"
                                else -> "${layoverMins} min"
                            }
                            val hasCritical = leg.isCriticalLayover || leg.airportChangeAfterSegment

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (hasCritical) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                                border = BorderStroke(
                                    1.dp,
                                    if (hasCritical) Color(0xFFFECACA) else Color(0xFFFDE68A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("stopover_connection_box_${leg.destinationIata}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (hasCritical) Color(0xFFDC2626) else Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "Escala em ${leg.destinationCity} (${leg.destinationIata}) • Espera de $layoverFormatted",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasCritical) Color(0xFF991B1B) else Color(0xFF92400E)
                                        )
                                        if (leg.airportChangeAfterSegment && leg.nextDepartureAirportIata.isNotBlank()) {
                                            Text(
                                                text = "⚠️ Troca de aeroporto na conexão: desembarque em ${leg.destinationIata} e embarque em ${leg.nextDepartureAirportIata}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB91C1C)
                                            )
                                        } else {
                                            Text(
                                                text = "Conexão no ${leg.destinationAirportName} • Bagagem transferida até ${offer.destinationCity} (${offer.destinationIata})",
                                                fontSize = 12.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                        if (!leg.criticalLayoverAlertMessage.isNullOrBlank()) {
                                            Text(
                                                text = leg.criticalLayoverAlertMessage,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Rodapé interno do cartão: Franquia de Bagagem Separada (Mão vs Despachada)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Bagagem de Mão (Carry-on): ${offer.carryOnBaggageAllowance}",
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Bagagem Despachada (Checked): ${offer.checkedBaggageAllowance}",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

