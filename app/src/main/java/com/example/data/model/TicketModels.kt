package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR = 25.00

@IgnoreExtraProperties
data class IssuedTicket(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderId: String = "",
    val bookingReference: String = "",
    val passengerName: String = "",
    val originIata: String = "",
    val originCity: String = "",
    val destinationIata: String = "",
    val destinationCity: String = "",
    val departureIso: String = "",
    val arrivalIso: String = "",
    val airlineName: String = "",
    val flightNumber: String = "",
    val aircraftName: String = "",
    val cabinClass: String = "business",
    val seatDesignator: String = "2A",
    val netAmountEur: Double = 0.0,
    val markupEur: Double = 0.0,
    val ancillariesEur: Double = 0.0,
    val totalChargedEur: Double = 0.0,
    val iataBarcodePayload: String = "",
    val addedToGoogleWallet: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isCancelled: Boolean
        get() = iataBarcodePayload.contains("|CANCELLED:")

    val parsedCancellationId: String
        get() = iataBarcodePayload
            .substringAfter("|CANCELLED:", "")
            .substringBefore(":")
            .ifBlank { "ore_${orderId.takeLast(8)}" }

    val parsedNetStripeRefundEur: Double
        get() = iataBarcodePayload
            .substringAfter("|CANCELLED:", "")
            .split(":")
            .getOrNull(1)
            ?.toDoubleOrNull() ?: 0.0

    val parsedAirlineRefundEur: Double
        get() = iataBarcodePayload
            .substringAfter("|CANCELLED:", "")
            .split(":")
            .getOrNull(2)
            ?.toDoubleOrNull() ?: 0.0
}

/**
 * Cotação de Cancelamento & Reembolso consultada via Duffel API (POST /air/order_cancellations)
 * e enriquecida com as Regras Financeiras do AguiaFly:
 * - Taxas operacionais originais da intermediação (€3.50 + 3.5%) retidas integralmente.
 * - Taxa administrativa do AguiaFly de €25.00 deduzida sobre o valor devolvido pela companhia aérea.
 * - Valor líquido final devolvido no cartão de crédito via Stripe.
 */
data class OrderCancellationQuote(
    val ticketId: String,
    val orderId: String,
    val bookingReference: String,
    val flightSummary: String,
    val cancellationId: String,
    val totalChargedOriginalEur: Double,
    val originalNetFareEur: Double,
    val retainedFixedFeeEur: Double,
    val retainedPercentageFeeEur: Double,
    val retainedIntermediationFeeEur: Double, // €3.50 + 3.5% retido
    val airlineRefundAmountEur: Double, // Valor devolvido pela companhia aérea (Duffel API)
    val aguiaFlyAdminFeeEur: Double = AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR, // €25.00
    val netStripeCustomerRefundEur: Double, // Valor líquido final devolvido no cartão via Stripe
    val currency: String = "EUR",
    val expiresAt: String? = null,
    val isLiveDuffelQuote: Boolean = false
)

data class OrderCancellationConfirmation(
    val ticketId: String,
    val orderId: String,
    val bookingReference: String,
    val cancellationId: String,
    val stripeRefundId: String,
    val confirmedAtIso: String,
    val airlineRefundAmountEur: Double,
    val retainedIntermediationFeeEur: Double,
    val aguiaFlyAdminFeeEur: Double = AGUIAFLY_CANCELLATION_ADMIN_FEE_EUR,
    val netStripeCustomerRefundEur: Double,
    val currency: String = "EUR"
)

data class EuropeanAirport(
    val iata: String,
    val city: String,
    val name: String,
    val country: String
)

val EUROPEAN_HUBS = listOf(
    EuropeanAirport("LIS", "Lisboa", "Aeroporto Humberto Delgado", "Portugal"),
    EuropeanAirport("OPO", "Porto", "Aeroporto Francisco Sá Carneiro", "Portugal"),
    EuropeanAirport("FAO", "Faro", "Aeroporto Gago Coutinho (Algarve)", "Portugal"),
    EuropeanAirport("FNC", "Funchal", "Aeroporto da Madeira Cristiano Ronaldo", "Portugal"),
    EuropeanAirport("MAD", "Madrid", "Adolfo Suárez Madrid-Barajas", "Espanha"),
    EuropeanAirport("BCN", "Barcelona", "Josep Tarradellas El Prat", "Espanha"),
    EuropeanAirport("IBZ", "Ibiza", "Aeroport d'Eivissa", "Espanha"),
    EuropeanAirport("CDG", "Paris", "Charles de Gaulle Airport", "França"),
    EuropeanAirport("NCE", "Nice", "Nice Côte d'Azur Airport", "França"),
    EuropeanAirport("LHR", "Londres", "Heathrow Airport", "Reino Unido"),
    EuropeanAirport("DUB", "Dublin", "Dublin International Airport", "Irlanda"),
    EuropeanAirport("FCO", "Roma", "Leonardo da Vinci–Fiumicino", "Itália"),
    EuropeanAirport("MXP", "Milão", "Milano Malpensa Airport", "Itália"),
    EuropeanAirport("VCE", "Veneza", "Venezia Marco Polo Airport", "Itália"),
    EuropeanAirport("ZRH", "Zurique", "Zurich Airport", "Suíça"),
    EuropeanAirport("GVA", "Genebra", "Genève Aéroport", "Suíça"),
    EuropeanAirport("AMS", "Amsterdão", "Airport Schiphol", "Países Baixos"),
    EuropeanAirport("BRU", "Bruxelas", "Brussels Airport Zaventem", "Bélgica"),
    EuropeanAirport("FRA", "Frankfurt", "Frankfurt Airport", "Alemanha"),
    EuropeanAirport("MUC", "Munique", "Munich Franz Josef Strauss", "Alemanha"),
    EuropeanAirport("BER", "Berlim", "Berlin Brandenburg Airport", "Alemanha"),
    EuropeanAirport("VIE", "Viena", "Vienna International Airport", "Áustria"),
    EuropeanAirport("ATH", "Atenas", "Athens Eleftherios Venizelos", "Grécia"),
    EuropeanAirport("PRG", "Praga", "Václav Havel Airport Prague", "Chéquia"),
    EuropeanAirport("CPH", "Copenhaga", "Copenhagen Airport Kastrup", "Dinamarca"),
    EuropeanAirport("DXB", "Dubai", "Dubai International Airport", "Emirados Árabes Unidos"),
    EuropeanAirport("GRU", "São Paulo", "Aeroporto Internacional de Guarulhos", "Brasil"),
    EuropeanAirport("GIG", "Rio de Janeiro", "Aeroporto Internacional Tom Jobim", "Brasil"),
    EuropeanAirport("JFK", "Nova Iorque", "John F. Kennedy International", "Estados Unidos"),
    EuropeanAirport("MIA", "Miami", "Miami International Airport", "Estados Unidos")
)
