package com.example

import com.example.domain.duffel.DuffelOfferProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuffelOfferProcessorTest {

    @Test
    fun `processRawDuffelJson enforces strict direct flight filter, baggage separation, and real elapsed duration`() {
        val rawJson = """
            {
              "data": {
                "id": "orq_test_001",
                "offers": [
                  {
                    "id": "off_direct_valid",
                    "total_amount": "240.00",
                    "total_currency": "EUR",
                    "owner": { "name": "TAP Portugal", "iata_code": "TP" },
                    "slices": [
                      {
                        "duration": "PT1H00M",
                        "origin": { "iata_code": "LIS", "name": "Humberto Delgado", "city_name": "Lisboa" },
                        "destination": { "iata_code": "MAD", "name": "Barajas", "city_name": "Madrid" },
                        "segments": [
                          {
                            "id": "seg_1",
                            "departing_at": "2026-11-15T10:00:00Z",
                            "arriving_at": "2026-11-15T11:25:00Z",
                            "origin": { "iata_code": "LIS", "name": "Humberto Delgado", "city_name": "Lisboa" },
                            "destination": { "iata_code": "MAD", "name": "Barajas", "city_name": "Madrid" },
                            "marketing_carrier": { "name": "TAP Portugal", "iata_code": "TP" },
                            "marketing_carrier_flight_number": "1014",
                            "aircraft": { "iata_code": "32Q", "name": "Airbus A321neo" },
                            "passengers": [
                              {
                                "passenger_id": "pas_1",
                                "cabin_class": "business",
                                "baggages": [
                                  { "type": "carry_on", "quantity": 2 },
                                  { "type": "checked", "quantity": 1 }
                                ]
                              }
                            ]
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "id": "off_with_stopover_valid",
                    "total_amount": "195.00",
                    "total_currency": "EUR",
                    "owner": { "name": "Iberia", "iata_code": "IB" },
                    "slices": [
                      {
                        "duration": "PT1H00M",
                        "origin": { "iata_code": "LIS", "name": "Humberto Delgado", "city_name": "Lisboa" },
                        "destination": { "iata_code": "MAD", "name": "Barajas", "city_name": "Madrid" },
                        "segments": [
                          {
                            "id": "seg_2a",
                            "departing_at": "2026-11-15T08:00:00Z",
                            "arriving_at": "2026-11-15T09:00:00Z",
                            "origin": { "iata_code": "LIS", "name": "Humberto Delgado", "city_name": "Lisboa" },
                            "destination": { "iata_code": "OPO", "name": "Francisco Sá Carneiro", "city_name": "Porto" },
                            "marketing_carrier_flight_number": "3101"
                          },
                          {
                            "id": "seg_2b",
                            "departing_at": "2026-11-15T10:30:00Z",
                            "arriving_at": "2026-11-15T11:45:00Z",
                            "origin": { "iata_code": "OPO", "name": "Francisco Sá Carneiro", "city_name": "Porto" },
                            "destination": { "iata_code": "MAD", "name": "Barajas", "city_name": "Madrid" },
                            "marketing_carrier_flight_number": "3102"
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val results = DuffelOfferProcessor.processRawDuffelJson(
            rawJson = rawJson,
            requestedOriginIata = "LIS",
            requestedDestinationIata = "MAD"
        )

        assertEquals(2, results.size)

        val directOffer = results.first { it.offerId == "off_direct_valid" }
        assertTrue(directOffer.isDirectFlight)
        assertTrue(directOffer.nonStop)
        assertEquals("Direto", directOffer.flightTypeClassification)
        assertEquals(0, directOffer.stopsCount)
        assertEquals(1, directOffer.segmentLegs.size)
        assertEquals("business", directOffer.segmentLegs.first().cabinClass)
        assertEquals("2x Bagagem de Mão", directOffer.carryOnBaggageAllowance)
        assertEquals("1x Bagagem Despachada", directOffer.checkedBaggageAllowance)
        // Duração real = 11:25 - 10:00 = 85 minutos
        assertEquals(85, directOffer.durationMinutes)
        assertEquals("1 h 25 min", directOffer.durationFormatted)

        val stopoverOffer = results.first { it.offerId == "off_with_stopover_valid" }
        assertFalse(stopoverOffer.isDirectFlight)
        assertFalse(stopoverOffer.nonStop)
        assertEquals("Com Escala", stopoverOffer.flightTypeClassification)
        assertEquals(1, stopoverOffer.stopsCount)
        assertEquals(2, stopoverOffer.segmentLegs.size)
        // Sem baggages explícito -> padrão "Não Incluída"
        assertEquals("Não Incluída", stopoverOffer.carryOnBaggageAllowance)
        assertEquals("Não Incluída", stopoverOffer.checkedBaggageAllowance)
        // Duração total real = 11:45 - 08:00 = 225 min (60m voo 1 + 90m layover + 75m voo 2)
        assertEquals(225, stopoverOffer.durationMinutes)
        assertEquals(90, stopoverOffer.totalConnectionMinutes)
        assertEquals("3 h 45 min", stopoverOffer.durationFormatted)
    }

    @Test
    fun `processRawDuffelJson detects airport_change in same city and critical layover alerts`() {
        val rawJson = """
            {
              "data": {
                "id": "orq_test_airport_change",
                "offers": [
                  {
                    "id": "off_lhr_to_lgw_short_layover",
                    "total_amount": "310.00",
                    "total_currency": "EUR",
                    "owner": { "name": "British Airways", "iata_code": "BA" },
                    "slices": [
                      {
                        "origin": { "iata_code": "LIS", "name": "Lisboa" },
                        "destination": { "iata_code": "JFK", "name": "New York JFK" },
                        "segments": [
                          {
                            "id": "s1",
                            "departing_at": "2026-11-15T07:00:00Z",
                            "arriving_at": "2026-11-15T09:40:00Z",
                            "origin": { "iata_code": "LIS", "name": "Lisboa" },
                            "destination": { "iata_code": "LHR", "name": "London Heathrow", "city_name": "London" }
                          },
                          {
                            "id": "s2",
                            "departing_at": "2026-11-15T10:15:00Z",
                            "arriving_at": "2026-11-15T18:05:00Z",
                            "origin": { "iata_code": "LGW", "name": "London Gatwick", "city_name": "London" },
                            "destination": { "iata_code": "JFK", "name": "New York JFK" }
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val results = DuffelOfferProcessor.processRawDuffelJson(
            rawJson = rawJson,
            requestedOriginIata = "LIS",
            requestedDestinationIata = "JFK"
        )

        assertEquals(1, results.size)
        val offer = results.first()
        // LHR -> LGW na mesma cidade (Londres) deve ativar airportChange = true
        assertTrue(offer.airportChange)
        assertTrue(offer.segmentLegs.first().airportChangeAfterSegment)
        assertEquals("LGW", offer.segmentLegs.first().nextDepartureAirportIata)
        // Layover de 35 min (< 45 min) deve disparar Alerta de Escala Crítica
        assertEquals(35, offer.totalConnectionMinutes)
        assertTrue(offer.hasCriticalLayoverAlert)
        assertNotNull(offer.criticalLayoverAlertSummary)
    }

    @Test
    fun `processRawDuffelJson discards corrupted offers and impossible connections`() {
        val rawJson = """
            {
              "data": {
                "id": "orq_test_corrupted",
                "offers": [
                  {
                    "id": "off_negative_layover",
                    "total_amount": "150.00",
                    "total_currency": "EUR",
                    "owner": { "name": "TAP Portugal", "iata_code": "TP" },
                    "slices": [
                      {
                        "origin": { "iata_code": "LIS", "name": "Lisboa" },
                        "destination": { "iata_code": "CDG", "name": "Paris" },
                        "segments": [
                          {
                            "id": "s1",
                            "departing_at": "2026-11-15T10:00:00Z",
                            "arriving_at": "2026-11-15T12:00:00Z",
                            "origin": { "iata_code": "LIS", "name": "Lisboa" },
                            "destination": { "iata_code": "MAD", "name": "Madrid" }
                          },
                          {
                            "id": "s2",
                            "departing_at": "2026-11-15T11:30:00Z",
                            "arriving_at": "2026-11-15T13:30:00Z",
                            "origin": { "iata_code": "MAD", "name": "Madrid" },
                            "destination": { "iata_code": "CDG", "name": "Paris" }
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "id": "off_different_city_airport_break",
                    "total_amount": "180.00",
                    "total_currency": "EUR",
                    "owner": { "name": "TAP Portugal", "iata_code": "TP" },
                    "slices": [
                      {
                        "origin": { "iata_code": "LIS", "name": "Lisboa" },
                        "destination": { "iata_code": "CDG", "name": "Paris" },
                        "segments": [
                          {
                            "id": "s1",
                            "departing_at": "2026-11-15T10:00:00Z",
                            "arriving_at": "2026-11-15T12:00:00Z",
                            "origin": { "iata_code": "LIS", "name": "Lisboa" },
                            "destination": { "iata_code": "MAD", "name": "Madrid", "city_name": "Madrid" }
                          },
                          {
                            "id": "s2",
                            "departing_at": "2026-11-15T14:00:00Z",
                            "arriving_at": "2026-11-15T16:00:00Z",
                            "origin": { "iata_code": "BCN", "name": "Barcelona", "city_name": "Barcelona" },
                            "destination": { "iata_code": "CDG", "name": "Paris" }
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "id": "off_missing_carrier",
                    "total_amount": "120.00",
                    "total_currency": "EUR",
                    "owner": { "name": "", "iata_code": null },
                    "slices": [
                      {
                        "origin": { "iata_code": "LIS", "name": "Lisboa" },
                        "destination": { "iata_code": "CDG", "name": "Paris" },
                        "segments": [
                          {
                            "id": "s1",
                            "departing_at": "2026-11-15T10:00:00Z",
                            "arriving_at": "2026-11-15T12:15:00Z",
                            "origin": { "iata_code": "LIS", "name": "Lisboa" },
                            "destination": { "iata_code": "CDG", "name": "Paris" }
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "id": "off_corrupted_price",
                    "total_amount": "-45.00",
                    "total_currency": "EUR",
                    "owner": { "name": "Vueling", "iata_code": "VY" },
                    "slices": []
                  }
                ]
              }
            }
        """.trimIndent()

        val results = DuffelOfferProcessor.processRawDuffelJson(
            rawJson = rawJson,
            requestedOriginIata = "LIS",
            requestedDestinationIata = "CDG"
        )

        // Todas as 4 ofertas corrompidas/inconsistentes devem ser descartadas silenciosamente!
        assertTrue(results.isEmpty())
    }
}
