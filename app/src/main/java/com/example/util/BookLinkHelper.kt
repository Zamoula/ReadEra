package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent

sealed interface ParsedBookLink {
    data class CircleInvite(val circleId: String) : ParsedBookLink
    data class DirectBook(val bookId: String) : ParsedBookLink
}

object BookLinkHelper {
    private val validIdRegex = Regex("^[a-zA-Z0-9_\\-]+$")

    fun buildCircleWebLink(circleId: String): String =
        "https://folio.read/circle/$circleId"

    fun buildCircleAppLink(circleId: String): String =
        "folio://circle/$circleId"

    fun buildDirectBookLink(bookId: String): String =
        "https://folio.read/book/$bookId"

    fun parseBookLink(rawInput: String): ParsedBookLink? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        // Extract from URL-style links such as https://folio.read/circle/abc or folio://circle/abc
        val normalized = trimmed
            .substringBefore("?")
            .substringBefore("#")
            .trimEnd('/')

        val circleMarker = "/circle/"
        if (normalized.contains(circleMarker, ignoreCase = true)) {
            val id = normalized.substringAfterLast(circleMarker).trim()
            if (id.isNotBlank() && validIdRegex.matches(id)) {
                return ParsedBookLink.CircleInvite(id)
            }
        }

        if (normalized.startsWith("folio://circle/", ignoreCase = true)) {
            val id = normalized.removePrefix("folio://circle/").trim()
            if (id.isNotBlank() && validIdRegex.matches(id)) {
                return ParsedBookLink.CircleInvite(id)
            }
        }

        val bookMarker = "/book/"
        if (normalized.contains(bookMarker, ignoreCase = true)) {
            val id = normalized.substringAfterLast(bookMarker).trim()
            if (id.isNotBlank() && validIdRegex.matches(id)) {
                return ParsedBookLink.DirectBook(id)
            }
        }

        if (normalized.startsWith("folio://book/", ignoreCase = true)) {
            val id = normalized.removePrefix("folio://book/").trim()
            if (id.isNotBlank() && validIdRegex.matches(id)) {
                return ParsedBookLink.DirectBook(id)
            }
        }

        // Plain circle share code (e.g., "meditations_a1b2c3")
        if (validIdRegex.matches(normalized) && normalized.length in 4..64) {
            return ParsedBookLink.CircleInvite(normalized)
        }

        return null
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun launchShareSheet(
        context: Context,
        bookTitle: String,
        bookAuthor: String,
        circleId: String
    ) {
        val webLink = buildCircleWebLink(circleId)
        val shareText =
            "Read \"$bookTitle\" by $bookAuthor with me on Folio and track our chapters together!\n\n" +
                "Open link: $webLink\n" +
                "Or paste circle code in Folio: $circleId"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Read \"$bookTitle\" together on Folio")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Send Book Link to a Friend"))
    }
}
