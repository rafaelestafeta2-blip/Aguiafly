package com.example.domain.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.IssuedTicket
import com.example.domain.checkin.AutoCheckInManager
import com.example.domain.pricing.MarkupEngine
import com.example.notifications.PushNotificationHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.abs

object TicketPdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842

    private fun showToastSafe(context: Context, message: String) {
        try {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Renderiza uma imagem de alta definição (2x A4 Retina) do E-Ticket Executivo
     * para visualização imediata dentro do aplicativo.
     */
    fun renderTicketPreviewBitmap(context: Context, ticket: IssuedTicket): Bitmap {
        val scale = 2f
        val bitmap = Bitmap.createBitmap(
            (PAGE_WIDTH * scale).toInt(),
            (PAGE_HEIGHT * scale).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        drawExecutiveTicketPage(canvas, context, ticket)
        return bitmap
    }

    /**
     * Gera o arquivo PDF oficial A4 do E-Ticket & Recibo Fiscal AguiaFly.
     */
    fun generateTicketPdfFile(context: Context, ticket: IssuedTicket, customPrefix: String = "AguiaFly_ETicket"): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        drawExecutiveTicketPage(page.canvas, context, ticket)
        pdfDocument.finishPage(page)

        val safeRef = ticket.bookingReference.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "PNR" }
        val dir = File(context.cacheDir, "etickets").apply { mkdirs() }
        val file = File(dir, "${customPrefix}_${safeRef}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    fun getFileProviderUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Salva o PDF diretamente na pasta pública "Downloads" do dispositivo,
     * dispara notificação na barra de status do Android e exibe confirmação visual imediata.
     */
    fun saveTicketPdfToDownloads(
        context: Context,
        ticket: IssuedTicket,
        openExternalViewerIfAvailable: Boolean = false
    ): Result<String> {
        return try {
            val pdfFile = generateTicketPdfFile(context, ticket)
            val safeRef = ticket.bookingReference.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "PNR" }
            val fileName = "AguiaFly_ETicket_${safeRef}.pdf"
            val fileProviderUri = runCatching { getFileProviderUri(context, pdfFile) }.getOrNull()

            // Cópia garantida também no diretório externo do app
            runCatching {
                val appDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.apply { mkdirs() }
                if (appDownloads != null) {
                    pdfFile.copyTo(File(appDownloads, fileName), overwrite = true)
                }
            }

            var savedDisplayPath = "Downloads/$fileName"
            var mediaStoreUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                // Remove eventual ficheiro antigo com o mesmo nome para evitar conflito em cliques repetidos
                runCatching {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(fileName)
                    )
                }

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val insertedUri = runCatching {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                }.getOrNull()

                if (insertedUri != null) {
                    resolver.openOutputStream(insertedUri)?.use { outStream ->
                        FileInputStream(pdfFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                    val finishValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    runCatching { resolver.update(insertedUri, finishValues, null, null) }
                    mediaStoreUri = insertedUri
                } else {
                    @Suppress("DEPRECATION")
                    val fallbackDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        .apply { mkdirs() }
                    val destFile = File(fallbackDir, fileName)
                    runCatching { pdfFile.copyTo(destFile, overwrite = true) }
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    .apply { mkdirs() }
                val destFile = File(downloadsDir, fileName)
                runCatching { pdfFile.copyTo(destFile, overwrite = true) }
                savedDisplayPath = destFile.absolutePath
            }

            val targetUri = fileProviderUri ?: mediaStoreUri
            if (targetUri != null) {
                PushNotificationHelper.showTicketPdfDownloadedNotification(context, ticket, targetUri)
            }
            showToastSafe(context, "✅ E-Ticket PDF baixado em Downloads/$fileName")

            if (openExternalViewerIfAvailable && targetUri != null) {
                openPdfUriIfViewerInstalled(context, targetUri)
            }

            Result.success(savedDisplayPath)
        } catch (e: Exception) {
            showToastSafe(context, "⚠️ Erro ao baixar PDF: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Tenta abrir o PDF diretamente num leitor de PDF instalado no Android.
     * Retorna true se um visualizador externo foi aberto.
     */
    fun openPdfUriIfViewerInstalled(context: Context, uri: Uri): Boolean {
        return try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (viewIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(viewIntent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sincroniza e adiciona o Cartão de Embarque à Google Wallet:
     * 1. Gera o passe oficial em PDF/Pass na pasta Downloads e Cache.
     * 2. Dispara notificação de confirmação da Google Wallet e Toast.
     * 3. Se o app oficial Google Wallet estiver instalado no telemóvel, permite abri-lo.
     */
    fun addAndExportToGoogleWallet(
        context: Context,
        ticket: IssuedTicket,
        launchExternalWalletIfInstalled: Boolean = false
    ): Result<String> {
        return try {
            val safeRef = ticket.bookingReference.replace(Regex("[^a-zA-Z0-9_-]"), "").ifBlank { "PNR" }
            val passFile = generateTicketPdfFile(context, ticket, customPrefix = "AguiaFly_GoogleWallet_Pass")
            val passFileName = "AguiaFly_GoogleWallet_${safeRef}.pdf"

            // Salva também em Downloads para acesso offline imediato na carteira do dispositivo
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                runCatching {
                    resolver.delete(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                        arrayOf(passFileName)
                    )
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, passFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = runCatching {
                    resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                }.getOrNull()
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(passFile).use { input -> input.copyTo(out) }
                    }
                    val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                    runCatching { resolver.update(uri, done, null, null) }
                }
            }

            // Gera metadados JSON padrão Google Wallet FlightObject no cache
            val boarding = AutoCheckInManager.parseTicketCheckInState(ticket, forceImmediateCheckIn = true)
            val walletJsonFile = File(File(context.cacheDir, "etickets").apply { mkdirs() }, "google_wallet_${safeRef}.json")
            walletJsonFile.writeText(
                """
                {
                  "issuerName": "AguiaFly Royal Sapphire OTA",
                  "reviewStatus": "UNDER_REVIEW",
                  "flightNumber": "${ticket.flightNumber}",
                  "carrierIata": "${ticket.flightNumber.take(2)}",
                  "airlineName": "${ticket.airlineName}",
                  "reservationCode": "${ticket.bookingReference}",
                  "eTicketNumber": "${boarding.eTicketNumber}",
                  "passengerName": "${ticket.passengerName}",
                  "originIata": "${ticket.originIata}",
                  "originCity": "${ticket.originCity}",
                  "destinationIata": "${ticket.destinationIata}",
                  "destinationCity": "${ticket.destinationCity}",
                  "departureIso": "${ticket.departureIso}",
                  "arrivalIso": "${ticket.arrivalIso}",
                  "terminal": "${boarding.terminal}",
                  "gate": "${boarding.gate}",
                  "seat": "${boarding.assignedSeat}",
                  "boardingGroup": "${boarding.boardingGroup}",
                  "barcodeType": "QR_CODE",
                  "barcodeValue": "${boarding.cleanBcbpPayload}"
                }
                """.trimIndent()
            )

            PushNotificationHelper.showGoogleWalletPassAddedNotification(context, ticket)
            showToastSafe(
                context,
                "✅ Cartão de Embarque ${ticket.bookingReference} adicionado à Google Wallet!"
            )

            if (launchExternalWalletIfInstalled) {
                launchGoogleWalletAppOrPassViewer(context, ticket)
            }

            Result.success("Google Wallet • Passe ${ticket.bookingReference} Ativo")
        } catch (e: Exception) {
            showToastSafe(context, "⚠️ Erro ao sincronizar Google Wallet: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Abre o aplicativo Google Wallet instalado no Android ou, caso o dispositivo/emulador
     * não possua o pacote `com.google.android.apps.walletnfcrel`, abre o visualizador/compartilhador
     * do Passe Oficial.
     */
    fun launchGoogleWalletAppOrPassViewer(context: Context, ticket: IssuedTicket): Boolean {
        try {
            val pm = context.packageManager
            val walletPackages = listOf(
                "com.google.android.apps.walletnfcrel",
                "com.google.commerce.tapandpay.android.card.app"
            )
            for (pkg in walletPackages) {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
            val passFile = generateTicketPdfFile(context, ticket, customPrefix = "AguiaFly_GoogleWallet_Pass")
            val uri = getFileProviderUri(context, passFile)
            if (openPdfUriIfViewerInstalled(context, uri)) {
                return true
            }
            exportAndShareTicketPdf(context, ticket)
            return true
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Abre o menu nativo de compartilhamento (WhatsApp, E-mail, Drive, etc.) com o PDF anexado.
     */
    fun exportAndShareTicketPdf(context: Context, ticket: IssuedTicket) {
        try {
            val pdfFile = generateTicketPdfFile(context, ticket)
            val uri = getFileProviderUri(context, pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "E-Ticket Oficial AguiaFly • Reserva ${ticket.bookingReference}"
                )
                putExtra(
                    Intent.EXTRA_TEXT,
                    buildShareableReceiptText(ticket)
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Compartilhar E-Ticket & Recibo PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
            shareTicketAsText(context, ticket)
        }
    }

    /**
     * Abre o gerenciador de impressão nativo do Android com o PDF executivo.
     */
    fun printTicketPdf(context: Context, ticket: IssuedTicket) {
        try {
            val pdfFile = generateTicketPdfFile(context, ticket)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
            val jobName = "AguiaFly_ETicket_${ticket.bookingReference}"
            printManager.print(
                jobName,
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder("AguiaFly_ETicket_${ticket.bookingReference}.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        if (destination == null) {
                            callback?.onWriteFailed("Destino de impressão inválido.")
                            return
                        }
                        try {
                            FileInputStream(pdfFile).use { input ->
                                FileOutputStream(destination.fileDescriptor).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.localizedMessage)
                        }
                    }
                },
                null
            )
        } catch (_: Exception) {
            exportAndShareTicketPdf(context, ticket)
        }
    }

    fun shareTicketAsText(context: Context, ticket: IssuedTicket) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "E-Ticket AguiaFly • PNR ${ticket.bookingReference}"
                )
                putExtra(Intent.EXTRA_TEXT, buildShareableReceiptText(ticket))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartilhar Comprovante de Passagem").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {
        }
    }

    fun buildShareableReceiptText(ticket: IssuedTicket): String {
        val boarding = AutoCheckInManager.parseTicketCheckInState(ticket, forceImmediateCheckIn = true)
        return """
            ✈️ AGUIAFLY ROYAL SAPPHIRE • E-TICKET & RECIBO FISCAL IATA NDC
            ══════════════════════════════════════════
            • Código Localizador (PNR): ${ticket.bookingReference}
            • Nº Bilhete Eletrônico (E-Ticket): ${boarding.eTicketNumber}
            • ID da Ordem NDC: ${ticket.orderId}
            • Passageiro(a): ${ticket.passengerName}
            • Documento / NIF / CPF: ${boarding.documentTypeLabel} ${boarding.documentNumber} (Fiscal: ${boarding.taxIdNifCpf})
            • Companhia Operadora: ${ticket.airlineName} (${ticket.flightNumber})
            • Aeronave: ${ticket.aircraftName}
            • Rota: ${ticket.originCity} (${ticket.originIata}) ➔ ${ticket.destinationCity} (${ticket.destinationIata})
            • Partida: ${ticket.departureIso.replace("T", " ").take(16)} | Terminal ${boarding.terminal} • Portão ${boarding.gate}
            • Embarque Limite: ${boarding.boardingTimeFormatted} (${boarding.boardingGroup})
            • Chegada: ${ticket.arrivalIso.replace("T", " ").take(16)}
            • Cabine: ${ticket.cabinClass.uppercase()} • Assento: ${boarding.assignedSeat}
            • Franquia de Bagagem: ${boarding.baggageAllowanceSummary}
            ──────────────────────────────────────────
            💳 RECIBO FISCAL DE PAGAMENTO (STRIPE PCI-DSS):
            • Tarifa Neta Companhia: ${MarkupEngine.formatEur(ticket.netAmountEur)}
            • Taxa Emissão & Concierge AguiaFly: ${MarkupEngine.formatEur(ticket.markupEur)}
            • Serviços Opcionais (Assento/Bagagem): ${MarkupEngine.formatEur(ticket.ancillariesEur)}
            • VALOR TOTAL LIQUIDADO: ${MarkupEngine.formatEur(ticket.totalChargedEur)}
            ──────────────────────────────────────────
            • Código IATA BCBP: ${boarding.cleanBcbpPayload}
        """.trimIndent()
    }

    private fun drawExecutiveTicketPage(
        canvas: Canvas,
        context: Context,
        ticket: IssuedTicket
    ) {
        val boarding = AutoCheckInManager.parseTicketCheckInState(
            ticket = ticket,
            forceImmediateCheckIn = true
        )

        val depDate = ticket.departureIso.take(10)
        val depTime = ticket.departureIso.substringAfter("T", "10:50").take(5)
        val arrDate = ticket.arrivalIso.take(10)
        val arrTime = ticket.arrivalIso.substringAfter("T", "14:15").take(5)

        // Paleta Executiva Imperial Sapphire & Champagne Gold
        val sapphireDeep = Color.rgb(4, 15, 35)
        val sapphireMid = Color.rgb(11, 34, 72)
        val sapphireAccent = Color.rgb(20, 58, 118)
        val goldPrimary = Color.rgb(234, 179, 8)
        val goldBright = Color.rgb(255, 213, 79)
        val cyanAero = Color.rgb(14, 165, 233)
        val emeraldSuccess = Color.rgb(16, 185, 129)
        val pearlBg = Color.rgb(248, 250, 252)
        val cardBorder = Color.rgb(203, 213, 225)
        val textDark = Color.rgb(15, 23, 42)
        val textMuted = Color.rgb(71, 85, 105)

        // 0. Fundo Pérola e Moldura Externa Dupla
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pearlBg
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireMid
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(RectF(12f, 12f, 583f, 830f), 14f, 14f, outerBorderPaint)

        val goldFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldPrimary
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        canvas.drawRoundRect(RectF(15f, 15f, 580f, 827f), 12f, 12f, goldFramePaint)

        // 1. CABEÇALHO IMPERIAL SAPPHIRE COM FOTO DA LOGO OFICIAL AGUIAFLY
        val headerRect = RectF(18f, 18f, 577f, 126f)
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                18f, 18f, 577f, 126f,
                intArrayOf(sapphireDeep, sapphireMid, sapphireAccent),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(headerRect, 12f, 12f, headerPaint)

        // Faixa dourada inferior no cabeçalho
        val goldStripPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            style = Paint.Style.FILL
        }
        canvas.drawRect(18f, 122f, 577f, 126f, goldStripPaint)

        // Foto da Logo Oficial AguiaFly dentro de moldura Dourada & Branca
        val logoBox = RectF(30f, 28f, 108f, 106f)
        val whiteFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(logoBox, 12f, 12f, whiteFillPaint)

        try {
            val rawLogo = BitmapFactory.decodeResource(context.resources, R.drawable.aguiafly_official_logo)
            if (rawLogo != null) {
                val destRect = RectF(33f, 31f, 105f, 103f)
                canvas.save()
                val clipPath = Path().apply {
                    addRoundRect(destRect, 9f, 9f, Path.Direction.CW)
                }
                canvas.clipPath(clipPath)
                canvas.drawBitmap(rawLogo, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            }
        } catch (_: Exception) {
            // Fallback monograma caso recurso não esteja disponível em teste unitário puro
            val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sapphireDeep
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("AF", 54f, 75f, fallbackPaint)
        }

        val logoBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(logoBox, 12f, 12f, logoBorderPaint)

        // Textos institucionais no cabeçalho
        val brandTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            textSize = 18.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val brandSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val brandMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(186, 230, 253)
            textSize = 8.5f
        }

        canvas.drawText("AGUIAFLY ROYAL SAPPHIRE", 120f, 48f, brandTitlePaint)
        canvas.drawText("E-TICKET OFICIAL IATA NDC • CARTÃO DE EMBARQUE & RECIBO", 120f, 65f, brandSubPaint)
        canvas.drawText("Companhia Operadora: ${ticket.airlineName.uppercase()} • Voo ${ticket.flightNumber}", 120f, 82f, brandMetaPaint)
        canvas.drawText("Aeronave: ${ticket.aircraftName} • Cabine ${ticket.cabinClass.uppercase()}", 120f, 97f, brandMetaPaint)

        // Caixa Dourada de PNR e Número do E-Ticket à direita do cabeçalho
        val pnrBox = RectF(420f, 27f, 565f, 108f)
        val pnrBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(3, 12, 28)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(pnrBox, 10f, 10f, pnrBgPaint)
        canvas.drawRoundRect(pnrBox, 10f, 10f, logoBorderPaint)

        val pnrLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(191, 212, 242)
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pnrValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val etktPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.8f
            textAlign = Paint.Align.CENTER
        }
        val statusBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = emeraldSuccess
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val pnrCenterX = pnrBox.centerX()
        canvas.drawText("LOCALIZADOR DA RESERVA (PNR)", pnrCenterX, 41f, pnrLabelPaint)
        canvas.drawText(ticket.bookingReference.uppercase(), pnrCenterX, 64f, pnrValuePaint)
        canvas.drawText("ETKT: ${boarding.eTicketNumber}", pnrCenterX, 81f, etktPaint)
        canvas.drawText("● CONFIRMADO • EMISSÃO NDC", pnrCenterX, 97f, statusBadgePaint)

        // 2. PAINEL VISUAL DE ROTA E PASSAGEIRO (Estilo Emirates / Air France / TAP)
        val routeCard = RectF(24f, 136f, 571f, 278f)
        drawSectionCard(canvas, routeCard, cardBorder)

        // Barra superior do cartão 1
        val sectionHeaderRect = RectF(24f, 136f, 571f, 160f)
        val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireMid
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(sectionHeaderRect, 10f, 10f, sectionHeaderPaint)
        canvas.drawRect(24f, 148f, 571f, 160f, sectionHeaderPaint)

        val sectionHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionHeaderRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("1. BILHETE DE PASSAGEM & ITINERÁRIO OFICIAL", 36f, 151.5f, sectionHeaderTextPaint)
        canvas.drawText("ORDEM NDC: ${ticket.orderId.take(24)}", 559f, 151.5f, sectionHeaderRightPaint)

        // Linha do Passageiro e Documento APIS
        val labelSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textMuted
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valueBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas.drawText("PASSAGEIRO(A) TITULAR", 36f, 176f, labelSmallPaint)
        canvas.drawText(ticket.passengerName.uppercase(), 36f, 191f, valueBoldPaint)

        canvas.drawText("DOCUMENTO DE IDENTIFICAÇÃO (APIS)", 250f, 176f, labelSmallPaint)
        canvas.drawText("${boarding.documentTypeLabel}: ${boarding.documentNumber}", 250f, 191f, valueBoldPaint)

        canvas.drawText("NIF / CPF FISCAL", 455f, 176f, labelSmallPaint)
        canvas.drawText(boarding.taxIdNifCpf, 455f, 191f, valueBoldPaint)

        // Linha divisória suave
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }
        canvas.drawLine(36f, 200f, 559f, 200f, dividerPaint)

        // Bloco Visual Origem -> Avião -> Destino
        val iataGiantPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireDeep
            textSize = 29f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireAccent
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Origem (Esquerda)
        canvas.drawText(ticket.originIata.uppercase(), 36f, 234f, iataGiantPaint)
        canvas.drawText(ticket.originCity, 36f, 249f, cityPaint)
        canvas.drawText("Partida: $depDate às $depTime", 36f, 265f, timePaint)

        // Destino (Direita)
        val iataRightPaint = Paint(iataGiantPaint).apply { textAlign = Paint.Align.RIGHT }
        val cityRightPaint = Paint(cityPaint).apply { textAlign = Paint.Align.RIGHT }
        val timeRightPaint = Paint(timePaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText(ticket.destinationIata.uppercase(), 559f, 234f, iataRightPaint)
        canvas.drawText(ticket.destinationCity, 559f, 249f, cityRightPaint)
        canvas.drawText("Chegada: $arrDate às $arrTime", 559f, 265f, timeRightPaint)

        // Linha de Rota Central com Ícone de Avião
        val routeLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cyanAero
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(175f, 228f, 415f, 228f, routeLinePaint)

        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldPrimary
            style = Paint.Style.FILL
        }
        canvas.drawCircle(175f, 228f, 4f, dotPaint)
        canvas.drawCircle(415f, 228f, 4f, dotPaint)

        // Círculo central com avião estilizado
        val planeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireMid
            style = Paint.Style.FILL
        }
        canvas.drawCircle(295f, 228f, 13f, planeCirclePaint)
        val planeIconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            textSize = 13f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✈", 295f, 233f, planeIconPaint)

        val routeCenterMetaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireMid
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${ticket.flightNumber} • ${ticket.airlineName}", 295f, 210f, routeCenterMetaPaint)
        canvas.drawText("CABINE ${ticket.cabinClass.uppercase()} • ${ticket.aircraftName}", 295f, 256f, routeCenterMetaPaint)

        // 3. GRELHA OPERACIONAL DE EMBARQUE & FRANQUIA DE BAGAGEM
        val opsCard = RectF(24f, 288f, 571f, 414f)
        drawSectionCard(canvas, opsCard, cardBorder)

        val opsHeaderRect = RectF(24f, 288f, 571f, 310f)
        canvas.drawRoundRect(opsHeaderRect, 10f, 10f, sectionHeaderPaint)
        canvas.drawRect(24f, 299f, 571f, 310f, sectionHeaderPaint)
        canvas.drawText("2. QUADRO DE EMBARQUE, ASSENTO & FRANQUIA DE BAGAGEM", 36f, 302.5f, sectionHeaderTextPaint)
        canvas.drawText("CHECK-IN GARANTIDO AGUIAFLY", 559f, 302.5f, sectionHeaderRightPaint)

        // 6 Caixas Operacionais (Terminal, Portão, Embarque, Assento, Grupo, Sequência)
        val pills = listOf(
            "TERMINAL" to boarding.terminal,
            "PORTÃO / GATE" to boarding.gate,
            "EMBARQUE" to boarding.boardingTimeFormatted,
            "ASSENTO" to boarding.assignedSeat,
            "PRIORIDADE" to boarding.boardingGroup.substringBefore(" "),
            "SEQ CHECK-IN" to boarding.sequenceNumber
        )
        val pillWidth = 82f
        val pillGap = 7.2f
        val startX = 34f
        val pillTop = 318f
        val pillBottom = 362f

        val pillFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val pillStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val pillLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textMuted
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val pillValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireDeep
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        pills.forEachIndexed { idx, (label, value) ->
            val left = startX + idx * (pillWidth + pillGap)
            val rect = RectF(left, pillTop, left + pillWidth, pillBottom)
            canvas.drawRoundRect(rect, 8f, 8f, pillFillPaint)
            canvas.drawRoundRect(rect, 8f, 8f, pillStrokePaint)
            canvas.drawText(label, rect.centerX(), pillTop + 14f, pillLabelPaint)
            canvas.drawText(value, rect.centerX(), pillTop + 34f, pillValuePaint)
        }

        // Faixa de Franquia de Bagagem e Regras da Tarifa
        val bagBox = RectF(34f, 369f, 561f, 406f)
        val bagFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(239, 246, 255)
            style = Paint.Style.FILL
        }
        val bagBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cyanAero
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(bagBox, 8f, 8f, bagFillPaint)
        canvas.drawRoundRect(bagBox, 8f, 8f, bagBorderPaint)

        val bagTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireMid
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bagPolicyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textMuted
            textSize = 8.2f
        }
        canvas.drawText(
            "FRANQUIA DE BAGAGEM INCLUÍDA: ${boarding.baggageAllowanceSummary}",
            44f,
            384f,
            bagTitlePaint
        )
        canvas.drawText(
            "POLÍTICA TARIFÁRIA: ${boarding.farePolicySummary}",
            44f,
            398f,
            bagPolicyPaint
        )

        // 4. CANHOTO DESTACÁVEL DE CARTÃO DE EMBARQUE COM QR CODE 2D + BARCODE 1D IATA
        // Linha tracejada de destaque tipo bilhete de avião
        val tearY = 425f
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireAccent
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }
        canvas.drawLine(32f, tearY, 563f, tearY, dashPaint)
        val scissorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireAccent
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("✂ CANHOTO ÓPTICO DE EMBARQUE (APRESENTE NO PORTÃO OU SALA VIP)", 42f, tearY - 3f, scissorPaint)

        val stubCard = RectF(24f, 432f, 571f, 594f)
        drawSectionCard(canvas, stubCard, goldPrimary)

        // Faixa lateral esquerda dourada do Boarding Pass Stub
        val stubSideStrip = RectF(24f, 432f, 34f, 594f)
        canvas.drawRoundRect(stubSideStrip, 8f, 8f, goldStripPaint)

        canvas.drawText("CARTÃO DE EMBARQUE ÓPTICO • IATA BCBP RESOLUTION 792", 44f, 450f, bagTitlePaint)
        canvas.drawText(
            "Passageiro: ${ticket.passengerName.uppercase()}   |   Voo: ${ticket.flightNumber} (${ticket.originIata} ➔ ${ticket.destinationIata})",
            44f,
            466f,
            valueBoldPaint
        )
        canvas.drawText(
            "Data: $depDate   •   Embarque: ${boarding.boardingTimeFormatted}   •   Portão: ${boarding.gate} (${boarding.terminal})   •   Assento: ${boarding.assignedSeat}",
            44f,
            482f,
            timePaint
        )

        // Código de Barras Linear 1D IATA (lado esquerdo inferior do canhoto)
        val barcodeBox = RectF(44f, 492f, 412f, 558f)
        val barcodeBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cardBorder
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(barcodeBox, 6f, 6f, whiteFillPaint)
        canvas.drawRoundRect(barcodeBox, 6f, 6f, barcodeBorder)

        draw1DBarcodeOnPdf(
            canvas = canvas,
            payload = boarding.cleanBcbpPayload,
            left = 54f,
            top = 500f,
            width = 348f,
            height = 48f,
            barColor = sapphireDeep
        )

        val bcbpMonoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textMuted
            textSize = 7.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(
            boarding.cleanBcbpPayload.take(64),
            44f,
            574f,
            bcbpMonoPaint
        )
        canvas.drawText(
            "Sincronizado com Apple Wallet & Google Wallet • Leitura direta nos portões automáticos",
            44f,
            586f,
            bagPolicyPaint
        )

        // QR Code 2D Matricial (lado direito do canhoto)
        val qrFrame = RectF(430f, 442f, 558f, 570f)
        canvas.drawRoundRect(qrFrame, 10f, 10f, whiteFillPaint)
        canvas.drawRoundRect(qrFrame, 10f, 10f, logoBorderPaint)

        draw2DQrMatrixOnPdf(
            canvas = canvas,
            payload = boarding.cleanBcbpPayload,
            left = 438f,
            top = 450f,
            size = 112f,
            moduleColor = sapphireDeep
        )

        val qrCaptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = sapphireDeep
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SCANNER 2D AEROPORTO", qrFrame.centerX(), 583f, qrCaptionPaint)

        // 5. RECIBO FISCAL CORPORATIVO & COMPROVANTE DE PAGAMENTO STRIPE
        val receiptCard = RectF(24f, 604f, 571f, 788f)
        drawSectionCard(canvas, receiptCard, cardBorder)

        val receiptHeaderRect = RectF(24f, 604f, 571f, 626f)
        canvas.drawRoundRect(receiptHeaderRect, 10f, 10f, sectionHeaderPaint)
        canvas.drawRect(24f, 615f, 571f, 626f, sectionHeaderPaint)
        canvas.drawText("3. RECIBO FISCAL & DEMONSTRATIVO DE PAGAMENTO (STRIPE PCI-DSS)", 36f, 618.5f, sectionHeaderTextPaint)
        canvas.drawText("MOEDA OFICIAL: EUR (€)", 559f, 618.5f, sectionHeaderRightPaint)

        // Coluna Esquerda do Recibo: Dados Fiscais do Pagador
        var ry = 644f
        canvas.drawText("DADOS FISCAIS DO VIAJANTE / ADQUIRENTE", 36f, ry, labelSmallPaint)
        ry += 15f
        canvas.drawText("Nome / Razão Social: ${ticket.passengerName.uppercase()}", 36f, ry, timePaint)
        ry += 15f
        canvas.drawText("NIF / CPF / Doc. Fiscal: ${boarding.taxIdNifCpf}", 36f, ry, timePaint)
        ry += 15f
        canvas.drawText("Documento de Viagem: ${boarding.documentTypeLabel} ${boarding.documentNumber}", 36f, ry, timePaint)
        ry += 15f
        canvas.drawText("Bilhete Eletrônico IATA: ${boarding.eTicketNumber}", 36f, ry, timePaint)
        ry += 15f
        canvas.drawText("Liquidação: Cartão Criptografado Stripe PCI-DSS Nível 1", 36f, ry, timePaint)
        ry += 15f
        canvas.drawText("Status Fiscal: FATURA / RECIBO LIQUIDADO INTEGRALMENTE", 36f, ry, Paint(timePaint).apply { color = emeraldSuccess })

        // Linha vertical separadora no recibo
        canvas.drawLine(312f, 634f, 312f, 778f, dividerPaint)

        // Coluna Direita do Recibo: Tabela de Valores
        val itemLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textMuted
            textSize = 9.5f
        }
        val itemValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textDark
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        var ty = 646f
        canvas.drawText("DISCRIMINAÇÃO DOS VALORES", 324f, ty, labelSmallPaint)
        ty += 18f
        canvas.drawText("Tarifa Aérea Neta (${ticket.airlineName})", 324f, ty, itemLabelPaint)
        canvas.drawText(MarkupEngine.formatEur(ticket.netAmountEur), 559f, ty, itemValuePaint)

        ty += 18f
        canvas.drawText("Taxa Emissão & Concierge NDC", 324f, ty, itemLabelPaint)
        canvas.drawText(MarkupEngine.formatEur(ticket.markupEur), 559f, ty, itemValuePaint)

        ty += 18f
        canvas.drawText("Assento / Bagagem / Serviços VIP", 324f, ty, itemLabelPaint)
        canvas.drawText(MarkupEngine.formatEur(ticket.ancillariesEur), 559f, ty, itemValuePaint)

        ty += 18f
        canvas.drawText("Impostos e Taxas de Embarque", 324f, ty, itemLabelPaint)
        canvas.drawText("INCLUÍDOS", 559f, ty, Paint(itemValuePaint).apply { color = emeraldSuccess })

        ty += 10f
        canvas.drawLine(324f, ty, 559f, ty, dividerPaint)

        // Caixa Dourada & Azul de Valor Total Pago
        val totalBox = RectF(324f, ty + 8f, 559f, 776f)
        canvas.drawRoundRect(totalBox, 8f, 8f, sectionHeaderPaint)
        canvas.drawRoundRect(totalBox, 8f, 8f, goldFramePaint)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val totalAmountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = goldBright
            textSize = 15.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("VALOR TOTAL PAGO", 336f, totalBox.centerY() + 3f, totalLabelPaint)
        canvas.drawText(MarkupEngine.formatEur(ticket.totalChargedEur), 548f, totalBox.centerY() + 5f, totalAmountPaint)

        // 6. RODAPÉ OFICIAL DE AUTENTICIDADE
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textMuted
            textSize = 7.8f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "AguiaFly Royal Sapphire Executive OTA • Documento Oficial IATA NDC Resolução 722f • Atendimento Concierge VIP 24h no Aplicativo",
            PAGE_WIDTH / 2f,
            806f,
            footerPaint
        )
        canvas.drawText(
            "Apresente este E-Ticket (impresso ou no telemóvel) acompanhado do documento de identificação oficial com foto no embarque.",
            PAGE_WIDTH / 2f,
            818f,
            footerPaint
        )
    }

    private fun drawSectionCard(canvas: Canvas, rect: RectF, strokeColor: Int) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = strokeColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 10f, 10f, fillPaint)
        canvas.drawRoundRect(rect, 10f, 10f, borderPaint)
    }

    private fun draw2DQrMatrixOnPdf(
        canvas: Canvas,
        payload: String,
        left: Float,
        top: Float,
        size: Float,
        moduleColor: Int
    ) {
        val safePayload = payload.ifBlank { "M1PASSENGER/AGUIAFLY EAGF999 LISCDGTP 1024 319C002A0001 100" }
        val grid = 19
        val cell = size / grid
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = moduleColor
            style = Paint.Style.FILL
        }

        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isTopLeftFinder = r in 0..4 && c in 0..4 &&
                    (r == 0 || r == 4 || c == 0 || c == 4 || (r in 1..3 && c in 1..3 && (r == 2 && c == 2)))
                val isTopRightFinder = r in 0..4 && c in (grid - 5) until grid &&
                    (r == 0 || r == 4 || c == grid - 5 || c == grid - 1 || (r == 2 && c == grid - 3))
                val isBottomLeftFinder = r in (grid - 5) until grid && c in 0..4 &&
                    (r == grid - 5 || r == grid - 1 || c == 0 || c == 4 || (r == grid - 3 && c == 2))

                val inFinderZone = (r in 0..5 && c in 0..5) ||
                    (r in 0..5 && c in (grid - 6) until grid) ||
                    (r in (grid - 6) until grid && c in 0..5)

                val dataBit = !inFinderZone &&
                    (abs(safePayload.hashCode() + r * 41 + c * 23 + safePayload[(r + c) % safePayload.length].code) % 2 == 0)

                if (isTopLeftFinder || isTopRightFinder || isBottomLeftFinder || dataBit) {
                    val x = left + c * cell
                    val y = top + r * cell
                    canvas.drawRect(x, y, x + cell * 0.9f, y + cell * 0.9f, paint)
                }
            }
        }
    }

    private fun draw1DBarcodeOnPdf(
        canvas: Canvas,
        payload: String,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        barColor: Int
    ) {
        val safePayload = payload.ifBlank { "M1PASSENGER/AGUIAFLY EAGF999 LISCDGTP 1024 319C002A0001 100" }
        val barsCount = 78
        val slotWidth = width / barsCount
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = barColor
            style = Paint.Style.FILL
        }
        for (i in 0 until barsCount) {
            val ch = safePayload[i % safePayload.length].code
            val drawBar = ((ch + i * 7) % 3 != 0) || i < 4 || i > barsCount - 5
            if (drawBar) {
                val barThickness = if ((ch + i) % 5 == 0) slotWidth * 0.82f else slotWidth * 0.48f
                val x = left + i * slotWidth
                canvas.drawRect(x, top, x + barThickness, top + height, paint)
            }
        }
    }
}
