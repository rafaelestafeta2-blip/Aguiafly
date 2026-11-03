package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CloudPassportVisualProfile
import com.example.data.model.SavedTravelerProfile
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

class TravelerProfileFirestoreRepository(
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

    fun observeTravelerProfile(userId: String = requireUserId()): Flow<SavedTravelerProfile?> = flow {
        val docRef = db.collection("traveler_profiles").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(
                            SavedTravelerProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun getTravelerProfile(userId: String = requireUserId()): Result<SavedTravelerProfile?> {
        val docRef = db.collection("traveler_profiles").document(userId)
        return try {
            val snap = docRef.get().await()
            val profile = if (snap.exists()) {
                snap.toObject(
                    SavedTravelerProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveTravelerProfile(profile: SavedTravelerProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("traveler_profiles").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "firstName" to profile.firstName.trim().take(80),
            "lastName" to profile.lastName.trim().take(80),
            "email" to profile.email.trim().take(120),
            "phone" to profile.phone.trim().take(32),
            "documentNumber" to profile.documentNumber.trim().uppercase().take(40),
            "birthDate" to profile.birthDate.trim().take(20),
            "expiryDate" to profile.expiryDate.trim().take(20),
            "frequentFlyerProgram" to profile.frequentFlyerProgram.trim().take(40),
            "frequentFlyerNumber" to profile.frequentFlyerNumber.trim().take(40),
            "autoCheckInEnabled" to profile.autoCheckInEnabled,
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

    suspend fun deleteTravelerProfile(): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("traveler_profiles").document(uid)
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

    fun observePassportVisualProfile(userId: String = requireUserId()): Flow<CloudPassportVisualProfile?> = flow {
        val docRef = db.collection("passport_profiles").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) {
                        null
                    } else {
                        snapshot.toObject(
                            CloudPassportVisualProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, docRef.path)
                    throw error
                }
        )
    }

    suspend fun getPassportVisualProfile(userId: String = requireUserId()): Result<CloudPassportVisualProfile?> {
        val docRef = db.collection("passport_profiles").document(userId)
        return try {
            val snap = docRef.get().await()
            val visual = if (snap.exists()) {
                snap.toObject(
                    CloudPassportVisualProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
            Result.success(visual)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePassportVisualProfile(profile: CloudPassportVisualProfile): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("passport_profiles").document(uid)
        val payload = mapOf(
            "userId" to uid,
            "profilePhotoData" to profile.profilePhotoData.take(350_000),
            "coverPhotoData" to profile.coverPhotoData.take(350_000),
            "coverPreset" to profile.coverPreset.trim().ifBlank { "IMPERIAL_NAVY_GOLD" }.take(40),
            "nationality" to profile.nationality.trim().ifBlank { "PORTUGAL / BRASIL" }.take(80),
            "memberTierTitle" to profile.memberTierTitle.trim().ifBlank { "AguiaFly Executive Gold • Passaporte Oficial" }.take(120),
            "passportCustomNumber" to profile.passportCustomNumber.trim().take(40),
            "stampPhotosJson" to profile.stampPhotosJson.ifBlank { "{}" }.take(450_000),
            "stampNotesJson" to profile.stampNotesJson.ifBlank { "{}" }.take(50_000),
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
