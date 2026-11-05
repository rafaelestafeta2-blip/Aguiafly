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
        "VCP" to DestinationVisualMeta(
            iataCode = "VCP",
            cityName = "Campinas / São Paulo",
            countryName = "Brasil",
            tagline = "Hub Azul Viracopos • Cambuí, Lagoa do Taquaral & Grande São Paulo",
            vipLoungeArrival = "Lounge Azul Internacional • Viracopos Terminal 1",
            coverImageUrl = "https://images.unsplash.com/photo-1543059080-f9b1272213d5?auto=format&fit=crop&w=1200&q=85"
        ),
        "CGH" to DestinationVisualMeta(
            iataCode = "CGH",
            cityName = "São Paulo (Congonhas)",
            countryName = "Brasil",
            tagline = "Ponte Aérea Executiva • Itaim Bibi, Paulista & Vila Olímpia",
            vipLoungeArrival = "Bradesco Cartões Lounge / Advantage VIP • Congonhas",
            coverImageUrl = "https://images.unsplash.com/photo-1578002573559-689b0abc4148?auto=format&fit=crop&w=1200&q=85"
        ),
        "SAO" to DestinationVisualMeta(
            iataCode = "SAO",
            cityName = "São Paulo",
            countryName = "Brasil",
            tagline = "Avenida Paulista • Ponte Estaiada & Parque Ibirapuera",
            vipLoungeArrival = "LATAM VIP Lounge • Guarulhos Terminal 3",
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
        "SDU" to DestinationVisualMeta(
            iataCode = "SDU",
            cityName = "Rio de Janeiro (Santos Dumont)",
            countryName = "Brasil",
            tagline = "Baía de Guanabara • Aterro do Flamengo & Pão de Açúcar",
            vipLoungeArrival = "Advantage VIP Lounge • Santos Dumont",
            coverImageUrl = "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=85"
        ),
        "BSB" to DestinationVisualMeta(
            iataCode = "BSB",
            cityName = "Brasília",
            countryName = "Brasil",
            tagline = "Esplanada dos Ministérios • Catedral de Niemeyer & Lago Paranoá",
            vipLoungeArrival = "VIP Club Internacional • Aeroporto JK",
            coverImageUrl = "https://images.unsplash.com/photo-1598301257982-0cf014dabbcd?auto=format&fit=crop&w=1200&q=85"
        ),
        "CNF" to DestinationVisualMeta(
            iataCode = "CNF",
            cityName = "Belo Horizonte",
            countryName = "Brasil",
            tagline = "Praça da Liberdade • Lagoa da Pampulha & Serra do Curral",
            vipLoungeArrival = "Ambaar Lounge Internacional • Confins",
            coverImageUrl = "https://images.unsplash.com/photo-1578002573559-689b0abc4148?auto=format&fit=crop&w=1200&q=85"
        ),
        "SSA" to DestinationVisualMeta(
            iataCode = "SSA",
            cityName = "Salvador",
            countryName = "Brasil",
            tagline = "Pelourinho • Elevador Lacerda & Farol da Barra",
            vipLoungeArrival = "Ambaar Club Lounge • Aeroporto de Salvador",
            coverImageUrl = "https://images.unsplash.com/photo-1555881400-74d7acaacd8b?auto=format&fit=crop&w=1200&q=85"
        ),
        "REC" to DestinationVisualMeta(
            iataCode = "REC",
            cityName = "Recife",
            countryName = "Brasil",
            tagline = "Marco Zero • Recife Antigo, Olinda & Porto de Galinhas",
            vipLoungeArrival = "W Premium Lounge • Aeroporto dos Guararapes",
            coverImageUrl = "https://images.unsplash.com/photo-1516306580123-e6e52b1b7b5f?auto=format&fit=crop&w=1200&q=85"
        ),
        "FOR" to DestinationVisualMeta(
            iataCode = "FOR",
            cityName = "Fortaleza",
            countryName = "Brasil",
            tagline = "Beira Mar • Praia do Futuro, Canoa Quebrada & Jericoacoara",
            vipLoungeArrival = "Ambaar Lounge • Aeroporto Pinto Martins",
            coverImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=85"
        ),
        "POA" to DestinationVisualMeta(
            iataCode = "POA",
            cityName = "Porto Alegre",
            countryName = "Brasil",
            tagline = "Orla do Guaíba • Serra Gaúcha & Gramado",
            vipLoungeArrival = "Ambaar Club • Salgado Filho",
            coverImageUrl = "https://images.unsplash.com/photo-1543059080-f9b1272213d5?auto=format&fit=crop&w=1200&q=85"
        ),
        "CWB" to DestinationVisualMeta(
            iataCode = "CWB",
            cityName = "Curitiba",
            countryName = "Brasil",
            tagline = "Jardim Botânico • Ópera de Arame & Parque Barigui",
            vipLoungeArrival = "Advantage VIP Lounge • Afonso Pena",
            coverImageUrl = "https://images.unsplash.com/photo-1578002573559-689b0abc4148?auto=format&fit=crop&w=1200&q=85"
        ),
        "FLN" to DestinationVisualMeta(
            iataCode = "FLN",
            cityName = "Florianópolis",
            countryName = "Brasil",
            tagline = "Ilha da Magia • Jurerê Internacional & Lagoa da Conceição",
            vipLoungeArrival = "The Lounge Florianópolis • Floripa Airport",
            coverImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=85"
        ),
        "JFK" to DestinationVisualMeta(
            iataCode = "JFK",
            cityName = "Nova Iorque",
            countryName = "Estados Unidos",
            tagline = "Manhattan Skyline • Fifth Avenue & Central Park",
            vipLoungeArrival = "Chelsea & Soho First Lounge • JFK Terminal 8",
            coverImageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=85"
        ),
        "EWR" to DestinationVisualMeta(
            iataCode = "EWR",
            cityName = "Nova Iorque (Newark)",
            countryName = "Estados Unidos",
            tagline = "Manhattan Skyline • Hudson Yards & Times Square",
            vipLoungeArrival = "United Polaris Lounge • Newark Terminal C",
            coverImageUrl = "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=85"
        ),
        "MIA" to DestinationVisualMeta(
            iataCode = "MIA",
            cityName = "Miami",
            countryName = "Estados Unidos",
            tagline = "South Beach • Biscayne Bay & Brickell Skyline",
            vipLoungeArrival = "Flagship First Dining • Miami Concourse D",
            coverImageUrl = "https://images.unsplash.com/photo-1535498730771-e735b998cd64?auto=format&fit=crop&w=1200&q=85"
        ),
        "MCO" to DestinationVisualMeta(
            iataCode = "MCO",
            cityName = "Orlando",
            countryName = "Estados Unidos",
            tagline = "Universal & Walt Disney World • Lake Eola Skyline",
            vipLoungeArrival = "Plaza Premium Lounge • Orlando Terminal C",
            coverImageUrl = "https://images.unsplash.com/photo-1575089776834-8be34696ffb9?auto=format&fit=crop&w=1200&q=85"
        ),
        "LAX" to DestinationVisualMeta(
            iataCode = "LAX",
            cityName = "Los Angeles",
            countryName = "Estados Unidos",
            tagline = "Beverly Hills • Santa Monica Pier & Hollywood Hills",
            vipLoungeArrival = "Star Alliance First Class Lounge • Tom Bradley Terminal",
            coverImageUrl = "https://images.unsplash.com/photo-1534190760961-74e8c1c5c3da?auto=format&fit=crop&w=1200&q=85"
        ),
        "SFO" to DestinationVisualMeta(
            iataCode = "SFO",
            cityName = "São Francisco",
            countryName = "Estados Unidos",
            tagline = "Golden Gate Bridge • Fisherman's Wharf & Silicon Valley",
            vipLoungeArrival = "United Polaris Lounge • SFO International Terminal",
            coverImageUrl = "https://images.unsplash.com/photo-1501594907352-04cda38ebc29?auto=format&fit=crop&w=1200&q=85"
        ),
        "ORD" to DestinationVisualMeta(
            iataCode = "ORD",
            cityName = "Chicago",
            countryName = "Estados Unidos",
            tagline = "Millennium Park • Magnificent Mile & Lake Michigan",
            vipLoungeArrival = "American Flagship Lounge • O'Hare Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1494522855154-9297ac14b55f?auto=format&fit=crop&w=1200&q=85"
        ),
        "YYZ" to DestinationVisualMeta(
            iataCode = "YYZ",
            cityName = "Toronto",
            countryName = "Canadá",
            tagline = "CN Tower • Harbourfront & Financial District",
            vipLoungeArrival = "Air Canada Signature Suite • Pearson Terminal 1",
            coverImageUrl = "https://images.unsplash.com/photo-1517090504586-fde19ea6066f?auto=format&fit=crop&w=1200&q=85"
        ),
        "CUN" to DestinationVisualMeta(
            iataCode = "CUN",
            cityName = "Cancún",
            countryName = "México",
            tagline = "Riviera Maya • Mar do Caribe, Tulum & Isla Mujeres",
            vipLoungeArrival = "MERA Business Lounge • Cancún Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1552074284-5e88ef1aef18?auto=format&fit=crop&w=1200&q=85"
        ),
        "EZE" to DestinationVisualMeta(
            iataCode = "EZE",
            cityName = "Buenos Aires",
            countryName = "Argentina",
            tagline = "Puerto Madero • Palermo Soho, Recoleta & Teatro Colón",
            vipLoungeArrival = "Star Alliance Lounge • Ezeiza Terminal B",
            coverImageUrl = "https://images.unsplash.com/photo-1589909202802-8f4aadce1849?auto=format&fit=crop&w=1200&q=85"
        ),
        "SCL" to DestinationVisualMeta(
            iataCode = "SCL",
            cityName = "Santiago",
            countryName = "Chile",
            tagline = "Cordilheira dos Andes • Providencia, Las Condes & Valle Nevado",
            vipLoungeArrival = "LATAM Signature Lounge • Nuevo Pudahuel",
            coverImageUrl = "https://images.unsplash.com/photo-1518105779142-d975f22f1b0a?auto=format&fit=crop&w=1200&q=85"
        ),
        "BOG" to DestinationVisualMeta(
            iataCode = "BOG",
            cityName = "Bogotá",
            countryName = "Colômbia",
            tagline = "Cerro de Monserrate • Zona T & La Candelaria",
            vipLoungeArrival = "Avianca Diamond Lounge • El Dorado",
            coverImageUrl = "https://images.unsplash.com/photo-1568632234157-ce7aecd03d0d?auto=format&fit=crop&w=1200&q=85"
        ),
        "LIM" to DestinationVisualMeta(
            iataCode = "LIM",
            cityName = "Lima",
            countryName = "Peru",
            tagline = "Miraflores • Costa Verde, Barranco & Gastronomia Nikkei",
            vipLoungeArrival = "Hanaq VIP Lounge • Jorge Chávez",
            coverImageUrl = "https://images.unsplash.com/photo-1531968455001-5c5272a41129?auto=format&fit=crop&w=1200&q=85"
        ),
        "HND" to DestinationVisualMeta(
            iataCode = "HND",
            cityName = "Tóquio",
            countryName = "Japão",
            tagline = "Shibuya Crossing • Torre de Tóquio, Ginza & Monte Fuji",
            vipLoungeArrival = "ANA Suite Lounge • Haneda Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1200&q=85"
        ),
        "NRT" to DestinationVisualMeta(
            iataCode = "NRT",
            cityName = "Tóquio (Narita)",
            countryName = "Japão",
            tagline = "Shinjuku Skyline • Templo Senso-ji & Baía de Tóquio",
            vipLoungeArrival = "JAL First Class Lounge • Narita Terminal 2",
            coverImageUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1200&q=85"
        ),
        "DOH" to DestinationVisualMeta(
            iataCode = "DOH",
            cityName = "Doha",
            countryName = "Catar",
            tagline = "West Bay Skyline • The Pearl-Qatar & Souq Waqif",
            vipLoungeArrival = "Al Mourjan Business Lounge • Hamad International",
            coverImageUrl = "https://images.unsplash.com/photo-1578895101408-1a36b834405b?auto=format&fit=crop&w=1200&q=85"
        ),
        "IST" to DestinationVisualMeta(
            iataCode = "IST",
            cityName = "Istambul",
            countryName = "Turquia",
            tagline = "Estreito de Bósforo • Hagia Sophia, Galata & Grande Bazar",
            vipLoungeArrival = "Turkish Airlines Business Lounge • Istanbul Airport",
            coverImageUrl = "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200?auto=format&fit=crop&w=1200&q=85"
        ),
        "SIN" to DestinationVisualMeta(
            iataCode = "SIN",
            cityName = "Singapura",
            countryName = "Singapura",
            tagline = "Marina Bay Sands • Gardens by the Bay & Jewel Changi",
            vipLoungeArrival = "SilverKris First Class Lounge • Changi Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=1200&q=85"
        ),
        "BKK" to DestinationVisualMeta(
            iataCode = "BKK",
            cityName = "Banguecoque",
            countryName = "Tailândia",
            tagline = "Rio Chao Phraya • Grande Palácio, Wat Arun & Rooftops",
            vipLoungeArrival = "Royal Orchid Prestige Lounge • Suvarnabhumi",
            coverImageUrl = "https://images.unsplash.com/photo-1508009603885-50cf7c579365?auto=format&fit=crop&w=1200&q=85"
        ),
        "MLE" to DestinationVisualMeta(
            iataCode = "MLE",
            cityName = "Maldivas (Malé)",
            countryName = "Maldivas",
            tagline = "Atol de Baa & Malé • Bangalôs Sobre as Águas Turquesas",
            vipLoungeArrival = "Leeli Lounge • Velana International",
            coverImageUrl = "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=85"
        ),
        "DPS" to DestinationVisualMeta(
            iataCode = "DPS",
            cityName = "Bali",
            countryName = "Indonésia",
            tagline = "Uluwatu & Ubud • Terraços de Arroz & Templos Oceânicos",
            vipLoungeArrival = "Concordia Lounge • Ngurah Rai International",
            coverImageUrl = "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=85"
        ),
        "RAK" to DestinationVisualMeta(
            iataCode = "RAK",
            cityName = "Marraquexe",
            countryName = "Marrocos",
            tagline = "Jardim Majorelle • Medina Histórica, Palácios & Atlas",
            vipLoungeArrival = "Pearl Lounge • Marrakech Menara",
            coverImageUrl = "https://images.unsplash.com/photo-1539020140153-e479b8c22e70?auto=format&fit=crop&w=1200&q=85"
        ),
        "CPT" to DestinationVisualMeta(
            iataCode = "CPT",
            cityName = "Cidade do Cabo",
            countryName = "África do Sul",
            tagline = "Table Mountain • V&A Waterfront, Camps Bay & Cabo da Boa Esperança",
            vipLoungeArrival = "Bidvest Premier Lounge • Cape Town International",
            coverImageUrl = "https://images.unsplash.com/photo-1580060839134-75a5edca2e99?auto=format&fit=crop&w=1200&q=85"
        ),
        "CAI" to DestinationVisualMeta(
            iataCode = "CAI",
            cityName = "Cairo",
            countryName = "Egito",
            tagline = "Pirâmides de Gizé • Rio Nilo & Grande Museu Egípcio",
            vipLoungeArrival = "EgyptAir Alioth Lounge • Cairo Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1572252009286-268acec5ca0a?auto=format&fit=crop&w=1200&q=85"
        ),
        "SYD" to DestinationVisualMeta(
            iataCode = "SYD",
            cityName = "Sydney",
            countryName = "Austrália",
            tagline = "Sydney Opera House • Harbour Bridge & Bondi Beach",
            vipLoungeArrival = "Qantas International First Lounge • Kingsford Smith",
            coverImageUrl = "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1200&q=85"
        ),
        "AGP" to DestinationVisualMeta(
            iataCode = "AGP",
            cityName = "Málaga",
            countryName = "Espanha",
            tagline = "Costa del Sol • Marbella, Alcazaba & Muelle Uno",
            vipLoungeArrival = "Sala VIP Costa del Sol • Terminal 3",
            coverImageUrl = "https://images.unsplash.com/photo-1561632669-7f55f7975606?auto=format&fit=crop&w=1200&q=85"
        ),
        "SVQ" to DestinationVisualMeta(
            iataCode = "SVQ",
            cityName = "Sevilha",
            countryName = "Espanha",
            tagline = "Plaza de España • Real Alcázar, Giralda & Triana",
            vipLoungeArrival = "Sala VIP Azahar • Aeroporto de Sevilha",
            coverImageUrl = "https://images.unsplash.com/photo-1558642084-fd07fae5282e?auto=format&fit=crop&w=1200&q=85"
        ),
        "VLC" to DestinationVisualMeta(
            iataCode = "VLC",
            cityName = "Valência",
            countryName = "Espanha",
            tagline = "Cidade das Artes e das Ciências • Praia da Malvarrosa",
            vipLoungeArrival = "Sala VIP Joan Olivert • Aeroporto de Valência",
            coverImageUrl = "https://images.unsplash.com/photo-1599940824399-b87987ceb72a?auto=format&fit=crop&w=1200&q=85"
        ),
        "TFS" to DestinationVisualMeta(
            iataCode = "TFS",
            cityName = "Tenerife",
            countryName = "Espanha",
            tagline = "Ilhas Canárias • Costa Adeje & Parque Nacional do Teide",
            vipLoungeArrival = "Sala VIP Montaña Roja • Tenerife Sul",
            coverImageUrl = "https://images.unsplash.com/photo-1544644181-1484b3fdfc62?auto=format&fit=crop&w=1200&q=85"
        )
    )

    private val countryCoverFallbacks: Map<String, String> = mapOf(
        "Brasil" to "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?auto=format&fit=crop&w=1200&q=85",
        "Portugal" to "https://images.unsplash.com/photo-1585208798174-6cedd86e019a?auto=format&fit=crop&w=1200&q=85",
        "Espanha" to "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?auto=format&fit=crop&w=1200&q=85",
        "França" to "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=85",
        "Itália" to "https://images.unsplash.com/photo-1552832230-c0197dd311b5?auto=format&fit=crop&w=1200&q=85",
        "Reino Unido" to "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?auto=format&fit=crop&w=1200&q=85",
        "Alemanha" to "https://images.unsplash.com/photo-1560969184-10fe8719e047?auto=format&fit=crop&w=1200&q=85",
        "Suíça" to "https://images.unsplash.com/photo-1515488764276-beab7607c1e6?auto=format&fit=crop&w=1200&q=85",
        "Países Baixos" to "https://images.unsplash.com/photo-1534351590666-13e3e96b5017?auto=format&fit=crop&w=1200&q=85",
        "Grécia" to "https://images.unsplash.com/photo-1603565816030-6b389eeb23cb?auto=format&fit=crop&w=1200&q=85",
        "Áustria" to "https://images.unsplash.com/photo-1516550893923-42d28e5677af?auto=format&fit=crop&w=1200&q=85",
        "Estados Unidos" to "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=85",
        "Canadá" to "https://images.unsplash.com/photo-1517090504586-fde19ea6066f?auto=format&fit=crop&w=1200&q=85",
        "México" to "https://images.unsplash.com/photo-1552074284-5e88ef1aef18?auto=format&fit=crop&w=1200&q=85",
        "Argentina" to "https://images.unsplash.com/photo-1589909202802-8f4aadce1849?auto=format&fit=crop&w=1200&q=85",
        "Chile" to "https://images.unsplash.com/photo-1518105779142-d975f22f1b0a?auto=format&fit=crop&w=1200&q=85",
        "Japão" to "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?auto=format&fit=crop&w=1200&q=85",
        "Emirados Árabes Unidos" to "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=1200&q=85",
        "Catar" to "https://images.unsplash.com/photo-1578895101408-1a36b834405b?auto=format&fit=crop&w=1200&q=85",
        "Tailândia" to "https://images.unsplash.com/photo-1508009603885-50cf7c579365?auto=format&fit=crop&w=1200&q=85",
        "Singapura" to "https://images.unsplash.com/photo-1525625293386-3f8f99389edd?auto=format&fit=crop&w=1200&q=85",
        "Maldivas" to "https://images.unsplash.com/photo-1514282401047-d79a71a590e8?auto=format&fit=crop&w=1200&q=85",
        "Indonésia" to "https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1200&q=85",
        "Marrocos" to "https://images.unsplash.com/photo-1539020140153-e479b8c22e70?auto=format&fit=crop&w=1200&q=85",
        "África do Sul" to "https://images.unsplash.com/photo-1580060839134-75a5edca2e99?auto=format&fit=crop&w=1200&q=85",
        "Austrália" to "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?auto=format&fit=crop&w=1200&q=85"
    )

    fun resolveDestination(iataCode: String, fallbackCity: String): DestinationVisualMeta {
        val code = iataCode.uppercase().trim()
        destinations[code]?.let { return it }

        val placeCatalogEntry = com.example.data.model.PlaceAutocompleteCatalog.resolveByIata(code)
        val effectiveCity = fallbackCity.ifBlank { placeCatalogEntry.cityName }
        val cleanTokens = effectiveCity.lowercase()
            .split("/", "-", ",", "(", ")")
            .map { it.trim() }
            .filter { it.length >= 3 }

        destinations.values.firstOrNull { meta ->
            val metaCityLower = meta.cityName.lowercase()
            cleanTokens.any { token ->
                metaCityLower.contains(token) || token.contains(metaCityLower.substringBefore(" "))
            }
        }?.let {
            return it.copy(
                iataCode = code.ifBlank { it.iataCode },
                cityName = effectiveCity.ifBlank { it.cityName }
            )
        }

        val resolvedCountry = inferCountryFromIataOrCity(code, effectiveCity)
            .let { if (it == "Destino Internacional" && placeCatalogEntry.countryName != "Internacional") placeCatalogEntry.countryName else it }
        val countryPhoto = countryCoverFallbacks[resolvedCountry]
            ?: "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?auto=format&fit=crop&w=1200&q=85"

        return DestinationVisualMeta(
            iataCode = code.ifBlank { "FLY" },
            cityName = effectiveCity.ifBlank { code },
            countryName = resolvedCountry,
            tagline = "${effectiveCity.ifBlank { code }} • $resolvedCountry ($code)",
            vipLoungeArrival = "AguiaFly Executive Partner Lounge • Aeroporto $code",
            coverImageUrl = countryPhoto
        )
    }

    fun isDynamicLookupNeeded(iataCode: String, fallbackCity: String): Boolean {
        val code = iataCode.uppercase().trim()
        if (destinations.containsKey(code)) return false
        val cleanTokens = fallbackCity.lowercase()
            .split("/", "-", ",", "(", ")")
            .map { it.trim() }
            .filter { it.length >= 3 }
        return destinations.values.none { meta ->
            val metaCityLower = meta.cityName.lowercase()
            cleanTokens.any { token ->
                metaCityLower.contains(token) || token.contains(metaCityLower.substringBefore(" "))
            }
        }
    }

    private fun inferCountryFromIataOrCity(iata: String, city: String): String {
        val c = city.lowercase()
        return when {
            "porto" in c || "lisbo" in c || "faro" in c || "funchal" in c || "portugal" in c -> "Portugal"
            "madrid" in c || "barcelon" in c || "ibiza" in c || "valencia" in c || "sevil" in c || "malaga" in c || "tenerife" in c || "maiorca" in c -> "Espanha"
            "paris" in c || "nice" in c || "lyon" in c || "marsel" in c -> "França"
            "roma" in c || "rome" in c || "mila" in c || "venez" in c || "venic" in c || "floren" in c -> "Itália"
            "lond" in c || "manchester" in c || "edimburg" in c || "edinburgh" in c -> "Reino Unido"
            "frankfurt" in c || "muni" in c || "munich" in c || "berli" in c || "hamburg" in c -> "Alemanha"
            "zuri" in c || "genebr" in c || "genev" in c || "basel" in c -> "Suíça"
            "amsterd" in c || "rotterd" in c -> "Países Baixos"
            "campinas" in c || "sao paulo" in c || "são paulo" in c || "rio" in c || "brasil" in c || "recife" in c || "salvador" in c || "fortaleza" in c || "curitiba" in c || "belo horizonte" in c || "florian" in c -> "Brasil"
            "nova iorque" in c || "new york" in c || "miami" in c || "orlando" in c || "los angeles" in c || "chicago" in c -> "Estados Unidos"
            "toquio" in c || "tóquio" in c || "tokyo" in c -> "Japão"
            "dubai" in c || "abu dhabi" in c -> "Emirados Árabes Unidos"
            "doha" in c -> "Catar"
            "cancun" in c || "cancún" in c -> "México"
            "buenos aires" in c -> "Argentina"
            "santiago" in c -> "Chile"
            else -> "Destino Internacional"
        }
    }
}
