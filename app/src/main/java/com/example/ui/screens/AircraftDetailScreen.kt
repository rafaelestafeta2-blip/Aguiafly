package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.EnrichedFlightOffer
import com.example.data.model.PlanespottersPhoto
import com.example.domain.aircraft.AircraftSpecsCatalog
import com.example.ui.components.AircraftBlueprintBanner
import com.example.ui.components.InteractiveSeatMapGrid

@Composable
fun AircraftDetailScreen(
    offer: EnrichedFlightOffer,
    photos: List<PlanespottersPhoto>,
    selectedSeat: String,
    onSelectSeat: (String) -> Unit,
    onProceedToCheckout: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val spec = offer.aircraftSpec
    val seatMap = remember(spec, offer.cabinClass) {
        AircraftSpecsCatalog.generateSeatMapForSpec(spec, offer.cabinClass)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("aircraft_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = spec.fullName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${offer.airlineName} ${offer.flightNumber} • Reg: ${spec.sampleRegistration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            AircraftBlueprintBanner(
                spec = spec,
                highlightedSeat = selectedSeat
            )
        }

        if (photos.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Galeria Planespotters (Alta Resolução)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(photos) { photo ->
                            photo.thumbnailLarge?.src?.let { url ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Column(modifier = Modifier.width(260.dp)) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Foto da aeronave ${spec.fullName}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(145.dp)
                                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                        )
                                        Text(
                                            text = "Foto: ${photo.photographer ?: "Planespotters"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ficha Técnica & Conforto a Bordo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AmenitySpecRow(
                        icon = Icons.Default.AirlineSeatFlat,
                        title = "Espaço para as Pernas (Pitch) & Layout",
                        detail = "Business: ${spec.seatPitchBusiness} (${spec.cabinLayoutBusiness}) | Economy: ${spec.seatPitchEconomy} (${spec.cabinLayoutEconomy})"
                    )
                    AmenitySpecRow(
                        icon = Icons.Default.Wifi,
                        title = "Conectividade a Bordo",
                        detail = spec.wifiSpecification
                    )
                    AmenitySpecRow(
                        icon = Icons.Default.Bolt,
                        title = "Energia & Carregamento",
                        detail = spec.powerOutlets
                    )
                    AmenitySpecRow(
                        icon = Icons.Default.Tv,
                        title = "Sistema de Entretenimento (IFE)",
                        detail = spec.ifeDisplay
                    )
                }
            }
        }

        item {
            InteractiveSeatMapGrid(
                seats = seatMap,
                selectedSeat = selectedSeat,
                onSelectSeat = onSelectSeat
            )
        }

        item {
            val isAutoFree = selectedSeat.isBlank() ||
                selectedSeat.equals("AUTO", ignoreCase = true) ||
                selectedSeat.startsWith("AUTO", ignoreCase = true)
            Button(
                onClick = onProceedToCheckout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("proceed_to_checkout_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (isAutoFree) {
                        "Manter Assento Automático (Grátis €0.00) & Ir para Checkout"
                    } else {
                        "Confirmar Assento $selectedSeat & Ir para Checkout"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AmenitySpecRow(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
