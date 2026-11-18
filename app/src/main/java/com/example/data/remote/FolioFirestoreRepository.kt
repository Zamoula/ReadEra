package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.BookCatalog
import com.example.data.model.BookDefinition
import com.example.data.model.CircleComment
import com.example.data.model.CircleMemberProgress
import com.example.data.model.LibraryBook
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingCircle
import com.example.data.model.UserProfile
import com.example.util.BookLinkHelper
import com.example.util.ParsedBookLink
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FolioFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun safeDisplayName(raw: String): String =
        raw.trim().ifBlank { "Reader" }.take(80)

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed." }
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName(displayName),
                    "dailyGoalMinutes" to 30L,
                    "streakDays" to 1L,
                    "totalMinutesRead" to 0L,
                    "booksCompleted" to 0L,
                    "nightMode" to false,
                    "readerFontSize" to 18L,
                    "readerTheme" to ReaderThemeMode.PAPER.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        userId: String,
        nightMode: Boolean,
        readerFontSize: Int,
        readerTheme: ReaderThemeMode,
        dailyGoalMinutes: Int
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val clampedGoal = dailyGoalMinutes.coerceIn(5, 240).toLong()
            val clampedFont = readerFontSize.coerceIn(14, 28).toLong()
            db.collection("users").document(uid).update(
                mapOf(
                    "nightMode" to nightMode,
                    "readerFontSize" to clampedFont,
                    "readerTheme" to readerTheme.name,
                    "dailyGoalMinutes" to clampedGoal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeLibrary(userId: String): Flow<List<LibraryBook>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed." }
        val colRef = db.collection("users").document(uid).collection("library")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LibraryBook::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveReadingProgress(
        userId: String,
        book: BookDefinition,
        currentChapter: Int,
        minutesDelta: Int,
        bookmarkChapter: Int? = null,
        bookmarkNote: String? = null,
        offlineSaved: Boolean? = null,
        markCompleted: Boolean? = null
    ): Result<Unit> {
        val path = "users/$userId/library/${book.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("users").document(uid).collection("library").document(book.id)
            val existingSnap = docRef.get().await()
            val safeChapter = currentChapter.coerceIn(0, (book.totalChapters - 1).coerceAtLeast(0))
            val computedPercent = if (markCompleted == true) {
                100L
            } else {
                (((safeChapter + 1).toDouble() / book.totalChapters.coerceAtLeast(1)) * 100.0)
                    .toInt()
                    .coerceIn(0, 100)
                    .toLong()
            }
            val safeMinutesDelta = minutesDelta.coerceIn(0, 1440).toLong()

            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "bookId" to book.id.take(64),
                    "title" to book.title.take(150),
                    "author" to book.author.take(100),
                    "currentChapter" to safeChapter.toLong(),
                    "totalChapters" to book.totalChapters.coerceIn(1, 10000).toLong(),
                    "progressPercent" to computedPercent,
                    "minutesRead" to safeMinutesDelta,
                    "bookmarkChapter" to (bookmarkChapter ?: safeChapter).coerceIn(0, 10000).toLong(),
                    "bookmarkNote" to (bookmarkNote ?: "").take(200),
                    "offlineSaved" to (offlineSaved ?: book.preCachedOffline),
                    "completed" to (markCompleted ?: false),
                    "lastReadAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val prevMinutes = existingSnap.getLong("minutesRead") ?: 0L
                val prevCompleted = existingSnap.getBoolean("completed") ?: false
                val prevOffline = existingSnap.getBoolean("offlineSaved") ?: book.preCachedOffline
                val prevBookmarkChapter = (existingSnap.getLong("bookmarkChapter") ?: 0L).toInt()
                val prevBookmarkNote = existingSnap.getString("bookmarkNote") ?: ""
                val nowCompleted = markCompleted ?: prevCompleted

                val updates = mapOf(
                    "currentChapter" to safeChapter.toLong(),
                    "progressPercent" to if (nowCompleted) 100L else computedPercent,
                    "minutesRead" to (prevMinutes + safeMinutesDelta).coerceAtMost(1_000_000L),
                    "bookmarkChapter" to (bookmarkChapter ?: prevBookmarkChapter).coerceIn(0, 10000).toLong(),
                    "bookmarkNote" to (bookmarkNote ?: prevBookmarkNote).take(200),
                    "offlineSaved" to (offlineSaved ?: prevOffline),
                    "completed" to nowCompleted,
                    "lastReadAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()

                // Update user aggregate reading minutes / books completed safely
                if (safeMinutesDelta > 0L || (nowCompleted && !prevCompleted)) {
                    val userRef = db.collection("users").document(uid)
                    val userSnap = userRef.get().await()
                    if (userSnap.exists()) {
                        val currTotalMin = userSnap.getLong("totalMinutesRead") ?: 0L
                        val currBooks = userSnap.getLong("booksCompleted") ?: 0L
                        val currStreak = (userSnap.getLong("streakDays") ?: 1L).coerceAtLeast(1L)
                        userRef.update(
                            mapOf(
                                "totalMinutesRead" to (currTotalMin + safeMinutesDelta).coerceAtMost(1_000_000L),
                                "booksCompleted" to (currBooks + if (nowCompleted && !prevCompleted) 1L else 0L).coerceAtMost(10_000L),
                                "streakDays" to currStreak,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun removeLibraryEntry(userId: String, bookId: String): Result<Unit> {
        val path = "users/$userId/library/$bookId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("users").document(uid).collection("library").document(bookId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeMyCircles(userId: String): Flow<List<ReadingCircle>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user changed." }
        val query = db.collection("reading_circles")
            .whereArrayContains("memberUids", uid)
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReadingCircle::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, "reading_circles")
                    }
                    throw error
                }
        )
    }

    fun observeCircleMembers(circle: ReadingCircle): Flow<List<CircleMemberProgress>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        if (circle.id.isBlank() || circle.memberUids.isEmpty() || uid !in circle.memberUids) {
            return flowOf(emptyList())
        }
        val memberFlows = circle.memberUids.distinct().take(20).map { memberUid ->
            val docRef = db.collection("reading_circles")
                .document(circle.id)
                .collection("members")
                .document(memberUid)
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            CircleMemberProgress::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    emit(null)
                }
        }
        return combine(memberFlows) { results ->
            results.filterNotNull().sortedByDescending { it.progressPercent }
        }
    }

    suspend fun createOrGetCircleForBook(
        userId: String,
        displayName: String,
        book: BookDefinition,
        currentChapter: Int
    ): Result<ReadingCircle> {
        var path = "reading_circles"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }

            // Check if user already created or belongs to a circle for this book
            val existingCircles = db.collection("reading_circles")
                .whereArrayContains("memberUids", uid)
                .get()
                .await()
                .toObjects(ReadingCircle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)

            val existing = existingCircles.firstOrNull { it.bookId == book.id }
            if (existing != null) {
                return Result.success(existing)
            }

            val shortSuffix = UUID.randomUUID().toString().replace("-", "").take(6)
            val circleId = "${book.id}_$shortSuffix".take(64)
            path = "reading_circles/$circleId"

            val circlePayload = mapOf(
                "creatorId" to uid,
                "bookId" to book.id.take(64),
                "bookTitle" to book.title.take(150),
                "bookAuthor" to book.author.take(100),
                "totalChapters" to book.totalChapters.coerceAtLeast(1).toLong(),
                "shareCode" to circleId,
                "memberUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val circleRef = db.collection("reading_circles").document(circleId)
            circleRef.set(circlePayload).await()

            val safeChapter = currentChapter.coerceIn(0, (book.totalChapters - 1).coerceAtLeast(0))
            val percent = (((safeChapter + 1).toDouble() / book.totalChapters.coerceAtLeast(1)) * 100.0)
                .toInt().coerceIn(0, 100).toLong()

            val memberRef = circleRef.collection("members").document(uid)
            memberRef.set(
                mapOf(
                    "memberUid" to uid,
                    "displayName" to safeDisplayName(displayName),
                    "currentChapter" to safeChapter.toLong(),
                    "totalChapters" to book.totalChapters.coerceAtLeast(1).toLong(),
                    "progressPercent" to percent,
                    "statusNote" to "Reading ${book.title}".take(140),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val created = circleRef.get().await()
                .toObject(ReadingCircle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: ReadingCircle(
                    id = circleId,
                    creatorId = uid,
                    bookId = book.id,
                    bookTitle = book.title,
                    bookAuthor = book.author,
                    totalChapters = book.totalChapters.toLong(),
                    shareCode = circleId,
                    memberUids = listOf(uid)
                )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinCircleByLinkOrCode(
        userId: String,
        displayName: String,
        rawInput: String
    ): Result<Pair<BookDefinition, ReadingCircle?>> {
        val parsed = BookLinkHelper.parseBookLink(rawInput)
            ?: return Result.failure(IllegalArgumentException("Enter a valid Folio book link or circle code."))

        return when (parsed) {
            is ParsedBookLink.DirectBook -> {
                val book = BookCatalog.findById(parsed.bookId)
                    ?: return Result.failure(IllegalArgumentException("Book '${parsed.bookId}' was not found in the catalog."))
                saveReadingProgress(userId, book, currentChapter = 0, minutesDelta = 0)
                Result.success(book to null)
            }
            is ParsedBookLink.CircleInvite -> {
                val circlePath = "reading_circles/${parsed.circleId}"
                try {
                    val uid = requireUserId()
                    check(uid == userId) { "Signed-in user mismatch" }
                    val circleRef = db.collection("reading_circles").document(parsed.circleId)
                    val snap = circleRef.get().await()
                    if (!snap.exists()) {
                        return Result.failure(IllegalArgumentException("No reading circle found for code '${parsed.circleId}'."))
                    }
                    val circle = snap.toObject(
                        ReadingCircle::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: return Result.failure(IllegalStateException("Could not parse reading circle."))

                    val book = BookCatalog.findById(circle.bookId)
                        ?: return Result.failure(IllegalArgumentException("Book '${circle.bookId}' is not available."))

                    if (uid !in circle.memberUids) {
                        if (circle.memberUids.size >= 20) {
                            return Result.failure(IllegalStateException("This reading circle has reached its 20-reader limit."))
                        }
                        val updatedMembers = (circle.memberUids + uid).distinct()
                        circleRef.update(
                            mapOf(
                                "memberUids" to updatedMembers,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }

                    // Ensure member progress doc exists
                    val memberRef = circleRef.collection("members").document(uid)
                    val memberSnap = memberRef.get().await()
                    if (!memberSnap.exists()) {
                        memberRef.set(
                            mapOf(
                                "memberUid" to uid,
                                "displayName" to safeDisplayName(displayName),
                                "currentChapter" to 0L,
                                "totalChapters" to book.totalChapters.coerceAtLeast(1).toLong(),
                                "progressPercent" to ((1.0 / book.totalChapters.coerceAtLeast(1)) * 100).toInt().toLong(),
                                "statusNote" to "Joined via book link",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }

                    // Also ensure book is in the user's library
                    saveReadingProgress(userId, book, currentChapter = 0, minutesDelta = 0)

                    val refreshed = circleRef.get().await().toObject(
                        ReadingCircle::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: circle
                    Result.success(book to refreshed)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, circlePath)
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun updateCircleProgress(
        circleId: String,
        userId: String,
        displayName: String,
        currentChapter: Int,
        totalChapters: Int,
        statusNote: String? = null
    ): Result<Unit> {
        val path = "reading_circles/$circleId/members/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val memberRef = db.collection("reading_circles")
                .document(circleId)
                .collection("members")
                .document(uid)

            val safeChapters = totalChapters.coerceIn(1, 500)
            val safeChapter = currentChapter.coerceIn(0, safeChapters - 1)
            val percent = (((safeChapter + 1).toDouble() / safeChapters) * 100.0).toInt().coerceIn(0, 100).toLong()

            val snap = memberRef.get().await()
            if (!snap.exists()) {
                memberRef.set(
                    mapOf(
                        "memberUid" to uid,
                        "displayName" to safeDisplayName(displayName),
                        "currentChapter" to safeChapter.toLong(),
                        "totalChapters" to safeChapters.toLong(),
                        "progressPercent" to percent,
                        "statusNote" to (statusNote ?: "Chapter ${safeChapter + 1}").take(140),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val prevNote = snap.getString("statusNote") ?: ""
                memberRef.update(
                    mapOf(
                        "displayName" to safeDisplayName(displayName),
                        "currentChapter" to safeChapter.toLong(),
                        "progressPercent" to percent,
                        "statusNote" to (statusNote ?: prevNote).take(140),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun leaveCircle(circleId: String, userId: String): Result<Unit> {
        val path = "reading_circles/$circleId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val circleRef = db.collection("reading_circles").document(circleId)
            val snap = circleRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val circle = snap.toObject(ReadingCircle::class.java) ?: return Result.success(Unit)

            if (circle.creatorId == uid) {
                circleRef.delete().await()
            } else {
                circleRef.collection("members").document(uid).delete().await()
                circleRef.update(
                    mapOf(
                        "memberUids" to circle.memberUids.filter { it != uid },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeCircleComments(circle: ReadingCircle): Flow<List<CircleComment>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        if (circle.id.isBlank() || uid !in circle.memberUids) {
            return flowOf(emptyList())
        }
        val colRef = db.collection("reading_circles")
            .document(circle.id)
            .collection("comments")

        return colRef.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    CircleComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                }
                emit(emptyList())
            }
    }

    suspend fun addCircleComment(
        circleId: String,
        bookId: String,
        userId: String,
        displayName: String,
        chapterIndex: Int,
        body: String,
        parentCommentId: String = "",
        parentAuthorName: String = ""
    ): Result<Unit> {
        val trimmed = body.trim().take(500)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        }
        val commentId = "cmt_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val path = "reading_circles/$circleId/comments/$commentId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            val docRef = db.collection("reading_circles")
                .document(circleId)
                .collection("comments")
                .document(commentId)

            val payload = mapOf(
                "circleId" to circleId.take(64),
                "bookId" to bookId.take(64),
                "authorUid" to uid,
                "authorName" to safeDisplayName(displayName),
                "chapterIndex" to chapterIndex.coerceIn(0, 10000).toLong(),
                "body" to trimmed,
                "parentCommentId" to parentCommentId.take(128),
                "parentAuthorName" to parentAuthorName.take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCircleComment(
        circleId: String,
        commentId: String,
        userId: String
    ): Result<Unit> {
        val path = "reading_circles/$circleId/comments/$commentId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch" }
            db.collection("reading_circles")
                .document(circleId)
                .collection("comments")
                .document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}

