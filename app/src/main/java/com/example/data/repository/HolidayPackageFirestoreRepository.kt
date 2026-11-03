package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.HolidayPackage
import com.example.data.model.PackageBookingRecord
import com.example.data.model.PackageHighlightBadge
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

/**
 * Repositório Firestore para:
 * 1. Painel de Gestão Manual (Admin CMS) de Pacotes de Férias & Experiências (/holiday_packages)
 * 2. Reservas Diretas e Sinais cobrados via Stripe (/package_bookings)
 * 3. Atualização automática do inventário de vagas restantes nos pacotes manuais
 */
class HolidayPackageFirestoreRepository(
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

    /**
     * Observa em tempo real todos os pacotes cadastrados manualmente no Admin CMS para o utilizador/admin.
     */
    fun observeAdminCmsPackages(userId: String = requireUserId()): Flow<List<HolidayPackage>> = flow {
        val path = "holiday_packages"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        HolidayPackage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getPackageById(packageId: String): Result<HolidayPackage> {
        val docRef = db.collection("holiday_packages").document(packageId)
        return try {
            val snap = docRef.get().await()
            val pkg = snap.toObject(
                HolidayPackage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Holiday package not found")
            Result.success(pkg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Cadastra um novo Pacote de Férias / Experiência Exclusiva no Painel Admin CMS (/holiday_packages).
     */
    suspend fun createHolidayPackage(pkg: HolidayPackage): Result<String> {
        val uid = requireUserId()
        val docRef = if (pkg.id.isNotBlank()) {
            db.collection("holiday_packages").document(pkg.id)
        } else {
            db.collection("holiday_packages").document()
        }
        val finalPrice = HolidayPackage.calculateFinalPricePerPerson(
            netEur = pkg.netPricePerPersonEur,
            commissionPercent = pkg.aguiaflyCommissionPercent,
            fixedFeeEur = pkg.aguiaflyFixedFeeEur
        )
        val payload = mapOf(
            "userId" to uid,
            "packageCode" to pkg.packageCode.ifBlank { "AGF-CMS-${docRef.id.take(5).uppercase()}" },
            "title" to pkg.title,
            "destinationCity" to pkg.destinationCity,
            "destinationIata" to pkg.destinationIata.uppercase().take(3).padEnd(3, 'X'),
            "durationLabel" to pkg.durationLabel,
            "durationNights" to pkg.durationNights,
            "netPricePerPersonEur" to pkg.netPricePerPersonEur,
            "aguiaflyCommissionPercent" to pkg.aguiaflyCommissionPercent,
            "aguiaflyFixedFeeEur" to pkg.aguiaflyFixedFeeEur,
            "finalPricePerPersonEur" to finalPrice,
            "depositPercentage" to pkg.depositPercentage.coerceIn(5, 100),
            "availableSpots" to pkg.availableSpots.coerceIn(0, 1000),
            "totalSpots" to pkg.totalSpots.coerceIn(1, 1000),
            "partnerAgencyName" to pkg.partnerAgencyName,
            "sourceType" to pkg.sourceType,
            "badgeType" to pkg.badgeType,
            "inclusionsCsv" to pkg.inclusionsCsv,
            "galleryPhotosJson" to pkg.galleryPhotosJson,
            "itineraryDaysJson" to pkg.itineraryDaysJson,
            "routeWaypointsJson" to pkg.routeWaypointsJson,
            "isActive" to pkg.isActive,
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

    /**
     * Atualiza automaticamente o inventário de vagas restantes nos pacotes manuais após reserva Stripe.
     */
    suspend fun updatePackageInventorySpots(
        packageId: String,
        newAvailableSpots: Int
    ): Result<Unit> {
        val docRef = db.collection("holiday_packages").document(packageId)
        val clampedSpots = newAvailableSpots.coerceIn(0, 1000)
        val updateFields = mutableMapOf<String, Any>(
            "availableSpots" to clampedSpots,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (clampedSpots in 1..4) {
            updateFields["badgeType"] = PackageHighlightBadge.ULTIMAS_VAGAS.code
        }
        return try {
            docRef.update(updateFields).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteHolidayPackage(packageId: String): Result<Unit> {
        val docRef = db.collection("holiday_packages").document(packageId)
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

    /**
     * Observa em tempo real todas as reservas de pacotes & experiências do utilizador (/package_bookings).
     */
    fun observeUserPackageBookings(userId: String = requireUserId()): Flow<List<PackageBookingRecord>> = flow {
        val path = "package_bookings"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PackageBookingRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun getPackageBookingById(bookingId: String): Result<PackageBookingRecord> {
        val docRef = db.collection("package_bookings").document(bookingId)
        return try {
            val snap = docRef.get().await()
            val booking = snap.toObject(
                PackageBookingRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: throw NoSuchElementException("Package booking not found")
            Result.success(booking)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Regista no Firestore uma reserva direta ou pagamento de sinal de pacote/experiência via Stripe.
     */
    suspend fun createPackageBooking(booking: PackageBookingRecord): Result<String> {
        val uid = requireUserId()
        val docRef = if (booking.id.isNotBlank()) {
            db.collection("package_bookings").document(booking.id)
        } else {
            db.collection("package_bookings").document()
        }
        val payload = mapOf(
            "userId" to uid,
            "bookingCode" to booking.bookingCode,
            "packageId" to booking.packageId,
            "packageTitle" to booking.packageTitle,
            "destinationCity" to booking.destinationCity,
            "destinationIata" to booking.destinationIata.uppercase().take(3).padEnd(3, 'X'),
            "durationLabel" to booking.durationLabel,
            "sourceType" to booking.sourceType,
            "partnerName" to booking.partnerName,
            "partnerConfirmationCode" to booking.partnerConfirmationCode,
            "travelerFullName" to booking.travelerFullName,
            "travelerEmail" to booking.travelerEmail,
            "travelerPhone" to booking.travelerPhone,
            "guestsCount" to booking.guestsCount.coerceIn(1, 50),
            "paymentMode" to booking.paymentMode,
            "totalPackageGrossEur" to booking.totalPackageGrossEur,
            "amountChargedNowEur" to booking.amountChargedNowEur,
            "remainingBalanceEur" to booking.remainingBalanceEur,
            "aguiaflyCommissionEur" to booking.aguiaflyCommissionEur,
            "stripeChargeId" to booking.stripeChargeId,
            "status" to booking.status,
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
}
