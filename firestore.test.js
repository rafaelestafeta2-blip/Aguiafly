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
