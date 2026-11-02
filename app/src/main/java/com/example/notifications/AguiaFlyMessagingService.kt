package com.example.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Firebase Cloud Messaging (FCM) service that receives background and foreground
 * promotional flight price drop notifications from the AguiaFly backend cron monitor.
 */
class AguiaFlyMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("AguiaFlyFCM", "Novo token FCM registado no dispositivo: ${token.take(16)}...")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val origin = data["originIata"] ?: "LIS"
        val destination = data["destinationIata"] ?: "CDG"
        val cabin = data["cabinClass"] ?: "business"
        val promoPrice = data["promoPriceEur"]?.toDoubleOrNull() ?: 299.0
        val baselinePrice = data["baselinePriceEur"]?.toDoubleOrNull() ?: 389.0
        val airlineName = data["airlineName"] ?: message.notification?.title ?: "Tarifa Promocional AguiaFly"

        PushNotificationHelper.showPromotionalPriceNotification(
            context = applicationContext,
            originIata = origin,
            destinationIata = destination,
            cabinClass = cabin,
            promoPriceEur = promoPrice,
            baselinePriceEur = baselinePrice,
            airlineName = airlineName
        )
    }
}
