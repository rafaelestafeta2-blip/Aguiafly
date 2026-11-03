package com.example.data.model

enum class CardBrand(
    val displayName: String,
    val expectedCvcLength: Int,
    val badgeColorHex: Long
) {
    VISA("VISA", 3, 0xFF1A1F71),
    MASTERCARD("MASTERCARD", 3, 0xFFEB001B),
    AMEX("AMEX", 4, 0xFF007BC1),
    ELO("ELO", 3, 0xFFFFCB05),
    UNKNOWN("CARTÃO", 3, 0xFF37474F);

    companion object {
        fun detectFromNumber(rawNumber: String): CardBrand {
            val digits = rawNumber.filter { it.isDigit() }
            if (digits.isEmpty()) return UNKNOWN
            return when {
                digits.startsWith("4") -> VISA
                digits.take(2).toIntOrNull() in 51..55 ||
                    digits.take(4).toIntOrNull() in 2221..2720 -> MASTERCARD
                digits.startsWith("34") || digits.startsWith("37") -> AMEX
                digits.startsWith("4011") || digits.startsWith("4312") ||
                    digits.startsWith("4389") || digits.startsWith("5067") ||
                    digits.startsWith("5090") || digits.startsWith("6363") -> ELO
                else -> UNKNOWN
            }
        }
    }
}

data class PaymentCardInput(
    val cardNumber: String = "",
    val cardholderName: String = "",
    val expiryMonthYear: String = "", // Formato MM/AA
    val cvc: String = ""
) {
    val cleanDigits: String
        get() = cardNumber.filter { it.isDigit() }.take(16)

    val brand: CardBrand
        get() = CardBrand.detectFromNumber(cleanDigits)

    val expMonth: String
        get() = expiryMonthYear.filter { it.isDigit() }.take(2)

    val expYearFourDigits: String
        get() {
            val yy = expiryMonthYear.filter { it.isDigit() }.drop(2).take(2)
            return if (yy.length == 2) "20$yy" else ""
        }

    val formattedCardNumberPreview: String
        get() {
            if (cleanDigits.isEmpty()) return "•••• •••• •••• ••••"
            return cleanDigits.chunked(4).joinToString(" ")
        }

    val isLuhnChecksumValid: Boolean
        get() = cleanDigits.length >= 13 && passesLuhnCheck(cleanDigits)

    val isCardNumberValid: Boolean
        get() {
            val minLen = if (brand == CardBrand.AMEX) 15 else 16
            return cleanDigits.length >= minLen && passesLuhnCheck(cleanDigits)
        }

    val isExpiryValid: Boolean
        get() {
            val month = expMonth.toIntOrNull() ?: return false
            val year = expYearFourDigits.toIntOrNull() ?: return false
            if (month !in 1..12 || year !in 2024..2045) return false
            return try {
                val cardYm = java.time.YearMonth.of(year, month)
                !cardYm.isBefore(java.time.YearMonth.now())
            } catch (_: Exception) {
                year >= 2026
            }
        }

    val isCvcValid: Boolean
        get() {
            val digits = cvc.filter { it.isDigit() }
            return digits.length in 3..4
        }

    val isCardholderValid: Boolean
        get() = cardholderName.trim().length >= 3

    val isCompleteAndValid: Boolean
        get() = isCardNumberValid && isExpiryValid && isCvcValid && isCardholderValid

    val validationMessage: String?
        get() = when {
            cleanDigits.isEmpty() -> "Digite o número real do seu cartão de crédito"
            !isCardNumberValid -> "Número de cartão reprovado no teste matemático de Luhn (verifique os dígitos)"
            !isCardholderValid -> "Informe o nome impresso no cartão"
            !isExpiryValid -> "Cartão vencido ou data inválida (use MM/AA futuro, ex: 08/29)"
            !isCvcValid -> "Informe o código CVC de segurança (3 ou 4 dígitos)"
            else -> null
        }

    companion object {
        fun passesLuhnCheck(digits: String): Boolean {
            if (digits.length < 13) return false
            var sum = 0
            var alternate = false
            for (i in digits.length - 1 downTo 0) {
                var n = digits[i] - '0'
                if (alternate) {
                    n *= 2
                    if (n > 9) n -= 9
                }
                sum += n
                alternate = !alternate
            }
            return sum % 10 == 0
        }
    }
}
