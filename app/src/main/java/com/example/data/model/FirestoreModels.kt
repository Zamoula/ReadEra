package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val dailyGoalMinutes: Long = 30L,
    val streakDays: Long = 1L,
    val totalMinutesRead: Long = 0L,
    val booksCompleted: Long = 0L,
    val nightMode: Boolean = false,
    val readerFontSize: Long = 18L,
    val readerTheme: String = ReaderThemeMode.PAPER.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LibraryBook(
    @DocumentId val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val currentChapter: Long = 0L,
    val totalChapters: Long = 1L,
    val progressPercent: Long = 0L,
    val minutesRead: Long = 0L,
    val bookmarkChapter: Long = 0L,
    val bookmarkNote: String = "",
    val offlineSaved: Boolean = false,
    val completed: Boolean = false,
    @ServerTimestamp val lastReadAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReadingCircle(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val bookId: String = "",
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val totalChapters: Long = 1L,
    val shareCode: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CircleMemberProgress(
    @DocumentId val id: String = "",
    val memberUid: String = "",
    val displayName: String = "",
    val currentChapter: Long = 0L,
    val totalChapters: Long = 1L,
    val progressPercent: Long = 0L,
    val statusNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CircleComment(
    @DocumentId val id: String = "",
    val circleId: String = "",
    val bookId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val chapterIndex: Long = 0L,
    val body: String = "",
    val quoteText: String = "",
    val parentCommentId: String = "",
    val parentAuthorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

