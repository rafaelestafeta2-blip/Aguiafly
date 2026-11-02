package com.example.data.model

import com.squareup.moshi.Json
import java.text.Normalizer

data class DuffelPlacesResponseWrapper(
    @Json(name = "data") val data: List<DuffelPlaceSuggestionDto> = emptyList()
)

data class DuffelPlaceSuggestionDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "type") val type: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "iata_code") val iataCode: String? = null,
    @Json(name = "city_name") val cityName: String? = null,
    @Json(name = "country_name") val countryName: String? = null,
    @Json(name = "city") val city: DuffelPlaceCityDto? = null
)

data class DuffelPlaceCityDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "iata_code") val iataCode: String? = null
)

data class AirportPlaceSuggestion(
    val iataCode: String,
    val cityName: String,
    val airportName: String,
    val countryName: String
) {
    /**
     * Formato exigido no menu suspenso:
     * Ex: "São Paulo - Guarulhos (GRU)" ou "Lisboa - Humberto Delgado (LIS)"
     */
    val dropdownDisplayLabel: String
        get() = "$cityName - $airportName ($iataCode)"
}

object PlaceAutocompleteCatalog {

    val GLOBAL_AIRPORTS: List<AirportPlaceSuggestion> = listOf(
        AirportPlaceSuggestion("GRU", "São Paulo", "Guarulhos", "Brasil"),
        AirportPlaceSuggestion("CGH", "São Paulo", "Congonhas", "Brasil"),
        AirportPlaceSuggestion("VCP", "Campinas / São Paulo", "Viracopos", "Brasil"),
        AirportPlaceSuggestion("GIG", "Rio de Janeiro", "Tom Jobim Galeão", "Brasil"),
        AirportPlaceSuggestion("SDU", "Rio de Janeiro", "Santos Dumont", "Brasil"),
        AirportPlaceSuggestion("BSB", "Brasília", "Presidente Juscelino Kubitschek", "Brasil"),
        AirportPlaceSuggestion("CNF", "Belo Horizonte", "Confins Tancredo Neves", "Brasil"),
        AirportPlaceSuggestion("SSA", "Salvador", "Deputado Luís Eduardo Magalhães", "Brasil"),
        AirportPlaceSuggestion("REC", "Recife", "Guararapes Gilberto Freyre", "Brasil"),
        AirportPlaceSuggestion("FOR", "Fortaleza", "Pinto Martins", "Brasil"),
        AirportPlaceSuggestion("POA", "Porto Alegre", "Salgado Filho", "Brasil"),
        AirportPlaceSuggestion("CWB", "Curitiba", "Afonso Pena", "Brasil"),
        AirportPlaceSuggestion("LIS", "Lisboa", "Humberto Delgado", "Portugal"),
        AirportPlaceSuggestion("OPO", "Porto", "Francisco Sá Carneiro", "Portugal"),
        AirportPlaceSuggestion("FAO", "Faro", "Gago Coutinho Algarve", "Portugal"),
        AirportPlaceSuggestion("FNC", "Funchal", "Cristiano Ronaldo Madeira", "Portugal"),
        AirportPlaceSuggestion("PDL", "Ponta Delgada", "João Paulo II Açores", "Portugal"),
        AirportPlaceSuggestion("MAD", "Madrid", "Adolfo Suárez Barajas", "Espanha"),
        AirportPlaceSuggestion("BCN", "Barcelona", "Josep Tarradellas El Prat", "Espanha"),
        AirportPlaceSuggestion("IBZ", "Ibiza", "Aeroport d'Eivissa Es Codolar", "Espanha"),
        AirportPlaceSuggestion("PMI", "Palma de Maiorca", "Son Sant Joan", "Espanha"),
        AirportPlaceSuggestion("MAH", "Menorca", "Aeroporto de Menorca", "Espanha"),
        AirportPlaceSuggestion("AGP", "Málaga", "Costa del Sol", "Espanha"),
        AirportPlaceSuggestion("VLC", "Valência", "Aeroporto de Valência Manises", "Espanha"),
        AirportPlaceSuggestion("ALC", "Alicante", "Alicante-Elche Miguel Hernández", "Espanha"),
        AirportPlaceSuggestion("SVQ", "Sevilha", "Aeroporto de Sevilha San Pablo", "Espanha"),
        AirportPlaceSuggestion("BIO", "Bilbau", "Aeroporto de Bilbau", "Espanha"),
        AirportPlaceSuggestion("TFS", "Tenerife", "Tenerife Sul Reina Sofía", "Espanha"),
        AirportPlaceSuggestion("LPA", "Gran Canária", "Aeroporto de Gran Canária", "Espanha"),
        AirportPlaceSuggestion("CDG", "Paris", "Charles de Gaulle", "França"),
        AirportPlaceSuggestion("ORY", "Paris", "Orly", "França"),
        AirportPlaceSuggestion("NCE", "Nice", "Côte d'Azur", "França"),
        AirportPlaceSuggestion("LHR", "Londres", "Heathrow", "Reino Unido"),
        AirportPlaceSuggestion("LGW", "Londres", "Gatwick", "Reino Unido"),
        AirportPlaceSuggestion("DUB", "Dublin", "Dublin International", "Irlanda"),
        AirportPlaceSuggestion("FCO", "Roma", "Leonardo da Vinci Fiumicino", "Itália"),
        AirportPlaceSuggestion("MXP", "Milão", "Malpensa", "Itália"),
        AirportPlaceSuggestion("LIN", "Milão", "Linate", "Itália"),
        AirportPlaceSuggestion("VCE", "Veneza", "Marco Polo", "Itália"),
        AirportPlaceSuggestion("ZRH", "Zurique", "Zurich Kloten", "Suíça"),
        AirportPlaceSuggestion("GVA", "Genebra", "Genève Cointrin", "Suíça"),
        AirportPlaceSuggestion("AMS", "Amsterdão", "Schiphol", "Países Baixos"),
        AirportPlaceSuggestion("BRU", "Bruxelas", "Zaventem", "Bélgica"),
        AirportPlaceSuggestion("FRA", "Frankfurt", "Frankfurt Main", "Alemanha"),
        AirportPlaceSuggestion("MUC", "Munique", "Franz Josef Strauss", "Alemanha"),
        AirportPlaceSuggestion("BER", "Berlim", "Brandenburg", "Alemanha"),
        AirportPlaceSuggestion("VIE", "Viena", "Schwechat International", "Áustria"),
        AirportPlaceSuggestion("ATH", "Atenas", "Eleftherios Venizelos", "Grécia"),
        AirportPlaceSuggestion("PRG", "Praga", "Václav Havel", "Chéquia"),
        AirportPlaceSuggestion("CPH", "Copenhaga", "Kastrup", "Dinamarca"),
        AirportPlaceSuggestion("DXB", "Dubai", "Dubai International", "Emirados Árabes Unidos"),
        AirportPlaceSuggestion("JFK", "Nova Iorque", "John F. Kennedy", "Estados Unidos"),
        AirportPlaceSuggestion("MIA", "Miami", "Miami International", "Estados Unidos")
    )

    fun resolveByIata(iataCode: String): AirportPlaceSuggestion {
        val code = iataCode.trim().uppercase()
        return GLOBAL_AIRPORTS.firstOrNull { it.iataCode.equals(code, ignoreCase = true) }
            ?: AirportPlaceSuggestion(
                iataCode = code.take(3).ifBlank { "LIS" },
                cityName = code.take(3).ifBlank { "Lisboa" },
                airportName = "Aeroporto Internacional",
                countryName = "Internacional"
            )
    }

    fun searchLocalCatalog(rawQuery: String, limit: Int = 8): List<AirportPlaceSuggestion> {
        val normalizedQuery = normalizeText(rawQuery)
        if (normalizedQuery.isBlank()) {
            return GLOBAL_AIRPORTS.take(limit)
        }
        return GLOBAL_AIRPORTS.filter { item ->
            val iataNorm = normalizeText(item.iataCode)
            val cityNorm = normalizeText(item.cityName)
            val airportNorm = normalizeText(item.airportName)
            val countryNorm = normalizeText(item.countryName)
            val fullLabelNorm = normalizeText(item.dropdownDisplayLabel)

            iataNorm.contains(normalizedQuery) ||
                cityNorm.contains(normalizedQuery) ||
                airportNorm.contains(normalizedQuery) ||
                countryNorm.contains(normalizedQuery) ||
                fullLabelNorm.contains(normalizedQuery)
        }.sortedWith(
            compareByDescending<AirportPlaceSuggestion> {
                normalizeText(it.iataCode) == normalizedQuery
            }.thenByDescending {
                normalizeText(it.cityName).startsWith(normalizedQuery)
            }.thenByDescending {
                normalizeText(it.countryName).startsWith(normalizedQuery)
            }
        ).take(limit)
    }

    fun mapDuffelDtoToSuggestion(dto: DuffelPlaceSuggestionDto): AirportPlaceSuggestion? {
        val code = (dto.iataCode ?: dto.city?.iataCode)?.trim()?.uppercase()
        if (code.isNullOrBlank() || code.length != 3) return null

        val localKnown = GLOBAL_AIRPORTS.firstOrNull { it.iataCode == code }
        val resolvedCity = dto.cityName?.takeIf { it.isNotBlank() }
            ?: dto.city?.name?.takeIf { it.isNotBlank() }
            ?: localKnown?.cityName
            ?: dto.name
        val cleanedAirport = dto.name
            .replace("International Airport", "", ignoreCase = true)
            .replace("Aeroporto Internacional de", "", ignoreCase = true)
            .replace("Aeroporto Internacional", "", ignoreCase = true)
            .replace("Airport", "", ignoreCase = true)
            .trim()
            .ifBlank { localKnown?.airportName ?: dto.name }
        val resolvedCountry = dto.countryName?.takeIf { it.isNotBlank() }
            ?: localKnown?.countryName
            ?: ""

        return AirportPlaceSuggestion(
            iataCode = code,
            cityName = resolvedCity,
            airportName = cleanedAirport,
            countryName = resolvedCountry
        )
    }

    private fun normalizeText(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
    }
}
