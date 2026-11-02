package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.roundToInt

/**
 * Firestore model representing a user's route price alert subscription
 * with Firebase Cloud Messaging (FCM) push token integration.
 */
@IgnoreExtraProperties
data class PriceAlertSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val originIata: String = "LIS",
    val destinationIata: String = "CDG",
    val cabinClass: String = "business",
    val targetMaxPriceEur: Double = 350.0,
    val baselinePriceEur: Double = 420.0,
    val lastObservedPriceEur: Double = 420.0,
    val fcmToken: String = "",
    val isActive: Boolean = true,
    val promoTriggered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val discountPercentage: Int
        get() {
            if (baselinePriceEur <= 0.0 || lastObservedPriceEur >= baselinePriceEur) return 0
            return (((baselinePriceEur - lastObservedPriceEur) / baselinePriceEur) * 100.0)
                .roundToInt()
                .coerceAtLeast(1)
        }
}
