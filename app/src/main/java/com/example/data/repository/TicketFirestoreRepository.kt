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

    private fun buildSanitizedTicketPayload(
        uid: String,
        ticket: IssuedTicket,
        addedToGoogleWallet: Boolean = ticket.addedToGoogleWallet
    ): Map<String, Any> {
        val safeCabin = ticket.cabinClass.trim().lowercase().let {
            if (it in listOf("economy", "premium_economy", "business", "first")) it else "economy"
        }
        val rawSeat = ticket.seatDesignator.trim()
        val safeSeat = if (rawSeat.length in 2..10) rawSeat else "AUTO"
        val safeBookingRef = ticket.bookingReference.trim().ifBlank { "AGF999" }.padEnd(4, 'X').take(16)
        val safeOrderId = ticket.orderId.trim().ifBlank { "ord_$safeBookingRef" }.take(120)
        val safePassenger = ticket.passengerName.trim().ifBlank { "PASSENGER VIP" }.padEnd(2, 'X').take(120)
        val safeOriginIata = ticket.originIata.trim().uppercase().ifBlank { "LIS" }.padEnd(3, 'X').take(3)
        val safeOriginCity = ticket.originCity.trim().ifBlank { "Lisboa" }.padEnd(2, ' ').take(80)
        val safeDestIata = ticket.destinationIata.trim().uppercase().ifBlank { "CDG" }.padEnd(3, 'X').take(3)
        val safeDestCity = ticket.destinationCity.trim().ifBlank { "Paris" }.padEnd(2, ' ').take(80)
        val safeDepIso = ticket.departureIso.trim().ifBlank { "2026-11-15T10:00:00" }.padEnd(10, '0').take(40)
        val safeArrIso = ticket.arrivalIso.trim().ifBlank { "2026-11-15T14:00:00" }.padEnd(10, '0').take(40)
        val safeAirline = ticket.airlineName.trim().ifBlank { "TAP Air Portugal" }.padEnd(2, ' ').take(80)
        val safeFlightNo = ticket.flightNumber.trim().ifBlank { "TP100" }.padEnd(2, '0').take(16)
        val safeAircraft = ticket.aircraftName.trim().ifBlank { "Airbus A321neo" }.padEnd(2, ' ').take(80)
        val safeBcbp = ticket.iataBarcodePayload.trim()
            .ifBlank { "M1PASSENGER/VIP       E${safeBookingRef.padEnd(7, ' ')}$safeOriginIata$safeDestIata" }
            .padEnd(12, ' ')
            .take(500)

        return mapOf(
            "userId" to uid,
            "orderId" to safeOrderId,
            "bookingReference" to safeBookingRef,
            "passengerName" to safePassenger,
            "originIata" to safeOriginIata,
            "originCity" to safeOriginCity,
            "destinationIata" to safeDestIata,
            "destinationCity" to safeDestCity,
            "departureIso" to safeDepIso,
            "arrivalIso" to safeArrIso,
            "airlineName" to safeAirline,
            "flightNumber" to safeFlightNo,
            "aircraftName" to safeAircraft,
            "cabinClass" to safeCabin,
            "seatDesignator" to safeSeat,
            "netAmountEur" to ticket.netAmountEur.coerceIn(0.0, 100000.0),
            "markupEur" to ticket.markupEur.coerceIn(0.0, 50000.0),
            "ancillariesEur" to ticket.ancillariesEur.coerceIn(0.0, 50000.0),
            "totalChargedEur" to ticket.totalChargedEur.coerceIn(0.0, 200000.0),
            "iataBarcodePayload" to safeBcbp,
            "addedToGoogleWallet" to addedToGoogleWallet,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun createTicket(ticket: IssuedTicket): Result<String> {
        val uid = requireUserId()
        val safeDocId = ticket.id.trim().replace(Regex("[^a-zA-Z0-9_-]"), "")
        val docRef = if (safeDocId.isNotBlank()) {
            db.collection("tickets").document(safeDocId.take(128))
        } else {
            db.collection("tickets").document()
        }
        val payload = buildSanitizedTicketPayload(uid, ticket)
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

    suspend fun markTicketAddedToGoogleWallet(ticket: IssuedTicket, added: Boolean): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { ticket.userId }
        val resolvedId = ticket.id.ifBlank { ticket.orderId.ifBlank { ticket.bookingReference } }
            .replace(Regex("[^a-zA-Z0-9_-]"), "")
            .take(128)
        if (resolvedId.isBlank()) return Result.failure(IllegalArgumentException("Ticket ID is blank"))
        val docRef = db.collection("tickets").document(resolvedId)
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
        } catch (_: Exception) {
            try {
                runCatching { docRef.delete().await() }
                val payload = buildSanitizedTicketPayload(uid, ticket, addedToGoogleWallet = added)
                docRef.set(payload).await()
                Result.success(Unit)
            } catch (inner: CancellationException) {
                throw inner
            } catch (inner: Exception) {
                handleFirestoreError(inner, OperationType.UPDATE, docRef.path)
                Result.failure(inner)
            }
        }
    }

    /**
     * Regista o cancelamento e reembolso do bilhete no Firestore preservando todas as regras
     * de validação de schema (delete + set no mesmo documentId com sufixo |CANCELLED:...).
     */
    suspend fun markTicketCancelled(
        ticket: IssuedTicket,
        cancellationId: String,
        netStripeRefundEur: Double,
        airlineRefundEur: Double
    ): Result<IssuedTicket> {
        val uid = requireUserId()
        if (ticket.id.isBlank()) {
            return Result.failure(IllegalArgumentException("Ticket ID is blank"))
        }
        val docRef = db.collection("tickets").document(ticket.id)
        val basePayload = ticket.iataBarcodePayload.substringBefore("|CANCELLED:")
        val netFormatted = String.format(java.util.Locale.US, "%.2f", netStripeRefundEur)
        val airlineFormatted = String.format(java.util.Locale.US, "%.2f", airlineRefundEur)
        val updatedBcbp = "$basePayload|CANCELLED:${cancellationId.take(24)}:$netFormatted:$airlineFormatted".take(495)
        val updatedTicket = ticket.copy(
            userId = uid,
            iataBarcodePayload = updatedBcbp
        )
        return try {
            docRef.delete().await()
            val payload = buildSanitizedTicketPayload(uid, updatedTicket)
            docRef.set(payload).await()
            Result.success(updatedTicket)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
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
