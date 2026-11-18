package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BookCatalog
import com.example.data.model.BookDefinition
import com.example.util.PdfBookImporter
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "offline_books")
data class OfflineBookEntity(
    @PrimaryKey val bookId: String,
    val title: String,
    val author: String,
    val totalChapters: Int,
    val approxSizeKb: Int,
    val downloadedAtMillis: Long
)

@Entity(
    tableName = "offline_chapters",
    primaryKeys = ["bookId", "chapterIndex"]
)
data class OfflineChapterEntity(
    val bookId: String,
    val chapterIndex: Int,
    val title: String,
    val estimatedMinutes: Int,
    val bodyJoined: String
)

@Entity(tableName = "daily_reading_logs")
data class DailyReadingLogEntity(
    @PrimaryKey val dateKey: String,
    val minutesRead: Int
)

@Entity(tableName = "uploaded_pdf_books")
data class UploadedPdfEntity(
    @PrimaryKey val bookId: String,
    val title: String,
    val author: String,
    val localFilePath: String,
    val pageCount: Int,
    val fileSizeKb: Int,
    val addedAtMillis: Long
)

@Dao
interface OfflineDao {
    @Query("SELECT * FROM offline_books ORDER BY downloadedAtMillis DESC")
    fun observeOfflineBooks(): Flow<List<OfflineBookEntity>>

    @Query("SELECT * FROM offline_chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    fun observeChaptersForBook(bookId: String): Flow<List<OfflineChapterEntity>>

    @Query("SELECT * FROM offline_chapters WHERE bookId = :bookId ORDER BY chapterIndex ASC")
    suspend fun getChaptersForBook(bookId: String): List<OfflineChapterEntity>

    @Query("SELECT COUNT(*) FROM offline_books WHERE bookId = :bookId")
    suspend fun isBookCached(bookId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineBook(book: OfflineBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfflineChapters(chapters: List<OfflineChapterEntity>)

    @Query("DELETE FROM offline_books WHERE bookId = :bookId")
    suspend fun deleteOfflineBook(bookId: String)

    @Query("DELETE FROM offline_chapters WHERE bookId = :bookId")
    suspend fun deleteOfflineChapters(bookId: String)

    @Query("SELECT * FROM daily_reading_logs ORDER BY dateKey DESC LIMIT 7")
    fun observeRecentDailyLogs(): Flow<List<DailyReadingLogEntity>>

    @Query("SELECT * FROM daily_reading_logs WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getLogForDate(dateKey: String): DailyReadingLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyLog(log: DailyReadingLogEntity)

    @Query("SELECT * FROM uploaded_pdf_books ORDER BY addedAtMillis DESC")
    fun observeUploadedPdfs(): Flow<List<UploadedPdfEntity>>

    @Query("SELECT * FROM uploaded_pdf_books WHERE bookId = :bookId LIMIT 1")
    suspend fun getUploadedPdf(bookId: String): UploadedPdfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadedPdf(pdf: UploadedPdfEntity)

    @Query("DELETE FROM uploaded_pdf_books WHERE bookId = :bookId")
    suspend fun deleteUploadedPdf(bookId: String)
}

@Database(
    entities = [
        OfflineBookEntity::class,
        OfflineChapterEntity::class,
        DailyReadingLogEntity::class,
        UploadedPdfEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun offlineDao(): OfflineDao

    companion object {
        @Volatile
        private var INSTANCE: FolioDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `uploaded_pdf_books` (
                        `bookId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `localFilePath` TEXT NOT NULL,
                        `pageCount` INTEGER NOT NULL,
                        `fileSizeKb` INTEGER NOT NULL,
                        `addedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`bookId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): FolioDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FolioDatabase::class.java,
                    "folio_offline.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class OfflineBookRepository(private val dao: OfflineDao) {
    val offlineBooks: Flow<List<OfflineBookEntity>> = dao.observeOfflineBooks()
    val recentDailyLogs: Flow<List<DailyReadingLogEntity>> = dao.observeRecentDailyLogs()
    val uploadedPdfs: Flow<List<UploadedPdfEntity>> = dao.observeUploadedPdfs()

    suspend fun ensureDefaultOfflineBooks() {
        for (book in BookCatalog.books.filter { it.preCachedOffline }) {
            if (dao.isBookCached(book.id) == 0) {
                saveBookOffline(book)
            }
        }
    }

    suspend fun saveBookOffline(book: BookDefinition) {
        if (book.isPdfUpload) return
        val totalChars = book.chapters.sumOf { ch -> ch.paragraphs.sumOf { it.length } }
        val approxKb = (totalChars / 1024).coerceAtLeast(4)
        dao.insertOfflineBook(
            OfflineBookEntity(
                bookId = book.id,
                title = book.title,
                author = book.author,
                totalChapters = book.totalChapters,
                approxSizeKb = approxKb,
                downloadedAtMillis = System.currentTimeMillis()
            )
        )
        dao.insertOfflineChapters(
            book.chapters.map { chapter ->
                OfflineChapterEntity(
                    bookId = book.id,
                    chapterIndex = chapter.index,
                    title = chapter.title,
                    estimatedMinutes = chapter.estimatedMinutes,
                    bodyJoined = chapter.paragraphs.joinToString("\n\n")
                )
            }
        )
    }

    suspend fun removeBookOffline(bookId: String) {
        dao.deleteOfflineChapters(bookId)
        dao.deleteOfflineBook(bookId)
    }

    suspend fun getCachedChapters(bookId: String): List<OfflineChapterEntity> =
        dao.getChaptersForBook(bookId)

    suspend fun addReadingMinutesForDate(dateKey: String, deltaMinutes: Int) {
        if (deltaMinutes <= 0) return
        val current = dao.getLogForDate(dateKey)?.minutesRead ?: 0
        dao.upsertDailyLog(
            DailyReadingLogEntity(
                dateKey = dateKey,
                minutesRead = current + deltaMinutes
            )
        )
    }

    suspend fun saveUploadedPdf(pdf: UploadedPdfEntity) {
        dao.insertUploadedPdf(pdf)
    }

    suspend fun removeUploadedPdf(bookId: String) {
        val existing = dao.getUploadedPdf(bookId)
        if (existing != null) {
            PdfBookImporter.deleteLocalPdf(existing.localFilePath)
        }
        dao.deleteUploadedPdf(bookId)
    }
}
