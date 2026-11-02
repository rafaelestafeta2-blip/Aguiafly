package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.PriceAlertSubscription
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class PriceAlertFirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun fetchDeviceFcmToken(): String {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (token.isNullOrBlank()) {
                "fcm_aguiafly_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            } else {
                token.take(500)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("PriceAlertRepo", "FCM token fallback used on device: ${e.message}")
            "fcm_aguiafly_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        }
    }

    fun observeUserPriceAlerts(userId: String = requireUserId()): Flow<List<PriceAlertSubscription>> = flow {
        val path = "price_alerts"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PriceAlertSubscription::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createPriceAlert(alert: PriceAlertSubscription): Result<String> {
        val uid = requireUserId()
        val docRef = if (alert.id.isNotBlank()) {
            db.collection("price_alerts").document(alert.id)
        } else {
            db.collection("price_alerts").document()
        }
        val resolvedToken = alert.fcmToken.ifBlank { fetchDeviceFcmToken() }
        val payload = mapOf(
            "userId" to uid,
            "originIata" to alert.originIata.uppercase().take(3),
            "destinationIata" to alert.destinationIata.uppercase().take(3),
            "cabinClass" to alert.cabinClass,
            "targetMaxPriceEur" to alert.targetMaxPriceEur.coerceIn(10.0, 100000.0),
            "baselinePriceEur" to alert.baselinePriceEur.coerceIn(10.0, 100000.0),
            "lastObservedPriceEur" to alert.lastObservedPriceEur.coerceIn(10.0, 100000.0),
            "fcmToken" to resolvedToken,
            "isActive" to alert.isActive,
            "promoTriggered" to alert.promoTriggered,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateObservedPriceAndPromoState(
        alertId: String,
        observedPriceEur: Double,
        promoTriggered: Boolean
    ): Result<Unit> {
        val docRef = db.collection("price_alerts").document(alertId)
        return try {
            docRef.update(
                mapOf(
                    "lastObservedPriceEur" to observedPriceEur.coerceIn(10.0, 100000.0),
                    "promoTriggered" to promoTriggered,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePriceAlert(alertId: String): Result<Unit> {
        val docRef = db.collection("price_alerts").document(alertId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
