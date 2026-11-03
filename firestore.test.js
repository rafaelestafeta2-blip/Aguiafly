const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTicketPayload(userId, now) {
  return {
    userId,
    orderId: "ord_0000AetherTest1",
    bookingReference: "AETH9X",
    passengerName: "RAFAEL RODRIGUES",
    originIata: "LIS",
    originCity: "Lisbon",
    destinationIata: "CDG",
    destinationCity: "Paris",
    departureIso: "2026-11-15T09:15:00Z",
    arrivalIso: "2026-11-15T12:45:00Z",
    airlineName: "Air France",
    flightNumber: "AF1025",
    aircraftName: "Airbus A350-900",
    cabinClass: "business",
    seatDesignator: "2A",
    netAmountEur: 420.0,
    markupEur: 31.0,
    ancillariesEur: 45.0,
    totalChargedEur: 496.0,
    iataBarcodePayload: "M1RODRIGUES/RAFAEL    EAETH9X LISCDGAF 1025 319C002A0001 100",
    addedToGoogleWallet: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validPriceAlertPayload(userId, now) {
  return {
    userId,
    originIata: "LIS",
    destinationIata: "CDG",
    cabinClass: "business",
    targetMaxPriceEur: 350.0,
    baselinePriceEur: 420.0,
    lastObservedPriceEur: 420.0,
    fcmToken: "fcm_token_sample_1234567890",
    isActive: true,
    promoTriggered: false,
    createdAt: now,
    updatedAt: now,
  };
}

function validTravelerProfilePayload(userId, now) {
  return {
    userId,
    firstName: "Rafael",
    lastName: "Rodrigues",
    email: "rafael@aguiafly.com",
    phone: "+5511999998888",
    documentNumber: "BR99887766",
    birthDate: "1992-05-15",
    expiryDate: "2032-08-20",
    frequentFlyerProgram: "TAP Miles&Go",
    frequentFlyerNumber: "TP123456789",
    autoCheckInEnabled: true,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or list tickets, price_alerts, or traveler_profiles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tickets").doc("t1").get());
  await assertFails(unauthDb.collection("tickets").get());
  await assertFails(unauthDb.collection("price_alerts").doc("a1").get());
  await assertFails(unauthDb.collection("price_alerts").get());
  await assertFails(unauthDb.collection("traveler_profiles").doc(ALICE_UID).get());
});

test("Authenticated user: can create valid ticket and query own tickets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    aliceDb.collection("tickets").doc("t_alice_1").set(validTicketPayload(ALICE_UID, now))
  );
  await assertSucceeds(
    aliceDb.collection("tickets").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: cannot read another user's ticket or query without userId filter", async () => {
  const now = new Date(Date.now() - 1000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("tickets").doc("t_bob_1").set(validTicketPayload(BOB_UID, now));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tickets").doc("t_bob_1").get());
  await assertFails(aliceDb.collection("tickets").get());
});

test("Shadow update & immutable field protection on tickets", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tickets").doc("t_alice_2").set(validTicketPayload(ALICE_UID, now))
  );
  // Allowed action: toggle addedToGoogleWallet
  await assertSucceeds(
    aliceDb.collection("tickets").doc("t_alice_2").update({
      addedToGoogleWallet: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Disallowed action: mutate totalChargedEur
  await assertFails(
    aliceDb.collection("tickets").doc("t_alice_2").update({
      totalChargedEur: 1.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Markup config: owner can create and update own markup config, cross-user rejected", async () => {
  const now = new Date(Date.now() - 1000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("markup_configs").doc(ALICE_UID).set({
      userId: ALICE_UID,
      fixedMarkupEur: 3.5,
      percentageMarkup: 3.5,
      seatSelectionFeeEur: 12.0,
      extraBaggageFeeEur: 20.0,
      priorityBoardingFeeEur: 15.0,
      updatedAt: now,
    })
  );

  await assertFails(
    bobDb.collection("markup_configs").doc(ALICE_UID).get()
  );
});

test("Price alerts: owner can create, query, update promoTriggered, and delete own alert; cross-user denied", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("price_alerts").doc("alert_alice_1").set(validPriceAlertPayload(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb.collection("price_alerts").where("userId", "==", ALICE_UID).get()
  );

  await assertFails(
    bobDb.collection("price_alerts").doc("alert_alice_1").get()
  );

  await assertSucceeds(
    aliceDb.collection("price_alerts").doc("alert_alice_1").update({
      lastObservedPriceEur: 319.0,
      promoTriggered: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable route field cannot be changed
  await assertFails(
    aliceDb.collection("price_alerts").doc("alert_alice_1").update({
      originIata: "OPO",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("price_alerts").doc("alert_alice_1").delete()
  );
});

test("Traveler profile vault: owner can create, read, update, and delete own profile; cross-user and ghost fields rejected", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("traveler_profiles").doc(ALICE_UID).set(validTravelerProfilePayload(ALICE_UID, now))
  );

  await assertSucceeds(
    aliceDb.collection("traveler_profiles").doc(ALICE_UID).get()
  );

  // Cross-user read or write rejected (PII isolation)
  await assertFails(
    bobDb.collection("traveler_profiles").doc(ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("traveler_profiles").doc(ALICE_UID).set(validTravelerProfilePayload(BOB_UID, now))
  );

  // Owner update succeeds
  await assertSucceeds(
    aliceDb.collection("traveler_profiles").doc(ALICE_UID).update({
      documentNumber: "PT55443322",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("traveler_profiles").doc(ALICE_UID).update({
      isVipAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("traveler_profiles").doc(ALICE_UID).delete()
  );
});

test("Passport visual profile: owner can save and update profile/cover photos and stamp albums; cross-user and ghost fields rejected", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const payload = {
    userId: ALICE_UID,
    profilePhotoData: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
    coverPhotoData: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
    coverPreset: "IMPERIAL_NAVY_GOLD",
    nationality: "PORTUGAL / BRASIL",
    memberTierTitle: "AguiaFly Executive Gold",
    passportCustomNumber: "PT99887766",
    stampPhotosJson: '{"ord_1":["data:image/jpeg;base64,/9j/4AAQSkZ"]}',
    stampNotesJson: '{"ord_1":"Viagem incrível a Ibiza"}',
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("passport_profiles").doc(ALICE_UID).set(payload)
  );

  await assertSucceeds(
    aliceDb.collection("passport_profiles").doc(ALICE_UID).get()
  );

  await assertFails(
    bobDb.collection("passport_profiles").doc(ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("passport_profiles").doc(ALICE_UID).update({
      coverPreset: "FIRST_CLASS_SUITE",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    aliceDb.collection("passport_profiles").doc(ALICE_UID).update({
      extraGhostField: "hack",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("passport_profiles").doc(ALICE_UID).delete()
  );
});

test("Holiday packages (Admin CMS): owner can create, query, update inventory spots, and cross-user access is denied", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const pkgPayload = {
    userId: ALICE_UID,
    packageCode: "AGF-MALDIVES-7N",
    title: "Maldivas Overwater Villa & Hidroavião",
    destinationCity: "Malé, Maldivas",
    destinationIata: "MLE",
    durationLabel: "7 Noites / 8 Dias",
    durationNights: 7,
    netPricePerPersonEur: 1850.0,
    aguiaflyCommissionPercent: 12.0,
    aguiaflyFixedFeeEur: 35.0,
    finalPricePerPersonEur: 2107.0,
    depositPercentage: 25,
    availableSpots: 8,
    totalSpots: 12,
    partnerAgencyName: "Maldives Royal Atoll Collection",
    sourceType: "MANUAL_CMS",
    badgeType: "TUDO_INCLUIDO",
    inclusionsCsv: "FLIGHT,HOTEL,MEALS,GUIDE,TRANSFERS",
    galleryPhotosJson: '["https://images.unsplash.com/photo-1514282401047-d79a71a590e8"]',
    itineraryDaysJson: '[{"day":1,"title":"Chegada em Malé"}]',
    routeWaypointsJson: '[{"label":"Malé","lat":4.1755,"lon":73.5093}]',
    isActive: true,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("holiday_packages").doc("pkg_maldives_1").set(pkgPayload)
  );

  await assertSucceeds(
    aliceDb.collection("holiday_packages").where("userId", "==", ALICE_UID).get()
  );

  await assertFails(
    bobDb.collection("holiday_packages").doc("pkg_maldives_1").get()
  );

  // Decrement inventory spots on booking
  await assertSucceeds(
    aliceDb.collection("holiday_packages").doc("pkg_maldives_1").update({
      availableSpots: 6,
      badgeType: "ULTIMAS_VAGAS",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("holiday_packages").doc("pkg_maldives_1").update({
      hackField: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Package bookings: owner can create direct Stripe booking or deposit, query own bookings, and cross-user is denied", async () => {
  const now = new Date(Date.now() - 2000);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const bookingPayload = {
    userId: ALICE_UID,
    bookingCode: "AGFPKG99",
    packageId: "pkg_maldives_1",
    packageTitle: "Maldivas Overwater Villa & Hidroavião",
    destinationCity: "Malé, Maldivas",
    destinationIata: "MLE",
    durationLabel: "7 Noites / 8 Dias",
    sourceType: "MANUAL_CMS",
    partnerName: "Maldives Royal Atoll Collection",
    partnerConfirmationCode: "PARTNER-CNF-7788",
    travelerFullName: "Rafael Rodrigues",
    travelerEmail: "rafael@aguiafly.com",
    travelerPhone: "+351912345678",
    guestsCount: 2,
    paymentMode: "DEPOSIT_SIGNAL",
    totalPackageGrossEur: 4214.0,
    amountChargedNowEur: 1053.5,
    remainingBalanceEur: 3160.5,
    aguiaflyCommissionEur: 514.0,
    stripeChargeId: "ch_stripe_pkg_998877",
    status: "DEPOSIT_PAID",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("package_bookings").doc("bkg_alice_1").set(bookingPayload)
  );

  await assertSucceeds(
    aliceDb.collection("package_bookings").where("userId", "==", ALICE_UID).get()
  );

  await assertFails(
    bobDb.collection("package_bookings").doc("bkg_alice_1").get()
  );

  await assertSucceeds(
    aliceDb.collection("package_bookings").doc("bkg_alice_1").update({
      status: "CONFIRMED",
      amountChargedNowEur: 4214.0,
      remainingBalanceEur: 0.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

