package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.IssuedTicket
import com.example.data.model.MarkupConfig
import com.example.data.model.PriceAlertSubscription
import com.example.data.model.SavedTravelerProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TicketRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private fun sampleTicket(uid: String) = IssuedTicket(
        id = "t_${UUID.randomUUID().toString().replace("-", "")}",
        userId = uid,
        orderId = "ord_test_1234",
        bookingReference = "AETH99",
        passengerName = "RAFAEL RODRIGUES",
        originIata = "LIS",
        originCity = "Lisboa",
        destinationIata = "CDG",
        destinationCity = "Paris",
        departureIso = "2026-11-15T08:30:00Z",
        arrivalIso = "2026-11-15T12:05:00Z",
        airlineName = "Air France",
        flightNumber = "AF1024",
        aircraftName = "Airbus A350-900 XWB",
        cabinClass = "business",
        seatDesignator = "2A",
        netAmountEur = 420.0,
        markupEur = 36.0,
        ancillariesEur = 32.0,
        totalChargedEur = 488.0,
        iataBarcodePayload = "M1RODRIGUES/RAFAEL    EAETH99 LISCDGAF 1024 319C002A0001 100",
        addedToGoogleWallet = false
    )

    @Test
    fun createAndObserveTicket_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice@aether.eu")
        val repo = TicketFirestoreRepository(firestore)
        val ticket = sampleTicket(uid)

        val createResult = withTimeout(5000L) { repo.createTicket(ticket) }
        assertTrue(createResult.isSuccess)
        val createdId = createResult.getOrThrow()

        val observed = withTimeout(4000L) {
            repo.observeUserTickets(uid).first { list -> list.any { it.id == createdId } }
        }
        assertTrue(observed.any { it.id == createdId })
    }

    @Test
    fun getTicketById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser("alice@aether.eu")
        val aliceRepo = TicketFirestoreRepository(firestore)
        val ticketId = withTimeout(5000L) {
            aliceRepo.createTicket(sampleTicket(aliceUid)).getOrThrow()
        }

        signInTestUser("bob@aether.eu")
        val bobRepo = TicketFirestoreRepository(firestore)
        val bobResult = withTimeout(5000L) { bobRepo.getTicketById(ticketId) }
        assertTrue(bobResult.isFailure)
        val fsEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun observeTickets_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = TicketFirestoreRepository(firestore)
        try {
            withTimeout(4000L) {
                repo.observeUserTickets("unauth_uid").first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun saveAndObserveMarkupConfig_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice@aether.eu")
        val repo = TicketFirestoreRepository(firestore)
        val saveRes = withTimeout(5000L) {
            repo.saveMarkupConfig(
                MarkupConfig(
                    userId = uid,
                    fixedMarkupEur = 3.5,
                    percentageMarkup = 3.5,
                    seatSelectionFeeEur = 15.0,
                    extraBaggageFeeEur = 25.0,
                    priorityBoardingFeeEur = 18.0
                )
            )
        }
        assertTrue(saveRes.isSuccess)
    }

    @Test
    fun createObserveAndUpdatePriceAlert_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser("alice@aether.eu")
        val alertRepo = PriceAlertFirestoreRepository(firestore)
        val alertId = "alert_${UUID.randomUUID().toString().replace("-", "")}"
        val alert = PriceAlertSubscription(
            id = alertId,
            userId = uid,
            originIata = "LIS",
            destinationIata = "CDG",
            cabinClass = "business",
            targetMaxPriceEur = 340.0,
            baselinePriceEur = 410.0,
            lastObservedPriceEur = 410.0,
            fcmToken = "fcm_test_token_12345678",
            isActive = true,
            promoTriggered = false
        )

        val createRes = withTimeout(5000L) { alertRepo.createPriceAlert(alert) }
        assertTrue(createRes.isSuccess)

        val updateRes = withTimeout(5000L) {
            alertRepo.updateObservedPriceAndPromoState(
                alertId = alertId,
                observedPriceEur = 319.0,
                promoTriggered = true
            )
        }
        assertTrue(updateRes.isSuccess)

        val observed = withTimeout(4000L) {
            alertRepo.observeUserPriceAlerts(uid).first { list -> list.any { it.id == alertId && it.promoTriggered } }
        }
        assertTrue(observed.any { it.id == alertId && it.promoTriggered })
    }

    @Test
    fun saveObserveAndCrossUserProtectTravelerProfile_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice@aether.eu")
        val profileRepo = TravelerProfileFirestoreRepository(firestore)
        val sampleProfile = SavedTravelerProfile(
            userId = aliceUid,
            firstName = "Rafael",
            lastName = "Rodrigues",
            email = "rafael@aguiafly.com",
            phone = "+5511999998888",
            documentNumber = "BR99887766",
            birthDate = "1992-05-15",
            expiryDate = "2032-08-20",
            frequentFlyerProgram = "TAP Miles&Go",
            frequentFlyerNumber = "TP123456789",
            autoCheckInEnabled = true
        )

        val saveRes = withTimeout(5000L) { profileRepo.saveTravelerProfile(sampleProfile) }
        assertTrue(saveRes.isSuccess)

        val observed = withTimeout(4000L) {
            profileRepo.observeTravelerProfile(aliceUid).first { it != null && it.documentNumber == "BR99887766" }
        }
        assertNotNull(observed)
        assertEquals("Rafael", observed?.firstName)

        // Cross-user read attempt by Bob must fail with PERMISSION_DENIED
        signInTestUser("bob@aether.eu")
        val bobResult = withTimeout(5000L) { profileRepo.getTravelerProfile(aliceUid) }
        assertTrue(bobResult.isFailure)
        val fsEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }
}
