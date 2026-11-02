package com.example.domain.pricing

import kotlin.math.roundToInt

enum class DisplayCurrency(
    val code: String,
    val symbol: String,
    val badgeLabel: String,
    val rateFromEur: Double
) {
    EUR("EUR", "€", "EUR €", 1.0),
    BRL("BRL", "R$", "BRL R$", 6.18),
    USD("USD", "$", "USD $", 1.09);

    fun convertFromEur(amountEur: Double): Double {
        return ((amountEur * rateFromEur) * 100.0).roundToInt() / 100.0
    }

    fun format(amountEur: Double): String {
        val converted = convertFromEur(amountEur)
        val whole = converted.toInt()
        val cents = ((converted - whole) * 100).roundToInt().coerceIn(0, 99)
        return "$symbol$whole.${cents.toString().padStart(2, '0')}"
    }
}
