package com.example.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookCategory
import com.example.data.model.BookDefinition
import com.example.ui.BookWithProgress
import com.example.ui.DashboardUiData
import com.example.ui.components.BookCoverArt
import com.example.ui.components.ConnectivityBadge
import com.example.ui.components.OfflineReadyChip

private enum class LibraryFilter(val label: String) {
    ALL("All Books"),
    MY_PDFS("My PDFs"),
    OFFLINE("Saved Offline"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

@Composable
fun LibraryScreen(
    data: DashboardUiData,
    onOpenBook: (BookDefinition, Int) -> Unit,
    onToggleOffline: (BookDefinition, Boolean) -> Unit,
    onDownloadAllOffline: () -> Unit,
    onShareBook: (BookDefinition) -> Unit,
    onUploadPdfClick: () -> Unit,
    onDeleteUploadedPdf: (String) -> Unit,
    onToggleNightMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var activeCategory by remember { mutableStateOf<BookCategory?>(null) }
    var browseClassicsWhenEmpty by remember { mutableStateOf(false) }

    val uploadedCount = remember(data.books) { data.books.count { it.book.isPdfUpload } }
    val hasUploadedBooks = uploadedCount > 0
    val offlineBooks = remember(data.books) { data.books.filter { it.isOfflineReady } }
    val totalOfflineKb = remember(offlineBooks) { offlineBooks.sumOf { it.offlineSizeKb } }

    val filteredBooks = remember(data.books, searchQuery, activeFilter, activeCategory) {
        data.books.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.book.title.contains(searchQuery, ignoreCase = true) ||
                item.book.author.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (activeFilter) {
                LibraryFilter.ALL -> true
                LibraryFilter.MY_PDFS -> item.book.isPdfUpload
                LibraryFilter.OFFLINE -> item.isOfflineReady
                LibraryFilter.IN_PROGRESS -> item.progressPercent in 1..99
                LibraryFilter.COMPLETED -> item.isCompleted
            }

            val matchesCat = activeCategory == null || item.book.category == activeCategory
            matchesQuery && matchesFilter && matchesCat
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Library",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (hasUploadedBooks) {
                                "$uploadedCount uploaded · ${data.books.size} books · ${offlineBooks.size} ready offline"
                            } else {
                                "No uploaded books yet · ${data.books.size} classics available"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ConnectivityBadge(isOnline = data.isOnline)
                        FilledTonalIconButton(onClick = onToggleNightMode) {
                            Icon(
                                imageVector = if (data.preferences.nightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle night mode"
                            )
                        }
                    }
                }
            }

            if (!hasUploadedBooks && !browseClassicsWhenEmpty) {
                item {
                    EmptyUploadedLibraryView(
                        classicsCount = data.books.size,
                        onUploadPdfClick = onUploadPdfClick,
                        onBrowseClassics = { browseClassicsWhenEmpty = true }
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search title or author…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("library_search_input")
                        )
                        FilledTonalButton(
                            onClick = onUploadPdfClick,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            modifier = Modifier.testTag("upload_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("PDF")
                        }
                    }
                }

                // Personal PDF Upload Callout Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Read Your Own PDF Books",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Import any PDF from your device with offline access, night mode & progress tracking.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            Button(
                                onClick = onUploadPdfClick,
                                modifier = Modifier.testTag("upload_pdf_banner_button")
                            ) {
                                Text("Upload")
                            }
                        }
                    }
                }

                // Offline Access Storage Banner
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Offline Access Vault",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "${offlineBooks.size} of ${data.books.size} books cached locally ($totalOfflineKb KB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                                )
                            }
                            if (offlineBooks.size < data.books.size) {
                                FilledTonalButton(
                                    onClick = onDownloadAllOffline,
                                    modifier = Modifier.testTag("download_all_offline_button")
                                ) {
                                    Text("Save All")
                                }
                            }
                        }
                    }
                }

                // Status Filter Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(LibraryFilter.entries) { filter ->
                            FilterChip(
                                selected = activeFilter == filter,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                // Category Chips
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = activeCategory == null,
                                onClick = { activeCategory = null },
                                label = { Text("All Genres") }
                            )
                        }
                        items(BookCategory.entries) { cat ->
                            FilterChip(
                                selected = activeCategory == cat,
                                onClick = { activeCategory = if (activeCategory == cat) null else cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }

                if (filteredBooks.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeFilter == LibraryFilter.MY_PDFS) {
                                        Icons.Default.PictureAsPdf
                                    } else {
                                        Icons.AutoMirrored.Filled.MenuBook
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = if (activeFilter == LibraryFilter.MY_PDFS) {
                                        "No PDF books uploaded yet"
                                    } else {
                                        "No matching books"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (activeFilter == LibraryFilter.MY_PDFS) {
                                        "Upload a PDF from your device to read with bookmarks, zoom, and night mode."
                                    } else {
                                        "Clear filters or search terms to explore the full shelf."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (activeFilter == LibraryFilter.MY_PDFS) {
                                    Button(onClick = onUploadPdfClick) {
                                        Text("Upload PDF Book")
                                    }
                                } else {
                                    TextButton(
                                        onClick = {
                                            searchQuery = ""
                                            activeFilter = LibraryFilter.ALL
                                            activeCategory = null
                                        }
                                    ) {
                                        Text("Reset Filters")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredBooks, key = { it.book.id }) { item ->
                        LibraryBookCard(
                            item = item,
                            isOnline = data.isOnline,
                            onRead = { onOpenBook(item.book, item.currentChapter) },
                            onToggleOffline = { onToggleOffline(item.book, item.isOfflineReady) },
                            onShare = { onShareBook(item.book) },
                            onDeletePdf = { onDeleteUploadedPdf(item.book.id) }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun EmptyUploadedLibraryView(
    classicsCount: Int,
    onUploadPdfClick: () -> Unit,
    onBrowseClassics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_library_upload_card"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = "Your personal library is empty",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Upload your own PDF books to build your offline collection, bookmark pages, and switch effortlessly between Day and Night reading modes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Button(
                    onClick = onUploadPdfClick,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("upload_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.UploadFile,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Upload a PDF Book")
                }

                OutlinedButton(
                    onClick = onBrowseClassics,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("show_curated_classics_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Browse Curated Classics ($classicsCount)")
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Uploaded PDFs are stored privately on your device so they always open offline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun LibraryBookCard(
    item: BookWithProgress,
    isOnline: Boolean,
    onRead: () -> Unit,
    onToggleOffline: () -> Unit,
    onShare: () -> Unit,
    onDeletePdf: () -> Unit
) {
    val canReadNow = isOnline || item.isOfflineReady
    val isPdf = item.book.isPdfUpload

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canReadNow) { onRead() }
            .testTag("book_card_${item.book.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BookCoverArt(
                    book = item.book,
                    modifier = Modifier.width(86.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.book.category.label} · ${item.book.yearLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.isOfflineReady) {
                            OfflineReadyChip()
                        }
                    }
                    Text(
                        text = item.book.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.book.synopsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isPdf) {
                            "${item.book.totalChapters} pages · ${item.book.pdfFileSizeKb} KB · PDF"
                        } else {
                            "${item.book.totalChapters} chapters · ~${item.book.totalMinutesEstimate} min read"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (item.progressPercent > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (item.isCompleted) {
                                "Completed"
                            } else if (isPdf) {
                                "Page ${item.currentChapter + 1} of ${item.book.totalChapters}"
                            } else {
                                "Chapter ${item.currentChapter + 1} of ${item.book.totalChapters}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${item.progressPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRead,
                    enabled = canReadNow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("read_book_${item.book.id}")
                ) {
                    Icon(
                        imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when {
                            !canReadNow -> "Offline Unavailable"
                            item.progressPercent > 0 -> "Continue"
                            else -> "Read"
                        }
                    )
                }

                if (isPdf) {
                    IconButton(
                        onClick = onDeletePdf,
                        modifier = Modifier.testTag("delete_pdf_${item.book.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove uploaded PDF",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleOffline,
                        modifier = Modifier.testTag("offline_toggle_${item.book.id}")
                    ) {
                        Icon(
                            imageVector = if (item.isOfflineReady) Icons.Default.DeleteOutline else Icons.Default.CloudDownload,
                            contentDescription = if (item.isOfflineReady) "Remove offline download" else "Save for offline reading",
                            tint = if (item.isOfflineReady) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.testTag("share_book_${item.book.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share book link with friend",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
