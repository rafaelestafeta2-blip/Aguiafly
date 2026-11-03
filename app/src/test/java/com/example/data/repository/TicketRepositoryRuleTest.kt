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

    @Test
    fun saveObserveAndCrossUserProtectPassportVisualProfile_succeeds() = runBlocking {
        val aliceUid = signInTestUser("alice@aether.eu")
        val profileRepo = TravelerProfileFirestoreRepository(firestore)
        val visualProfile = com.example.data.model.CloudPassportVisualProfile(
            id = aliceUid,
            userId = aliceUid,
            profilePhotoData = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
            coverPhotoData = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
            coverPreset = "FIRST_CLASS_SUITE",
            nationality = "PORTUGAL / BRASIL",
            memberTierTitle = "AguiaFly Executive Gold",
            passportCustomNumber = "PT99887766",
            stampPhotosJson = "{\"ord_1\":[\"data:image/jpeg;base64,/9j/4AAQSkZ\"]}",
            stampNotesJson = "{\"ord_1\":\"Ibiza\"}"
        )

        val saveRes = withTimeout(5000L) { profileRepo.savePassportVisualProfile(visualProfile) }
        assertTrue(saveRes.isSuccess)

        val observed = withTimeout(4000L) {
            profileRepo.observePassportVisualProfile(aliceUid).first { it != null && it.coverPreset == "FIRST_CLASS_SUITE" }
        }
        assertNotNull(observed)
        assertEquals("PT99887766", observed?.passportCustomNumber)

        signInTestUser("bob@aether.eu")
        val bobResult = withTimeout(5000L) { profileRepo.getPassportVisualProfile(aliceUid) }
        assertTrue(bobResult.isFailure)
        val fsEx = bobResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun createObserveAndDecrementHolidayPackage_andCreateBooking_succeedsAndBlocksCrossUser() = runBlocking {
        val aliceUid = signInTestUser("alice@aether.eu")
        val pkgRepo = HolidayPackageFirestoreRepository(firestore)
        val pkgId = "pkg_${UUID.randomUUID().toString().replace("-", "")}"
        val pkg = com.example.data.model.HolidayPackage(
            id = pkgId,
            userId = aliceUid,
            packageCode = "AGF-CMS-MLE01",
            title = "Maldivas Overwater Villa & Hidroavião",
            destinationCity = "Malé, Maldivas",
            destinationIata = "MLE",
            durationLabel = "7 Noites / 8 Dias",
            durationNights = 7,
            netPricePerPersonEur = 1850.0,
            aguiaflyCommissionPercent = 12.0,
            aguiaflyFixedFeeEur = 35.0,
            finalPricePerPersonEur = 2107.0,
            depositPercentage = 25,
            availableSpots = 6,
            totalSpots = 10,
            partnerAgencyName = "Maldives Royal Atoll Collection",
            sourceType = "MANUAL_CMS",
            badgeType = "TUDO_INCLUIDO",
            inclusionsCsv = "FLIGHT,HOTEL,MEALS,GUIDE,TRANSFERS",
            galleryPhotosJson = "[\"https://images.unsplash.com/photo-1514282401047-d79a71a590e8\"]",
            itineraryDaysJson = "[{\"day\":1,\"title\":\"Chegada\"}]",
            routeWaypointsJson = "[{\"idx\":1,\"label\":\"Malé\",\"lat\":4.17,\"lon\":73.50}]",
            isActive = true
        )

        val createPkgRes = withTimeout(5000L) { pkgRepo.createHolidayPackage(pkg) }
        assertTrue("Create package failed: ${createPkgRes.exceptionOrNull()}", createPkgRes.isSuccess)

        val decrementRes = withTimeout(5000L) { pkgRepo.updatePackageInventorySpots(pkgId, 4) }
        assertTrue(decrementRes.isSuccess)

        val observedPkg = withTimeout(4000L) {
            pkgRepo.observeAdminCmsPackages(aliceUid).first { list -> list.any { it.id == pkgId && it.availableSpots == 4 } }
        }
        assertTrue(observedPkg.any { it.id == pkgId && it.availableSpots == 4 })

        val bookingId = "bkg_${UUID.randomUUID().toString().replace("-", "")}"
        val booking = com.example.data.model.PackageBookingRecord(
            id = bookingId,
            userId = aliceUid,
            bookingCode = "AGFPKG88",
            packageId = pkgId,
            packageTitle = pkg.title,
            destinationCity = pkg.destinationCity,
            destinationIata = pkg.destinationIata,
            durationLabel = pkg.durationLabel,
            sourceType = "MANUAL_CMS",
            partnerName = pkg.partnerAgencyName,
            partnerConfirmationCode = "CMS-CNF-9988",
            travelerFullName = "Rafael Rodrigues",
            travelerEmail = "rafael@aguiafly.com",
            travelerPhone = "+351912345678",
            guestsCount = 2,
            paymentMode = "DEPOSIT_SIGNAL",
            totalPackageGrossEur = 4214.0,
            amountChargedNowEur = 1053.5,
            remainingBalanceEur = 3160.5,
            aguiaflyCommissionEur = 514.0,
            stripeChargeId = "ch_stripe_pkg_test_1",
            status = "DEPOSIT_PAID"
        )

        val createBookingRes = withTimeout(5000L) { pkgRepo.createPackageBooking(booking) }
        assertTrue("Create booking failed: ${createBookingRes.exceptionOrNull()}", createBookingRes.isSuccess)

        // Cross-user access by Bob must fail with PERMISSION_DENIED
        signInTestUser("bob@aether.eu")
        val bobPkgRes = withTimeout(5000L) { pkgRepo.getPackageById(pkgId) }
        assertTrue(bobPkgRes.isFailure)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobPkgRes.exceptionOrNull()?.unwrapFirestoreException()?.code)

        val bobBookingRes = withTimeout(5000L) { pkgRepo.getPackageBookingById(bookingId) }
        assertTrue(bobBookingRes.isFailure)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, bobBookingRes.exceptionOrNull()?.unwrapFirestoreException()?.code)
    }
}

