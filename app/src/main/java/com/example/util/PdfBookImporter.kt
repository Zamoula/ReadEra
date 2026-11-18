package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

data class ImportedPdfMetadata(
    val bookId: String,
    val suggestedTitle: String,
    val localFilePath: String,
    val pageCount: Int,
    val fileSizeKb: Int
)

object PdfBookImporter {

    suspend fun importPdf(context: Context, uri: Uri): Result<ImportedPdfMetadata> =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val bookId = "pdf_${UUID.randomUUID().toString().replace("-", "").take(8)}"
            val pdfDir = File(appContext.filesDir, "pdfs").apply { mkdirs() }
            val destFile = File(pdfDir, "$bookId.pdf")

            try {
                val rawName = queryDisplayName(appContext, uri)
                val cleanTitle = formatPdfTitle(rawName)

                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext Result.failure(
                    IllegalStateException("Unable to open selected PDF file.")
                )

                if (!destFile.exists() || destFile.length() == 0L) {
                    destFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException("Selected PDF file is empty.")
                    )
                }

                val pageCount = ParcelFileDescriptor.open(
                    destFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                ).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.pageCount
                    }
                }

                if (pageCount <= 0) {
                    destFile.delete()
                    return@withContext Result.failure(
                        IllegalStateException("The selected PDF has no readable pages.")
                    )
                }

                val fileSizeKb = (destFile.length() / 1024L).toInt().coerceAtLeast(1)

                Result.success(
                    ImportedPdfMetadata(
                        bookId = bookId,
                        suggestedTitle = cleanTitle,
                        localFilePath = destFile.absolutePath,
                        pageCount = pageCount,
                        fileSizeKb = fileSizeKb
                    )
                )
            } catch (e: Exception) {
                destFile.delete()
                Result.failure(
                    IllegalStateException(
                        e.localizedMessage ?: "Could not read PDF file. Please choose a valid PDF."
                    )
                )
            }
        }

    suspend fun renderPageBitmap(
        filePath: String,
        pageIndex: Int,
        targetWidthPx: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount <= 0) return@withContext null
                    val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
                    renderer.openPage(safeIndex).use { page ->
                        val width = targetWidthPx.coerceIn(480, 1800)
                        val height = ((width.toFloat() / page.width.coerceAtLeast(1)) * page.height)
                            .toInt()
                            .coerceIn(480, 2600)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun deleteLocalPdf(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun queryDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        return uri.lastPathSegment ?: "Uploaded Book"
    }

    private fun formatPdfTitle(rawName: String): String {
        val stripped = rawName
            .substringAfterLast('/')
            .removeSuffix(".pdf")
            .removeSuffix(".PDF")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
        if (stripped.isBlank()) return "Uploaded PDF Book"
        return stripped.split(Regex("\\s+")).joinToString(" ") { word ->
            word.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }.take(120)
    }
}
