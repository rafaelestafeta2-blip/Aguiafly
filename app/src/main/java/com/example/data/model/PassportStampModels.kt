package com.example.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

enum class PassportCoverPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val accentHex: Long
) {
    IMPERIAL_NAVY_GOLD(
        id = "IMPERIAL_NAVY_GOLD",
        title = "Passaporte Diplomático AguiaFly",
        subtitle = "Azul Imperial & Brasão Dourado 24K",
        primaryHex = 0xFF061020,
        secondaryHex = 0xFF13294B,
        accentHex = 0xFFE5B84B
    ),
    FIRST_CLASS_SUITE(
        id = "FIRST_CLASS_SUITE",
        title = "Suíte First Class Private",
        subtitle = "Ébano, Champagne & Ouro Escovado",
        primaryHex = 0xFF14110F,
        secondaryHex = 0xFF2B2219,
        accentHex = 0xFFF3C969
    ),
    A350_SUNSET_WING(
        id = "A350_SUNSET_WING",
        title = "Horizonte Airbus A350 Sunset",
        subtitle = "Crepúsculo Atlântico & Dourado Solar",
        primaryHex = 0xFF0D1B38,
        secondaryHex = 0xFF3E1F47,
        accentHex = 0xFFFFB347
    ),
    EMERALD_DIPLOMAT(
        id = "EMERALD_DIPLOMAT",
        title = "Passaporte Europeu Esmeralda",
        subtitle = "Verde Real & Filigrana Dourada",
        primaryHex = 0xFF07211B,
        secondaryHex = 0xFF113E32,
        accentHex = 0xFFE8C15A
    );

    companion object {
        fun fromId(id: String?): PassportCoverPreset =
            entries.firstOrNull { it.id == id } ?: IMPERIAL_NAVY_GOLD
    }
}

enum class StampInkStyle(
    val id: String,
    val label: String,
    val inkHex: Long,
    val bgTintHex: Long
) {
    NAVY_OFFICIAL(
        id = "NAVY_OFFICIAL",
        label = "Azul Imigração Oficial",
        inkHex = 0xFF1D4ED8,
        bgTintHex = 0x1A1D4ED8
    ),
    CRIMSON_CUSTOMS(
        id = "CRIMSON_CUSTOMS",
        label = "Vermelho Alfândega Internacional",
        inkHex = 0xFFDC2626,
        bgTintHex = 0x1ADC2626
    ),
    GOLD_DIPLOMATIC(
        id = "GOLD_DIPLOMATIC",
        label = "Dourado VIP AguiaFly",
        inkHex = 0xFFD97706,
        bgTintHex = 0x1AD97706
    ),
    EMERALD_SCHENGEN(
        id = "EMERALD_SCHENGEN",
        label = "Verde Fronteira Schengen",
        inkHex = 0xFF059669,
        bgTintHex = 0x1A059669
    );

    companion object {
        fun fromId(id: String?): StampInkStyle =
            entries.firstOrNull { it.id == id } ?: NAVY_OFFICIAL
    }
}

const val MAX_PHOTOS_PER_STAMP = 15

data class PassportTravelStamp(
    val id: String,
    val iataCode: String,
    val cityName: String,
    val countryName: String,
    val countryFlagEmoji: String,
    val travelDateIso: String,
    val flightNumber: String,
    val airlineName: String,
    val cabinClass: String,
    val photoUris: List<String> = emptyList(),
    val photoUri: String? = photoUris.firstOrNull(),
    val memoryNote: String = "",
    val isFromIssuedTicket: Boolean = true,
    val bookingReference: String = "",
    val inkStyle: StampInkStyle = StampInkStyle.NAVY_OFFICIAL,
    val rotationDegrees: Float = -4f
)

data class LocalProfileVisualState(
    val profilePhotoUri: String? = null,
    val coverPhotoUri: String? = null,
    val coverPreset: PassportCoverPreset = PassportCoverPreset.IMPERIAL_NAVY_GOLD,
    val nationality: String = "PORTUGAL / BRASIL",
    val memberTierTitle: String = "AguiaFly Executive Gold • Passaporte Oficial",
    val passportCustomNumber: String = "",
    val manualStamps: List<PassportTravelStamp> = emptyList(),
    val ticketPhotoOverrides: Map<String, String> = emptyMap(),
    val ticketPhotoLists: Map<String, List<String>> = emptyMap(),
    val ticketNoteOverrides: Map<String, String> = emptyMap()
)

@IgnoreExtraProperties
data class CloudPassportVisualProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val profilePhotoData: String = "",
    val coverPhotoData: String = "",
    val coverPreset: String = PassportCoverPreset.IMPERIAL_NAVY_GOLD.id,
    val nationality: String = "PORTUGAL / BRASIL",
    val memberTierTitle: String = "AguiaFly Executive Gold • Passaporte Oficial",
    val passportCustomNumber: String = "",
    val stampPhotosJson: String = "{}",
    val stampNotesJson: String = "{}",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object CountryPassportCatalog {

    fun flagForCountry(countryName: String): String {
        val norm = countryName.trim().lowercase()
        return when {
            norm.contains("portugal") -> "🇵🇹"
            norm.contains("brasil") -> "🇧🇷"
            norm.contains("espanha") -> "🇪🇸"
            norm.contains("franc") -> "🇫🇷"
            norm.contains("reino unido") || norm.contains("inglaterra") -> "🇬🇧"
            norm.contains("irlanda") -> "🇮🇪"
            norm.contains("ital") -> "🇮🇹"
            norm.contains("suic") -> "🇨🇭"
            norm.contains("paises baixos") || norm.contains("holanda") -> "🇳🇱"
            norm.contains("belgica") -> "🇧🇪"
            norm.contains("alemanha") -> "🇩🇪"
            norm.contains("austria") -> "🇦🇹"
            norm.contains("grecia") -> "🇬🇷"
            norm.contains("chequia") -> "🇨🇿"
            norm.contains("dinamarca") -> "🇩🇰"
            norm.contains("emirados") || norm.contains("dubai") -> "🇦🇪"
            norm.contains("estados unidos") || norm.contains("eua") -> "🇺🇸"
            else -> "🌍"
        }
    }

    fun mrzCountryCode(nationality: String): String {
        val norm = nationality.trim().lowercase()
        return when {
            norm.contains("port") -> "PRT"
            norm.contains("bras") -> "BRA"
            norm.contains("esp") -> "ESP"
            norm.contains("fran") -> "FRA"
            norm.contains("ital") -> "ITA"
            norm.contains("usa") || norm.contains("estados") -> "USA"
            else -> "PRT"
        }
    }

    fun formatStampDate(isoDate: String): String {
        val clean = isoDate.take(10)
        val parts = clean.split("-")
        if (parts.size != 3) return clean.uppercase()
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull() ?: 1
        val day = parts[2]
        val monthShort = when (monthNum) {
            1 -> "JAN"
            2 -> "FEV"
            3 -> "MAR"
            4 -> "ABR"
            5 -> "MAI"
            6 -> "JUN"
            7 -> "JUL"
            8 -> "AGO"
            9 -> "SET"
            10 -> "OUT"
            11 -> "NOV"
            12 -> "DEZ"
            else -> "DEZ"
        }
        return "$day $monthShort $year"
    }

    /**
     * Gera carimbos oficiais exclusivamente a partir dos bilhetes comprados/emitidos pelo utilizador,
     * impedindo carimbos artificiais e permitindo anexar até 15 fotos em cada carimbo de viagem.
     */
    fun buildCombinedStamps(
        issuedTickets: List<IssuedTicket>,
        visualState: LocalProfileVisualState
    ): List<PassportTravelStamp> {
        return issuedTickets.mapIndexed { idx, ticket ->
            val destPlace = PlaceAutocompleteCatalog.resolveByIata(ticket.destinationIata)
            val country = destPlace.countryName
            val flag = flagForCountry(country)
            val inkStyles = StampInkStyle.entries
            val ink = inkStyles[abs(ticket.destinationIata.hashCode() + idx) % inkStyles.size]
            val angle = when (idx % 4) {
                0 -> -5f
                1 -> 4f
                2 -> -3f
                else -> 5f
            }
            val key = ticket.id.ifBlank { ticket.orderId }
            val multiPhotos = visualState.ticketPhotoLists[key]
                ?.take(MAX_PHOTOS_PER_STAMP)
                ?.takeIf { it.isNotEmpty() }
                ?: listOfNotNull(visualState.ticketPhotoOverrides[key])

            PassportTravelStamp(
                id = "ticket_$key",
                iataCode = ticket.destinationIata.uppercase(),
                cityName = ticket.destinationCity.ifBlank { destPlace.cityName },
                countryName = country,
                countryFlagEmoji = flag,
                travelDateIso = ticket.departureIso.take(10),
                flightNumber = ticket.flightNumber,
                airlineName = ticket.airlineName,
                cabinClass = ticket.cabinClass,
                photoUris = multiPhotos,
                photoUri = multiPhotos.firstOrNull(),
                memoryNote = visualState.ticketNoteOverrides[key]
                    ?: "Carimbo Oficial Automático • Voo ${ticket.originIata} → ${ticket.destinationIata} • PNR ${ticket.bookingReference}",
                isFromIssuedTicket = true,
                bookingReference = ticket.bookingReference,
                inkStyle = ink,
                rotationDegrees = angle
            )
        }.sortedByDescending { it.travelDateIso }
    }
}

object ProfilePassportStorage {
    private const val PREF_NAME = "aguiafly_profile_passport_prefs"

    fun loadState(context: Context, userId: String): LocalProfileVisualState {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val storedRaw = prefs.getString("state_$userId", null) ?: return LocalProfileVisualState()
        val raw = com.example.domain.security.KeystoreVaultCipher.decrypt(storedRaw).ifBlank { storedRaw }
        return try {
            val json = JSONObject(raw)

            val photoOverridesObj = json.optJSONObject("ticketPhotoOverrides") ?: JSONObject()
            val photoMap = mutableMapOf<String, String>()
            photoOverridesObj.keys().forEach { k ->
                val v = photoOverridesObj.optString(k, "")
                if (v.isNotBlank()) photoMap[k] = v
            }

            val photoListsObj = json.optJSONObject("ticketPhotoLists") ?: JSONObject()
            val photoListsMap = mutableMapOf<String, List<String>>()
            photoListsObj.keys().forEach { k ->
                val arr = photoListsObj.optJSONArray(k) ?: JSONArray()
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val uriStr = arr.optString(i, "")
                    if (uriStr.isNotBlank()) list.add(uriStr)
                }
                if (list.isNotEmpty()) {
                    photoListsMap[k] = list.take(MAX_PHOTOS_PER_STAMP)
                }
            }
            // Migra foto única antiga se ainda não estiver na lista de até 15 fotos
            photoMap.forEach { (k, singleUri) ->
                if (photoListsMap[k].isNullOrEmpty() && singleUri.isNotBlank()) {
                    photoListsMap[k] = listOf(singleUri)
                }
            }

            val noteOverridesObj = json.optJSONObject("ticketNoteOverrides") ?: JSONObject()
            val noteMap = mutableMapOf<String, String>()
            noteOverridesObj.keys().forEach { k ->
                val v = noteOverridesObj.optString(k, "")
                if (v.isNotBlank()) noteMap[k] = v
            }

            LocalProfileVisualState(
                profilePhotoUri = json.optString("profilePhotoUri", "").takeIf { it.isNotBlank() },
                coverPhotoUri = json.optString("coverPhotoUri", "").takeIf { it.isNotBlank() },
                coverPreset = PassportCoverPreset.fromId(json.optString("coverPreset")),
                nationality = json.optString("nationality", "PORTUGAL / BRASIL"),
                memberTierTitle = json.optString("memberTierTitle", "AguiaFly Executive Gold • Passaporte Oficial"),
                passportCustomNumber = json.optString("passportCustomNumber", ""),
                manualStamps = emptyList(),
                ticketPhotoOverrides = photoMap,
                ticketPhotoLists = photoListsMap,
                ticketNoteOverrides = noteMap
            )
        } catch (_: Exception) {
            LocalProfileVisualState()
        }
    }

    fun saveState(context: Context, userId: String, state: LocalProfileVisualState) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("profilePhotoUri", state.profilePhotoUri ?: "")
            put("coverPhotoUri", state.coverPhotoUri ?: "")
            put("coverPreset", state.coverPreset.id)
            put("nationality", state.nationality)
            put("memberTierTitle", state.memberTierTitle)
            put("passportCustomNumber", state.passportCustomNumber)

            val photoObj = JSONObject()
            state.ticketPhotoOverrides.forEach { (k, v) -> photoObj.put(k, v) }
            put("ticketPhotoOverrides", photoObj)

            val photoListsObj = JSONObject()
            state.ticketPhotoLists.forEach { (k, list) ->
                val arr = JSONArray()
                list.take(MAX_PHOTOS_PER_STAMP).forEach { uri -> arr.put(uri) }
                photoListsObj.put(k, arr)
            }
            put("ticketPhotoLists", photoListsObj)

            val noteObj = JSONObject()
            state.ticketNoteOverrides.forEach { (k, v) -> noteObj.put(k, v) }
            put("ticketNoteOverrides", noteObj)
        }
        val encryptedPayload = com.example.domain.security.KeystoreVaultCipher.encrypt(json.toString())
        prefs.edit().putString("state_$userId", encryptedPayload).apply()
    }

    /**
     * Copia a foto selecionada via Android Photo Picker para armazenamento interno do app,
     * garantindo que a foto de perfil, capa ou carimbo continue visível permanentemente.
     */
    fun persistPickedImageToInternalStorage(context: Context, sourceUri: Uri, prefix: String): String? {
        return try {
            val dir = File(context.filesDir, "passport_media").apply { mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            sourceUri.toString()
        }
    }

    /**
     * Converte uma imagem local (file:// ou content://) num Data URI JPEG compacto (Base64)
     * para sincronizar no Firebase Firestore e sobreviver à desinstalação/reinstalação do app.
     */
    fun encodeImageUriToCloudDataUri(
        context: Context,
        uriString: String?,
        maxDimension: Int = 340,
        jpegQuality: Int = 62
    ): String {
        if (uriString.isNullOrBlank()) return ""
        if (uriString.startsWith("data:image/") || uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString.take(340_000)
        }
        return try {
            val parsedUri = Uri.parse(uriString)
            val rawBytes = when (parsedUri.scheme) {
                "file" -> parsedUri.path?.let { File(it).takeIf { f -> f.exists() }?.readBytes() }
                else -> context.contentResolver.openInputStream(parsedUri)?.use { it.readBytes() }
            } ?: return ""

            val decoded = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
            val compressedBytes = if (decoded != null) {
                val maxSide = max(decoded.width, decoded.height).coerceAtLeast(1)
                val scaledBitmap = if (maxSide > maxDimension) {
                    val ratio = maxDimension.toFloat() / maxSide.toFloat()
                    val targetW = (decoded.width * ratio).roundToInt().coerceAtLeast(1)
                    val targetH = (decoded.height * ratio).roundToInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                } else {
                    decoded
                }
                val out = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                out.toByteArray()
            } else {
                rawBytes.take(180_000).toByteArray()
            }

            val base64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64".take(340_000)
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Materializa um Data URI Base64 vindo do Firestore num ficheiro físico local em `filesDir/passport_media`
     * para que o Coil (`AsyncImage`) exiba a foto instantaneamente após reinstalar o app.
     */
    fun restoreCloudDataUriToLocalFile(
        context: Context,
        cloudData: String,
        deterministicFileName: String
    ): String? {
        if (cloudData.isBlank()) return null
        if (cloudData.startsWith("http://") || cloudData.startsWith("https://") || cloudData.startsWith("file://")) {
            return cloudData
        }
        return try {
            val commaIdx = cloudData.indexOf(',')
            val base64Part = if (commaIdx >= 0) cloudData.substring(commaIdx + 1) else cloudData
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            if (bytes.isEmpty()) return null
            val dir = File(context.filesDir, "passport_media").apply { mkdirs() }
            val destFile = File(dir, "$deterministicFileName.jpg")
            FileOutputStream(destFile).use { it.write(bytes) }
            Uri.fromFile(destFile).toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Constrói o payload CloudPassportVisualProfile pronto para salvar na coleção `/passport_profiles/{userId}`.
     */
    fun buildCloudVisualProfilePayload(
        context: Context,
        userId: String,
        state: LocalProfileVisualState
    ): CloudPassportVisualProfile {
        val profileData = encodeImageUriToCloudDataUri(context, state.profilePhotoUri, maxDimension = 340, jpegQuality = 64)
        val coverData = encodeImageUriToCloudDataUri(context, state.coverPhotoUri, maxDimension = 420, jpegQuality = 62)

        val stampPhotosObj = JSONObject()
        var currentPhotosBudgetChars = 0
        state.ticketPhotoLists.forEach { (ticketKey, uriList) ->
            val arr = JSONArray()
            uriList.take(MAX_PHOTOS_PER_STAMP).forEach { uriStr ->
                val encoded = encodeImageUriToCloudDataUri(context, uriStr, maxDimension = 220, jpegQuality = 54)
                if (encoded.isNotBlank() && (currentPhotosBudgetChars + encoded.length) < 420_000) {
                    arr.put(encoded)
                    currentPhotosBudgetChars += encoded.length
                }
            }
            if (arr.length() > 0) {
                stampPhotosObj.put(ticketKey, arr)
            }
        }

        val stampNotesObj = JSONObject()
        state.ticketNoteOverrides.forEach { (k, v) ->
            if (v.isNotBlank()) {
                stampNotesObj.put(k, v.take(500))
            }
        }

        return CloudPassportVisualProfile(
            id = userId,
            userId = userId,
            profilePhotoData = profileData.take(350_000),
            coverPhotoData = coverData.take(350_000),
            coverPreset = state.coverPreset.id.take(40),
            nationality = state.nationality.trim().ifBlank { "PORTUGAL / BRASIL" }.take(80),
            memberTierTitle = state.memberTierTitle.trim().ifBlank { "AguiaFly Executive Gold • Passaporte Oficial" }.take(120),
            passportCustomNumber = state.passportCustomNumber.trim().take(40),
            stampPhotosJson = stampPhotosObj.toString().take(450_000),
            stampNotesJson = stampNotesObj.toString().take(50_000)
        )
    }

    /**
     * Restaura do Firestore para o armazenamento local as fotos de perfil, capa, carimbos e configurações
     * quando o utilizador reinstala o app ou entra noutro telemóvel.
     */
    fun mergeAndRestoreCloudVisualProfile(
        context: Context,
        userId: String,
        localState: LocalProfileVisualState,
        cloud: CloudPassportVisualProfile
    ): LocalProfileVisualState {
        val localProfileExists = localState.profilePhotoUri?.let { uriStr ->
            if (uriStr.startsWith("file://")) {
                Uri.parse(uriStr).path?.let { File(it).exists() } ?: false
            } else {
                true
            }
        } ?: false

        val localCoverExists = localState.coverPhotoUri?.let { uriStr ->
            if (uriStr.startsWith("file://")) {
                Uri.parse(uriStr).path?.let { File(it).exists() } ?: false
            } else {
                true
            }
        } ?: false

        val restoredProfileUri = if (localProfileExists) {
            localState.profilePhotoUri
        } else {
            restoreCloudDataUriToLocalFile(context, cloud.profilePhotoData, "cloud_profile_${userId.take(12)}")
        }

        val restoredCoverUri = if (localCoverExists && cloud.coverPhotoData.isNotBlank()) {
            localState.coverPhotoUri
        } else if (cloud.coverPhotoData.isNotBlank()) {
            restoreCloudDataUriToLocalFile(context, cloud.coverPhotoData, "cloud_cover_${userId.take(12)}")
        } else {
            null
        }

        val mergedPhotoLists = localState.ticketPhotoLists.toMutableMap()
        val mergedPhotoOverrides = localState.ticketPhotoOverrides.toMutableMap()
        try {
            val cloudPhotosObj = JSONObject(cloud.stampPhotosJson.ifBlank { "{}" })
            cloudPhotosObj.keys().forEach { ticketKey ->
                val existingList = mergedPhotoLists[ticketKey].orEmpty().filter { uriStr ->
                    if (uriStr.startsWith("file://")) {
                        Uri.parse(uriStr).path?.let { File(it).exists() } ?: false
                    } else {
                        true
                    }
                }
                if (existingList.isEmpty()) {
                    val arr = cloudPhotosObj.optJSONArray(ticketKey) ?: JSONArray()
                    val restoredList = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val rawData = arr.optString(i, "")
                        val localUri = restoreCloudDataUriToLocalFile(
                            context,
                            rawData,
                            "cloud_stamp_${ticketKey.takeLast(8)}_$i"
                        )
                        if (!localUri.isNullOrBlank()) {
                            restoredList.add(localUri)
                        }
                    }
                    if (restoredList.isNotEmpty()) {
                        mergedPhotoLists[ticketKey] = restoredList.take(MAX_PHOTOS_PER_STAMP)
                        mergedPhotoOverrides[ticketKey] = restoredList.first()
                    }
                }
            }
        } catch (_: Exception) {
        }

        val mergedNotes = localState.ticketNoteOverrides.toMutableMap()
        try {
            val cloudNotesObj = JSONObject(cloud.stampNotesJson.ifBlank { "{}" })
            cloudNotesObj.keys().forEach { ticketKey ->
                val note = cloudNotesObj.optString(ticketKey, "")
                if (note.isNotBlank() && mergedNotes[ticketKey].isNullOrBlank()) {
                    mergedNotes[ticketKey] = note
                }
            }
        } catch (_: Exception) {
        }

        val merged = localState.copy(
            profilePhotoUri = restoredProfileUri ?: localState.profilePhotoUri,
            coverPhotoUri = restoredCoverUri,
            coverPreset = PassportCoverPreset.fromId(cloud.coverPreset),
            nationality = cloud.nationality.ifBlank { localState.nationality },
            memberTierTitle = cloud.memberTierTitle.ifBlank { localState.memberTierTitle },
            passportCustomNumber = cloud.passportCustomNumber.ifBlank { localState.passportCustomNumber },
            ticketPhotoLists = mergedPhotoLists,
            ticketPhotoOverrides = mergedPhotoOverrides,
            ticketNoteOverrides = mergedNotes
        )
        saveState(context, userId, merged)
        return merged
    }
}
