package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PromotionalDealsCatalog
import com.example.data.model.TravelPromotionDeal
import com.example.domain.pricing.MarkupEngine

/**
 * Helper responsible for creating the Android Notification Channel,
 * dispatching intelligent, non-intrusive Push Notifications for promotional flight fares,
 * and running the Automatic Global Multi-Country Super-Promo Radar on launch and in background.
 */
object PushNotificationHelper {
    const val CHANNEL_ID = "aguiafly_promo_alerts"
    private const val CHANNEL_NAME = "Radar Global de Super Promoções AguiaFly"
    private const val CHANNEL_DESC = "Notificações Push inteligentes de super promoções internacionais e quedas reais de tarifa NDC"

    private const val PREFS_NAME = "aguiafly_global_promo_radar_prefs"
    private const val KEY_AUTO_RADAR_ENABLED = "auto_global_radar_enabled"
    private const val KEY_LAST_AUTO_PUSH_MS = "last_auto_push_timestamp_ms"
    private const val KEY_LAST_DEAL_INDEX = "last_notified_deal_index"
    private const val KEY_LAST_NOTIFIED_DEAL_ID = "last_notified_deal_id"
    private const val KEY_ROUTE_ALERT_LAST_MS_PREFIX = "route_alert_last_ms_"
    private const val GLOBAL_RADAR_JOB_ID = 7701

    // ID fixo para o Radar Global: substitui silenciosamente qualquer banner anterior em vez de empilhar várias notificações
    private const val NOTIFICATION_ID_GLOBAL_PROMO = 8801
    private const val NOTIFICATION_ID_ROUTE_BASE = 8900

    // Janela inteligente de 4 horas entre notificações automáticas globais para evitar spam
    private const val SMART_PROMO_COOLDOWN_MS = 4L * 60L * 60L * 1000L
    // Janela mínima de 2 horas para alertas de rota específica
    private const val ROUTE_ALERT_COOLDOWN_MS = 2L * 60L * 60L * 1000L

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isGlobalAutoPromoRadarEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_RADAR_ENABLED, true)
    }

    fun setGlobalAutoPromoRadarEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_RADAR_ENABLED, enabled).apply()
        if (enabled) {
            scheduleBackgroundGlobalPromoRadar(context)
        } else {
            cancelBackgroundGlobalPromoRadar(context)
        }
    }

    /**
     * Agenda o Radar Global Automático em segundo plano usando JobScheduler nativo do Android
     * para verificar super promoções para diferentes países periodicamente.
     */
    fun scheduleBackgroundGlobalPromoRadar(context: Context) {
        if (!isGlobalAutoPromoRadarEnabled(context)) return
        try {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            val alreadyScheduled = scheduler.allPendingJobs.any { it.id == GLOBAL_RADAR_JOB_ID }
            if (alreadyScheduled) return

            val serviceComponent = ComponentName(context, GlobalPromoRadarJobService::class.java)
            val builder = JobInfo.Builder(GLOBAL_RADAR_JOB_ID, serviceComponent)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(false)
                .setPeriodic(6L * 60L * 60L * 1000L) // A cada 6 horas em segundo plano

            scheduler.schedule(builder.build())
        } catch (_: Exception) {
            // Ignora em ambientes de teste sem JobScheduler ativo
        }
    }

    private fun cancelBackgroundGlobalPromoRadar(context: Context) {
        try {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
            scheduler.cancel(GLOBAL_RADAR_JOB_ID)
        } catch (_: Exception) {
        }
    }

    /**
     * Dispara de forma inteligente uma única notificação Push de Super Promoção Internacional
     * apenas quando respeitado o intervalo inteligente (4h) e priorizando a menor tarifa real.
     */
    @Synchronized
    fun triggerAutoGlobalPromoRadarIfEligible(
        context: Context,
        currentOriginIata: String,
        deals: List<TravelPromotionDeal> = emptyList(),
        forceImmediate: Boolean = false
    ): TravelPromotionDeal? {
        ensureNotificationChannel(context)
        if (!forceImmediate && !isGlobalAutoPromoRadarEnabled(context)) return null
        if (!hasNotificationPermission(context)) return null

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastPushMs = prefs.getLong(KEY_LAST_AUTO_PUSH_MS, 0L)

        if (!forceImmediate && (now - lastPushMs) < SMART_PROMO_COOLDOWN_MS) {
            return null
        }

        val catalogDeals = if (deals.isNotEmpty()) {
            deals
        } else {
            PromotionalDealsCatalog.getFilteredDeals("Todas", currentOriginIata)
        }
        if (catalogDeals.isEmpty()) return null

        val lastNotifiedDealId = prefs.getString(KEY_LAST_NOTIFIED_DEAL_ID, null)
        // Seleciona de forma inteligente a promoção mais barata e com maior desconto que ainda não foi notificada
        val sortedByBestPrice = catalogDeals.sortedWith(
            compareBy<TravelPromotionDeal> { it.displayTotalEur }
                .thenByDescending { it.discountPercent }
        )
        val chosenDeal = sortedByBestPrice.firstOrNull { it.id != lastNotifiedDealId }
            ?: sortedByBestPrice.first()

        val nextIdx = (prefs.getInt(KEY_LAST_DEAL_INDEX, -1) + 1) % catalogDeals.size
        prefs.edit()
            .putLong(KEY_LAST_AUTO_PUSH_MS, now)
            .putInt(KEY_LAST_DEAL_INDEX, nextIdx)
            .putString(KEY_LAST_NOTIFIED_DEAL_ID, chosenDeal.id)
            .apply()

        dispatchGlobalPromoNotificationInternal(context, chosenDeal)
        return chosenDeal
    }

    fun showAutomaticGlobalCountryPromoNotification(
        context: Context,
        deal: TravelPromotionDeal
    ) {
        // Respeita o filtro inteligente para nunca disparar notificações seguidas
        triggerAutoGlobalPromoRadarIfEligible(
            context = context,
            currentOriginIata = deal.originIata,
            deals = listOf(deal),
            forceImmediate = false
        )
    }

    private fun dispatchGlobalPromoNotificationInternal(
        context: Context,
        deal: TravelPromotionDeal
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("promo_origin", deal.originIata)
            putExtra("promo_destination", deal.destinationIata)
            putExtra("promo_deal_id", deal.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_GLOBAL_PROMO,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🌍 Super Promoção (${deal.discountPercent}% OFF): ${deal.originCity} ➔ ${deal.destinationCity}, ${deal.destinationCountry}!"
        val body = "Menor tarifa encontrada pelo Radar Inteligente AguiaFly: voe de ${deal.originIata} para ${deal.destinationCity} (${deal.destinationIata}) por apenas ${deal.formattedDisplayPrice} (antes ${deal.formattedOriginalPrice}) na ${deal.airlineName}. Toque para reservar!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_GLOBAL_PROMO, notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked at runtime
        }
    }

    @Synchronized
    fun showPromotionalPriceNotification(
        context: Context,
        originIata: String,
        destinationIata: String,
        cabinClass: String,
        promoPriceEur: Double,
        baselinePriceEur: Double,
        airlineName: String = "Tarifa Direta NDC"
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val routeKey = "${originIata.uppercase()}_${destinationIata.uppercase()}"
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastRoutePushMs = prefs.getLong("$KEY_ROUTE_ALERT_LAST_MS_PREFIX$routeKey", 0L)
        if ((now - lastRoutePushMs) < ROUTE_ALERT_COOLDOWN_MS) {
            return
        }
        prefs.edit().putLong("$KEY_ROUTE_ALERT_LAST_MS_PREFIX$routeKey", now).apply()

        val routeNotificationId = NOTIFICATION_ID_ROUTE_BASE + (routeKey.hashCode() and 0x0FFF)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("promo_origin", originIata)
            putExtra("promo_destination", destinationIata)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            routeNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val savingsEur = (baselinePriceEur - promoPriceEur).coerceAtLeast(0.0)
        val title = "🔥 Alerta de Preço: $originIata ➔ $destinationIata por ${MarkupEngine.formatEur(promoPriceEur)}!"
        val body = buildString {
            append("$airlineName (${cabinClass.uppercase()}) atingiu sua meta! ")
            if (savingsEur > 0.0) {
                append("Economia imediata de ${MarkupEngine.formatEur(savingsEur)} face à tarifa anterior (${MarkupEngine.formatEur(baselinePriceEur)}). Toque para emitir agora.")
            } else {
                append("Tarifa promocional disponível agora no AguiaFly. Toque para emitir.")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(routeNotificationId, notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked at runtime
        }
    }

    fun showTicketPdfDownloadedNotification(
        context: Context,
        ticket: com.example.data.model.IssuedTicket,
        pdfUri: android.net.Uri
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(pdfUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val fallbackIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val targetIntent = if (viewIntent.resolveActivity(context.packageManager) != null) {
            viewIntent
        } else {
            fallbackIntent
        }

        val notifId = 9100 + (ticket.bookingReference.hashCode() and 0x0FFF)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📄 E-Ticket PDF Baixado • PNR ${ticket.bookingReference}"
        val body = "Arquivo AguiaFly_ETicket_${ticket.bookingReference}.pdf salvo na pasta Downloads (${ticket.originIata} ➔ ${ticket.destinationIata} • Voo ${ticket.flightNumber}). Toque para abrir."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showGoogleWalletPassAddedNotification(
        context: Context,
        ticket: com.example.data.model.IssuedTicket
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val notifId = 9500 + (ticket.bookingReference.hashCode() and 0x0FFF)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "💳 Cartão de Embarque na Google Wallet • ${ticket.bookingReference}"
        val body = "Voo ${ticket.flightNumber} (${ticket.originIata} ➔ ${ticket.destinationIata}) • Assento ${ticket.seatDesignator} sincronizado na sua Carteira Digital e pronto para embarque."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        }
    }
}

