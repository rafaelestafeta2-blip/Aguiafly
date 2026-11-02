package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.pricing.MarkupEngine
import kotlin.random.Random

/**
 * Helper responsible for creating the Android Notification Channel and
 * dispatching high-priority Push Notifications for promotional flight fares.
 */
object PushNotificationHelper {
    const val CHANNEL_ID = "aguiafly_promo_alerts"
    private const val CHANNEL_NAME = "Alertas de Promoções AguiaFly"
    private const val CHANNEL_DESC = "Notificações Push de queda de preço e tarifas promocionais NDC"

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

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("promo_origin", originIata)
            putExtra("promo_destination", destinationIata)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Random.nextInt(1000, 9999),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val savingsEur = (baselinePriceEur - promoPriceEur).coerceAtLeast(0.0)
        val title = "🔥 Alerta de Promoção: $originIata ➔ $destinationIata por ${MarkupEngine.formatEur(promoPriceEur)}!"
        val body = buildString {
            append("$airlineName (${cabinClass.uppercase()}) atingiu sua meta! ")
            if (savingsEur > 0.0) {
                append("Economia imediata de ${MarkupEngine.formatEur(savingsEur)} face à tarifa anterior (${MarkupEngine.formatEur(baselinePriceEur)}).")
            } else {
                append("Tarifa promocional disponível agora no AguiaFly.")
            }
        }

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
            NotificationManagerCompat.from(context).notify(Random.nextInt(2000, 99999), notification)
        } catch (_: SecurityException) {
            // Ignored if permission was revoked at runtime
        }
    }
}
