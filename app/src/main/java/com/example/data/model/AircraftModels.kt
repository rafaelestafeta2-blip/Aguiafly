package com.example.data.model

import com.squareup.moshi.Json

enum class BodyType(val label: String) {
    WIDEBODY("Widebody Twin-Aisle"),
    NARROWBODY("Narrowbody Single-Aisle")
}

data class AircraftTechnicalSpec(
    val iataCode: String,
    val icaoCode: String,
    val fullName: String,
    val manufacturer: String,
    val bodyType: BodyType,
    val seatPitchBusiness: String,
    val seatPitchEconomy: String,
    val cabinLayoutBusiness: String,
    val cabinLayoutEconomy: String,
    val wifiSpecification: String,
    val powerOutlets: String,
    val ifeDisplay: String,
    val cabinPressureAltitudeFt: Int,
    val cruiseSpeedMach: String,
    val rangeKm: Int,
    val cabinNoiseDb: Int,
    val sampleRegistration: String,
    val exteriorPhotoUrl: String = "",
    val cabinPhotoUrl: String = ""
)

data class PlanespottersResponse(
    @Json(name = "photos") val photos: List<PlanespottersPhoto> = emptyList()
)

data class PlanespottersPhoto(
    @Json(name = "id") val id: String,
    @Json(name = "thumbnail_large") val thumbnailLarge: PlanespottersImageSource? = null,
    @Json(name = "photographer") val photographer: String? = null,
    @Json(name = "link") val link: String? = null
)

data class PlanespottersImageSource(
    @Json(name = "src") val src: String
)

data class SeatOption(
    val designator: String,
    val row: Int,
    val column: String,
    val isBusinessSuite: Boolean,
    val isWindow: Boolean,
    val isAvailable: Boolean,
    val extraLegroom: Boolean,
    val priceEur: Double = 0.0,
    val serviceId: String? = null
)
