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

    @Test
    fun `single segment on route without commercial direct flights (IBZ to OPO) is strictly reclassified as Com Escala`() {
        // Simula oferta retornada pela Duffel para a rota Ibiza (IBZ) -> Porto (OPO)
        // com apenas 1 segmento atribuído a Air France (AF) ou TAP (TP)
        val rawJson = """
            {
              "data": {
                "id": "orq_ibz_opo_test",
                "offers": [
                  {
                    "id": "off_af_ibz_opo",
                    "total_amount": "148.00",
                    "total_currency": "EUR",
                    "owner": { "name": "Air France", "iata_code": "AF" },
                    "slices": [
                      {
                        "duration": "PT2H20M",
                        "origin": { "iata_code": "IBZ", "name": "Ibiza", "city_name": "Ibiza" },
                        "destination": { "iata_code": "OPO", "name": "Francisco Sá Carneiro", "city_name": "Porto" },
                        "segments": [
                          {
                            "id": "seg_af_1",
                            "departing_at": "2026-11-15T18:30:00Z",
                            "arriving_at": "2026-11-15T20:50:00Z",
                            "origin": { "iata_code": "IBZ", "name": "Ibiza", "city_name": "Ibiza" },
                            "destination": { "iata_code": "OPO", "name": "Francisco Sá Carneiro", "city_name": "Porto" },
                            "marketing_carrier": { "name": "Air France", "iata_code": "AF" },
                            "marketing_carrier_flight_number": "359"
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "id": "off_tp_ibz_opo",
                    "total_amount": "112.00",
                    "total_currency": "EUR",
                    "owner": { "name": "TAP Portugal", "iata_code": "TP" },
                    "slices": [
                      {
                        "duration": "PT2H15M",
                        "origin": { "iata_code": "IBZ", "name": "Ibiza", "city_name": "Ibiza" },
                        "destination": { "iata_code": "OPO", "name": "Francisco Sá Carneiro", "city_name": "Porto" },
                        "segments": [
                          {
                            "id": "seg_tp_1",
                            "departing_at": "2026-11-15T16:55:00Z",
                            "arriving_at": "2026-11-15T19:10:00Z",
                            "origin": { "iata_code": "IBZ", "name": "Ibiza", "city_name": "Ibiza" },
                            "destination": { "iata_code": "OPO", "name": "Francisco Sá Carneiro", "city_name": "Porto" },
                            "marketing_carrier": { "name": "TAP Portugal", "iata_code": "TP" },
                            "marketing_carrier_flight_number": "339"
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
            requestedOriginIata = "IBZ",
            requestedDestinationIata = "OPO"
        )

        assertEquals(2, results.size)

        val afOffer = results.first { it.offerId == "off_af_ibz_opo" }
        assertFalse(afOffer.isDirectFlight)
        assertFalse(afOffer.nonStop)
        assertEquals("Com Escala", afOffer.flightTypeClassification)
        assertEquals(1, afOffer.stopsCount)
        assertTrue(afOffer.stopoverHubsSummary.contains("CDG") || afOffer.stopoverHubsSummary.contains("Paris"))
        assertEquals(2, afOffer.segmentLegs.size)

        val tpOffer = results.first { it.offerId == "off_tp_ibz_opo" }
        assertFalse(tpOffer.isDirectFlight)
        assertFalse(tpOffer.nonStop)
        assertEquals("Com Escala", tpOffer.flightTypeClassification)
        assertEquals(1, tpOffer.stopsCount)
        assertTrue(tpOffer.stopoverHubsSummary.contains("LIS") || tpOffer.stopoverHubsSummary.contains("Lisboa"))
        assertEquals(2, tpOffer.segmentLegs.size)
    }

    @Test
    fun `single segment with mismatched origin or destination is discarded or reclassified`() {
        val rawJson = """
            {
              "data": {
                "id": "orq_mismatch_test",
                "offers": [
                  {
                    "id": "off_mismatched_dest",
                    "total_amount": "150.00",
                    "total_currency": "EUR",
                    "owner": { "name": "TAP Portugal", "iata_code": "TP" },
                    "slices": [
                      {
                        "origin": { "iata_code": "IBZ", "name": "Ibiza" },
                        "destination": { "iata_code": "MAD", "name": "Madrid" },
                        "segments": [
                          {
                            "id": "seg_mismatch",
                            "departing_at": "2026-11-15T10:00:00Z",
                            "arriving_at": "2026-11-15T11:30:00Z",
                            "origin": { "iata_code": "IBZ", "name": "Ibiza" },
                            "destination": { "iata_code": "MAD", "name": "Madrid" }
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        // O usuário pesquisou IBZ -> OPO, mas o segmento é IBZ -> MAD
        val results = DuffelOfferProcessor.processRawDuffelJson(
            rawJson = rawJson,
            requestedOriginIata = "IBZ",
            requestedDestinationIata = "OPO"
        )

        // Segmento divergente da busca final solicitada é descartado por conter dados truncados/incompatíveis
        assertTrue(results.isEmpty())
    }

    @Test
    fun `anti-SAO geographic error trapping blocks low-cost airlines and impossible short durations on intercontinental routes`() {
        val rawJson = """
            {
              "data": {
                "id": "orq_sao_test",
                "offers": [
                  {
                    "id": "off_ryanair_absurd_sao",
                    "total_amount": "45.00",
                    "total_currency": "EUR",
                    "owner": { "name": "Ryanair", "iata_code": "FR" },
                    "slices": [
                      {
                        "origin": { "iata_code": "IBZ", "name": "Ibiza" },
                        "destination": { "iata_code": "GRU", "name": "Guarulhos", "city_name": "São Paulo" },
                        "segments": [
                          {
                            "id": "seg_fr_absurd",
                            "departing_at": "2026-11-15T06:30:00Z",
                            "arriving_at": "2026-11-15T08:10:00Z",
                            "origin": { "iata_code": "IBZ", "name": "Ibiza" },
                            "destination": { "iata_code": "GRU", "name": "Guarulhos" },
                            "marketing_carrier": { "name": "Ryanair", "iata_code": "FR" }
                          }
                        ]
                      }
                    ]
                  },
                  {
                    "id": "off_iberia_legit_sao",
                    "total_amount": "480.00",
                    "total_currency": "EUR",
                    "owner": { "name": "Iberia", "iata_code": "IB" },
                    "slices": [
                      {
                        "origin": { "iata_code": "IBZ", "name": "Ibiza" },
                        "destination": { "iata_code": "GRU", "name": "Guarulhos", "city_name": "São Paulo" },
                        "segments": [
                          {
                            "id": "seg_ib_feeder",
                            "departing_at": "2026-11-15T10:00:00Z",
                            "arriving_at": "2026-11-15T11:15:00Z",
                            "origin": { "iata_code": "IBZ", "name": "Ibiza" },
                            "destination": { "iata_code": "MAD", "name": "Barajas" },
                            "marketing_carrier": { "name": "Iberia", "iata_code": "IB" }
                          },
                          {
                            "id": "seg_ib_longhaul",
                            "departing_at": "2026-11-15T13:00:00Z",
                            "arriving_at": "2026-11-15T23:40:00Z",
                            "origin": { "iata_code": "MAD", "name": "Barajas" },
                            "destination": { "iata_code": "GRU", "name": "Guarulhos" },
                            "marketing_carrier": { "name": "Iberia", "iata_code": "IB" }
                          }
                        ]
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        // O usuário pesquisou IBZ para SAO (área metropolitana de São Paulo)
        val results = DuffelOfferProcessor.processRawDuffelJson(
            rawJson = rawJson,
            requestedOriginIata = "IBZ",
            requestedDestinationIata = "SAO"
        )

        // A oferta absurda da Ryanair (1h40 e 45€ em rota transatlântica) DEVE ser sumariamente descartada!
        assertEquals(1, results.size)
        val legit = results.first()
        assertEquals("off_iberia_legit_sao", legit.offerId)
        assertEquals("IB", legit.airlineIata)
        assertFalse(legit.isDirectFlight)
        assertEquals("Com Escala", legit.flightTypeClassification)
        assertEquals(1, legit.stopsCount)
        assertTrue(legit.durationMinutes >= 570) // Pelo menos 9h30m
    }

    @Test
    fun `metro equivalence recognizes SAO to GRU, RIO to GIG and PAR to CDG`() {
        assertTrue(com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent("SAO", "GRU"))
        assertTrue(com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent("GRU", "SAO"))
        assertTrue(com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent("SAO", "CGH"))
        assertTrue(com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent("RIO", "GIG"))
        assertTrue(com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent("PAR", "CDG"))
        assertFalse(com.example.domain.concierge.AirportAndLoyaltyCatalog.areAirportCodesEquivalent("SAO", "LIS"))
    }
}
