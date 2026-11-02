package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

data class StripePaymentIntentResponse(
    @Json(name = "id") val id: String,
    @Json(name = "client_secret") val clientSecret: String?,
    @Json(name = "amount") val amountCents: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "status") val status: String
)

data class StripeTokenResponse(
    @Json(name = "id") val id: String,
    @Json(name = "object") val objectType: String = "token",
    @Json(name = "card") val card: StripeCardDetails? = null,
    @Json(name = "created") val created: Long = 0,
    @Json(name = "livemode") val livemode: Boolean = false
)

data class StripeCardDetails(
    @Json(name = "id") val id: String = "",
    @Json(name = "brand") val brand: String = "Visa",
    @Json(name = "last4") val last4: String = "4242",
    @Json(name = "exp_month") val expMonth: Int = 12,
    @Json(name = "exp_year") val expYear: Int = 2028
)

interface StripePaymentService {

    /**
     * Tokeniza o cartão no frontend utilizando a Chave Publicável (STRIPE_PUBLISHABLE_KEY).
     * Garante conformidade PCI-DSS (os dados do cartão não transitam em texto claro pelo servidor da aplicação).
     */
    @FormUrlEncoded
    @POST("v1/tokens")
    suspend fun tokenizeCard(
        @Header("Authorization") bearerPublishableKey: String,
        @Field("card[number]") cardNumber: String,
        @Field("card[exp_month]") expMonth: String,
        @Field("card[exp_year]") expYear: String,
        @Field("card[cvc]") cvc: String,
        @Field("card[name]") cardholderName: String = "Cliente AguiaFly"
    ): StripeTokenResponse

    /**
     * Cria e liquida o PaymentIntent na Stripe em EUR.
     * Utiliza a Chave Secreta (STRIPE_SECRET_KEY) exclusivamente no fluxo seguro de cobrança do valor final
     * (€3.50 + 3.5% + Tarifa Neta + Opcionais).
     */
    @FormUrlEncoded
    @POST("v1/payment_intents")
    suspend fun createEurPaymentIntent(
        @Header("Authorization") bearerSecretKey: String,
        @Field("amount") amountCents: Long,
        @Field("currency") currency: String = "eur",
        @Field("payment_method") paymentMethod: String = "pm_card_visa",
        @Field("confirm") confirm: Boolean = true,
        @Field("automatic_payment_methods[enabled]") autoMethods: Boolean = true,
        @Field("automatic_payment_methods[allow_redirects]") allowRedirects: String = "never",
        @Field("description") description: String,
        @Field("metadata[duffel_offer_id]") duffelOfferId: String,
        @Field("metadata[net_fare_eur]") netFareEur: String,
        @Field("metadata[ota_markup_eur]") otaMarkupEur: String,
        @Field("metadata[pricing_formula]") pricingFormula: String = "€3.50 + 3.5% + Tarifa Neta"
    ): StripePaymentIntentResponse

    @FormUrlEncoded
    @POST("v1/payment_intents")
    suspend fun createEurPaymentIntentWithToken(
        @Header("Authorization") bearerSecretKey: String,
        @Field("amount") amountCents: Long,
        @Field("currency") currency: String = "eur",
        @Field("confirm") confirm: Boolean = true,
        @Field("payment_method_data[type]") paymentMethodType: String = "card",
        @Field("payment_method_data[card][token]") cardToken: String,
        @Field("receipt_email") receiptEmail: String,
        @Field("automatic_payment_methods[enabled]") autoMethods: Boolean = true,
        @Field("automatic_payment_methods[allow_redirects]") allowRedirects: String = "never",
        @Field("description") description: String,
        @Field("metadata[duffel_offer_id]") duffelOfferId: String,
        @Field("metadata[net_fare_eur]") netFareEur: String,
        @Field("metadata[ota_markup_eur]") otaMarkupEur: String,
        @Field("metadata[pricing_formula]") pricingFormula: String = "€3.50 + 3.5% + Tarifa Neta"
    ): StripePaymentIntentResponse

    /**
     * Executa o estorno líquido no cartão de crédito do cliente via Stripe (POST /v1/refunds)
     * após retenção das taxas de intermediação (€3.50 + 3.5%) e dedução da Taxa Administrativa AguiaFly (€25.00).
     */
    @FormUrlEncoded
    @POST("v1/refunds")
    suspend fun createRefund(
        @Header("Authorization") bearerSecretKey: String,
        @Field("payment_intent") paymentIntentId: String,
        @Field("amount") amountCents: Long,
        @Field("reason") reason: String = "requested_by_customer",
        @Field("metadata[duffel_order_id]") duffelOrderId: String,
        @Field("metadata[booking_reference]") bookingReference: String,
        @Field("metadata[airline_refund_eur]") airlineRefundEur: String,
        @Field("metadata[aguiafly_admin_fee_eur]") aguiaFlyAdminFeeEur: String = "25.00",
        @Field("metadata[retained_intermediation_fee_eur]") retainedIntermediationFeeEur: String,
        @Field("metadata[net_customer_refund_eur]") netCustomerRefundEur: String
    ): StripeRefundResponse
}

data class StripeRefundResponse(
    @Json(name = "id") val id: String,
    @Json(name = "amount") val amountCents: Long = 0,
    @Json(name = "currency") val currency: String = "eur",
    @Json(name = "status") val status: String = "succeeded"
)

