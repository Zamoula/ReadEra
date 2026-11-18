const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_reader_1";
const BOB_UID = "bob_reader_2";
const CHARLIE_UID = "charlie_outsider_3";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated users cannot read or write profiles, libraries, or circles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("library").get());
  await assertFails(unauthDb.collection("reading_circles").doc("circle_meditations_1").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      dailyGoalMinutes: 30,
      streakDays: 1,
      totalMinutesRead: 0,
      booksCompleted: 0,
      nightMode: false,
      readerFontSize: 18,
      readerTheme: "PAPER",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile creation, partial update, cross-user isolation, and ghost field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      dailyGoalMinutes: 30,
      streakDays: 3,
      totalMinutesRead: 45,
      booksCompleted: 1,
      nightMode: false,
      readerFontSize: 18,
      readerTheme: "PAPER",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update nightMode & theme succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      nightMode: true,
      readerTheme: "NIGHT",
      updatedAt: pastDate(1000),
    })
  );

  // Ghost field & invalid theme rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      readerTheme: "NEON_PINK",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      dailyGoalMinutes: 30,
      streakDays: 0,
      totalMinutesRead: 0,
      booksCompleted: 0,
      nightMode: false,
      readerFontSize: 18,
      readerTheme: "PAPER",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      dailyGoalMinutes: 45,
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("User library subcollection: list, create, partial progress update, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  // Empty collection list succeeds for owner, fails for other user
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("library").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("library").get());

  // Create book in Alice's library (NO @DocumentId id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("meditations").set({
      userId: ALICE_UID,
      bookId: "meditations",
      title: "Meditations",
      author: "Marcus Aurelius",
      currentChapter: 0,
      totalChapters: 5,
      progressPercent: 20,
      minutesRead: 12,
      bookmarkChapter: 0,
      bookmarkNote: "",
      offlineSaved: true,
      completed: false,
      lastReadAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial progress update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("meditations").update({
      currentChapter: 2,
      progressPercent: 60,
      minutesRead: 25,
      lastReadAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("meditations").update({
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );

  // Large uploaded PDF (e.g. 750 pages) progress save & update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("pdf_a1b2c3d4").set({
      userId: ALICE_UID,
      bookId: "pdf_a1b2c3d4",
      title: "My Uploaded Novel",
      author: "Personal PDF",
      currentChapter: 0,
      totalChapters: 750,
      progressPercent: 1,
      minutesRead: 5,
      bookmarkChapter: 0,
      bookmarkNote: "",
      offlineSaved: true,
      completed: false,
      lastReadAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject extra id field on create
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("library").doc("odyssey").set({
      id: "odyssey",
      userId: ALICE_UID,
      bookId: "odyssey",
      title: "The Odyssey",
      author: "Homer",
      currentChapter: 0,
      totalChapters: 6,
      progressPercent: 0,
      minutesRead: 0,
      bookmarkChapter: 0,
      bookmarkNote: "",
      offlineSaved: false,
      completed: false,
      lastReadAt: created,
      createdAt: created,
      updatedAt: created,
    })
  );
});

test("Shared reading circles & friend progress tracking via book link", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = pastDate(10000);
  const circleId = "circle_meditations_abc123";

  // Alice creates a shared book link circle
  await assertSucceeds(
    aliceDb.collection("reading_circles").doc(circleId).set({
      creatorId: ALICE_UID,
      bookId: "meditations",
      bookTitle: "Meditations",
      bookAuthor: "Marcus Aurelius",
      totalChapters: 5,
      shareCode: circleId,
      memberUids: [ALICE_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice records her progress inside the circle
  await assertSucceeds(
    aliceDb.collection("reading_circles").doc(circleId).collection("members").doc(ALICE_UID).set({
      memberUid: ALICE_UID,
      displayName: "Alice",
      currentChapter: 1,
      totalChapters: 5,
      progressPercent: 40,
      statusNote: "Book II reflections",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unfiltered list on reading_circles fails; filtered list succeeds for Alice
  await assertFails(aliceDb.collection("reading_circles").get());
  await assertSucceeds(
    aliceDb.collection("reading_circles").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Charlie (not a member) cannot read Alice's progress doc
  await assertFails(
    charlieDb.collection("reading_circles").doc(circleId).collection("members").doc(ALICE_UID).get()
  );

  // Bob opens the book link (get circle preview) and joins by adding ONLY himself to memberUids
  await assertSucceeds(bobDb.collection("reading_circles").doc(circleId).get());
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(2000),
    })
  );

  // Bob writes his own progress doc; cannot overwrite Alice's progress doc
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).collection("members").doc(BOB_UID).set({
      memberUid: BOB_UID,
      displayName: "Bob",
      currentChapter: 0,
      totalChapters: 5,
      progressPercent: 20,
      statusNote: "Just started!",
      createdAt: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );
  await assertFails(
    bobDb.collection("reading_circles").doc(circleId).collection("members").doc(ALICE_UID).update({
      progressPercent: 99,
      updatedAt: pastDate(1000),
    })
  );

  // Now Bob can read Alice's progress doc, and Alice can read Bob's progress doc!
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).collection("members").doc(ALICE_UID).get()
  );
  await assertSucceeds(
    aliceDb.collection("reading_circles").doc(circleId).collection("members").doc(BOB_UID).get()
  );

  // Collection list on members is disallowed (clients observe individual member docs)
  await assertFails(
    aliceDb.collection("reading_circles").doc(circleId).collection("members").get()
  );
});

test("Shared book comments and threaded replies among circle link members", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const created = pastDate(10000);
  const circleId = "circle_odyssey_9988";

  // Alice creates circle
  await assertSucceeds(
    aliceDb.collection("reading_circles").doc(circleId).set({
      creatorId: ALICE_UID,
      bookId: "odyssey",
      bookTitle: "The Odyssey",
      bookAuthor: "Homer",
      totalChapters: 6,
      shareCode: circleId,
      memberUids: [ALICE_UID],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Alice posts a top-level comment while reading
  await assertSucceeds(
    aliceDb.collection("reading_circles").doc(circleId).collection("comments").doc("cmt_1").set({
      circleId,
      bookId: "odyssey",
      authorUid: ALICE_UID,
      authorName: "Alice",
      chapterIndex: 1,
      body: "The hospitality scene in Book I sets the whole tone.",
      parentCommentId: "",
      parentAuthorName: "",
      createdAt: pastDate(6000),
      updatedAt: pastDate(6000),
    })
  );

  // Non-member Charlie cannot read or write comments
  await assertFails(
    charlieDb.collection("reading_circles").doc(circleId).collection("comments").get()
  );
  await assertFails(
    charlieDb.collection("reading_circles").doc(circleId).collection("comments").doc("cmt_charlie").set({
      circleId,
      bookId: "odyssey",
      authorUid: CHARLIE_UID,
      authorName: "Charlie",
      chapterIndex: 0,
      body: "Intruder comment",
      parentCommentId: "",
      parentAuthorName: "",
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );

  // Bob opens the book link and joins
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).update({
      memberUids: [ALICE_UID, BOB_UID],
      updatedAt: pastDate(5000),
    })
  );

  // Bob can list Alice's earlier comment and post a reply to it
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).collection("comments").get()
  );
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).collection("comments").doc("cmt_2").set({
      circleId,
      bookId: "odyssey",
      authorUid: BOB_UID,
      authorName: "Bob",
      chapterIndex: 1,
      body: "Completely agree — Athena's arrival is iconic!",
      parentCommentId: "cmt_1",
      parentAuthorName: "Alice",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Impersonating another authorUid fails
  await assertFails(
    bobDb.collection("reading_circles").doc(circleId).collection("comments").doc("cmt_spoof").set({
      circleId,
      bookId: "odyssey",
      authorUid: ALICE_UID,
      authorName: "Alice",
      chapterIndex: 1,
      body: "Spoofed",
      parentCommentId: "",
      parentAuthorName: "",
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );

  // Bob cannot delete Alice's comment, but can delete his own
  await assertFails(
    bobDb.collection("reading_circles").doc(circleId).collection("comments").doc("cmt_1").delete()
  );
  await assertSucceeds(
    bobDb.collection("reading_circles").doc(circleId).collection("comments").doc("cmt_2").delete()
  );
});

