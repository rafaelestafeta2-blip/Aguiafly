package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AircraftTechnicalSpec
import com.example.data.model.PlanespottersPhoto
import com.example.data.remote.NetworkClient
import com.example.domain.aircraft.AircraftSpecsCatalog

@Composable
fun TicketAircraftSpecsSection(
    aircraftName: String,
    cabinClass: String,
    seatDesignator: String = "2A",
    modifier: Modifier = Modifier
) {
    val spec: AircraftTechnicalSpec = remember(aircraftName) {
        AircraftSpecsCatalog.resolveSpec(null, aircraftName)
    }
    var photos by remember(spec.icaoCode) { mutableStateOf<List<PlanespottersPhoto>>(emptyList()) }

    LaunchedEffect(spec.icaoCode, spec.sampleRegistration) {
        photos = try {
            val byReg = NetworkClient.planespottersApi.getPhotosByRegistration(spec.sampleRegistration).photos
            if (byReg.isNotEmpty()) byReg else NetworkClient.planespottersApi.getPhotosByAircraftType(spec.icaoCode).photos
        } catch (_: Exception) {
            emptyList()
        }
    }

    val isBusinessOrFirst = cabinClass.lowercase() in listOf("business", "first")
    val pitchDisplay = if (isBusinessOrFirst) spec.seatPitchBusiness else spec.seatPitchEconomy
    val layoutDisplay = if (isBusinessOrFirst) spec.cabinLayoutBusiness else spec.cabinLayoutEconomy

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(14.dp)
            .testTag("ticket_aircraft_specs_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Aeronave Operadora & Experiência de Cabine",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${spec.fullName} • ${spec.bodyType.label} • Reg: ${spec.sampleRegistration}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AircraftBlueprintBanner(
            spec = spec,
            highlightedSeat = seatDesignator
        )

        if (photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Fotos Reais da Aeronave (Planespotters HD)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(photos) { photo ->
                        photo.thumbnailLarge?.src?.let { imageUrl ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(modifier = Modifier.width(210.dp)) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Foto ${spec.fullName}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(115.dp)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    )
                                    Text(
                                        text = "Crédito: ${photo.photographer ?: "Planespotters"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecBadgeItem(
                icon = Icons.Default.AirlineSeatFlat,
                label = "Pitch & Layout (${cabinClass.uppercase()})",
                value = "$pitchDisplay • Configuração $layoutDisplay"
            )
            SpecBadgeItem(
                icon = Icons.Default.Wifi,
                label = "Wi-Fi Satélite a Bordo",
                value = spec.wifiSpecification
            )
            SpecBadgeItem(
                icon = Icons.Default.Bolt,
                label = "Energia no Assento",
                value = spec.powerOutlets
            )
            SpecBadgeItem(
                icon = Icons.Default.Tv,
                label = "Entretenimento (IFE)",
                value = spec.ifeDisplay
            )
            SpecBadgeItem(
                icon = Icons.Default.Speed,
                label = "Conforto Acústico & Pressurização",
                value = "${spec.cabinNoiseDb} dB • Altitude de cabine ${spec.cabinPressureAltitudeFt} ft"
            )
        }
    }
}

@Composable
private fun SpecBadgeItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
