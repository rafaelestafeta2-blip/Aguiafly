package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class SavedTravelerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val documentNumber: String = "",
    val birthDate: String = "",
    val expiryDate: String = "",
    val frequentFlyerProgram: String = "",
    val frequentFlyerNumber: String = "",
    val autoCheckInEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val isPopulated: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            phone.isNotBlank() &&
            documentNumber.isNotBlank()
}
