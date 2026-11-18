package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DailyReadingLogEntity
import com.example.data.local.OfflineBookEntity
import com.example.data.local.OfflineBookRepository
import com.example.data.local.PreferencesRepository
import com.example.data.local.ReaderPreferences
import com.example.data.local.UploadedPdfEntity
import com.example.data.model.BookCatalog
import com.example.data.model.BookCategory
import com.example.data.model.BookDefinition
import com.example.data.model.CircleComment
import com.example.data.model.CircleMemberProgress
import com.example.data.model.LibraryBook
import com.example.data.model.ReaderThemeMode
import com.example.data.model.ReadingCircle
import com.example.data.model.UserProfile
import com.example.data.remote.FolioFirestoreRepository
import com.example.util.BookCommentNotifier
import com.example.util.CommentNotificationTracker
import com.example.util.ImportedPdfMetadata
import com.example.util.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class DailyActivityPoint(
    val dateKey: String,
    val dayLabel: String,
    val minutesRead: Int
)

data class BookWithProgress(
    val book: BookDefinition,
    val libraryState: LibraryBook?,
    val offlineEntity: OfflineBookEntity?
) {
    val isOfflineReady: Boolean get() = book.isPdfUpload || offlineEntity != null
    val offlineSizeKb: Int get() = if (book.isPdfUpload) book.pdfFileSizeKb else (offlineEntity?.approxSizeKb ?: 0)
    val currentChapter: Int get() = (libraryState?.currentChapter?.toInt() ?: 0).coerceIn(0, (book.totalChapters - 1).coerceAtLeast(0))
    val progressPercent: Int get() = (libraryState?.progressPercent?.toInt() ?: 0).coerceIn(0, 100)
    val isCompleted: Boolean get() = libraryState?.completed == true
    val minutesRead: Int get() = libraryState?.minutesRead?.toInt() ?: 0
    val bookmarkChapter: Int get() = libraryState?.bookmarkChapter?.toInt() ?: 0
    val bookmarkNote: String get() = libraryState?.bookmarkNote ?: ""
}

data class CircleWithMembers(
    val circle: ReadingCircle,
    val book: BookDefinition?,
    val members: List<CircleMemberProgress>,
    val comments: List<CircleComment> = emptyList(),
    val unreadCommentCount: Int = 0
)

data class DashboardUiData(
    val profile: UserProfile?,
    val books: List<BookWithProgress>,
    val circles: List<CircleWithMembers>,
    val weeklyActivity: List<DailyActivityPoint>,
    val todayMinutes: Int,
    val preferences: ReaderPreferences,
    val isOnline: Boolean,
    val totalUnreadComments: Int = 0
)

class FolioViewModel(
    private val appContext: Context,
    private val firestoreRepository: FolioFirestoreRepository,
    private val offlineRepository: OfflineBookRepository,
    private val preferencesRepository: PreferencesRepository,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val displayName: String
) : ViewModel() {

    private val commentTracker = CommentNotificationTracker(appContext, currentUserId)
    private val _seenRevision = MutableStateFlow(0L)

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _shareDialogCircle = MutableStateFlow<ReadingCircle?>(null)
    val shareDialogCircle: StateFlow<ReadingCircle?> = _shareDialogCircle.asStateFlow()

    private val _pendingOpenBookId = MutableStateFlow<String?>(null)
    val pendingOpenBookId: StateFlow<String?> = _pendingOpenBookId.asStateFlow()

    init {
        viewModelScope.launch {
            offlineRepository.ensureDefaultOfflineBooks()
            firestoreRepository.ensureUserProfile(currentUserId, displayName)
        }
    }

    private val profileFlow = firestoreRepository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Profile stream error", firestoreError)
            emit(UiState.Error(firestoreError.message ?: "Unable to sync reading profile"))
        }

    private val libraryFlow = firestoreRepository.observeLibrary(currentUserId)
        .map<List<LibraryBook>, UiState<List<LibraryBook>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Library stream error", firestoreError)
            emit(UiState.Error(firestoreError.message ?: "Unable to sync library"))
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val circlesWithMembersFlow = combine(
        firestoreRepository.observeMyCircles(currentUserId),
        _seenRevision
    ) { circles, _ -> circles }
        .flatMapLatest { circles ->
            if (circles.isEmpty()) {
                flowOf(emptyList())
            } else {
                val perCircleFlows = circles.map { circle ->
                    combine(
                        firestoreRepository.observeCircleMembers(circle),
                        firestoreRepository.observeCircleComments(circle),
                        _seenRevision
                    ) { members, comments, _ ->
                        notifyOnUnseenFriendComments(circle, comments)
                        val lastSeen = commentTracker.getLastSeenMillis(circle.id)
                        val unread = comments.count { comment ->
                            val createdMs = comment.createdAt?.toDate()?.time ?: 0L
                            comment.authorUid != currentUserId && createdMs > lastSeen
                        }
                        CircleWithMembers(
                            circle = circle,
                            book = BookCatalog.findById(circle.bookId),
                            members = members,
                            comments = comments,
                            unreadCommentCount = unread
                        )
                    }
                }
                combine(perCircleFlows) {
                    it.toList().sortedByDescending { c -> c.circle.updatedAt?.seconds ?: 0L }
                }
            }
        }
        .map<List<CircleWithMembers>, UiState<List<CircleWithMembers>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreErrorOrThrow(error)
            Log.w(TAG, "Circles stream error", firestoreError)
            emit(UiState.Error(firestoreError.message ?: "Unable to sync friend reading circles"))
        }

    val preferences: StateFlow<ReaderPreferences> = preferencesRepository.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ReaderPreferences()
        )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true
        )

    val dashboardState: StateFlow<UiState<DashboardUiData>> = combine(
        profileFlow,
        libraryFlow,
        offlineRepository.offlineBooks,
        offlineRepository.uploadedPdfs,
        offlineRepository.recentDailyLogs,
        circlesWithMembersFlow,
        preferences,
        isOnline
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val profState = values[0] as UiState<UserProfile?>
        @Suppress("UNCHECKED_CAST")
        val libState = values[1] as UiState<List<LibraryBook>>
        @Suppress("UNCHECKED_CAST")
        val offlineList = values[2] as List<OfflineBookEntity>
        @Suppress("UNCHECKED_CAST")
        val uploadedPdfList = values[3] as List<UploadedPdfEntity>
        @Suppress("UNCHECKED_CAST")
        val dailyLogs = values[4] as List<DailyReadingLogEntity>
        @Suppress("UNCHECKED_CAST")
        val circlesState = values[5] as UiState<List<CircleWithMembers>>
        val prefs = values[6] as ReaderPreferences
        val online = values[7] as Boolean

        val libraryMap = (libState as? UiState.Success)?.data?.associateBy { it.bookId } ?: emptyMap()
        val offlineMap = offlineList.associateBy { it.bookId }

        val pdfBooks = uploadedPdfList.map { pdf ->
            BookDefinition(
                id = pdf.bookId,
                title = pdf.title,
                author = pdf.author,
                subtitle = "${pdf.pageCount} pages · Uploaded PDF",
                category = BookCategory.PERSONAL,
                yearLabel = "PDF",
                synopsis = "Personal PDF book stored locally on your device for offline reading.",
                coverDrawableRes = null,
                clothColorHex = 0xFF0F172A,
                accentColorHex = 0xFFFACC15,
                preCachedOffline = true,
                chapters = emptyList(),
                isPdfUpload = true,
                localPdfPath = pdf.localFilePath,
                pdfPageCount = pdf.pageCount,
                pdfFileSizeKb = pdf.fileSizeKb
            )
        }

        val mergedBooks = (pdfBooks + BookCatalog.books).map { book ->
            BookWithProgress(
                book = book,
                libraryState = libraryMap[book.id],
                offlineEntity = offlineMap[book.id]
            )
        }.sortedByDescending { item ->
            item.libraryState?.lastReadAt?.seconds
                ?: if (item.book.isPdfUpload) 2L else if (item.isOfflineReady) 1L else 0L
        }

        val weekly = buildLast7Days(dailyLogs)
        val todayMin = weekly.lastOrNull()?.minutesRead ?: 0

        val errorMsg = (profState as? UiState.Error)?.message
            ?: (libState as? UiState.Error)?.message
            ?: (circlesState as? UiState.Error)?.message

        val profile = (profState as? UiState.Success)?.data
        val circles = (circlesState as? UiState.Success)?.data ?: emptyList()

        // If online and profile failed hard with no cached data, surface error; otherwise show rich UI
        if (errorMsg != null && profile == null && online && libraryMap.isEmpty()) {
            UiState.Error(errorMsg)
        } else {
            UiState.Success(
                DashboardUiData(
                    profile = profile,
                    books = mergedBooks,
                    circles = circles,
                    weeklyActivity = weekly,
                    todayMinutes = todayMin,
                    preferences = prefs,
                    isOnline = online,
                    totalUnreadComments = circles.sumOf { it.unreadCommentCount }
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun toggleNightMode() {
        viewModelScope.launch {
            val next = !preferences.value.nightMode
            preferencesRepository.setNightMode(next, syncReaderTheme = true)
            val currentProfile = (dashboardState.value as? UiState.Success)?.data?.profile
            val nextTheme = if (next) ReaderThemeMode.NIGHT else ReaderThemeMode.PAPER
            firestoreRepository.updatePreferences(
                userId = currentUserId,
                nightMode = next,
                readerFontSize = preferences.value.fontSizeSp,
                readerTheme = nextTheme,
                dailyGoalMinutes = currentProfile?.dailyGoalMinutes?.toInt() ?: 30
            )
        }
    }

    fun setReaderTheme(theme: ReaderThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setReaderTheme(theme)
            val isNight = theme == ReaderThemeMode.NIGHT || theme == ReaderThemeMode.OLED
            val currentProfile = (dashboardState.value as? UiState.Success)?.data?.profile
            firestoreRepository.updatePreferences(
                userId = currentUserId,
                nightMode = isNight,
                readerFontSize = preferences.value.fontSizeSp,
                readerTheme = theme,
                dailyGoalMinutes = currentProfile?.dailyGoalMinutes?.toInt() ?: 30
            )
        }
    }

    fun adjustFontSize(delta: Int) {
        viewModelScope.launch {
            val next = (preferences.value.fontSizeSp + delta).coerceIn(14, 28)
            preferencesRepository.setFontSize(next)
            val currentProfile = (dashboardState.value as? UiState.Success)?.data?.profile
            firestoreRepository.updatePreferences(
                userId = currentUserId,
                nightMode = preferences.value.nightMode,
                readerFontSize = next,
                readerTheme = preferences.value.readerTheme,
                dailyGoalMinutes = currentProfile?.dailyGoalMinutes?.toInt() ?: 30
            )
        }
    }

    fun setLineSpacing(level: Int) {
        viewModelScope.launch {
            preferencesRepository.setLineSpacingLevel(level)
        }
    }

    fun setUseSerifFont(useSerif: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setUseSerifFont(useSerif)
        }
    }

    fun updateDailyGoal(minutes: Int) {
        viewModelScope.launch {
            val clamped = minutes.coerceIn(5, 240)
            val prefs = preferences.value
            val result = firestoreRepository.updatePreferences(
                userId = currentUserId,
                nightMode = prefs.nightMode,
                readerFontSize = prefs.fontSizeSp,
                readerTheme = prefs.readerTheme,
                dailyGoalMinutes = clamped
            )
            result.onSuccess {
                _statusBanner.value = "Daily goal updated to $clamped min"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Saved locally; will sync online"
            }
        }
    }

    fun toggleOfflineBook(book: BookDefinition, currentlyOffline: Boolean) {
        viewModelScope.launch {
            if (currentlyOffline) {
                offlineRepository.removeBookOffline(book.id)
                firestoreRepository.saveReadingProgress(
                    userId = currentUserId,
                    book = book,
                    currentChapter = currentChapterFor(book.id),
                    minutesDelta = 0,
                    offlineSaved = false
                )
                _statusBanner.value = "Removed \"${book.title}\" from offline storage"
            } else {
                offlineRepository.saveBookOffline(book)
                firestoreRepository.saveReadingProgress(
                    userId = currentUserId,
                    book = book,
                    currentChapter = currentChapterFor(book.id),
                    minutesDelta = 0,
                    offlineSaved = true
                )
                _statusBanner.value = "\"${book.title}\" saved for offline reading"
            }
        }
    }

    fun downloadAllOffline() {
        viewModelScope.launch {
            for (book in BookCatalog.books) {
                offlineRepository.saveBookOffline(book)
            }
            _statusBanner.value = "All ${BookCatalog.books.size} books cached for offline access"
        }
    }

    fun importPdfBook(
        meta: ImportedPdfMetadata,
        customTitle: String,
        customAuthor: String,
        onImported: (String) -> Unit
    ) {
        viewModelScope.launch {
            val title = customTitle.trim().ifBlank { meta.suggestedTitle }.take(150)
            val author = customAuthor.trim().ifBlank { "Personal PDF" }.take(100)
            val entity = UploadedPdfEntity(
                bookId = meta.bookId,
                title = title,
                author = author,
                localFilePath = meta.localFilePath,
                pageCount = meta.pageCount,
                fileSizeKb = meta.fileSizeKb,
                addedAtMillis = System.currentTimeMillis()
            )
            offlineRepository.saveUploadedPdf(entity)

            val pdfBookDef = BookDefinition(
                id = meta.bookId,
                title = title,
                author = author,
                subtitle = "${meta.pageCount} pages · Uploaded PDF",
                category = BookCategory.PERSONAL,
                yearLabel = "PDF",
                synopsis = "Personal PDF book stored locally on your device.",
                coverDrawableRes = null,
                clothColorHex = 0xFF0F172A,
                accentColorHex = 0xFFFACC15,
                preCachedOffline = true,
                chapters = emptyList(),
                isPdfUpload = true,
                localPdfPath = meta.localFilePath,
                pdfPageCount = meta.pageCount,
                pdfFileSizeKb = meta.fileSizeKb
            )
            firestoreRepository.saveReadingProgress(
                userId = currentUserId,
                book = pdfBookDef,
                currentChapter = 0,
                minutesDelta = 0,
                offlineSaved = true
            )
            _statusBanner.value = "Added \"$title\" (${meta.pageCount} pages) to your library"
            onImported(meta.bookId)
        }
    }

    fun deleteUploadedPdf(bookId: String) {
        viewModelScope.launch {
            offlineRepository.removeUploadedPdf(bookId)
            firestoreRepository.removeLibraryEntry(currentUserId, bookId)
            _statusBanner.value = "Removed uploaded PDF book"
        }
    }

    fun showStatusMessage(message: String) {
        _statusBanner.value = message
    }

    fun saveChapterProgress(
        book: BookDefinition,
        chapterIndex: Int,
        minutesDelta: Int = 0,
        markCompleted: Boolean = false
    ) {
        viewModelScope.launch {
            if (minutesDelta > 0) {
                offlineRepository.addReadingMinutesForDate(todayKey(), minutesDelta)
            }
            val result = firestoreRepository.saveReadingProgress(
                userId = currentUserId,
                book = book,
                currentChapter = chapterIndex,
                minutesDelta = minutesDelta,
                markCompleted = if (markCompleted) true else null
            )
            result.onFailure {
                // Keep quiet when offline; Room + Firestore local cache handles it gracefully
                Log.w(TAG, "Progress queued locally: ${it.message}")
            }

            // Also update any reading circles the user belongs to for this book
            val activeCircles = (dashboardState.value as? UiState.Success)?.data?.circles
                ?.filter { it.circle.bookId == book.id } ?: emptyList()
            for (cwm in activeCircles) {
                firestoreRepository.updateCircleProgress(
                    circleId = cwm.circle.id,
                    userId = currentUserId,
                    displayName = displayName,
                    currentChapter = chapterIndex,
                    totalChapters = book.totalChapters,
                    statusNote = if (markCompleted) "Finished reading!" else "On Chapter ${chapterIndex + 1}"
                )
            }
        }
    }

    fun saveBookmark(book: BookDefinition, chapterIndex: Int, note: String) {
        viewModelScope.launch {
            val result = firestoreRepository.saveReadingProgress(
                userId = currentUserId,
                book = book,
                currentChapter = chapterIndex,
                minutesDelta = 0,
                bookmarkChapter = chapterIndex,
                bookmarkNote = note
            )
            result.onSuccess {
                _statusBanner.value = "Bookmark saved at Chapter ${chapterIndex + 1}"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Bookmark queued for sync"
            }
        }
    }

    fun createOrShareBookLink(book: BookDefinition) {
        viewModelScope.launch {
            val chapter = currentChapterFor(book.id)
            val result = firestoreRepository.createOrGetCircleForBook(
                userId = currentUserId,
                displayName = displayName,
                book = book,
                currentChapter = chapter
            )
            result.onSuccess { circle ->
                _shareDialogCircle.value = circle
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Unable to create share link while offline"
            }
        }
    }

    fun dismissShareDialog() {
        _shareDialogCircle.value = null
    }

    fun openOrJoinBookLink(rawInput: String, onOpenBook: (String) -> Unit) {
        viewModelScope.launch {
            val result = firestoreRepository.joinCircleByLinkOrCode(
                userId = currentUserId,
                displayName = displayName,
                rawInput = rawInput
            )
            result.onSuccess { (book, circle) ->
                offlineRepository.saveBookOffline(book)
                _statusBanner.value = if (circle != null) {
                    "Joined friend circle for \"${book.title}\""
                } else {
                    "Opened \"${book.title}\" from book link"
                }
                onOpenBook(book.id)
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not open book link"
            }
        }
    }

    fun updateCircleNote(circle: ReadingCircle, note: String) {
        viewModelScope.launch {
            val chapter = currentChapterFor(circle.bookId)
            val result = firestoreRepository.updateCircleProgress(
                circleId = circle.id,
                userId = currentUserId,
                displayName = displayName,
                currentChapter = chapter,
                totalChapters = circle.totalChapters.toInt(),
                statusNote = note
            )
            result.onSuccess {
                _statusBanner.value = "Updated reading note for friends"
            }.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not update note"
            }
        }
    }

    fun leaveCircle(circleId: String) {
        viewModelScope.launch {
            firestoreRepository.leaveCircle(circleId, currentUserId)
                .onSuccess { _statusBanner.value = "Left reading circle" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Failed to leave circle" }
        }
    }

    fun postBookComment(
        book: BookDefinition,
        chapterIndex: Int,
        body: String,
        quoteText: String = "",
        parentComment: CircleComment? = null,
        preferredCircleId: String? = null
    ) {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existingCircleId = preferredCircleId
                ?: (dashboardState.value as? UiState.Success)?.data?.circles
                    ?.firstOrNull { it.circle.bookId == book.id }?.circle?.id

            val resolvedCircleId = if (existingCircleId != null) {
                existingCircleId
            } else {
                val created = firestoreRepository.createOrGetCircleForBook(
                    userId = currentUserId,
                    displayName = displayName,
                    book = book,
                    currentChapter = chapterIndex
                )
                created.getOrElse { err ->
                    _statusBanner.value = err.localizedMessage ?: "Could not start discussion"
                    return@launch
                }.id
            }

            commentTracker.markSeen(resolvedCircleId, System.currentTimeMillis())
            commentTracker.markNotified(resolvedCircleId, System.currentTimeMillis())
            _seenRevision.update { it + 1L }

            val result = firestoreRepository.addCircleComment(
                circleId = resolvedCircleId,
                bookId = book.id,
                userId = currentUserId,
                displayName = displayName,
                chapterIndex = chapterIndex,
                body = trimmed,
                quoteText = quoteText.trim(),
                parentCommentId = parentComment?.id.orEmpty(),
                parentAuthorName = parentComment?.authorName.orEmpty()
            )
            result.onFailure { err ->
                _statusBanner.value = err.localizedMessage ?: "Could not post comment"
            }
        }
    }

    fun deleteBookComment(circleId: String, commentId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteCircleComment(circleId, commentId, currentUserId)
                .onFailure { err ->
                    _statusBanner.value = err.localizedMessage ?: "Could not delete comment"
                }
        }
    }

    fun markCircleCommentsRead(circleId: String) {
        val circles = (dashboardState.value as? UiState.Success)?.data?.circles.orEmpty()
        val latest = circles.firstOrNull { it.circle.id == circleId }
            ?.comments?.maxOfOrNull { it.createdAt?.toDate()?.time ?: 0L }
            ?: 0L
        commentTracker.markSeen(circleId, maxOf(latest, System.currentTimeMillis()))
        _seenRevision.update { it + 1L }
    }

    private fun notifyOnUnseenFriendComments(
        circle: ReadingCircle,
        comments: List<CircleComment>
    ) {
        val lastNotified = commentTracker.getLastNotifiedMillis(circle.id)
        val newFriendComments = comments.filter { comment ->
            val createdMs = comment.createdAt?.toDate()?.time ?: 0L
            comment.authorUid != currentUserId && createdMs > lastNotified
        }
        if (newFriendComments.isEmpty()) return

        val latest = newFriendComments.last()
        val latestMs = newFriendComments.maxOf { it.createdAt?.toDate()?.time ?: 0L }
        if (latestMs <= 0L) return

        commentTracker.markNotified(circle.id, latestMs)

        val isReply = latest.parentCommentId.isNotBlank()
        _statusBanner.value = when {
            isReply -> "${latest.authorName} replied on \"${circle.bookTitle}\": ${latest.body.take(48)}"
            latest.quoteText.isNotBlank() -> "${latest.authorName} highlighted \"${latest.quoteText.take(36)}\": ${latest.body.take(42)}"
            else -> "${latest.authorName} commented on \"${circle.bookTitle}\": ${latest.body.take(48)}"
        }

        BookCommentNotifier.notifyNewComment(
            context = appContext,
            circleId = circle.id,
            bookTitle = circle.bookTitle,
            authorName = latest.authorName,
            commentBody = latest.body,
            isReply = isReply,
            quoteText = latest.quoteText
        )
    }

    fun handleIncomingDeepLink(rawUri: String) {
        openOrJoinBookLink(rawUri) { bookId ->
            _pendingOpenBookId.value = bookId
        }
    }

    fun consumePendingBookId() {
        _pendingOpenBookId.value = null
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    private fun currentChapterFor(bookId: String): Int {
        val books = (dashboardState.value as? UiState.Success)?.data?.books ?: return 0
        return books.firstOrNull { it.book.id == bookId }?.currentChapter ?: 0
    }

    private fun unwrapFirestoreErrorOrThrow(error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return firestoreError ?: error
    }

    private fun todayKey(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Calendar.getInstance().time)
    }

    private fun buildLast7Days(logs: List<DailyReadingLogEntity>): List<DailyActivityPoint> {
        val logMap = logs.associateBy { it.dateKey }
        val keyFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFmt = SimpleDateFormat("EEE", Locale.US)
        val result = mutableListOf<DailyActivityPoint>()
        for (daysAgo in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
            val key = keyFmt.format(cal.time)
            val label = dayFmt.format(cal.time).take(3)
            result.add(
                DailyActivityPoint(
                    dateKey = key,
                    dayLabel = label,
                    minutesRead = logMap[key]?.minutesRead ?: 0
                )
            )
        }
        return result
    }

    private companion object {
        const val TAG = "FolioViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
