package com.example.domain.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.data.model.AirportPlaceSuggestion
import com.example.data.model.PlaceAutocompleteCatalog
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoAirportCoordinate(
    val iataCode: String,
    val cityName: String,
    val airportName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double
)

data class DetectedAirportResult(
    val airport: AirportPlaceSuggestion,
    val distanceKm: Int?,
    val detectionSource: String,
    val isGpsVerified: Boolean
)

object NearestAirportLocator {

    private val GEO_AIRPORTS: List<GeoAirportCoordinate> = listOf(
        // Brasil
        GeoAirportCoordinate("GRU", "São Paulo", "Guarulhos", "Brasil", -23.4356, -46.4731),
        GeoAirportCoordinate("CGH", "São Paulo", "Congonhas", "Brasil", -23.6261, -46.6564),
        GeoAirportCoordinate("VCP", "Campinas", "Viracopos", "Brasil", -23.0074, -47.1345),
        GeoAirportCoordinate("GIG", "Rio de Janeiro", "Tom Jobim Galeão", "Brasil", -22.8099, -43.2505),
        GeoAirportCoordinate("SDU", "Rio de Janeiro", "Santos Dumont", "Brasil", -22.9105, -43.1631),
        GeoAirportCoordinate("BSB", "Brasília", "Juscelino Kubitschek", "Brasil", -15.8711, -47.9186),
        GeoAirportCoordinate("CNF", "Belo Horizonte", "Confins Tancredo Neves", "Brasil", -19.6244, -43.9719),
        GeoAirportCoordinate("SSA", "Salvador", "Luís Eduardo Magalhães", "Brasil", -12.9086, -38.3225),
        GeoAirportCoordinate("REC", "Recife", "Guararapes Gilberto Freyre", "Brasil", -8.1265, -34.9236),
        GeoAirportCoordinate("FOR", "Fortaleza", "Pinto Martins", "Brasil", -3.7763, -38.5326),
        GeoAirportCoordinate("POA", "Porto Alegre", "Salgado Filho", "Brasil", -29.9944, -51.1714),
        GeoAirportCoordinate("CWB", "Curitiba", "Afonso Pena", "Brasil", -25.5285, -49.1758),
        // Portugal
        GeoAirportCoordinate("LIS", "Lisboa", "Humberto Delgado", "Portugal", 38.7756, -9.1354),
        GeoAirportCoordinate("OPO", "Porto", "Francisco Sá Carneiro", "Portugal", 41.2481, -8.6814),
        GeoAirportCoordinate("FAO", "Faro", "Gago Coutinho Algarve", "Portugal", 37.0144, -7.9659),
        GeoAirportCoordinate("FNC", "Funchal", "Cristiano Ronaldo Madeira", "Portugal", 32.6979, -16.7745),
        GeoAirportCoordinate("PDL", "Ponta Delgada", "João Paulo II Açores", "Portugal", 37.7412, -25.6979),
        // Europa & Mundo
        GeoAirportCoordinate("MAD", "Madrid", "Adolfo Suárez Barajas", "Espanha", 40.4983, -3.5676),
        GeoAirportCoordinate("BCN", "Barcelona", "Josep Tarradellas El Prat", "Espanha", 41.2974, 2.0833),
        GeoAirportCoordinate("CDG", "Paris", "Charles de Gaulle", "França", 49.0097, 2.5479),
        GeoAirportCoordinate("ORY", "Paris", "Orly", "França", 48.7262, 2.3652),
        GeoAirportCoordinate("LHR", "Londres", "Heathrow", "Reino Unido", 51.4700, -0.4543),
        GeoAirportCoordinate("DUB", "Dublin", "Dublin International", "Irlanda", 53.4264, -6.2499),
        GeoAirportCoordinate("FCO", "Roma", "Leonardo da Vinci Fiumicino", "Itália", 41.8003, 12.2389),
        GeoAirportCoordinate("MXP", "Milão", "Malpensa", "Itália", 45.6301, 8.7255),
        GeoAirportCoordinate("ZRH", "Zurique", "Zurich Kloten", "Suíça", 47.4582, 8.5555),
        GeoAirportCoordinate("AMS", "Amsterdão", "Schiphol", "Países Baixos", 52.3105, 4.7683),
        GeoAirportCoordinate("FRA", "Frankfurt", "Frankfurt Main", "Alemanha", 50.0379, 8.5622),
        GeoAirportCoordinate("DXB", "Dubai", "Dubai International", "Emirados Árabes Unidos", 25.2532, 55.3657),
        GeoAirportCoordinate("JFK", "Nova Iorque", "John F. Kennedy", "Estados Unidos", 40.6413, -73.7781),
        GeoAirportCoordinate("MIA", "Miami", "Miami International", "Estados Unidos", 25.7959, -80.2870)
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun detectNearestAirport(context: Context): DetectedAirportResult {
        if (hasLocationPermission(context)) {
            val location = getBestLastKnownLocation(context)
            if (location != null) {
                return findClosestByCoordinates(location.latitude, location.longitude)
            }
        }
        return detectByTimezoneAndLocale()
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(context: Context): Location? {
        return try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return null
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = runCatching { manager.getLastKnownLocation(provider) }.getOrNull() ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            bestLocation
        } catch (_: Exception) {
            null
        }
    }

    fun findClosestByCoordinates(latitude: Double, longitude: Double): DetectedAirportResult {
        val closestPair = GEO_AIRPORTS.map { hub ->
            hub to haversineDistanceKm(latitude, longitude, hub.latitude, hub.longitude)
        }.minByOrNull { it.second } ?: (GEO_AIRPORTS.first() to 18.0)

        val hub = closestPair.first
        val distanceKm = closestPair.second.roundToInt().coerceAtLeast(1)
        return DetectedAirportResult(
            airport = AirportPlaceSuggestion(
                iataCode = hub.iataCode,
                cityName = hub.cityName,
                airportName = hub.airportName,
                countryName = hub.countryName
            ),
            distanceKm = distanceKm,
            detectionSource = "GPS • a ${distanceKm} km de si",
            isGpsVerified = true
        )
    }

    private fun detectByTimezoneAndLocale(): DetectedAirportResult {
        val tzId = TimeZone.getDefault().id
        val country = Locale.getDefault().country.uppercase()

        val resolvedIata = when {
            tzId.contains("Sao_Paulo", ignoreCase = true) -> "GRU"
            tzId.contains("Fortaleza", ignoreCase = true) -> "FOR"
            tzId.contains("Recife", ignoreCase = true) -> "REC"
            tzId.contains("Bahia", ignoreCase = true) -> "SSA"
            tzId.contains("Lisbon", ignoreCase = true) -> "LIS"
            tzId.contains("Madrid", ignoreCase = true) -> "MAD"
            tzId.contains("Paris", ignoreCase = true) -> "CDG"
            tzId.contains("London", ignoreCase = true) -> "LHR"
            tzId.contains("Rome", ignoreCase = true) -> "FCO"
            tzId.contains("New_York", ignoreCase = true) -> "JFK"
            country == "BR" -> "GRU"
            country == "PT" -> "LIS"
            country == "ES" -> "MAD"
            country == "FR" -> "CDG"
            country == "US" -> "JFK"
            else -> "GRU"
        }

        val airport = PlaceAutocompleteCatalog.resolveByIata(resolvedIata)
        return DetectedAirportResult(
            airport = airport,
            distanceKm = null,
            detectionSource = "Região local (${airport.cityName})",
            isGpsVerified = false
        )
    }

    private fun haversineDistanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
