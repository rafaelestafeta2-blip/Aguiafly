package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AircraftTechnicalSpec

/**
 * Cartão Premium de Foto e Identificação Real da Aeronave.
 * Exibe a imagem de alta fidelidade e especificações exteriores (ICAO, Registro, Mach),
 * mantendo o layout limpo e sem botões duplicados de seleção de bancos.
 */
@Composable
fun AircraftXRayVisualizer(
    spec: AircraftTechnicalSpec,
    highlightedSeat: String = "AUTO",
    modifier: Modifier = Modifier
) {
    val champagneGold = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF132D59),
                        Color(0xFF0E2347)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = champagneGold.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp)
            .testTag("aircraft_xray_visualizer"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "FOTO REAL DA AERONAVE • ICAO ${spec.icaoCode}",
                style = MaterialTheme.typography.labelLarge,
                color = champagneGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${spec.fullName} (${spec.bodyType.label})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = spec.exteriorPhotoUrl,
                contentDescription = "Foto real ${spec.fullName}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x66081730),
                                Color(0x1A081730),
                                Color(0xD9081730)
                            )
                        )
                    )
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xD90B1F3F),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .border(1.dp, champagneGold.copy(alpha = 0.7f), RoundedCornerShape(50))
            ) {
                Text(
                    text = "${spec.manufacturer} • Reg ${spec.sampleRegistration} • ${spec.cruiseSpeedMach}",
                    style = MaterialTheme.typography.bodySmall,
                    color = champagneGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = spec.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Alcance: ${spec.rangeKm} km • Altitude da Cabine: ${spec.cabinPressureAltitudeFt} ft",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}
