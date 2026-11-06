package com.example.domain.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.IssuedTicket
import com.example.domain.checkin.AutoCheckInManager
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Gerador Oficial de Passes Google Wallet (`FlightClass` & `FlightObject` JWT RS256 +
 * Cartão de Embarque Óptico PNG com QR Code ISO/IEC 18004 Real para Importação Nativa com Biometria).
 *
 * Funciona tanto com bilhetes em Modo de Teste (Duffel Sandbox `duffel_test_...`) quanto em Produção:
 * 1. Se `GOOGLE_WALLET_ISSUER_ID`, `GOOGLE_WALLET_SERVICE_ACCOUNT_EMAIL` e `GOOGLE_WALLET_PRIVATE_KEY`
 *    estiverem configurados nos GitHub Secrets / `.env`, gera o JWT assinado com `SHA256withRSA` e abre
 *    `https://pay.google.com/gp/v/save/{jwt}` (fluxo direto Google Pay & Wallet API com digital/biometria).
 * 2. Caso as chaves da Google Pay Console ainda não estejam configuradas, gera um Cartão de Embarque
 *    PNG de alto contraste com QR Code ISO/IEC 18004 100% real e envia/salva na Galeria para o recurso
 *    nativo "Adicionar à Carteira -> Foto" do Google Wallet (que lê o QR Code real, pede a digital e
 *    adiciona o bilhete de teste diretamente dentro do app Google Wallet).
 */
object GoogleWalletPassSigner {

    fun isDirectJwtConfigured(): Boolean {
        val issuerId = runCatching { BuildConfig.GOOGLE_WALLET_ISSUER_ID.trim() }.getOrDefault("")
        val email = runCatching { BuildConfig.GOOGLE_WALLET_SERVICE_ACCOUNT_EMAIL.trim() }.getOrDefault("")
        val privKey = runCatching { BuildConfig.GOOGLE_WALLET_PRIVATE_KEY.trim() }.getOrDefault("")
        return issuerId.isNotBlank() &&
            !issuerId.contains("PLACEHOLDER", ignoreCase = true) &&
            email.contains("@") &&
            !email.contains("PLACEHOLDER", ignoreCase = true) &&
            privKey.contains("PRIVATE KEY") &&
            !privKey.contains("PLACEHOLDER", ignoreCase = true)
    }

    /**
     * Gera a URL oficial `https://pay.google.com/gp/v/save/{signed_jwt}` assinada com RS256 (SHA256withRSA)
     * para adicionar o `FlightObject` diretamente na Google Wallet (suporta Modo Teste/Demo e Produção).
     */
    fun buildSignedSaveToGoogleWalletUrl(ticket: IssuedTicket): Result<String> {
        return try {
            if (!isDirectJwtConfigured()) {
                return Result.failure(
                    IllegalStateException("Chaves GOOGLE_WALLET_ISSUER_ID / GOOGLE_WALLET_PRIVATE_KEY não configuradas.")
                )
            }
            val issuerId = BuildConfig.GOOGLE_WALLET_ISSUER_ID.trim()
            val serviceEmail = BuildConfig.GOOGLE_WALLET_SERVICE_ACCOUNT_EMAIL.trim()
            val rawPrivateKey = BuildConfig.GOOGLE_WALLET_PRIVATE_KEY.trim()

            val boarding = AutoCheckInManager.parseTicketCheckInState(ticket, forceImmediateCheckIn = true)
            val safeRef = ticket.bookingReference.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "AGF999" }
            val safeOrder = ticket.orderId.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { safeRef }
            val classId = "$issuerId.aguiafly_flight_class_${ticket.flightNumber.replace(Regex("[^a-zA-Z0-9]"), "")}_${ticket.departureIso.take(10).replace("-", "")}"
            val objectId = "$issuerId.aguiafly_ticket_${safeOrder}_$safeRef"

            val carrierCode = ticket.flightNumber.takeWhile { it.isLetter() }.ifBlank { "TP" }.uppercase().take(2)
            val flightNumberDigits = ticket.flightNumber.filter { it.isDigit() }.ifBlank { "100" }
            val depIsoFormatted = if (ticket.departureIso.length >= 16) {
                ticket.departureIso.take(19)
            } else {
                "${ticket.departureIso.take(10)}T10:00:00"
            }
            val arrIsoFormatted = if (ticket.arrivalIso.length >= 16) {
                ticket.arrivalIso.take(19)
            } else {
                "${ticket.arrivalIso.take(10)}T14:00:00"
            }

            val headerJson = """{"alg":"RS256","typ":"JWT"}"""
            val nowEpochSeconds = System.currentTimeMillis() / 1000L

            val payloadJson = """
                {
                  "iss": "${escapeJson(serviceEmail)}",
                  "aud": "google",
                  "typ": "savetowallet",
                  "iat": $nowEpochSeconds,
                  "origins": ["https://aguiafly.com"],
                  "payload": {
                    "flightClasses": [
                      {
                        "id": "${escapeJson(classId)}",
                        "issuerName": "AguiaFly",
                        "reviewStatus": "UNDER_REVIEW",
                        "localScheduledDepartureDateTime": "${escapeJson(depIsoFormatted)}",
                        "localScheduledArrivalDateTime": "${escapeJson(arrIsoFormatted)}",
                        "flightHeader": {
                          "carrier": {
                            "carrierIataCode": "${escapeJson(carrierCode)}",
                            "airlineName": {
                              "defaultValue": {
                                "language": "pt-PT",
                                "value": "${escapeJson(ticket.airlineName)}"
                              }
                            }
                          },
                          "flightNumber": "${escapeJson(flightNumberDigits)}"
                        },
                        "origin": {
                          "airportIataCode": "${escapeJson(ticket.originIata.uppercase())}",
                          "terminal": "${escapeJson(boarding.terminal.replace("T", "").ifBlank { "1" })}",
                          "gate": "${escapeJson(boarding.gate)}"
                        },
                        "destination": {
                          "airportIataCode": "${escapeJson(ticket.destinationIata.uppercase())}"
                        }
                      }
                    ],
                    "flightObjects": [
                      {
                        "id": "${escapeJson(objectId)}",
                        "classId": "${escapeJson(classId)}",
                        "state": "ACTIVE",
                        "passengerName": "${escapeJson(ticket.passengerName.uppercase())}",
                        "boardingAndSeatingInfo": {
                          "boardingGroup": "${escapeJson(boarding.boardingGroup.substringBefore(" "))}",
                          "seatNumber": "${escapeJson(boarding.assignedSeat)}",
                          "seatClass": "${escapeJson(ticket.cabinClass.uppercase())}"
                        },
                        "reservationInfo": {
                          "confirmationCode": "${escapeJson(ticket.bookingReference.uppercase())}",
                          "eticketNumber": "${escapeJson(boarding.eTicketNumber)}"
                        },
                        "barcode": {
                          "type": "QR_CODE",
                          "value": "${escapeJson(boarding.cleanBcbpPayload)}",
                          "alternateText": "PNR ${escapeJson(ticket.bookingReference.uppercase())} • ASSENTO ${escapeJson(boarding.assignedSeat)}"
                        }
                      }
                    ]
                  }
                }
            """.trimIndent()

            val encodedHeader = base64UrlEncode(headerJson.encodeToByteArray())
            val encodedPayload = base64UrlEncode(payloadJson.encodeToByteArray())
            val signingInput = "$encodedHeader.$encodedPayload"

            val cleanPem = rawPrivateKey
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace(Regex("\\s+"), "")

            val keyBytes = Base64.decode(cleanPem, Base64.DEFAULT)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)

            val signer = Signature.getInstance("SHA256withRSA")
            signer.initSign(privateKey)
            signer.update(signingInput.encodeToByteArray())
            val signatureBytes = signer.sign()
            val encodedSignature = base64UrlEncode(signatureBytes)

            val jwt = "$signingInput.$encodedSignature"
            Result.success("https://pay.google.com/gp/v/save/$jwt")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Renderiza um Cartão de Embarque de alta nitidez (1080x1680 px) especificamente otimizado
     * para o leitor óptico nativo da Google Wallet ("Adicionar à Carteira -> Foto / Cartão de Embarque").
     * Contém um QR Code ISO/IEC 18004 matematicamente real e de alto contraste no centro.
     */
    fun renderGoogleWalletBoardingPassPng(context: Context, ticket: IssuedTicket): Bitmap {
        val width = 1080
        val height = 1680
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val boarding = AutoCheckInManager.parseTicketCheckInState(ticket, forceImmediateCheckIn = true)

        val depDate = ticket.departureIso.take(10)
        val depTime = ticket.departureIso.substringAfter("T", "10:50").take(5)
        val arrTime = ticket.arrivalIso.substringAfter("T", "14:15").take(5)

        // Fundo Branco Puro ao redor do cartão para leitura 100% precisa pela Google Wallet AI / Scanner
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), whitePaint)

        // Cabeçalho Azul Royal AguiaFly
        val headerRect = RectF(40f, 40f, 1040f, 300f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                40f, 40f, 1040f, 300f,
                intArrayOf(Color.rgb(4, 15, 35), Color.rgb(15, 45, 95)),
                null,
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 36f, 36f, headerPaint)

        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 213, 79)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("BOARDING PASS • CARTÃO DE EMBARQUE", 84f, 120f, goldPaint)
        canvas.drawText("${ticket.airlineName.uppercase()} • VOO ${ticket.flightNumber}", 84f, 180f, headerSubPaint)
        canvas.drawText("PNR / CONFIRMAÇÃO: ${ticket.bookingReference.uppercase()}", 84f, 240f, goldPaint)

        // Bloco de Rota Grande (IATA -> IATA)
        val darkBlue = Color.rgb(10, 25, 49)
        val mutedGray = Color.rgb(71, 85, 105)
        val iataPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkBlue
            textSize = 112f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGray
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText(ticket.originIata.uppercase(), 84f, 445f, iataPaint)
        canvas.drawText(ticket.originCity, 84f, 498f, cityPaint)
        canvas.drawText("Partida: $depDate • $depTime", 84f, 550f, cityPaint)

        val rightIataPaint = Paint(iataPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightCityPaint = Paint(cityPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText(ticket.destinationIata.uppercase(), 996f, 445f, rightIataPaint)
        canvas.drawText(ticket.destinationCity, 996f, 498f, rightCityPaint)
        canvas.drawText("Chegada: $arrTime", 996f, 550f, rightCityPaint)

        val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(14, 165, 233)
            textSize = 76f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✈", 540f, 435f, arrowPaint)

        // Grelha de Passageiro, Assento, Portão, Terminal e Embarque
        val boxStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val infoRect = RectF(64f, 600f, 1016f, 870f)
        canvas.drawRoundRect(infoRect, 28f, 28f, boxStroke)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = mutedGray
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkBlue
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("PASSENGER / PASSAGEIRO", 100f, 655f, labelPaint)
        canvas.drawText(ticket.passengerName.uppercase(), 100f, 708f, valPaint)

        canvas.drawText("FLIGHT / VOO", 720f, 655f, labelPaint)
        canvas.drawText(ticket.flightNumber.uppercase(), 720f, 708f, valPaint)

        canvas.drawText("TERMINAL", 100f, 775f, labelPaint)
        canvas.drawText(boarding.terminal, 100f, 830f, valPaint)

        canvas.drawText("GATE / PORTÃO", 340f, 775f, labelPaint)
        canvas.drawText(boarding.gate, 340f, 830f, valPaint)

        canvas.drawText("BOARDING", 590f, 775f, labelPaint)
        canvas.drawText(boarding.boardingTimeFormatted, 590f, 830f, valPaint)

        canvas.drawText("SEAT / ASSENTO", 820f, 775f, labelPaint)
        canvas.drawText(boarding.assignedSeat, 820f, 830f, valPaint)

        // QR CODE ISO/IEC 18004 REAL E GIGANTE NO CENTRO INFERIOR (Para leitura imediata pelo Google Wallet)
        val qrMatrix = IsoQrCodeEncoder.encodeToMatrix(boarding.cleanBcbpPayload)
        val matrixSize = qrMatrix.size
        val qrBoxSize = 560f
        val quietZoneModules = 4
        val totalModules = matrixSize + quietZoneModules * 2
        val modulePx = qrBoxSize / totalModules
        val qrLeft = (width - qrBoxSize) / 2f
        val qrTop = 920f

        val qrBorderRect = RectF(qrLeft - 16f, qrTop - 16f, qrLeft + qrBoxSize + 16f, qrTop + qrBoxSize + 16f)
        canvas.drawRoundRect(qrBorderRect, 24f, 24f, whitePaint)
        canvas.drawRoundRect(qrBorderRect, 24f, 24f, boxStroke)

        val blackModulePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = false
        }

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (qrMatrix[r][c]) {
                    val left = qrLeft + (c + quietZoneModules) * modulePx
                    val top = qrTop + (r + quietZoneModules) * modulePx
                    canvas.drawRect(left, top, left + modulePx + 0.6f, top + modulePx + 0.6f, blackModulePaint)
                }
            }
        }

        val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkBlue
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "IATA BCBP: ${boarding.cleanBcbpPayload.take(48)}",
            width / 2f,
            1545f,
            monoPaint
        )
        canvas.drawText(
            "ETKT ${boarding.eTicketNumber} • GOOGLE WALLET READY PASS",
            width / 2f,
            1600f,
             Paint(labelPaint).apply { textAlign = Paint.Align.CENTER }
        )

        return bitmap
    }

    private fun base64UrlEncode(bytes: ByteArray): String {
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
    }
}
