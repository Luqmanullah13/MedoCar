const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_customer_1";
const BOB_UID = "bob_customer_2";
const ADMIN_UID = "pharmacist_admin_1";
const ADMIN_EMAIL = "luqmankhan89lk23@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function validProductPayload(overrides = {}) {
  const now = pastDate(5000);
  return {
    nameEn: "Panadol Extra 500mg",
    nameUr: "پیناڈول ایکسٹرا 500mg",
    genericName: "Paracetamol + Caffeine",
    manufacturer: "GSK",
    categoryId: "cat_pain",
    descriptionEn: "Fast-acting analgesic tablets for mild to moderate pain.",
    descriptionUr: "ہلکے سے درمیانے درجے کے درد کے لیے مؤثر گولیاں۔",
    packSize: "10 Tablets",
    pricePkr: 120,
    discountPercent: 10,
    stock: 25,
    prescriptionRequired: false,
    popular: true,
    active: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("products").get());
  await assertFails(unauthDb.collection("categories").get());
  await assertFails(unauthDb.collection("orders").get());
  await assertFails(unauthDb.collection("prescriptions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Role escalation and email spoofing are blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = pastDate();

  // Customer cannot self-register as ADMIN
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredLanguage: "en",
      role: "ADMIN",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer registering as CUSTOMER succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredLanguage: "en",
      role: "CUSTOMER",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot escalate own role on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(500),
    })
  );

  // Unverified spoofed admin email cannot write categories or products
  const spoofedDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofedDb.collection("products").doc("prod_1").set(validProductPayload())
  );
});

test("Verified admin can manage categories, products, and customers", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = pastDate();

  await assertSucceeds(
    adminDb.collection("categories").doc("cat_pain").set({
      nameEn: "Pain Relief",
      nameUr: "درد سے نجات",
      iconKey: "healing",
      sortOrder: 1,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").set(validProductPayload())
  );

  // Customer cannot create or delete products, nor increase stock or alter price
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("products").doc("prod_2").set(validProductPayload()));
  await assertFails(aliceDb.collection("products").doc("prod_1").update({ pricePkr: 1, updatedAt: pastDate(500) }));
  await assertFails(aliceDb.collection("products").doc("prod_1").update({ stock: 99, updatedAt: pastDate(500) }));
  await assertFails(aliceDb.collection("products").doc("prod_1").delete());

  // Customer checkout stock decrement succeeds
  await assertSucceeds(
    aliceDb.collection("products").doc("prod_1").update({
      stock: 23,
      updatedAt: pastDate(500),
    })
  );
});

test("Cross-user isolation for addresses, cart, wishlist, prescriptions, and orders", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = pastDate(5000);
    await db.collection("products").doc("prod_otc").set(validProductPayload());
    await db.collection("products").doc("prod_rx").set(validProductPayload({ prescriptionRequired: true, nameEn: "Augmentin 625mg" }));
    await db.collection("users").doc(BOB_UID).collection("addresses").doc("addr_bob").set({
      userId: BOB_UID,
      label: "Home",
      recipientName: "Bob Khan",
      phone: "03001234567",
      street: "14 Gulberg III",
      city: "Lahore",
      defaultAddress: true,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("prescriptions").doc("rx_bob").set({
      userId: BOB_UID,
      customerName: "Bob Khan",
      customerEmail: "bob@example.com",
      imageBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
      status: "PENDING_REVIEW",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("addresses").get());
  await assertFails(aliceDb.collection("prescriptions").doc("rx_bob").get());
  // Unfiltered list on prescriptions/orders fails
  await assertFails(aliceDb.collection("prescriptions").get());
  await assertFails(aliceDb.collection("orders").get());

  // Filtered list for own UID succeeds
  await assertSucceeds(aliceDb.collection("prescriptions").where("userId", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("orders").where("userId", "==", ALICE_UID).get());

  // Alice cart & wishlist work with valid product
  const now = pastDate();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cart").doc("prod_otc").set({
      userId: ALICE_UID,
      productId: "prod_otc",
      quantity: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("wishlist").doc("prod_otc").set({
      userId: ALICE_UID,
      productId: "prod_otc",
      createdAt: now,
    })
  );
});

test("Prescription verification cannot be bypassed or self-approved", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = pastDate(5000);

  // Customer cannot submit pre-approved prescription
  await assertFails(
    aliceDb.collection("prescriptions").doc("rx_fake").set({
      userId: ALICE_UID,
      customerName: "Alice",
      customerEmail: "alice@example.com",
      imageBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
      status: "APPROVED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer cannot create Rx order without prescription doc
  await assertFails(
    aliceDb.collection("orders").doc("ord_no_rx").set({
      orderNumber: "DM-1001",
      userId: ALICE_UID,
      customerName: "Alice",
      customerEmail: "alice@example.com",
      addressLabel: "Home",
      recipientName: "Alice",
      deliveryPhone: "03001112233",
      deliveryStreet: "Street 4, F-10",
      deliveryCity: "Islamabad",
      items: [{ productId: "prod_rx", nameEn: "Augmentin 625mg", qty: 1, unitPricePkr: 450 }],
      itemCount: 1,
      subtotalPkr: 450,
      discountPkr: 0,
      deliveryFeePkr: 150,
      totalPkr: 600,
      paymentMethod: "CASH_ON_DELIVERY",
      requiresPrescription: true,
      prescriptionId: "missing_rx",
      prescriptionStatus: "PENDING_REVIEW",
      status: "AWAITING_RX_VERIFICATION",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Atomic batch creating prescription + Rx order succeeds
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("prescriptions").doc("rx_valid"), {
    userId: ALICE_UID,
    customerName: "Alice",
    customerEmail: "alice@example.com",
    imageBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/",
    orderId: "ord_rx_1",
    status: "PENDING_REVIEW",
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("orders").doc("ord_rx_1"), {
    orderNumber: "DM-1002",
    userId: ALICE_UID,
    customerName: "Alice",
    customerEmail: "alice@example.com",
    addressLabel: "Home",
    recipientName: "Alice",
    deliveryPhone: "03001112233",
    deliveryStreet: "Street 4, F-10",
    deliveryCity: "Islamabad",
    items: [{ productId: "prod_rx", nameEn: "Augmentin 625mg", qty: 1, unitPricePkr: 450 }],
    itemCount: 1,
    subtotalPkr: 450,
    discountPkr: 0,
    deliveryFeePkr: 150,
    totalPkr: 600,
    paymentMethod: "CASH_ON_DELIVERY",
    requiresPrescription: true,
    prescriptionId: "rx_valid",
    prescriptionStatus: "PENDING_REVIEW",
    status: "AWAITING_RX_VERIFICATION",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Customer cannot self-approve their prescription
  await assertFails(
    aliceDb.collection("prescriptions").doc("rx_valid").update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );

  // Admin cannot advance Rx order to CONFIRMED while prescriptionStatus is still PENDING_REVIEW
  await assertFails(
    adminDb.collection("orders").doc("ord_rx_1").update({
      status: "CONFIRMED",
      updatedAt: pastDate(500),
    })
  );

  // Admin approves prescription and unlocks order atomically
  const adminBatch = adminDb.batch();
  adminBatch.update(adminDb.collection("prescriptions").doc("rx_valid"), {
    status: "APPROVED",
    pharmacistNotes: "Verified valid prescription from licensed physician.",
    reviewedBy: ADMIN_EMAIL,
    updatedAt: pastDate(200),
  });
  adminBatch.update(adminDb.collection("orders").doc("ord_rx_1"), {
    prescriptionStatus: "APPROVED",
    status: "CONFIRMED",
    pharmacistNote: "Rx verified by pharmacist.",
    updatedAt: pastDate(200),
  });
  await assertSucceeds(adminBatch.commit());
});

test("OTC COD checkout and customer early-stage cancellation rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_otc_1").set({
      orderNumber: "DM-2001",
      userId: ALICE_UID,
      customerName: "Alice",
      customerEmail: "alice@example.com",
      addressLabel: "Home",
      recipientName: "Alice",
      deliveryPhone: "03001112233",
      deliveryStreet: "Street 4, F-10",
      deliveryCity: "Islamabad",
      items: [{ productId: "prod_otc", nameEn: "Panadol Extra", qty: 2, unitPricePkr: 108 }],
      itemCount: 2,
      subtotalPkr: 216,
      discountPkr: 24,
      deliveryFeePkr: 150,
      totalPkr: 366,
      paymentMethod: "CASH_ON_DELIVERY",
      requiresPrescription: false,
      prescriptionStatus: "NOT_REQUIRED",
      status: "CONFIRMED",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Customer can cancel while CONFIRMED
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_otc_1").update({
      status: "CANCELLED",
      updatedAt: pastDate(500),
    })
  );

  // Admin delivers a separate order; customer cannot cancel after DELIVERED
  await assertSucceeds(
    aliceDb.collection("orders").doc("ord_otc_2").set({
      orderNumber: "DM-2002",
      userId: ALICE_UID,
      customerName: "Alice",
      customerEmail: "alice@example.com",
      addressLabel: "Home",
      recipientName: "Alice",
      deliveryPhone: "03001112233",
      deliveryStreet: "Street 4, F-10",
      deliveryCity: "Islamabad",
      items: [{ productId: "prod_otc", nameEn: "Panadol Extra", qty: 1, unitPricePkr: 108 }],
      itemCount: 1,
      subtotalPkr: 108,
      discountPkr: 12,
      deliveryFeePkr: 150,
      totalPkr: 258,
      paymentMethod: "CASH_ON_DELIVERY",
      requiresPrescription: false,
      prescriptionStatus: "NOT_REQUIRED",
      status: "CONFIRMED",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("orders").doc("ord_otc_2").update({
      status: "DELIVERED",
      updatedAt: pastDate(300),
    })
  );
  await assertFails(
    aliceDb.collection("orders").doc("ord_otc_2").update({
      status: "CANCELLED",
      updatedAt: pastDate(100),
    })
  );
});
