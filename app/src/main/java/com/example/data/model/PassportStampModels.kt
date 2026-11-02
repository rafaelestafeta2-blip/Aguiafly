package com.example.data.model

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
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
        val raw = prefs.getString("state_$userId", null) ?: return LocalProfileVisualState()
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
        prefs.edit().putString("state_$userId", json.toString()).apply()
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
}
