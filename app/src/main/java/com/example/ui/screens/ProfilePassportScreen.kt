package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CountryPassportCatalog
import com.example.data.model.IssuedTicket
import com.example.data.model.PassportCoverPreset
import com.example.data.model.ProfilePassportStorage
import com.example.data.model.SavedTravelerProfile
import com.example.domain.pricing.DisplayCurrency
import com.example.ui.components.AddPastTripStampDialog
import com.example.ui.components.OfficialPassportIdPageCard
import com.example.ui.components.PassportTimelineGalleryCard
import com.example.ui.components.PassportVisaPageStampCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

private enum class ProfileSectionTab {
    PASSPORT_GALLERY,
    USER_SETTINGS
}

private enum class PassportGalleryDisplayMode {
    VISA_BOOK_PAGES,
    CHRONOLOGICAL_TIMELINE
}

@Composable
fun ProfilePassportScreen(
    currentUserId: String,
    issuedTickets: List<IssuedTicket>,
    savedTravelerProfile: SavedTravelerProfile?,
    selectedCurrency: DisplayCurrency,
    onSelectCurrency: (DisplayCurrency) -> Unit,
    onSaveTravelerProfile: (SavedTravelerProfile) -> Unit,
    onNavigateToFlights: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    val context = LocalContext.current
    val firebaseUser = remember { Firebase.auth.currentUser }

    var visualState by remember(currentUserId) {
        mutableStateOf(ProfilePassportStorage.loadState(context, currentUserId))
    }

    fun updateAndPersistVisualState(
        transform: (com.example.data.model.LocalProfileVisualState) -> com.example.data.model.LocalProfileVisualState
    ) {
        val updated = transform(visualState)
        visualState = updated
        ProfilePassportStorage.saveState(context, currentUserId, updated)
    }

    // Campos de configuração do usuário (sincronizados exclusivamente com dados reais do Firebase Auth e Firestore)
    var firstName by rememberSaveable {
        mutableStateOf(
            savedTravelerProfile?.firstName?.ifBlank { null }
                ?: firebaseUser?.displayName?.trim()?.split(" ")?.firstOrNull()
                ?: ""
        )
    }
    var lastName by rememberSaveable {
        mutableStateOf(
            savedTravelerProfile?.lastName?.ifBlank { null }
                ?: firebaseUser?.displayName?.trim()?.split(" ")?.drop(1)?.joinToString(" ")
                ?: ""
        )
    }
    var email by rememberSaveable {
        mutableStateOf(
            savedTravelerProfile?.email?.ifBlank { null }
                ?: firebaseUser?.email
                ?: ""
        )
    }
    var phone by rememberSaveable {
        mutableStateOf(savedTravelerProfile?.phone ?: "")
    }
    var documentNumber by rememberSaveable {
        mutableStateOf(
            savedTravelerProfile?.documentNumber?.ifBlank { null }
                ?: visualState.passportCustomNumber
        )
    }
    var birthDate by rememberSaveable {
        mutableStateOf(savedTravelerProfile?.birthDate ?: "")
    }
    var expiryDate by rememberSaveable {
        mutableStateOf(savedTravelerProfile?.expiryDate ?: "")
    }
    var frequentFlyerProgram by rememberSaveable {
        mutableStateOf(savedTravelerProfile?.frequentFlyerProgram ?: "")
    }
    var frequentFlyerNumber by rememberSaveable {
        mutableStateOf(savedTravelerProfile?.frequentFlyerNumber ?: "")
    }
    var autoCheckInEnabled by rememberSaveable {
        mutableStateOf(savedTravelerProfile?.autoCheckInEnabled ?: true)
    }
    var nationalityInput by rememberSaveable {
        mutableStateOf(visualState.nationality)
    }
    var memberTierInput by rememberSaveable {
        mutableStateOf(visualState.memberTierTitle)
    }

    LaunchedEffect(savedTravelerProfile) {
        savedTravelerProfile?.let { prof ->
            if (prof.firstName.isNotBlank()) firstName = prof.firstName
            if (prof.lastName.isNotBlank()) lastName = prof.lastName
            if (prof.email.isNotBlank()) email = prof.email
            if (prof.phone.isNotBlank()) phone = prof.phone
            if (prof.documentNumber.isNotBlank()) documentNumber = prof.documentNumber
            if (prof.birthDate.isNotBlank()) birthDate = prof.birthDate
            if (prof.expiryDate.isNotBlank()) expiryDate = prof.expiryDate
            if (prof.frequentFlyerProgram.isNotBlank()) frequentFlyerProgram = prof.frequentFlyerProgram
            if (prof.frequentFlyerNumber.isNotBlank()) frequentFlyerNumber = prof.frequentFlyerNumber
            autoCheckInEnabled = prof.autoCheckInEnabled
        }
    }

    var activeSectionTab by rememberSaveable { mutableStateOf(ProfileSectionTab.PASSPORT_GALLERY) }
    var galleryDisplayMode by rememberSaveable { mutableStateOf(PassportGalleryDisplayMode.CHRONOLOGICAL_TIMELINE) }
    var showCoverPresetsBar by rememberSaveable { mutableStateOf(false) }
    var stampIdPendingPhoto by remember { mutableStateOf<String?>(null) }
    var saveFeedbackBanner by remember { mutableStateOf<String?>(null) }

    // Launchers Android Photo Picker (Zero-Permission Google Play Compliant)
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedUri = ProfilePassportStorage.persistPickedImageToInternalStorage(
                context = context,
                sourceUri = uri,
                prefix = "profile_$currentUserId"
            )
            updateAndPersistVisualState { it.copy(profilePhotoUri = savedUri) }
        }
    }

    val coverPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedUri = ProfilePassportStorage.persistPickedImageToInternalStorage(
                context = context,
                sourceUri = uri,
                prefix = "cover_$currentUserId"
            )
            updateAndPersistVisualState { it.copy(coverPhotoUri = savedUri) }
        }
    }

    // Seletor múltiplo de até 15 fotos para cada carimbo gerado automaticamente pelo bilhete emitido
    val stampMultiplePhotosPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(com.example.data.model.MAX_PHOTOS_PER_STAMP)
    ) { uris ->
        val targetId = stampIdPendingPhoto
        if (uris.isNotEmpty() && targetId != null && targetId.startsWith("ticket_")) {
            val rawTicketKey = targetId.removePrefix("ticket_")
            val persistedList = uris.mapNotNull { pickedUri ->
                ProfilePassportStorage.persistPickedImageToInternalStorage(
                    context = context,
                    sourceUri = pickedUri,
                    prefix = "stamp_${rawTicketKey.takeLast(8)}"
                )
            }
            if (persistedList.isNotEmpty()) {
                updateAndPersistVisualState { state ->
                    val existing = state.ticketPhotoLists[rawTicketKey]
                        ?: listOfNotNull(state.ticketPhotoOverrides[rawTicketKey])
                    val merged = (existing + persistedList)
                        .distinct()
                        .take(com.example.data.model.MAX_PHOTOS_PER_STAMP)
                    state.copy(
                        ticketPhotoLists = state.ticketPhotoLists + (rawTicketKey to merged),
                        ticketPhotoOverrides = if (merged.isNotEmpty()) {
                            state.ticketPhotoOverrides + (rawTicketKey to merged.first())
                        } else {
                            state.ticketPhotoOverrides
                        }
                    )
                }
            }
        }
        stampIdPendingPhoto = null
    }

    val allStamps = remember(issuedTickets, visualState) {
        CountryPassportCatalog.buildCombinedStamps(issuedTickets, visualState)
    }
    val uniqueCountriesCount = remember(allStamps) {
        allStamps.map { it.countryName.lowercase() }.distinct().size
    }
    val fullNameDisplay = "$firstName $lastName".trim().ifBlank { "Viajante AguiaFly" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("profile_passport_screen"),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Foto de Capa + Foto de Perfil + Identificação VIP
        item {
            ProfileCoverAndAvatarHeader(
                fullName = fullNameDisplay,
                memberTierTitle = visualState.memberTierTitle,
                profilePhotoUri = visualState.profilePhotoUri ?: firebaseUser?.photoUrl?.toString(),
                coverPhotoUri = visualState.coverPhotoUri,
                coverPreset = visualState.coverPreset,
                showCoverPresetsBar = showCoverPresetsBar,
                onPickProfilePhoto = {
                    profilePhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickCoverPhoto = {
                    coverPhotoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onTogglePresetsBar = { showCoverPresetsBar = !showCoverPresetsBar },
                onSelectCoverPreset = { preset ->
                    updateAndPersistVisualState {
                        it.copy(coverPreset = preset, coverPhotoUri = null)
                    }
                }
            )
        }

        // 2. Seletor entre "Passaporte & Galeria de Viagens" e "Configurações do Usuário"
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { activeSectionTab = ProfileSectionTab.PASSPORT_GALLERY },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSectionTab == ProfileSectionTab.PASSPORT_GALLERY) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (activeSectionTab == ProfileSectionTab.PASSPORT_GALLERY) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_passport_gallery_button")
                ) {
                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Passaporte & Viagens", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { activeSectionTab = ProfileSectionTab.USER_SETTINGS },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeSectionTab == ProfileSectionTab.USER_SETTINGS) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (activeSectionTab == ProfileSectionTab.USER_SETTINGS) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_user_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configurações", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (activeSectionTab == ProfileSectionTab.PASSPORT_GALLERY) {
            // 3A. Cartão Oficial de Identificação do Passaporte AguiaFly (com MRZ)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OfficialPassportIdPageCard(
                        holderFullName = fullNameDisplay,
                        passportNumber = documentNumber,
                        nationality = visualState.nationality,
                        birthDate = birthDate,
                        expiryDate = expiryDate,
                        memberTierTitle = visualState.memberTierTitle,
                        profilePhotoUri = visualState.profilePhotoUri ?: firebaseUser?.photoUrl?.toString(),
                        totalStampsCount = allStamps.size,
                        uniqueCountriesCount = uniqueCountriesCount,
                        onChangeProfilePhotoClick = {
                            profilePhotoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }

            // 3B. Cabeçalho de Carimbos Automáticos (100% gerados pela compra de bilhetes) e alternador de visão
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carimbos Oficiais de Bilhetes Emitidos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Gerados automaticamente na compra do bilhete • Adicione até 15 fotos em cada carimbo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Alternador de visualização: Linha do Tempo & Galeria vs Folha de Carimbos do Passaporte
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = galleryDisplayMode == PassportGalleryDisplayMode.CHRONOLOGICAL_TIMELINE,
                            onClick = { galleryDisplayMode = PassportGalleryDisplayMode.CHRONOLOGICAL_TIMELINE },
                            leadingIcon = {
                                Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Linha do Tempo & Galeria") }
                        )
                        FilterChip(
                            selected = galleryDisplayMode == PassportGalleryDisplayMode.VISA_BOOK_PAGES,
                            onClick = { galleryDisplayMode = PassportGalleryDisplayMode.VISA_BOOK_PAGES },
                            leadingIcon = {
                                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Folha de Carimbos") }
                        )
                    }
                }
            }

            if (allStamps.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                            Text(
                                text = "Carimbos Oficiais Automáticos AguiaFly",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Para garantir autenticidade, os carimbos de países no Passaporte são gerados automaticamente sempre que você compra um bilhete no AguiaFly. Após a emissão do bilhete, você pode anexar até 15 fotos reais da viagem no seu carimbo!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = onNavigateToFlights) {
                                Icon(Icons.Default.FlightTakeoff, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pesquisar Voos e Emitir Bilhete")
                            }
                        }
                    }
                }
            } else if (galleryDisplayMode == PassportGalleryDisplayMode.VISA_BOOK_PAGES) {
                // Grade de 2 colunas estilo páginas de vistos de passaporte
                val chunkedStamps = allStamps.chunked(2)
                items(chunkedStamps) { rowPair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowPair.forEach { stamp ->
                            PassportVisaPageStampCard(
                                stamp = stamp,
                                onPickDestinationPhoto = {
                                    stampIdPendingPhoto = stamp.id
                                    stampMultiplePhotosPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onRemovePhotoUri = { uriToRemove ->
                                    val rawTicketKey = stamp.id.removePrefix("ticket_")
                                    updateAndPersistVisualState { state ->
                                        val updatedList = (state.ticketPhotoLists[rawTicketKey] ?: emptyList())
                                            .filterNot { it == uriToRemove }
                                        state.copy(
                                            ticketPhotoLists = state.ticketPhotoLists + (rawTicketKey to updatedList),
                                            ticketPhotoOverrides = if (updatedList.isNotEmpty()) {
                                                state.ticketPhotoOverrides + (rawTicketKey to updatedList.first())
                                            } else {
                                                state.ticketPhotoOverrides - rawTicketKey
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // Visão Linha do Tempo & Galeria Cronológica (com até 15 fotos por carimbo emitido)
                items(allStamps, key = { it.id }) { stamp ->
                    PassportTimelineGalleryCard(
                        stamp = stamp,
                        onPickDestinationPhoto = {
                            stampIdPendingPhoto = stamp.id
                            stampMultiplePhotosPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onRemovePhotoUri = { uriToRemove ->
                            val rawTicketKey = stamp.id.removePrefix("ticket_")
                            updateAndPersistVisualState { state ->
                                val updatedList = (state.ticketPhotoLists[rawTicketKey] ?: emptyList())
                                    .filterNot { it == uriToRemove }
                                state.copy(
                                    ticketPhotoLists = state.ticketPhotoLists + (rawTicketKey to updatedList),
                                    ticketPhotoOverrides = if (updatedList.isNotEmpty()) {
                                        state.ticketPhotoOverrides + (rawTicketKey to updatedList.first())
                                    } else {
                                        state.ticketPhotoOverrides - rawTicketKey
                                    }
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // 4. Painel Completo de Configurações do Usuário, Passaporte e Preferências
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Dados Pessoais & Passaporte Oficial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Estes dados preenchem automaticamente o seu Passaporte Digital e o Cofre de Viajante para compras em 1 toque.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("Nome") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_first_name_input")
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Sobrenome") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_last_name_input")
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = documentNumber,
                                onValueChange = { documentNumber = it.uppercase() },
                                label = { Text("Nº do Passaporte / Doc") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_passport_number_input")
                            )
                            OutlinedTextField(
                                value = nationalityInput,
                                onValueChange = { nationalityInput = it.uppercase() },
                                label = { Text("Nacionalidade") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = birthDate,
                                onValueChange = { birthDate = it },
                                label = { Text("Data de Nascimento") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expiryDate,
                                onValueChange = { expiryDate = it },
                                label = { Text("Validade Passaporte") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = memberTierInput,
                            onValueChange = { memberTierInput = it },
                            label = { Text("Título / Bio no Perfil") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail Oficial") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Telemóvel / WhatsApp") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = frequentFlyerProgram,
                                onValueChange = { frequentFlyerProgram = it },
                                label = { Text("Programa de Milhas") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = frequentFlyerNumber,
                                onValueChange = { frequentFlyerNumber = it },
                                label = { Text("Nº Cartão Milhas") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Moeda Preferida
                        Text(
                            text = "Moeda Preferida de Exibição:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DisplayCurrency.entries.forEach { curr ->
                                FilterChip(
                                    selected = selectedCurrency == curr,
                                    onClick = { onSelectCurrency(curr) },
                                    label = { Text("${curr.symbol} ${curr.code}") }
                                )
                            }
                        }

                        // Switch Check-in Automático
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check-in Automático 24h Antes do Voo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gera automaticamente seu cartão de embarque IATA BCBP nas emissões",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoCheckInEnabled,
                                onCheckedChange = { autoCheckInEnabled = it }
                            )
                        }

                        if (saveFeedbackBanner != null) {
                            val isErrorFeedback = saveFeedbackBanner?.startsWith("Atenção:") == true
                            Surface(
                                color = if (isErrorFeedback) Color(0xFF991B1B) else Color(0xFF065F46),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Text(
                                        text = saveFeedbackBanner.orEmpty(),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val cleanFirst = firstName.trim()
                                val cleanLast = lastName.trim()
                                val cleanEmail = email.trim()
                                val cleanPhone = phone.trim()
                                val cleanDoc = documentNumber.trim().uppercase()

                                if (cleanFirst.isBlank() || cleanLast.isBlank()) {
                                    saveFeedbackBanner = "Atenção: preencha o seu Nome e Sobrenome reais para salvar no Cofre Cloud."
                                    return@Button
                                }
                                if (cleanEmail.length < 3 || !cleanEmail.contains("@")) {
                                    saveFeedbackBanner = "Atenção: informe um E-mail válido para emissão de bilhetes e recibos."
                                    return@Button
                                }
                                if (cleanPhone.length < 5) {
                                    saveFeedbackBanner = "Atenção: informe o seu Telemóvel / WhatsApp real (mínimo 5 dígitos)."
                                    return@Button
                                }
                                if (cleanDoc.length < 4) {
                                    saveFeedbackBanner = "Atenção: informe o Nº do Passaporte ou Documento Oficial (mínimo 4 caracteres)."
                                    return@Button
                                }

                                updateAndPersistVisualState {
                                    it.copy(
                                        nationality = nationalityInput.trim().ifBlank { "PORTUGAL / BRASIL" },
                                        memberTierTitle = memberTierInput.trim().ifBlank { "AguiaFly Executive Gold" },
                                        passportCustomNumber = cleanDoc
                                    )
                                }
                                onSaveTravelerProfile(
                                    SavedTravelerProfile(
                                        userId = currentUserId,
                                        firstName = cleanFirst,
                                        lastName = cleanLast,
                                        email = cleanEmail,
                                        phone = cleanPhone,
                                        documentNumber = cleanDoc,
                                        birthDate = birthDate.trim(),
                                        expiryDate = expiryDate.trim(),
                                        frequentFlyerProgram = frequentFlyerProgram.trim(),
                                        frequentFlyerNumber = frequentFlyerNumber.trim(),
                                        autoCheckInEnabled = autoCheckInEnabled
                                    )
                                )
                                saveFeedbackBanner = "Perfil, Passaporte e Cofre de Viajante sincronizados no Firebase Firestore!"
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_profile_passport_button")
                        ) {
                            Text("Salvar Perfil e Passaporte Digital", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCoverAndAvatarHeader(
    fullName: String,
    memberTierTitle: String,
    profilePhotoUri: String?,
    coverPhotoUri: String?,
    coverPreset: PassportCoverPreset,
    showCoverPresetsBar: Boolean,
    onPickProfilePhoto: () -> Unit,
    onPickCoverPhoto: () -> Unit,
    onTogglePresetsBar: () -> Unit,
    onSelectCoverPreset: (PassportCoverPreset) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(225.dp)
        ) {
            // 1. Foto de Capa (Galeria do telemóvel ou Capa Executiva AguiaFly)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(coverPreset.primaryHex),
                                Color(coverPreset.secondaryHex)
                            )
                        )
                    )
                    .clickable { onPickCoverPhoto() }
            ) {
                if (!coverPhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(coverPhotoUri),
                        contentDescription = "Foto de Capa do Perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0x33000000), Color(0xCC060E1C))
                                )
                            )
                    )
                } else {
                    // Arte vetorial executiva da capa AguiaFly
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val accent = Color(coverPreset.accentHex)
                        drawCircle(
                            color = accent.copy(alpha = 0.14f),
                            radius = size.minDimension * 0.55f,
                            center = Offset(size.width * 0.8f, size.height * 0.25f)
                        )
                        drawCircle(
                            color = accent.copy(alpha = 0.22f),
                            radius = size.minDimension * 0.38f,
                            center = Offset(size.width * 0.8f, size.height * 0.25f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // Botões rápidos no topo direito da Capa
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = Color(0xCC0A1426),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Color(0xFFE5B84B).copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable { onTogglePresetsBar() }
                            .testTag("toggle_cover_presets_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = Color(0xFFE5B84B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Capas VIP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Surface(
                        color = Color(0xCC0A1426),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Color(0xFFE5B84B).copy(alpha = 0.6f), RoundedCornerShape(50))
                            .clickable { onPickCoverPhoto() }
                            .testTag("pick_cover_photo_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = Color(0xFFE5B84B),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Foto de Capa",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // 2. Avatar de Foto de Perfil sobreposto + Nome e Status
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clickable { onPickProfilePhoto() }
                        .testTag("pick_profile_photo_avatar")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF10203B))
                            .border(3.dp, Color(0xFFE5B84B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhotoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(profilePhotoUri),
                                contentDescription = "Foto de Perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFFE5B84B),
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE5B84B),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(30.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Trocar foto de perfil",
                                tint = Color(0xFF0A1426),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = memberTierTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Barra expansível de Capas Executivas AguiaFly
        if (showCoverPresetsBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PassportCoverPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = coverPreset == preset && coverPhotoUri.isNullOrBlank(),
                        onClick = { onSelectCoverPreset(preset) },
                        label = { Text(preset.title) }
                    )
                }
            }
        }
    }
}
