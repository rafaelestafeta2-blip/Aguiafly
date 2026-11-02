package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.destination.DestinationCoverCatalog
import com.example.domain.destination.WikipediaDestinationService

@Composable
fun DestinationCoverHeader(
    destinationIata: String,
    destinationCity: String,
    bookingReference: String,
    cabinClass: String,
    bannerHeight: Dp = 215.dp,
    modifier: Modifier = Modifier
) {
    val baseMeta = remember(destinationIata, destinationCity) {
        DestinationCoverCatalog.resolveDestination(destinationIata, destinationCity)
    }
    var dynamicPhotoUrl by remember(destinationIata, destinationCity) {
        mutableStateOf<String?>(null)
    }
    var dynamicDescription by remember(destinationIata, destinationCity) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(destinationIata, destinationCity) {
        if (DestinationCoverCatalog.isDynamicLookupNeeded(destinationIata, destinationCity)) {
            val (wikiPhoto, wikiDesc) = WikipediaDestinationService.fetchCityCoverAndCountry(
                destinationCity.ifBlank { destinationIata }
            )
            if (wikiPhoto != null) dynamicPhotoUrl = wikiPhoto
            if (wikiDesc != null) dynamicDescription = wikiDesc
        }
    }

    val activeCoverUrl = dynamicPhotoUrl ?: baseMeta.coverImageUrl
    val activeTagline = dynamicDescription ?: baseMeta.tagline
    val champagneGold = MaterialTheme.colorScheme.primary
    val aeroCyan = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .testTag("destination_cover_header_${baseMeta.iataCode}")
    ) {
        // 1. Fundo Gradiente Royal Sapphire AguiaFly
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1F3F),
                        Color(0xFF163566),
                        Color(0xFF0E254B)
                    )
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(champagneGold.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.35f),
                    radius = size.width * 0.45f
                ),
                center = Offset(size.width * 0.75f, size.height * 0.35f),
                radius = size.width * 0.45f
            )
        }

        // 2. Foto de Capa Real da Cidade e País de Destino
        AsyncImage(
            model = activeCoverUrl,
            contentDescription = "Foto de capa de ${baseMeta.cityName}, ${baseMeta.countryName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Vinheta Cinematográfica Azul Safira para Legibilidade Perfeita
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x880B1F3F),
                        0.45f to Color(0x220B1F3F),
                        0.82f to Color(0xCC0D2449),
                        1.0f to Color(0xFF132D59)
                    )
                )
        )

        // 4. Selos Flutuantes no Topo: CIDADE • PAÍS + CLASSE DA CABINE (Safira & Dourado Champagne)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xD90B1F3F),
                modifier = Modifier.border(
                    width = 1.dp,
                    color = champagneGold.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = champagneGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${baseMeta.cityName.uppercase()} • ${baseMeta.countryName.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = champagneGold,
                contentColor = Color(0xFF07152B)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cabinClass.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 5. Título Editorial do Destino + País + Lounge VIP no Rodapé da Capa
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Text(
                text = "DESTINO DA VIAGEM • ${baseMeta.iataCode} (${baseMeta.countryName.uppercase()})",
                style = MaterialTheme.typography.labelLarge,
                color = champagneGold,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${baseMeta.cityName}, ${baseMeta.countryName}",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = activeTagline,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFF1F5F9)
            )
            Text(
                text = "Lounge VIP: ${baseMeta.vipLoungeArrival} • Confirmação $bookingReference",
                style = MaterialTheme.typography.bodySmall,
                color = aeroCyan
            )
        }
    }
}
