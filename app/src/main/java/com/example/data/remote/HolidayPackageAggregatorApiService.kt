package com.example.data.remote

import com.example.BuildConfig
import com.example.data.model.HolidayPackage
import com.example.data.model.HolidayPackageJsonSerializer
import com.example.data.model.PackageHighlightBadge
import com.example.data.model.PackageInclusionItem
import com.example.data.model.PackageItineraryDay
import com.example.data.model.PackageRouteWaypoint
import com.example.data.model.PackageSourceType
import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Conectores oficiais de Agregadores de Viagens via API (GetYourGuide, Viator e Civitatis):
 * - Consomem pacotes multi-dia, excursões VIP e experiências exclusivas de parceiros globais
 * - Confirmam reservas diretamente no parceiro quando o checkout Stripe é concluído
 */
interface ViatorPartnerApiService {
    @GET("partner/products/search")
    suspend fun searchHolidayPackages(
        @Header("exp-api-key") apiKey: String,
        @Query("destName") destinationQuery: String,
        @Query("currency") currency: String = "EUR"
    ): ViatorSearchResponseDto

    @POST("partner/bookings/book")
    suspend fun confirmPartnerBooking(
        @Header("exp-api-key") apiKey: String,
        @Body payload: PartnerBookingConfirmationRequestDto
    ): PartnerBookingConfirmationResponseDto
}

interface CivitatisPartnerApiService {
    @GET("v2/activities")
    suspend fun getDestinationActivitiesAndPackages(
        @Header("X-Civitatis-Api-Key") apiKey: String,
        @Query("destination") destinationQuery: String,
        @Query("lang") lang: String = "pt",
        @Query("currency") currency: String = "EUR"
    ): CivitatisActivitiesResponseDto

    @POST("v2/bookings/{activityId}/confirm")
    suspend fun confirmCivitatisBooking(
        @Header("X-Civitatis-Api-Key") apiKey: String,
        @Path("activityId") activityId: String,
        @Body payload: PartnerBookingConfirmationRequestDto
    ): PartnerBookingConfirmationResponseDto
}

data class ViatorSearchResponseDto(
    @Json(name = "products") val products: List<ViatorProductDto> = emptyList()
)

data class ViatorProductDto(
    @Json(name = "productCode") val productCode: String = "",
    @Json(name = "title") val title: String = ""
)

data class CivitatisActivitiesResponseDto(
    @Json(name = "data") val data: List<CivitatisActivityDto> = emptyList()
)

data class CivitatisActivityDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = ""
)

data class PartnerBookingConfirmationRequestDto(
    @Json(name = "package_code") val packageCode: String,
    @Json(name = "lead_traveler_name") val leadTravelerName: String,
    @Json(name = "lead_traveler_email") val leadTravelerEmail: String,
    @Json(name = "guests_count") val guestsCount: Int,
    @Json(name = "stripe_charge_id") val stripeChargeId: String
)

data class PartnerBookingConfirmationResponseDto(
    @Json(name = "status") val status: String = "CONFIRMED",
    @Json(name = "partner_voucher_code") val partnerVoucherCode: String = ""
)

object HolidayPackageAggregatorCatalog {

    fun isGetYourGuideLive(): Boolean {
        val k = runCatching { BuildConfig.GETYOURGUIDE_API_KEY }.getOrDefault("")
        return k.isNotBlank() && !k.contains("PLACEHOLDER", ignoreCase = true)
    }

    fun isViatorLive(): Boolean {
        val k = runCatching { BuildConfig.VIATOR_API_KEY }.getOrDefault("")
        return k.isNotBlank() && !k.contains("PLACEHOLDER", ignoreCase = true)
    }

    fun isCivitatisLive(): Boolean {
        val k = runCatching { BuildConfig.CIVITATIS_API_KEY }.getOrDefault("")
        return k.isNotBlank() && !k.contains("PLACEHOLDER", ignoreCase = true)
    }

    /**
     * Pacotes iniciais do Admin CMS (Agências Parceiras & Particulares) que são semeados
     * ou mesclados com os pacotes cadastrados manualmente pelo administrador no Firestore.
     */
    fun getDefaultManualCmsPackages(ownerUserId: String = "cms_seed"): List<HolidayPackage> {
        return listOf(
            HolidayPackage(
                id = "cms_maldives_overwater_7n",
                userId = ownerUserId,
                packageCode = "AGF-CMS-MLE01",
                title = "Maldivas Overwater Villa & Hidroavião Privado",
                destinationCity = "Atol de Baa & Malé, Maldivas",
                destinationIata = "MLE",
                durationLabel = "7 Noites / 8 Dias",
                durationNights = 7,
                netPricePerPersonEur = 1980.0,
                aguiaflyCommissionPercent = 12.0,
                aguiaflyFixedFeeEur = 40.0,
                finalPricePerPersonEur = HolidayPackage.calculateFinalPricePerPerson(1980.0, 12.0, 40.0),
                depositPercentage = 25,
                availableSpots = 4,
                totalSpots = 10,
                partnerAgencyName = "Maldives Royal Atoll Collection (Parceiro Oficial)",
                sourceType = PackageSourceType.MANUAL_CMS.code,
                badgeType = PackageHighlightBadge.TUDO_INCLUIDO.code,
                inclusionsCsv = PackageInclusionItem.toCsv(
                    setOf(
                        PackageInclusionItem.FLIGHT,
                        PackageInclusionItem.HOTEL,
                        PackageInclusionItem.MEALS,
                        PackageInclusionItem.GUIDE,
                        PackageInclusionItem.TRANSFERS
                    )
                ),
                galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(
                    listOf(
                        "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1573843981267-be1999ff37cd?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1540202404-a2f29016b523?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1586861635167-e5223aadc9fe?auto=format&fit=crop&w=1200&q=85"
                    )
                ),
                itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Voo Internacional & Transfer em Hidroavião Privado",
                            locationName = "Aeroporto Internacional de Malé → Atol de Baa",
                            description = "Recepção VIP no lounge aquático de Malé e embarque em hidroavião panorâmico sobre os atóis de coral azul-turquesa até à sua Overwater Pool Villa.",
                            includedMealsLabel = "Jantar de Boas-Vindas sobre o Mar",
                            highlightTag = "Transfer Hidroavião VIP"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Reserva da Biosfera UNESCO & Snorkeling com Raias-Manta",
                            locationName = "Baía de Hanifaru • Atol de Baa",
                            description = "Expedição guiada por biólogo marinho em iate rápido até Hanifaru Bay para nadar com raias-manta e tartarugas marinhas, seguida de almoço gourmet num banco de areia deserto.",
                            includedMealsLabel = "All-Inclusive Gourmet & Champagne",
                            highlightTag = "Banco de Areia Privado"
                        ),
                        PackageItineraryDay(
                            dayNumber = 3,
                            title = "Spa Overwater & Cruzeiro Pôr do Sol com Golfinhos",
                            locationName = "Lagoa Sul do Atol",
                            description = "Manhã livre na piscina infinita privativa da villa, ritual balinês de 90 minutos no Spa sobre estacas e cruzeiro em dhoni tradicional ao pôr do sol.",
                            includedMealsLabel = "All-Inclusive Premium",
                            highlightTag = "Spa & Sunset Cruise"
                        ),
                        PackageItineraryDay(
                            dayNumber = 4,
                            title = "Jantar Subaquático 5 Estrelas & Observação Astronómica",
                            locationName = "Recife de Coral Profundo",
                            description = "Menu degustação de 7 pratos em restaurante panorâmico subaquático a 6 metros de profundidade, finalizado com sessão privada no observatório astronómico.",
                            includedMealsLabel = "Degustação Subaquática Incluída",
                            highlightTag = "Restaurante Subaquático"
                        ),
                        PackageItineraryDay(
                            dayNumber = 5,
                            title = "Ilha Local Maldiviana, Desportos Náuticos & Check-out Late",
                            locationName = "Atol de Baa → Malé",
                            description = "Manhã dedicada a Seabob, caiaque transparente e paddleboard, retorno em hidroavião para Malé e acesso à Sala VIP antes do voo de regresso.",
                            includedMealsLabel = "Pequeno-almoço & Almoço",
                            highlightTag = "Late Check-out & Lounge VIP"
                        )
                    )
                ),
                routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(
                    listOf(
                        PackageRouteWaypoint(1, "Malé Intl (MLE)", "Chegada & Lounge Hidroavião", 4.1918, 73.5291),
                        PackageRouteWaypoint(2, "Atol de Baa Villa", "Overwater Pool Resort 5★", 5.1550, 73.1320),
                        PackageRouteWaypoint(3, "Hanifaru Bay", "Santuário UNESCO Raias-Manta", 5.1744, 73.1497),
                        PackageRouteWaypoint(4, "Sandbank Privado", "Almoço Exclusivo no Oceano", 5.2100, 73.0900)
                    )
                )
            ),
            HolidayPackage(
                id = "cms_amalfi_capri_yacht_6n",
                userId = ownerUserId,
                packageCode = "AGF-CMS-NAP02",
                title = "Costa Amalfitana, Positano & Ilha de Capri em Iate Privado",
                destinationCity = "Positano, Ravello & Capri, Itália",
                destinationIata = "FCO",
                durationLabel = "6 Noites / 7 Dias",
                durationNights = 6,
                netPricePerPersonEur = 1420.0,
                aguiaflyCommissionPercent = 14.0,
                aguiaflyFixedFeeEur = 30.0,
                finalPricePerPersonEur = HolidayPackage.calculateFinalPricePerPerson(1420.0, 14.0, 30.0),
                depositPercentage = 30,
                availableSpots = 3,
                totalSpots = 8,
                partnerAgencyName = "Amalfi DolceVita Luxury Tours (Agência Parceira)",
                sourceType = PackageSourceType.MANUAL_CMS.code,
                badgeType = PackageHighlightBadge.EXCLUSIVO_AGUIAFLY.code,
                inclusionsCsv = PackageInclusionItem.toCsv(
                    setOf(
                        PackageInclusionItem.FLIGHT,
                        PackageInclusionItem.HOTEL,
                        PackageInclusionItem.MEALS,
                        PackageInclusionItem.GUIDE,
                        PackageInclusionItem.TRANSFERS
                    )
                ),
                galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(
                    listOf(
                        "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1516483638261-f4dbaf036963?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1534445867742-43195f401b6c?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1523906834658-6e24ef2386f9?auto=format&fit=crop&w=1200&q=85"
                    )
                ),
                itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Voo Executivo & Transfer Mercedes Classe V até Positano",
                            locationName = "Roma / Nápoles → Falésias de Positano",
                            description = "Voo direto incluído e recepção com motorista particular percorrendo a estrada panorâmica SS163 até ao hotel boutique debruçado sobre o Mar Tirreno.",
                            includedMealsLabel = "Aperitivo Italiano & Jantar Vista Mar",
                            highlightTag = "Chauffeur Privado"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Travessia em Iate Riva/Gozzo até à Ilha de Capri & Gruta Azul",
                            locationName = "Positano → Faraglioni di Capri",
                            description = "Dia completo a bordo de iate privado com skipper, paragens para banho nas águas esmeralda dos Faraglioni, visita à Grotta Azzurra e almoço no La Fontelina.",
                            includedMealsLabel = "Pequeno-almoço & Espumante a Bordo",
                            highlightTag = "Iate Privado Capri"
                        ),
                        PackageItineraryDay(
                            dayNumber = 3,
                            title = "Jardins de Villa Cimbrone em Ravello & Degustação de Limoncello",
                            locationName = "Amalfi & Terraço do Infinito (Ravello)",
                            description = "Passeio com guia historiador privado pela Catedral de Santo André em Amalfi, subida a Ravello e degustação em pomar histórico de limões Sfusato Amalfitano.",
                            includedMealsLabel = "Pequeno-almoço & Almoço Agriturismo",
                            highlightTag = "Terraço do Infinito"
                        ),
                        PackageItineraryDay(
                            dayNumber = 4,
                            title = "Masterclass Gastronómica Michelin & Regresso",
                            locationName = "Sorrento → Aeroporto",
                            description = "Aula prática de massas artesanais e vinhos da Campânia com chef local antes do transfer executivo privado para o aeroporto.",
                            includedMealsLabel = "Menu Degustação Incluído",
                            highlightTag = "Gastronomia Campânia"
                        )
                    )
                ),
                routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(
                    listOf(
                        PackageRouteWaypoint(1, "Nápoles / Roma", "Recepção & Transfer VIP", 40.8518, 14.2681),
                        PackageRouteWaypoint(2, "Positano", "Hotel Boutique 5★ Vista Mar", 40.6281, 14.4850),
                        PackageRouteWaypoint(3, "Ilha de Capri", "Iate Privado & Faraglioni", 40.5532, 14.2222),
                        PackageRouteWaypoint(4, "Ravello & Amalfi", "Villa Cimbrone & Pomares", 40.6489, 14.6128)
                    )
                )
            ),
            HolidayPackage(
                id = "cms_dubai_desert_royal_5n",
                userId = ownerUserId,
                packageCode = "AGF-CMS-DXB03",
                title = "Dubai Royal Mirage, Helicóptero & Acampamento Bedouin de Luxo",
                destinationCity = "Dubai & Deserto de Al Marmoom, EAU",
                destinationIata = "DXB",
                durationLabel = "5 Noites / 6 Dias",
                durationNights = 5,
                netPricePerPersonEur = 1290.0,
                aguiaflyCommissionPercent = 12.5,
                aguiaflyFixedFeeEur = 30.0,
                finalPricePerPersonEur = HolidayPackage.calculateFinalPricePerPerson(1290.0, 12.5, 30.0),
                depositPercentage = 25,
                availableSpots = 6,
                totalSpots = 12,
                partnerAgencyName = "Emirates Arabian Oasis Concierge (CMS)",
                sourceType = PackageSourceType.MANUAL_CMS.code,
                badgeType = PackageHighlightBadge.TUDO_INCLUIDO.code,
                inclusionsCsv = PackageInclusionItem.toCsv(
                    setOf(
                        PackageInclusionItem.FLIGHT,
                        PackageInclusionItem.HOTEL,
                        PackageInclusionItem.MEALS,
                        PackageInclusionItem.GUIDE,
                        PackageInclusionItem.TRANSFERS
                    )
                ),
                galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(
                    listOf(
                        "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1518684079-3c830dcef090?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1546412414-e1885259563a?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1580674684081-7617fbf3d745?auto=format&fit=crop&w=1200&q=85"
                    )
                ),
                itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Chegada em Dubai, Fast-Track Imigração & Limousine até Palm Jumeirah",
                            locationName = "Aeroporto DXB → Palm Jumeirah",
                            description = "Assistência Meet & Greet à saída da aeronave, passagem prioritária pela alfândega e transfer privado até ao resort 5 estrelas na Palm Jumeirah.",
                            includedMealsLabel = "Jantar Buffet Internacional",
                            highlightTag = "Fast-Track & Palm Resort"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Voo de Helicóptero sobre The Palm & Burj Khalifa SKY 148º Andar",
                            locationName = "Heliporto Atlantis → Downtown Dubai",
                            description = "Voo panorâmico de 22 minutos sobre o Burj Al Arab, The World Islands e Palm Jumeirah, seguido de acesso sem fila ao lounge VIP 148º andar do Burj Khalifa.",
                            includedMealsLabel = "Pequeno-almoço & High Tea no Topo",
                            highlightTag = "Helicóptero + Burj SKY"
                        ),
                        PackageItineraryDay(
                            dayNumber = 3,
                            title = "Safari em Land Rover Vintage & Jantar Privado nas Dunas",
                            locationName = "Reserva de Conservação do Deserto de Dubai",
                            description = "Travessia ecológica pelas dunas douradas ao pôr do sol, espetáculo de falcoaria real e jantar privado sob as estrelas em oásis iluminado por tochas.",
                            includedMealsLabel = "Pequeno-almoço & Banquete Árabe VIP",
                            highlightTag = "Oásis Privado no Deserto"
                        )
                    )
                ),
                routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(
                    listOf(
                        PackageRouteWaypoint(1, "Dubai Intl (DXB)", "Meet & Greet VIP", 25.2532, 55.3657),
                        PackageRouteWaypoint(2, "Palm Jumeirah", "Resort 5★ & Voo Helicóptero", 25.1124, 55.1390),
                        PackageRouteWaypoint(3, "Burj Khalifa SKY", "Lounge 148º Andar (555m)", 25.1972, 55.2744),
                        PackageRouteWaypoint(4, "Deserto Al Marmoom", "Acampamento Real Privado", 24.8350, 55.3600)
                    )
                )
            )
        )
    }

    /**
     * Pacotes e experiências automáticas consumidas dos conectores de API externas:
     * - GetYourGuide Partner API
     * - Viator (Tripadvisor Partner API)
     * - Civitatis Global Agency API
     */
    fun getAggregatorApiPackages(): List<HolidayPackage> {
        return listOf(
            HolidayPackage(
                id = "api_gyg_santorini_mykonos_5n",
                userId = "aggregator_api",
                packageCode = "GYG-API-JTR88",
                title = "Ilhas Gregas: Santorini Caldera Suite & Mykonos Catamarã VIP",
                destinationCity = "Santorini (Oia) & Mykonos, Grécia",
                destinationIata = "ATH",
                durationLabel = "5 Noites / 6 Dias",
                durationNights = 5,
                netPricePerPersonEur = 1150.0,
                aguiaflyCommissionPercent = 12.0,
                aguiaflyFixedFeeEur = 25.0,
                finalPricePerPersonEur = HolidayPackage.calculateFinalPricePerPerson(1150.0, 12.0, 25.0),
                depositPercentage = 25,
                availableSpots = 7,
                totalSpots = 14,
                partnerAgencyName = "GetYourGuide Verified Collection",
                sourceType = PackageSourceType.GETYOURGUIDE_API.code,
                badgeType = PackageHighlightBadge.EXCLUSIVO_AGUIAFLY.code,
                inclusionsCsv = PackageInclusionItem.toCsv(
                    setOf(
                        PackageInclusionItem.HOTEL,
                        PackageInclusionItem.MEALS,
                        PackageInclusionItem.GUIDE,
                        PackageInclusionItem.TRANSFERS
                    )
                ),
                galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(
                    listOf(
                        "https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=85"
                    )
                ),
                itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Chegada a Santorini & Check-in em Cave Suite com Jacuzzi na Caldeira",
                            locationName = "Oia • Santorini",
                            description = "Transfer privado do porto/aeroporto até Oia e final de tarde livre no terraço privativo com vista para as cúpulas azuis.",
                            includedMealsLabel = "Vinho Assyrtiko & Jantar de Boas-Vindas",
                            highlightTag = "Cave Suite c/ Jacuzzi"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Cruzeiro em Catamarã pela Caldeira Vulcânica & Fontes Termais",
                            locationName = "Red Beach, White Beach & Vulcão Nea Kameni",
                            description = "Navegação de 5 horas em catamarã semi-privado com churrasco grego preparado a bordo, mergulho nas águas termais e pôr do sol em Oia.",
                            includedMealsLabel = "Pequeno-almoço & Churrasco a Bordo",
                            highlightTag = "Catamarã no Vulcão"
                        ),
                        PackageItineraryDay(
                            dayNumber = 3,
                            title = "Fast Ferry Primeira Classe para Mykonos & Tour Little Venice",
                            locationName = "Santorini → Mykonos Town",
                            description = "Travessia rápida em poltrona Platinum, transfer ao hotel boutique em Mykonos e caminhada guiada pelos moinhos históricos e Little Venice.",
                            includedMealsLabel = "Pequeno-almoço & Tapas Gregas",
                            highlightTag = "Ferry Platinum + Mykonos"
                        )
                    )
                ),
                routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(
                    listOf(
                        PackageRouteWaypoint(1, "Oia, Santorini", "Cave Suite na Caldeira", 36.4618, 25.3753),
                        PackageRouteWaypoint(2, "Nea Kameni", "Cruzeiro Catamarã Vulcão", 36.4044, 25.3961),
                        PackageRouteWaypoint(3, "Mykonos Chora", "Moinhos & Little Venice", 37.4467, 25.3289)
                    )
                )
            ),
            HolidayPackage(
                id = "api_viator_swiss_alps_glacier_4n",
                userId = "aggregator_api",
                packageCode = "VIA-API-ZRH42",
                title = "Alpes Suíços: Glacier Express Excellence, Zermatt & Jungfraujoch",
                destinationCity = "Zurique, Zermatt (Matterhorn) & Interlaken, Suíça",
                destinationIata = "ZRH",
                durationLabel = "4 Noites / 5 Dias",
                durationNights = 4,
                netPricePerPersonEur = 1340.0,
                aguiaflyCommissionPercent = 11.0,
                aguiaflyFixedFeeEur = 25.0,
                finalPricePerPersonEur = HolidayPackage.calculateFinalPricePerPerson(1340.0, 11.0, 25.0),
                depositPercentage = 30,
                availableSpots = 5,
                totalSpots = 10,
                partnerAgencyName = "Viator Luxury Rail & Alpine Tours",
                sourceType = PackageSourceType.VIATOR_API.code,
                badgeType = PackageHighlightBadge.TUDO_INCLUIDO.code,
                inclusionsCsv = PackageInclusionItem.toCsv(
                    setOf(
                        PackageInclusionItem.HOTEL,
                        PackageInclusionItem.MEALS,
                        PackageInclusionItem.GUIDE,
                        PackageInclusionItem.TRANSFERS
                    )
                ),
                galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(
                    listOf(
                        "https://images.unsplash.com/photo-1530122037265-a5f1f91d3b99?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1527668752968-14dc70a27c95?auto=format&fit=crop&w=1200&q=85"
                    )
                ),
                itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Chegada a Zurique & Comboio Panorâmico até Zermatt (Aos Pés do Matterhorn)",
                            locationName = "Zurique → Zermatt",
                            description = "Bilhete Swiss Travel Pass 1ª Classe incluído e check-in em chalet alpino 5 estrelas com spa aquecido e vista direta para o Matterhorn.",
                            includedMealsLabel = "Jantar Fondue Trufado Suíço",
                            highlightTag = "Swiss 1st Class Rail"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Comboio Glacier Express Panorâmico (291 Pontes & 91 Túneis)",
                            locationName = "Zermatt → Passo Oberalp → St. Moritz / Interlaken",
                            description = "Viagem lendária em carruagem panorâmica de teto de vidro com almoço gourmet de 5 pratos harmonizado com vinhos do Valais servido no lugar.",
                            includedMealsLabel = "Pequeno-almoço & Almoço a Bordo",
                            highlightTag = "Glacier Express VIP"
                        ),
                        PackageItineraryDay(
                            dayNumber = 3,
                            title = "Topo da Europa: Jungfraujoch (3.454m) & Palácio de Gelo",
                            locationName = "Interlaken → Eiger Express → Jungfraujoch",
                            description = "Subida no teleférico Eiger Express e comboio de cremalheira até à estação ferroviária mais alta da Europa sobre o Glaciar Aletsch.",
                            includedMealsLabel = "Pequeno-almoço & Almoço Alpino",
                            highlightTag = "Jungfraujoch 3.454m"
                        )
                    )
                ),
                routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(
                    listOf(
                        PackageRouteWaypoint(1, "Zurique (ZRH)", "Ponto de Partida 1ª Classe", 47.3769, 8.5417),
                        PackageRouteWaypoint(2, "Zermatt & Matterhorn", "Chalet Alpino & Gornergrat", 46.0207, 7.7491),
                        PackageRouteWaypoint(3, "Jungfraujoch", "Top of Europe (3.454m)", 46.5475, 7.9851)
                    )
                )
            ),
            HolidayPackage(
                id = "api_civitatis_paris_loire_normandy_4n",
                userId = "aggregator_api",
                packageCode = "CIV-API-CDG19",
                title = "Paris Imperial, Castelos do Vale do Loire & Mont Saint-Michel em Português",
                destinationCity = "Paris, Vale do Loire & Normandia, França",
                destinationIata = "CDG",
                durationLabel = "4 Noites / 5 Dias",
                durationNights = 4,
                netPricePerPersonEur = 790.0,
                aguiaflyCommissionPercent = 12.0,
                aguiaflyFixedFeeEur = 20.0,
                finalPricePerPersonEur = HolidayPackage.calculateFinalPricePerPerson(790.0, 12.0, 20.0),
                depositPercentage = 20,
                availableSpots = 3,
                totalSpots = 15,
                partnerAgencyName = "Civitatis Circuito Exclusivo em Português",
                sourceType = PackageSourceType.CIVITATIS_API.code,
                badgeType = PackageHighlightBadge.ULTIMAS_VAGAS.code,
                inclusionsCsv = PackageInclusionItem.toCsv(
                    setOf(
                        PackageInclusionItem.HOTEL,
                        PackageInclusionItem.MEALS,
                        PackageInclusionItem.GUIDE,
                        PackageInclusionItem.TRANSFERS
                    )
                ),
                galleryPhotosJson = HolidayPackageJsonSerializer.encodeGalleryPhotos(
                    listOf(
                        "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?auto=format&fit=crop&w=1200&q=85",
                        "https://images.unsplash.com/photo-1549144511-f099e773c147?auto=format&fit=crop&w=1200&q=85"
                    )
                ),
                itineraryDaysJson = HolidayPackageJsonSerializer.encodeItineraryDays(
                    listOf(
                        PackageItineraryDay(
                            dayNumber = 1,
                            title = "Transfer Privado CDG, Cruzeiro Jantar no Sena & Torre Eiffel Sem Fila",
                            locationName = "Paris • Champs-Élysées & Rio Sena",
                            description = "Recepção no aeroporto Charles de Gaulle, subida prioritária ao 2º andar da Torre Eiffel com guia lusófono e jantar panorâmico Bateaux Parisiens.",
                            includedMealsLabel = "Jantar Cruzeiro no Sena",
                            highlightTag = "Torre Eiffel + Sena VIP"
                        ),
                        PackageItineraryDay(
                            dayNumber = 2,
                            title = "Castelos de Chambord e Chenonceau com Prova de Vinhos do Loire",
                            locationName = "Vale do Loire • Património UNESCO",
                            description = "Excursão executiva com entrada direta no Château de Chenonceau e na escadaria dupla de Leonardo da Vinci em Chambord, incluindo degustação em adega real.",
                            includedMealsLabel = "Pequeno-almoço & Prova de Vinhos",
                            highlightTag = "Castelos do Loire"
                        ),
                        PackageItineraryDay(
                            dayNumber = 3,
                            title = "Ilha Abadia de Mont Saint-Michel & Costa da Normandia",
                            locationName = "Mont Saint-Michel • Normandia",
                            description = "Visita guiada em português pela abadia gótica medieval erguida sobre a ilha rochosa das marés mais altas da Europa.",
                            includedMealsLabel = "Pequeno-almoço & Almoço Normando",
                            highlightTag = "Mont Saint-Michel"
                        )
                    )
                ),
                routeWaypointsJson = HolidayPackageJsonSerializer.encodeWaypoints(
                    listOf(
                        PackageRouteWaypoint(1, "Paris (CDG)", "Torre Eiffel & Rio Sena", 48.8584, 2.2945),
                        PackageRouteWaypoint(2, "Chambord & Chenonceau", "Castelos do Vale do Loire", 47.6161, 1.5170),
                        PackageRouteWaypoint(3, "Mont Saint-Michel", "Abadia Medieval na Ilha", 48.6361, -1.5115)
                    )
                )
            )
        )
    }
}
