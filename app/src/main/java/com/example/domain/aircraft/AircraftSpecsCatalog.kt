package com.example.domain.aircraft

import com.example.data.model.AircraftTechnicalSpec
import com.example.data.model.BodyType
import com.example.data.model.SeatOption

object AircraftSpecsCatalog {

    private val specsByIata = mapOf(
        "359" to AircraftTechnicalSpec(
            iataCode = "359",
            icaoCode = "A359",
            fullName = "Airbus A350-900 XWB",
            manufacturer = "Airbus",
            bodyType = BodyType.WIDEBODY,
            seatPitchBusiness = "78\" (198 cm) Lie-Flat Suite",
            seatPitchEconomy = "32\" (81 cm) Ergonomic Recaro",
            cabinLayoutBusiness = "1 - 2 - 1 (Direct Aisle Access)",
            cabinLayoutEconomy = "3 - 3 - 3",
            wifiSpecification = "Ka-Band High-Speed Satellite Wi-Fi (150 Mbps)",
            powerOutlets = "110V AC Universal + USB-C 60W PD + Wireless Pad",
            ifeDisplay = "21.5\" 4K OLED Touch + Bluetooth Audio",
            cabinPressureAltitudeFt = 5800,
            cruiseSpeedMach = "Mach 0.85 (905 km/h)",
            rangeKm = 15000,
            cabinNoiseDb = 57,
            sampleRegistration = "F-HTYA",
            exteriorPhotoUrl = "https://images.unsplash.com/photo-1540339832862-474599807836?auto=format&fit=crop&w=1400&q=85",
            cabinPhotoUrl = "https://images.unsplash.com/photo-1583202075376-8c75d97b0428?auto=format&fit=crop&w=1200&q=85"
        ),
        "789" to AircraftTechnicalSpec(
            iataCode = "789",
            icaoCode = "B789",
            fullName = "Boeing 787-9 Dreamliner",
            manufacturer = "Boeing",
            bodyType = BodyType.WIDEBODY,
            seatPitchBusiness = "76\" (193 cm) Reverse Herringbone",
            seatPitchEconomy = "31\" (79 cm) Comfort Slimline",
            cabinLayoutBusiness = "1 - 2 - 1 (Private Pod)",
            cabinLayoutEconomy = "3 - 3 - 3",
            wifiSpecification = "Panasonic Ku-Band Global Wi-Fi",
            powerOutlets = "110V AC + Dual USB-C / USB-A",
            ifeDisplay = "18.5\" Full HD Capacitive Screen",
            cabinPressureAltitudeFt = 6000,
            cruiseSpeedMach = "Mach 0.85 (903 km/h)",
            rangeKm = 14140,
            cabinNoiseDb = 59,
            sampleRegistration = "PH-BHA",
            exteriorPhotoUrl = "https://images.unsplash.com/photo-1569154941061-e231b4725ef1?auto=format&fit=crop&w=1400&q=85",
            cabinPhotoUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=85"
        ),
        "339" to AircraftTechnicalSpec(
            iataCode = "339",
            icaoCode = "A339",
            fullName = "Airbus A330-900neo",
            manufacturer = "Airbus",
            bodyType = BodyType.WIDEBODY,
            seatPitchBusiness = "75\" (190 cm) Airspace Lie-Flat",
            seatPitchEconomy = "31\" (79 cm) Airspace Cabin",
            cabinLayoutBusiness = "1 - 2 - 1",
            cabinLayoutEconomy = "2 - 4 - 2 (Couple Friendly)",
            wifiSpecification = "Inmarsat GX Aviation High-Speed Wi-Fi",
            powerOutlets = "110V AC + USB-C Fast Charge",
            ifeDisplay = "17.3\" 4K HDR Display",
            cabinPressureAltitudeFt = 6200,
            cruiseSpeedMach = "Mach 0.82 (871 km/h)",
            rangeKm = 13334,
            cabinNoiseDb = 60,
            sampleRegistration = "CS-TUA",
            exteriorPhotoUrl = "https://images.unsplash.com/photo-1436491865332-7a61a109cc05?auto=format&fit=crop&w=1400&q=85",
            cabinPhotoUrl = "https://images.unsplash.com/photo-1583202075376-8c75d97b0428?auto=format&fit=crop&w=1200&q=85"
        ),
        "32Q" to AircraftTechnicalSpec(
            iataCode = "32Q",
            icaoCode = "A21N",
            fullName = "Airbus A321neo Airspace",
            manufacturer = "Airbus",
            bodyType = BodyType.NARROWBODY,
            seatPitchBusiness = "34\" (86 cm) Euro-Business (Middle Blocked)",
            seatPitchEconomy = "30\" (76 cm) Collins Meridian",
            cabinLayoutBusiness = "2 - 2 (Club Europe Layout)",
            cabinLayoutEconomy = "3 - 3",
            wifiSpecification = "European Aviation Network (EAN) 4G/Satellite",
            powerOutlets = "USB-C 45W & USB-A at every seat",
            ifeDisplay = "13.3\" 4K Seatback + Personal Device Streaming",
            cabinPressureAltitudeFt = 6800,
            cruiseSpeedMach = "Mach 0.78 (833 km/h)",
            rangeKm = 7400,
            cabinNoiseDb = 62,
            sampleRegistration = "D-AIEA",
            exteriorPhotoUrl = "https://images.unsplash.com/photo-1520437358207-323b43b50729?auto=format&fit=crop&w=1400&q=85",
            cabinPhotoUrl = "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=1200&q=85"
        )
    )

    fun resolveSpec(iataCode: String?, rawName: String?): AircraftTechnicalSpec {
        if (iataCode != null && specsByIata.containsKey(iataCode.uppercase())) {
            return specsByIata.getValue(iataCode.uppercase())
        }
        val normalized = rawName.orEmpty().lowercase()
        return when {
            "350" in normalized -> specsByIata.getValue("359")
            "787" in normalized || "dreamliner" in normalized -> specsByIata.getValue("789")
            "330" in normalized -> specsByIata.getValue("339")
            else -> specsByIata.getValue("32Q")
        }
    }

    fun isWindowSeat(seatDesignator: String): Boolean {
        val col = seatDesignator.filter { it.isLetter() }.uppercase()
        return col == "A" || col == "K" || col == "F"
    }

    fun describeSeatPosition(seatDesignator: String, spec: AircraftTechnicalSpec): String {
        val col = seatDesignator.filter { it.isLetter() }.uppercase()
        return when (col) {
            "A" -> "JANELA ESQUERDA (Coluna A) • Vista Panorâmica + Acesso ao Corredor"
            "K", "F" -> "JANELA DIREITA (Coluna $col) • Vista Panorâmica + Acesso ao Corredor"
            "C", "D", "G" -> "CORREDOR CENTRAL (Coluna $col) • Acesso Direto ao Corredor"
            else -> "ASSENTO INTERMEDIÁRIO (Coluna $col) • Entre Janela e Corredor"
        }
    }

    fun generateSeatMapForSpec(
        spec: AircraftTechnicalSpec,
        cabinClass: String,
        seedKey: String = ""
    ): List<SeatOption> {
        val isBusiness = cabinClass.lowercase() in listOf("business", "first")
        val columns = if (spec.bodyType == BodyType.WIDEBODY && isBusiness) {
            listOf("A", "D", "G", "K")
        } else if (isBusiness) {
            listOf("A", "C", "D", "F")
        } else {
            listOf("A", "B", "C", "D", "E", "F")
        }
        val rows = if (isBusiness) 1..6 else 10..17

        // Geração dinâmica e determinística por voo (evita que voos diferentes tenham os mesmos assentos ocupados)
        val effectiveSeed = if (seedKey.isNotBlank()) {
            seedKey
        } else {
            "${spec.icaoCode}_${spec.sampleRegistration}_${cabinClass}"
        }

        return rows.flatMap { row ->
            columns.map { col ->
                val designator = "$row$col"
                val hash = kotlin.math.abs((effectiveSeed + "_" + designator).hashCode())
                // Ocupa ~35% dos assentos de forma realista e totalmente variável por voo
                val isOccupied = (hash % 100) < 35
                SeatOption(
                    designator = designator,
                    row = row,
                    column = col,
                    isBusinessSuite = isBusiness,
                    isWindow = col == "A" || col == "K" || col == "F",
                    isAvailable = !isOccupied,
                    extraLegroom = row == rows.first
                )
            }
        }
    }
}
