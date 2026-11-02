package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FlightLand
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.CountryPassportCatalog
import com.example.data.model.PassportTravelStamp
import com.example.data.model.PlaceAutocompleteCatalog
import com.example.data.model.StampInkStyle
import java.util.UUID

@Composable
fun OfficialPassportIdPageCard(
    holderFullName: String,
    passportNumber: String,
    nationality: String,
    birthDate: String,
    expiryDate: String,
    memberTierTitle: String,
    profilePhotoUri: String?,
    totalStampsCount: Int,
    uniqueCountriesCount: Int,
    onChangeProfilePhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanSurname = holderFullName.trim().split(" ").lastOrNull()?.uppercase()?.ifBlank { "TITULAR" } ?: "TITULAR"
    val cleanGiven = holderFullName.trim().split(" ").dropLast(1).joinToString("<").uppercase().ifBlank { "AGUIAFLY" }
    val cleanDoc = passportNumber.trim().uppercase().replace(" ", "")
    val mrzDoc = cleanDoc.ifBlank { "<<<<<<<<<" }
    val isoCountry = CountryPassportCatalog.mrzCountryCode(nationality)
    val mrzLine1 = "P<${isoCountry}${cleanSurname}<<${cleanGiven}<<<<<<<<<<<<<<<<<<<<<<<<<<<<".take(44).padEnd(44, '<')
    val mrzLine2 = "${mrzDoc.padEnd(9, '<')}8${isoCountry}0000000M0000000AGUIAFLYNDC<<<<06".take(44).padEnd(44, '<')

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1426)),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFFE5B84B), Color(0xFF8C6920), Color(0xFFE5B84B))
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Filigrana de segurança estilo passaporte no fundo
            Canvas(modifier = Modifier.matchParentSize()) {
                val goldStroke = Color(0xFFE5B84B).copy(alpha = 0.06f)
                for (r in 40..280 step 35) {
                    drawCircle(
                        color = goldStroke,
                        radius = r.dp.toPx(),
                        center = Offset(size.width * 0.82f, size.height * 0.42f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Topo dourado do Passaporte
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aguiafly_official_logo),
                            contentDescription = "Brasão AguiaFly",
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = "PASSAPORTE OFICIAL • AGUIAFLY NDC",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE5B84B),
                                letterSpacing = 1.1.sp
                            )
                            Text(
                                text = memberTierTitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBCCCE4),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = Color(0xFFE5B84B).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.border(
                            1.dp,
                            Color(0xFFE5B84B).copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                    ) {
                        Text(
                            text = "TIPO P • VIP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE5B84B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Linha principal com Foto 3x4 do Passaporte + Dados Oficiais
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF132544))
                            .border(
                                1.5.dp,
                                Color(0xFFE5B84B),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onChangeProfilePhotoClick() }
                            .testTag("passport_id_photo_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!profilePhotoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(profilePhotoUri),
                                contentDescription = "Foto do Passaporte",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFFE5B84B),
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Trocar Foto",
                                    fontSize = 10.sp,
                                    color = Color(0xFFD5E0F2),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PassportFieldRow(
                            label = "TITULAR / HOLDER",
                            value = holderFullName.ifBlank { "VIAJANTE EXECUTIVO AGUIAFLY" }.uppercase()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PassportFieldRow(
                                label = "Nº PASSAPORTE",
                                value = cleanDoc.ifBlank { "NÃO INFORMADO" },
                                modifier = Modifier.weight(1f)
                            )
                            PassportFieldRow(
                                label = "NACIONALIDADE",
                                value = nationality.ifBlank { "NÃO INFORMADO" }.uppercase(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PassportFieldRow(
                                label = "NASCIMENTO",
                                value = birthDate.ifBlank { "NÃO INFORMADO" },
                                modifier = Modifier.weight(1f)
                            )
                            PassportFieldRow(
                                label = "VALIDADE",
                                value = expiryDate.ifBlank { "NÃO INFORMADO" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Estatísticas rápidas de carimbos e países
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF10203B))
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PassportStatItem(value = "$totalStampsCount", label = "Carimbos")
                    PassportStatItem(value = "$uniqueCountriesCount", label = "Países")
                    PassportStatItem(value = "${totalStampsCount * 1650} km", label = "Voados")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Zona MRZ estilo Passaporte Internacional
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF060C18))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = mrzLine1,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF9EB4D8),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = mrzLine2,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF9EB4D8),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun PassportFieldRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8EA5C8),
            letterSpacing = 0.7.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PassportStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFFE5B84B)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFBCCCE4)
        )
    }
}

/**
 * Carimbo oficial de imigração/passaporte com duplo anel pontilhado, ícone do país,
 * código IATA do aeroporto e data carimbada.
 */
@Composable
fun ImmigrationStampBadge(
    stamp: PassportTravelStamp,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val inkColor = Color(stamp.inkStyle.inkHex)
    val bgTint = Color(stamp.inkStyle.bgTintHex)
    val formattedDate = CountryPassportCatalog.formatStampDate(stamp.travelDateIso)
    val boxSize = if (compact) 118.dp else 138.dp

    Box(
        modifier = modifier
            .size(boxSize)
            .rotate(stamp.rotationDegrees),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 4.dp.toPx()
            drawCircle(
                color = bgTint,
                radius = radius
            )
            // Anel externo duplo de tinta de carimbo
            drawCircle(
                color = inkColor,
                radius = radius,
                style = Stroke(width = 2.6.dp.toPx())
            )
            // Anel interno pontilhado de imigração
            drawCircle(
                color = inkColor.copy(alpha = 0.85f),
                radius = radius - 6.dp.toPx(),
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(10.dp)
        ) {
            Text(
                text = "${stamp.countryFlagEmoji} ${stamp.countryName.uppercase()}",
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = inkColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "★ IMMIGRATION ★",
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                color = inkColor.copy(alpha = 0.85f),
                letterSpacing = 0.8.sp
            )
            Text(
                text = stamp.iataCode,
                fontSize = if (compact) 22.sp else 26.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = inkColor
            )
            Text(
                text = stamp.cityName.uppercase(),
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = inkColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = inkColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .padding(top = 2.dp)
                    .border(1.dp, inkColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    color = inkColor,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Cartão em estilo página de vistos do Passaporte (visão Folha de Carimbos).
 */
@Composable
fun PassportVisaPageStampCard(
    stamp: PassportTravelStamp,
    onPickDestinationPhoto: () -> Unit,
    onDeleteManualStamp: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFBF7EE) // Papel moeda creme de página de passaporte
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFD6C7A9),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stamp.countryFlagEmoji} ${stamp.countryName}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (stamp.isFromIssuedTicket) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Bilhete Emitido",
                        tint = Color(0xFF0D9488),
                        modifier = Modifier.size(16.dp)
                    )
                } else if (onDeleteManualStamp != null) {
                    IconButton(
                        onClick = onDeleteManualStamp,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remover carimbo",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ImmigrationStampBadge(stamp = stamp, compact = true)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini foto da viagem na folha do passaporte
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF0F203C), Color(0xFF1E3A5F))
                        )
                    )
                    .clickable { onPickDestinationPhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (!stamp.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = Uri.parse(stamp.photoUri),
                        contentDescription = "Foto de ${stamp.cityName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAPhoto,
                            contentDescription = null,
                            tint = Color(0xFFE5B84B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Adicionar Foto de ${stamp.cityName}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${stamp.flightNumber} • ${stamp.airlineName}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Cartão completo da Linha do Tempo & Galeria com foto grande do destino,
 * carimbo de imigração sobreposto, bandeira do país e detalhes do voo.
 */
@Composable
fun PassportTimelineGalleryCard(
    stamp: PassportTravelStamp,
    onPickDestinationPhoto: () -> Unit,
    onDeleteManualStamp: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Eixo vertical da Linha do Tempo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(34.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stamp.countryFlagEmoji,
                        fontSize = 16.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(210.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            )
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            modifier = Modifier
                .weight(1f)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column {
                // Galeria de foto do destino + Carimbo sobreposto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0B192E), Color(0xFF1F3A60), Color(0xFF102542))
                            )
                        )
                        .clickable { onPickDestinationPhoto() }
                ) {
                    if (!stamp.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(stamp.photoUri),
                            contentDescription = "Foto da viagem para ${stamp.cityName}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC060D1A))
                                    )
                                )
                        )
                    } else {
                        // Postal ilustrado do destino quando ainda não tem foto da galeria
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${stamp.countryFlagEmoji} ${stamp.countryName.uppercase()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE5B84B)
                            )
                            Text(
                                text = stamp.cityName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = Color(0xFFE5B84B),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Toque para colocar sua foto da viagem",
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Carimbo oficial do passaporte no canto direito
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .clip(CircleShape)
                            .background(Color(0xF2FBF7EE))
                    ) {
                        ImmigrationStampBadge(
                            stamp = stamp,
                            compact = true
                        )
                    }
                }

                // Rodapé da memória de viagem
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlightLand,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "${stamp.cityName} (${stamp.iataCode}) • ${stamp.countryName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (!stamp.isFromIssuedTicket && onDeleteManualStamp != null) {
                            IconButton(
                                onClick = onDeleteManualStamp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Excluir viagem",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Text(
                        text = "Data do Carimbo: ${CountryPassportCatalog.formatStampDate(stamp.travelDateIso)} • Voo ${stamp.flightNumber} (${stamp.airlineName})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (stamp.memoryNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stamp.memoryNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddPastTripStampDialog(
    onDismiss: () -> Unit,
    onPersistPickedPhoto: (Uri) -> String?,
    onConfirmStamp: (PassportTravelStamp) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val matchingAirports = remember(searchQuery) {
        PlaceAutocompleteCatalog.searchLocalCatalog(searchQuery, limit = 12)
    }
    var selectedAirport by remember {
        mutableStateOf<AirportPlaceSuggestion>(
            PlaceAutocompleteCatalog.resolveByIata("OPO")
        )
    }
    var travelDateIso by remember { mutableStateOf("2026-12-10") }
    var flightCode by remember { mutableStateOf("AF-VIP") }
    var memoryNote by remember { mutableStateOf("Viagem inesquecível carimbada no Passaporte AguiaFly!") }
    var selectedInk by remember { mutableStateOf(StampInkStyle.NAVY_OFFICIAL) }
    var pickedPhotoUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            pickedPhotoUri = onPersistPickedPhoto(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Carimbar Nova Viagem no Passaporte",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Procurar Destino ou País (ex: Ibiza, Porto, Paris)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips de seleção de destino
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    matchingAirports.forEach { airport ->
                        val flag = CountryPassportCatalog.flagForCountry(airport.countryName)
                        FilterChip(
                            selected = selectedAirport.iataCode == airport.iataCode,
                            onClick = { selectedAirport = airport },
                            label = {
                                Text("$flag ${airport.cityName} (${airport.iataCode})")
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = travelDateIso,
                        onValueChange = { travelDateIso = it },
                        label = { Text("Data (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = flightCode,
                        onValueChange = { flightCode = it },
                        label = { Text("Voo / Cia") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = memoryNote,
                    onValueChange = { memoryNote = it },
                    label = { Text("Memória ou Legenda da Viagem") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Cor da Tinta do Carimbo Oficial:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StampInkStyle.entries.forEach { ink ->
                        FilterChip(
                            selected = selectedInk == ink,
                            onClick = { selectedInk = ink },
                            label = { Text(ink.label) }
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (pickedPhotoUri != null) "Foto Selecionada da Galeria ✓ (Trocar)"
                        else "Escolher Foto da Viagem na Galeria"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val flag = CountryPassportCatalog.flagForCountry(selectedAirport.countryName)
                    val stamp = PassportTravelStamp(
                        id = UUID.randomUUID().toString(),
                        iataCode = selectedAirport.iataCode,
                        cityName = selectedAirport.cityName,
                        countryName = selectedAirport.countryName,
                        countryFlagEmoji = flag,
                        travelDateIso = travelDateIso.ifBlank { "2026-12-10" },
                        flightNumber = flightCode.ifBlank { "AF-VIP" }.uppercase(),
                        airlineName = "Passaporte AguiaFly",
                        cabinClass = "business",
                        photoUri = pickedPhotoUri,
                        memoryNote = memoryNote,
                        isFromIssuedTicket = false,
                        bookingReference = "STAMP",
                        inkStyle = selectedInk,
                        rotationDegrees = -4f
                    )
                    onConfirmStamp(stamp)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Carimbar Passaporte", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
