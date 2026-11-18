package com.example

import com.example.data.model.BookCatalog
import com.example.data.model.BookCategory
import com.example.data.model.BookDefinition
import com.example.data.model.ReaderThemeMode
import com.example.util.BookLinkHelper
import com.example.util.ParsedBookLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun bookLinkHelper_parsesWebAndAppLinksAndShareCodes() {
        assertEquals(
            ParsedBookLink.CircleInvite("meditations_abc123"),
            BookLinkHelper.parseBookLink("https://folio.read/circle/meditations_abc123")
        )
        assertEquals(
            ParsedBookLink.CircleInvite("wonderland_99a1"),
            BookLinkHelper.parseBookLink("folio://circle/wonderland_99a1?ref=friend")
        )
        assertEquals(
            ParsedBookLink.DirectBook("odyssey"),
            BookLinkHelper.parseBookLink("https://folio.read/book/odyssey")
        )
        assertEquals(
            ParsedBookLink.CircleInvite("meditations_abc123"),
            BookLinkHelper.parseBookLink("meditations_abc123")
        )
        assertNull(BookLinkHelper.parseBookLink("   "))
    }

    @Test
    fun bookCatalog_containsValidChaptersAndMetadata() {
        assertTrue(BookCatalog.books.size >= 6)
        BookCatalog.books.forEach { book ->
            assertTrue(book.chapters.isNotEmpty())
            assertNotNull(BookCatalog.findById(book.id))
        }
        assertEquals(ReaderThemeMode.NIGHT, ReaderThemeMode.fromRaw("NIGHT"))
        assertEquals(ReaderThemeMode.PAPER, ReaderThemeMode.fromRaw("UNKNOWN"))
    }

    @Test
    fun pdfBookDefinition_computesPagesAndMinutes() {
        val pdfBook = BookDefinition(
            id = "pdf_deadbeef",
            title = "Clean Architecture Notes",
            author = "Personal PDF",
            subtitle = "120 pages · Uploaded PDF",
            category = BookCategory.PERSONAL,
            yearLabel = "PDF",
            synopsis = "Personal PDF",
            coverDrawableRes = null,
            clothColorHex = 0xFF0F172A,
            accentColorHex = 0xFFFACC15,
            preCachedOffline = true,
            chapters = emptyList(),
            isPdfUpload = true,
            localPdfPath = "/data/user/0/com.example/files/pdfs/pdf_deadbeef.pdf",
            pdfPageCount = 120,
            pdfFileSizeKb = 1024
        )
        assertEquals(120, pdfBook.totalChapters)
        assertEquals(240, pdfBook.totalMinutesEstimate)
        assertTrue(pdfBook.isPdfUpload)
    }
}

