package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.IssuedTicket
import com.example.data.model.MarkupConfig
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TicketFirestoreRepository(
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

    fun observeUserTickets(userId: String = requireUserId()): Flow<List<IssuedTicket>> = flow {
        val path = "tickets"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        IssuedTicket::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getTicketById(ticketId: String): Result<IssuedTicket> {
        val docRef = db.collection("tickets").document(ticketId)
        return try {
            val snap = docRef.get().await()
            val ticket = snap.toObject(
                IssuedTicket::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Ticket not found")
            Result.success(ticket)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createTicket(ticket: IssuedTicket): Result<String> {
        val uid = requireUserId()
        val docRef = if (ticket.id.isNotBlank()) {
            db.collection("tickets").document(ticket.id)
        } else {
            db.collection("tickets").document()
        }
        val payload = mapOf(
            "userId" to uid,
            "orderId" to ticket.orderId,
            "bookingReference" to ticket.bookingReference,
            "passengerName" to ticket.passengerName,
            "originIata" to ticket.originIata,
            "originCity" to ticket.originCity,
            "destinationIata" to ticket.destinationIata,
            "destinationCity" to ticket.destinationCity,
            "departureIso" to ticket.departureIso,
            "arrivalIso" to ticket.arrivalIso,
            "airlineName" to ticket.airlineName,
            "flightNumber" to ticket.flightNumber,
            "aircraftName" to ticket.aircraftName,
            "cabinClass" to ticket.cabinClass,
            "seatDesignator" to ticket.seatDesignator,
            "netAmountEur" to ticket.netAmountEur,
            "markupEur" to ticket.markupEur,
            "ancillariesEur" to ticket.ancillariesEur,
            "totalChargedEur" to ticket.totalChargedEur,
            "iataBarcodePayload" to ticket.iataBarcodePayload,
            "addedToGoogleWallet" to ticket.addedToGoogleWallet,
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

    suspend fun markAddedToGoogleWallet(ticketId: String, added: Boolean): Result<Unit> {
        val docRef = db.collection("tickets").document(ticketId)
        return try {
            docRef.update(
                mapOf(
                    "addedToGoogleWallet" to added,
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

    fun observeMarkupConfig(userId: String = requireUserId()): Flow<MarkupConfig> = flow {
        val docRef = db.collection("markup_configs").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snap ->
                    val loaded = snap.toObject(
                        MarkupConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: MarkupConfig(userId = userId)
                    if (loaded.fixedMarkupEur == 15.0 && loaded.percentageMarkup == 5.0) {
                        loaded.copy(fixedMarkupEur = 3.50, percentageMarkup = 3.5)
                    } else {
                        loaded
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun saveMarkupConfig(config: MarkupConfig): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("markup_configs").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "fixedMarkupEur" to config.fixedMarkupEur,
            "percentageMarkup" to config.percentageMarkup,
            "seatSelectionFeeEur" to config.seatSelectionFeeEur,
            "extraBaggageFeeEur" to config.extraBaggageFeeEur,
            "priorityBoardingFeeEur" to config.priorityBoardingFeeEur,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
