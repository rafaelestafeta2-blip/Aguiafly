package com.example.domain.destination

data class DestinationVisualMeta(
    val iataCode: String,
    val cityName: String,
    val countryName: String,
    val tagline: String,
    val vipLoungeArrival: String,
    val coverImageUrl: String
)

object DestinationCoverCatalog {

    private val destinations = mapOf(
        "OPO" to DestinationVisualMeta(
            iataCode = "OPO",
            cityName = "Porto",
            countryName = "Portugal",
            tagline = "Ribeira do Douro • Ponte Dom Luís I & Caves de Vinho do Porto",
            vipLoungeArrival = "ANA Lounge Porto • Aeroporto Francisco Sá Carneiro",
            coverImageUrl = "https://images.unsplash.com/photo-1555881400-74d7acaacd8b?auto=format&fit=crop&w=1200&q=85"
        ),
        "LIS" to DestinationVisualMeta(
            iataCode = "LIS",
            cityName = "Lisboa",
            countryName = "Portugal",
            tagline = "Costa do Tejo • Torre de Belém & Alfama Histórica",
            vipLoungeArrival = "TAP Premium Lounge Atlântico • Terminal 1",
            coverImageUrl = "https://images.unsplash.com/photo-1513735492246-483525079686?auto=format&fit=crop&w=1200&q=85"
        ),
        "FAO" to DestinationVisualMeta(
            iataCode = "FAO",
            cityName = "Faro (Algarve)",
            countryName = "Portugal",
            tagline = "Falésias Douradas do Algarve • Vilamoura & Ria Formosa",
            vipLoungeArrival = "CIP Lounge Faro • Terminal Principal",
            coverImageUrl = "https://images.unsplash.com/photo-1560703650-ef3e0f254ae0?auto=format&fit=crop&w=1200&q=85"
        ),
        "FNC" to DestinationVisualMeta(
            iataCode = "FNC",
            cityName = "Funchal (Madeira)",
            countryName = "Portugal",
            tagline = "Pérola do Atlântico • Baía do Funchal & Pico do Arieiro",
            vipLoungeArrival = "ANA Lounge Madeira • Piso 2",
            coverImageUrl = "https://images.unsplash.com/photo-1597926596303-59c6b4d90822?auto=format&fit=crop&w=1200&q=85"
        ),
        "PDL" to DestinationVisualMeta(
            iataCode = "PDL",
            cityName = "Ponta Delgada (Açores)",
            countryName = "Portugal",
            tagline = "Lagoa das Sete Cidades • Ilha de São Miguel",
            vipLoungeArrival = "SATA Azores Lounge • Ponta Delgada",
            coverImageUrl = "https://images.unsplash.com/photo-1586699640150-d1f36b64008b?auto=format&fit=crop&w=1200&q=85"
        ),
        "MAD" to DestinationVisualMeta(
            iataCode = "MAD",
            cityName = "Madrid",
            countryName = "Espanha",
            tagline = "Gran Vía • Palácio Real & Bairro de Salamanca",
            vipLoungeArrival = "Iberia Dalí VIP Lounge • Terminal 4",
            coverImageUrl = "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?auto=format&fit=crop&w=1200&q=85"
        ),
        "BCN" to DestinationVisualMeta(
            iataCode = "BCN",
            cityName = "Barcelona",
            countryName = "Espanha",
            tagline = "Marina Port Vell • Sagrada Família & Passeig de Gràcia",
            vipLoungeArrival = "Sala VIP Pau Casals • Terminal 1",
            coverImageUrl = "https://images.unsplash.com/photo-1583422409516-2895a77efded?auto=format&fit=crop&w=1200&q=85"
        ),
        "IBZ" to DestinationVisualMeta(
            iataCode = "IBZ",
            cityName = "Ibiza",
            countryName = "Espanha",
            tagline = "Marina de Dalt Vila • Iates & Ilhas Baleares",
            vipLoungeArrival = "Sala VIP Cap des Falcó • Ibiza Airport",
            coverImageUrl = "https://images.unsplash.com/photo-1563789031959-4c02bcb41319?auto=format&fit=crop&w=1200&q=85"
        ),
        "PMI" to DestinationVisualMeta(
            iataCode = "PMI",
            cityName = "Palma de Maiorca",
            countryName = "Espanha",
            tagline = "Catedral La Seu • Baía de Palma & Serra de Tramuntana",
            vipLoungeArrival = "Sala VIP Valldemossa • Palma Airport",
            coverImageUrl = "https://images.unsplash.com/photo-1536494126589-29fadf0d7e3c?auto=format&fit=crop&w=1200&q=85"
        ),
        "CDG" to DestinationVisualMeta(
            iataCode = "CDG",
            cityName = "Paris",
            countryName = "França",
            tagline = "Ville Lumière • Avenue Montaigne & Torre Eiffel",
            vipLoungeArrival = "Salon Air France La Première • Terminal 2E",
            coverImageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=85"
        ),
        "ORY" to DestinationVisualMeta(
            iataCode = "ORY",
            cityName = "Paris (Orly)",
            countryName = "França",
            tagline = "Saint-Germain-des-Prés • Rio Sena & Torre Eiffel",
            vipLoungeArrival = "Extime Lounge Paris Orly • Terminal 1",
            coverImageUrl = "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=85"
        ),
        "NCE" to DestinationVisualMeta(
            iataCode = "NCE",
            cityName = "Nice (Côte d'Azur)",
            countryName = "França",
            tagline = "Promenade des Anglais • Riviera Francesa & Mônaco",
            vipLoungeArrival = "The Infinity Lounge • Terminal 2",
            coverImageUrl = "https://images.unsplash.com/photo-1533105079780-92b9be482077?auto=format&fit=crop&w=1200&q=85"
        ),
        "LHR" to DestinationVisualMeta(
            iataCode = "LHR",
            cityName = "Londres",
            countryName = "Reino Unido",
            tagline = "Mayfair • Rio Tâmisa, Tower Bridge & Westminster",
            vipLoungeArrival = "The Concorde Room • Terminal 5",
            coverImageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=1200&q=85"
        ),
        "LGW" to DestinationVisualMeta(
            iataCode = "LGW",
            cityName = "Londres (Gatwick)",
            countryName = "Reino Unido",
            tagline = "Kensington • Rio Tâmisa & Big Ben",
            vipLoungeArrival = "No1 Lounge Gatwick • North Terminal",
            coverImageUrl = "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=1200&q=85"
        ),
        "DUB" to DestinationVisualMeta(
            iataCode = "DUB",
            cityName = "Dublin",
            countryName = "Irlanda",
            tagline = "Rio Liffey • Trinity College & Georgian Quarter",
            vipLoungeArrival = "The East Lounge • Dublin Terminal 2",
            coverImageUrl = "https://images.unsplash.com/photo-1549918864-48ac978761a4?auto=format&fit=crop&w=1200&q=85"
        ),
        "FCO" to DestinationVisualMeta(
            iataCode = "FCO",
            cityName = "Roma",
            countryName = "Itália",
            tagline = "Cidade Eterna • Via Condotti, Fontana di Trevi & Coliseu",
            vipLoungeArrival = "Plaza Premium First Lounge • Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=85"
        ),
        "MXP" to DestinationVisualMeta(
            iataCode = "MXP",
            cityName = "Milão",
            countryName = "Itália",
            tagline = "Piazza del Duomo • Quadrilátero da Moda & Galleria Vittorio",
            vipLoungeArrival = "Sala Montale Exclusive Lounge • Malpensa T1",
            coverImageUrl = "https://images.unsplash.com/photo-1520440229-6469a149ac59?auto=format&fit=crop&w=1200&q=85"
        ),
        "VCE" to DestinationVisualMeta(
            iataCode = "VCE",
            cityName = "Veneza",
            countryName = "Itália",
            tagline = "Grande Canal • Piazza San Marco & Ponte Rialto",
            vipLoungeArrival = "Marco Polo Club Lounge • Venice Airport",
            coverImageUrl = "https://images.unsplash.com/photo-1514890547357-a9ee288728e0?auto=format&fit=crop&w=1200&q=85"
        ),
        "ZRH" to DestinationVisualMeta(
            iataCode = "ZRH",
            cityName = "Zurique",
            countryName = "Suíça",
            tagline = "Bahnhofstrasse • Lago de Zurique & Alpes Suíços",
            vipLoungeArrival = "SWISS First Lounge E • Panorama Terrace",
            coverImageUrl = "https://images.unsplash.com/photo-1515488764276-beab7607c1e6?auto=format&fit=crop&w=1200&q=85"
        ),
        "GVA" to DestinationVisualMeta(
            iataCode = "GVA",
            cityName = "Genebra",
            countryName = "Suíça",
            tagline = "Lago Léman • Jet d'Eau & Relojoaria Suíça",
            vipLoungeArrival = "SWISS Senator Lounge • Genève Aéroport",
            coverImageUrl = "https://images.unsplash.com/photo-1573108037329-37aa135a142e?auto=format&fit=crop&w=1200&q=85"
        ),
        "AMS" to DestinationVisualMeta(
            iataCode = "AMS",
            cityName = "Amsterdão",
            countryName = "Países Baixos",
            tagline = "Canais Património UNESCO & Museumkwartier",
            vipLoungeArrival = "KLM Crown Lounge 52 • Schiphol",
            coverImageUrl = "https://images.unsplash.com/photo-1512470876302-972faa2aa9a4?auto=format&fit=crop&w=1200&q=85"
        ),
        "BRU" to DestinationVisualMeta(
            iataCode = "BRU",
            cityName = "Bruxelas",
            countryName = "Bélgica",
            tagline = "Grand-Place • Quartier Européen & Galeries Royales",
            vipLoungeArrival = "Brussels Airlines The Loft • Pier A",
            coverImageUrl = "https://images.unsplash.com/photo-1559113202-c916b8e44373?auto=format&fit=crop&w=1200&q=85"
        ),
        "FRA" to DestinationVisualMeta(
            iataCode = "FRA",
            cityName = "Frankfurt",
            countryName = "Alemanha",
            tagline = "Mainhattan Skyline • Rio Meno & Distrito Financeiro",
            vipLoungeArrival = "Lufthansa First Class Terminal • Pier A",
            coverImageUrl = "https://images.unsplash.com/photo-1577185816322-21f2a92b1342?auto=format&fit=crop&w=1200&q=85"
        ),
        "MUC" to DestinationVisualMeta(
            iataCode = "MUC",
            cityName = "Munique",
            countryName = "Alemanha",
            tagline = "Maximilianstraße • Marienplatz & Alpes da Baviera",
            vipLoungeArrival = "Lufthansa Senator Lounge • Terminal 2",
            coverImageUrl = "https://images.unsplash.com/photo-1595867818082-083862f3d630?auto=format&fit=crop&w=1200&q=85"
        ),
        "BER" to DestinationVisualMeta(
            iataCode = "BER",
            cityName = "Berlim",
            countryName = "Alemanha",
            tagline = "Portão de Brandemburgo • Rio Spree & Ilha dos Museus",
            vipLoungeArrival = "Lounge Tempelhof • Terminal 1",
            coverImageUrl = "https://images.unsplash.com/photo-1560969184-10fe8719e047?auto=format&fit=crop&w=1200&q=85"
        ),
        "VIE" to DestinationVisualMeta(
            iataCode = "VIE",
            cityName = "Viena",
            countryName = "Áustria",
            tagline = "Ringstraße • Ópera Estatal & Palácio de Schönbrunn",
            vipLoungeArrival = "Austrian Star Alliance HON Lounge • Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1516550893923-42d28e5677af?auto=format&fit=crop&w=1200&q=85"
        ),
        "ATH" to DestinationVisualMeta(
            iataCode = "ATH",
            cityName = "Atenas",
            countryName = "Grécia",
            tagline = "Acrópole & Partenon • Riviera Ateniense",
            vipLoungeArrival = "Aegean Business Lounge • Intra-Schengen",
            coverImageUrl = "https://images.unsplash.com/photo-1555993539-1732b0258235?auto=format&fit=crop&w=1200&q=85"
        ),
        "PRG" to DestinationVisualMeta(
            iataCode = "PRG",
            cityName = "Praga",
            countryName = "Chéquia",
            tagline = "Ponte Carlos • Rio Moldava & Castelo de Praga",
            vipLoungeArrival = "Erste Premier Lounge • Terminal 2",
            coverImageUrl = "https://images.unsplash.com/photo-1519677100203-a0e668c92439?auto=format&fit=crop&w=1200&q=85"
        ),
        "CPH" to DestinationVisualMeta(
            iataCode = "CPH",
            cityName = "Copenhaga",
            countryName = "Dinamarca",
            tagline = "Porto Histórico de Nyhavn • Design Escandinavo",
            vipLoungeArrival = "SAS Gold Lounge • Copenhagen Kastrup",
            coverImageUrl = "https://images.unsplash.com/photo-1513622470522-26c3c8a854bc?auto=format&fit=crop&w=1200&q=85"
        ),
        "DXB" to DestinationVisualMeta(
            iataCode = "DXB",
            cityName = "Dubai",
            countryName = "Emirados Árabes Unidos",
            tagline = "Downtown Dubai • Burj Khalifa & Palm Jumeirah",
            vipLoungeArrival = "Emirates First Class Lounge • Concourse A",
            coverImageUrl = "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=1200&q=85"
        ),
        "GRU" to DestinationVisualMeta(
            iataCode = "GRU",
            cityName = "São Paulo",
            countryName = "Brasil",
            tagline = "Avenida Paulista • Faria Lima & Jardins",
            vipLoungeArrival = "Espaço Banco Safra / Star Alliance Lounge • Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1543059080-f9b1272213d5?auto=format&fit=crop&w=1200&q=85"
        ),
        "GIG" to DestinationVisualMeta(
            iataCode = "GIG",
            cityName = "Rio de Janeiro",
            countryName = "Brasil",
            tagline = "Cristo Redentor • Pão de Açúcar, Ipanema & Copacabana",
            vipLoungeArrival = "Plaza Premium Lounge Internacional • Galeão",
            coverImageUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=85"
        ),
        "JFK" to DestinationVisualMeta(
            iataCode = "JFK",
            cityName = "Nova Iorque",
            countryName = "Estados Unidos",
            tagline = "Manhattan Skyline • Fifth Avenue & Central Park",
            vipLoungeArrival = "Chelsea & Soho First Lounge • JFK Terminal 8",
            coverImageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=85"
        ),
        "MIA" to DestinationVisualMeta(
            iataCode = "MIA",
            cityName = "Miami",
            countryName = "Estados Unidos",
            tagline = "South Beach • Biscayne Bay & Brickell Skyline",
            vipLoungeArrival = "Flagship First Dining • Miami Concourse D",
            coverImageUrl = "https://images.unsplash.com/photo-1535498730771-e735b998cd64?auto=format&fit=crop&w=1200&q=85"
        )
    )

    fun resolveDestination(iataCode: String, fallbackCity: String): DestinationVisualMeta {
        val code = iataCode.uppercase().trim()
        destinations[code]?.let { return it }

        val cleanCity = fallbackCity.trim().lowercase()
        destinations.values.firstOrNull { meta ->
            cleanCity.isNotEmpty() && (
                meta.cityName.lowercase().contains(cleanCity) ||
                    cleanCity.contains(meta.cityName.lowercase().substringBefore(" "))
                )
        }?.let { return it.copy(iataCode = code.ifBlank { it.iataCode }) }

        return DestinationVisualMeta(
            iataCode = code.ifBlank { "FLY" },
            cityName = fallbackCity.ifBlank { code },
            countryName = inferCountryFromIataOrCity(code, fallbackCity),
            tagline = "Rota Internacional AguiaFly • Destino Verificado ($code)",
            vipLoungeArrival = "AguiaFly Executive Partner Lounge • Aeroporto $code",
            coverImageUrl = "https://images.unsplash.com/photo-1436491865332-7a61a109cc05?auto=format&fit=crop&w=1200&q=85"
        )
    }

    fun isDynamicLookupNeeded(iataCode: String, fallbackCity: String): Boolean {
        val code = iataCode.uppercase().trim()
        if (destinations.containsKey(code)) return false
        val cleanCity = fallbackCity.trim().lowercase()
        return destinations.values.none {
            cleanCity.isNotEmpty() && (
                it.cityName.lowercase().contains(cleanCity) ||
                    cleanCity.contains(it.cityName.lowercase().substringBefore(" "))
                )
        }
    }

    private fun inferCountryFromIataOrCity(iata: String, city: String): String {
        val c = city.lowercase()
        return when {
            "porto" in c || "lisbo" in c || "faro" in c || "funchal" in c || "portugal" in c -> "Portugal"
            "madrid" in c || "barcelon" in c || "ibiza" in c || "valencia" in c || "sevil" in c || "malaga" in c -> "Espanha"
            "paris" in c || "nice" in c || "lyon" in c || "marsel" in c -> "França"
            "roma" in c || "rome" in c || "mila" in c || "venez" in c || "venic" in c || "floren" in c -> "Itália"
            "lond" in c || "manchester" in c || "edimburg" in c || "edinburgh" in c -> "Reino Unido"
            "frankfurt" in c || "muni" in c || "munich" in c || "berli" in c || "hamburg" in c -> "Alemanha"
            "zuri" in c || "genebr" in c || "genev" in c || "basel" in c -> "Suíça"
            "amsterd" in c || "rotterd" in c -> "Países Baixos"
            "sao paulo" in c || "são paulo" in c || "rio" in c || "brasil" in c || "recife" in c || "salvador" in c -> "Brasil"
            else -> "Destino Internacional"
        }
    }
}
