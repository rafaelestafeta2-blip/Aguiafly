package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.round
import org.json.JSONArray
import org.json.JSONObject

/**
 * Origem do pacote ou experiência exclusiva:
 * - MANUAL_CMS: Cadastrado/gerido manualmente no Admin Dashboard (Agências Parceiras / Particulares)
 * - GETYOURGUIDE_API: Conectado via API GetYourGuide Partner
 * - VIATOR_API: Conectado via API Viator (Tripadvisor Experiences)
 * - CIVITATIS_API: Conectado via API Civitatis (Excursões & Pacotes em Português)
 */
enum class PackageSourceType(
    val code: String,
    val displayLabel: String,
    val badgeShortLabel: String
) {
    MANUAL_CMS("MANUAL_CMS", "Agência Parceira • Admin CMS", "Parceiro Oficial CMS"),
    GETYOURGUIDE_API("GETYOURGUIDE_API", "GetYourGuide Partner API", "API GetYourGuide"),
    VIATOR_API("VIATOR_API", "Viator Tripadvisor API", "API Viator"),
    CIVITATIS_API("CIVITATIS_API", "Civitatis Global API", "API Civitatis");

    companion object {
        fun fromCode(code: String): PackageSourceType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: MANUAL_CMS
        }
    }
}

/**
 * Badges de destaque visual (estilo Airbnb Luxe / Instagram):
 * - TUDO_INCLUIDO: "Tudo Incluído"
 * - EXCLUSIVO_AGUIAFLY: "Exclusivo Aguiafly"
 * - ULTIMAS_VAGAS: "Últimas Vagas"
 * - PARCEIRO_OFICIAL: "Verificado API"
 */
enum class PackageHighlightBadge(
    val code: String,
    val label: String
) {
    TUDO_INCLUIDO("TUDO_INCLUIDO", "★ Tudo Incluído"),
    EXCLUSIVO_AGUIAFLY("EXCLUSIVO_AGUIAFLY", "✦ Exclusivo Aguiafly"),
    ULTIMAS_VAGAS("ULTIMAS_VAGAS", "🔥 Últimas Vagas"),
    PARCEIRO_OFICIAL("PARCEIRO_OFICIAL", "✓ Parceiro Verificado");

    companion object {
        fun fromCode(code: String): PackageHighlightBadge {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: EXCLUSIVO_AGUIAFLY
        }
    }
}

/**
 * Ícones de inclusão no pacote de férias/experiência:
 * Voo, Alojamento, Refeições, Guias, Transfers
 */
enum class PackageInclusionItem(
    val code: String,
    val label: String,
    val shortDescription: String
) {
    FLIGHT("FLIGHT", "Voo Incluído", "Passagem aérea ida e volta c/ franquia"),
    HOTEL("HOTEL", "Alojamento 5★", "Hotéis Boutique, Resorts ou Villas selecionadas"),
    MEALS("MEALS", "Refeições", "Pequeno-almoço, meia pensão ou All-Inclusive"),
    GUIDE("GUIDE", "Guias Privados", "Guia especialista certificado no idioma local/PT"),
    TRANSFERS("TRANSFERS", "Transfers VIP", "Transporte executivo aeroporto-hotel e passeios");

    companion object {
        fun parseCsv(csv: String): Set<PackageInclusionItem> {
            if (csv.isBlank()) return emptySet()
            return csv.split(",")
                .map { it.trim().uppercase() }
                .mapNotNull { token -> entries.firstOrNull { it.code == token } }
                .toSet()
        }

        fun toCsv(items: Set<PackageInclusionItem>): String {
            return items.joinToString(",") { it.code }
        }
    }
}

/**
 * Item do Itinerário Detalhado Dia a Dia (exibido em aba sanfonada / Accordion).
 */
data class PackageItineraryDay(
    val dayNumber: Int = 1,
    val title: String = "",
    val locationName: String = "",
    val description: String = "",
    val includedMealsLabel: String = "Pequeno-almoço incluído",
    val highlightTag: String = "Experiência Principal"
)

/**
 * Ponto geográfico do roteiro para o Mapa Interativo do Pacote.
 */
data class PackageRouteWaypoint(
    val orderIndex: Int = 1,
    val label: String = "",
    val subtitle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

/**
 * Modelo Firestore e Domínio unificado para Pacotes de Férias & Experiências Exclusivas
 * (combina pacotes cadastrados manualmente no Admin CMS e pacotes das APIs GetYourGuide, Viator e Civitatis).
 */
@IgnoreExtraProperties
data class HolidayPackage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val packageCode: String = "",
    val title: String = "",
    val destinationCity: String = "",
    val destinationIata: String = "LIS",
    val durationLabel: String = "7 Noites / 8 Dias",
    val durationNights: Int = 7,
    val netPricePerPersonEur: Double = 850.0,
    val aguiaflyCommissionPercent: Double = 12.0,
    val aguiaflyFixedFeeEur: Double = 25.0,
    val finalPricePerPersonEur: Double = 977.0,
    val depositPercentage: Int = 25,
    val availableSpots: Int = 8,
    val totalSpots: Int = 12,
    val partnerAgencyName: String = "AguiaFly Private Concierge",
    val sourceType: String = PackageSourceType.MANUAL_CMS.code,
    val badgeType: String = PackageHighlightBadge.EXCLUSIVO_AGUIAFLY.code,
    val inclusionsCsv: String = "FLIGHT,HOTEL,MEALS,GUIDE,TRANSFERS",
    val galleryPhotosJson: String = "[]",
    val itineraryDaysJson: String = "[]",
    val routeWaypointsJson: String = "[]",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val sourceEnum: PackageSourceType
        get() = PackageSourceType.fromCode(sourceType)

    val effectiveBadge: PackageHighlightBadge
        get() = if (availableSpots in 1..4) {
            PackageHighlightBadge.ULTIMAS_VAGAS
        } else {
            PackageHighlightBadge.fromCode(badgeType)
        }

    val inclusions: Set<PackageInclusionItem>
        get() = PackageInclusionItem.parseCsv(inclusionsCsv)

    val galleryPhotos: List<String>
        get() = HolidayPackageJsonSerializer.decodeGalleryPhotos(galleryPhotosJson)

    val itineraryDays: List<PackageItineraryDay>
        get() = HolidayPackageJsonSerializer.decodeItineraryDays(itineraryDaysJson)

    val routeWaypoints: List<PackageRouteWaypoint>
        get() = HolidayPackageJsonSerializer.decodeWaypoints(routeWaypointsJson)

    val commissionAmountPerPersonEur: Double
        get() = round((finalPricePerPersonEur - netPricePerPersonEur).coerceAtLeast(0.0) * 100.0) / 100.0

    val depositPerPersonEur: Double
        get() = round(finalPricePerPersonEur * (depositPercentage / 100.0) * 100.0) / 100.0

    companion object {
        fun calculateFinalPricePerPerson(
            netEur: Double,
            commissionPercent: Double,
            fixedFeeEur: Double
        ): Double {
            val commission = netEur * (commissionPercent / 100.0) + fixedFeeEur
            return round((netEur + commission) * 100.0) / 100.0
        }
    }
}

/**
 * Modalidade de pagamento no Checkout Direto de Pacotes via Stripe:
 * - FULL_PAYMENT: Compra Instantânea (100% do valor total)
 * - DEPOSIT_SIGNAL: Sinal de Reserva (ex: 25% ou 30% agora via Stripe para bloquear as vagas, saldo antes do embarque)
 */
enum class PackagePaymentMode(val code: String, val label: String) {
    FULL_PAYMENT("FULL_PAYMENT", "Compra Instantânea (100%)"),
    DEPOSIT_SIGNAL("DEPOSIT_SIGNAL", "Sinal de Reserva Garantida")
}

/**
 * Reserva confirmada de Pacote de Férias ou Experiência Exclusiva (persistida em /package_bookings).
 */
@IgnoreExtraProperties
data class PackageBookingRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookingCode: String = "",
    val packageId: String = "",
    val packageTitle: String = "",
    val destinationCity: String = "",
    val destinationIata: String = "LIS",
    val durationLabel: String = "",
    val sourceType: String = PackageSourceType.MANUAL_CMS.code,
    val partnerName: String = "",
    val partnerConfirmationCode: String = "",
    val travelerFullName: String = "",
    val travelerEmail: String = "",
    val travelerPhone: String = "",
    val guestsCount: Int = 2,
    val paymentMode: String = PackagePaymentMode.FULL_PAYMENT.code,
    val totalPackageGrossEur: Double = 0.0,
    val amountChargedNowEur: Double = 0.0,
    val remainingBalanceEur: Double = 0.0,
    val aguiaflyCommissionEur: Double = 0.0,
    val stripeChargeId: String = "",
    val status: String = "CONFIRMED",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object HolidayPackageJsonSerializer {

    fun encodeGalleryPhotos(urls: List<String>): String {
        val arr = JSONArray()
        urls.filter { it.isNotBlank() }.forEach { arr.put(it.trim()) }
        return arr.toString()
    }

    fun decodeGalleryPhotos(json: String): List<String> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val url = arr.optString(i).trim()
                    if (url.isNotBlank()) add(url)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeItineraryDays(days: List<PackageItineraryDay>): String {
        val arr = JSONArray()
        days.forEach { d ->
            val obj = JSONObject()
            obj.put("day", d.dayNumber)
            obj.put("title", d.title)
            obj.put("location", d.locationName)
            obj.put("description", d.description)
            obj.put("meals", d.includedMealsLabel)
            obj.put("tag", d.highlightTag)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeItineraryDays(json: String): List<PackageItineraryDay> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        PackageItineraryDay(
                            dayNumber = obj.optInt("day", i + 1),
                            title = obj.optString("title", "Dia ${i + 1}"),
                            locationName = obj.optString("location", ""),
                            description = obj.optString("description", ""),
                            includedMealsLabel = obj.optString("meals", "Pequeno-almoço incluído"),
                            highlightTag = obj.optString("tag", "Roteiro Exclusivo")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeWaypoints(points: List<PackageRouteWaypoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            val obj = JSONObject()
            obj.put("idx", p.orderIndex)
            obj.put("label", p.label)
            obj.put("sub", p.subtitle)
            obj.put("lat", p.latitude)
            obj.put("lon", p.longitude)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun decodeWaypoints(json: String): List<PackageRouteWaypoint> {
        if (json.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        PackageRouteWaypoint(
                            orderIndex = obj.optInt("idx", i + 1),
                            label = obj.optString("label", "Parada ${i + 1}"),
                            subtitle = obj.optString("sub", ""),
                            latitude = obj.optDouble("lat", 0.0),
                            longitude = obj.optDouble("lon", 0.0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
