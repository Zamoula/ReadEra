package com.example.ui.reader

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineBookRepository
import com.example.data.local.ReaderPreferences
import com.example.data.model.BookChapter
import com.example.data.model.BookDefinition
import com.example.data.model.ReaderThemeMode
import com.example.ui.BookWithProgress
import com.example.ui.theme.LiterataFontFamily
import com.example.ui.theme.NewsreaderFontFamily
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.ReaderNightBg
import com.example.ui.theme.ReaderNightInk
import com.example.ui.theme.ReaderNightMuted
import com.example.ui.theme.ReaderOledBg
import com.example.ui.theme.ReaderOledInk
import com.example.ui.theme.ReaderOledMuted
import com.example.ui.theme.ReaderPaperBg
import com.example.ui.theme.ReaderPaperInk
import com.example.ui.theme.ReaderPaperMuted
import com.example.ui.theme.ReaderSepiaBg
import com.example.ui.theme.ReaderSepiaInk
import com.example.ui.theme.ReaderSepiaMuted
import com.example.util.PdfBookImporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookItem: BookWithProgress,
    initialChapterIndex: Int,
    preferences: ReaderPreferences,
    isOnline: Boolean,
    offlineRepository: OfflineBookRepository,
    onSaveChapterProgress: (chapterIndex: Int, minutesDelta: Int, markCompleted: Boolean) -> Unit,
    onSaveBookmark: (chapterIndex: Int, note: String) -> Unit,
    onShareBook: (BookDefinition) -> Unit,
    onToggleOffline: (BookDefinition, Boolean) -> Unit,
    onSelectReaderTheme: (ReaderThemeMode) -> Unit,
    onToggleAppNightMode: () -> Unit,
    onAdjustFontSize: (Int) -> Unit,
    onSetLineSpacing: (Int) -> Unit,
    onSetUseSerif: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val book = bookItem.book
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val maxUnits = (book.totalChapters - 1).coerceAtLeast(0)

    var currentChapterIndex by remember(book.id, initialChapterIndex) {
        mutableIntStateOf(initialChapterIndex.coerceIn(0, maxUnits))
    }
    var showChrome by remember { mutableStateOf(true) }
    var showAppearanceSheet by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }
    var offlineChapters by remember(book.id) { mutableStateOf<List<BookChapter>?>(null) }

    // Load chapters from Room if available locally (for non-PDF catalog books)
    LaunchedEffect(book.id, bookItem.isOfflineReady) {
        if (!book.isPdfUpload) {
            val cached = offlineRepository.getCachedChapters(book.id)
            offlineChapters = if (cached.isNotEmpty()) {
                cached.map { entity ->
                    BookChapter(
                        index = entity.chapterIndex,
                        title = entity.title,
                        estimatedMinutes = entity.estimatedMinutes,
                        paragraphs = entity.bodyJoined.split("\n\n")
                    )
                }
            } else {
                null
            }
        }
    }

    // Active reading timer: logs 1 minute every 60 seconds while reading
    LaunchedEffect(book.id, currentChapterIndex) {
        while (true) {
            delay(60_000L)
            onSaveChapterProgress(currentChapterIndex, 1, false)
        }
    }

    // Scroll to top and sync page/chapter index when changing pages/chapters
    LaunchedEffect(currentChapterIndex) {
        scrollState.scrollTo(0)
        onSaveChapterProgress(currentChapterIndex, 0, false)
    }

    val effectiveChapters = offlineChapters ?: if (isOnline) book.chapters else emptyList()
    val chapter = effectiveChapters.getOrNull(currentChapterIndex)

    val (bgColor, inkColor, mutedColor) = when (preferences.readerTheme) {
        ReaderThemeMode.PAPER -> Triple(ReaderPaperBg, ReaderPaperInk, ReaderPaperMuted)
        ReaderThemeMode.SEPIA -> Triple(ReaderSepiaBg, ReaderSepiaInk, ReaderSepiaMuted)
        ReaderThemeMode.NIGHT -> Triple(ReaderNightBg, ReaderNightInk, ReaderNightMuted)
        ReaderThemeMode.OLED -> Triple(ReaderOledBg, ReaderOledInk, ReaderOledMuted)
    }

    val bodyFamily = if (preferences.useSerifFont) LiterataFontFamily else PlusJakartaFontFamily
    val lineMultiplier = when (preferences.lineSpacingLevel) {
        0 -> 1.42f
        2 -> 1.82f
        else -> 1.62f
    }
    val bodySizeSp = preferences.fontSizeSp.sp
    val bodyLineHeightSp = (preferences.fontSizeSp * lineMultiplier).sp

    val isBookmarkedHere = bookItem.bookmarkChapter == currentChapterIndex && bookItem.libraryState != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showChrome = !showChrome
            }
    ) {
        if (book.isPdfUpload && book.localPdfPath != null) {
            PdfPageReaderContent(
                filePath = book.localPdfPath,
                pageIndex = currentChapterIndex,
                totalPages = book.totalChapters,
                readerTheme = preferences.readerTheme,
                inkColor = inkColor,
                mutedColor = mutedColor,
                onTapCenter = { showChrome = !showChrome },
                onPreviousPage = {
                    if (currentChapterIndex > 0) currentChapterIndex -= 1
                },
                onNextPage = {
                    if (currentChapterIndex < book.totalChapters - 1) {
                        val next = currentChapterIndex + 1
                        onSaveChapterProgress(next, 1, false)
                        currentChapterIndex = next
                    }
                },
                onFinishPdf = {
                    onSaveChapterProgress(currentChapterIndex, 1, true)
                    onBack()
                }
            )
        } else if (chapter == null) {
            // Offline and not downloaded
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = mutedColor,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = "Not Saved for Offline Reading",
                    style = MaterialTheme.typography.headlineSmall,
                    color = inkColor
                )
                Text(
                    text = "Reconnect to the internet to read \"${book.title}\" or download it from your Library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor
                )
                OutlinedButton(onClick = onBack) {
                    Text("Back to Shelf")
                }
            }
        } else {
            // Scrollable Editorial Prose Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = 72.dp, bottom = 92.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 620.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "CHAPTER ${currentChapterIndex + 1} OF ${effectiveChapters.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor,
                            letterSpacing = 1.2.sp
                        )
                        if (bookItem.isOfflineReady) {
                            Icon(
                                imageVector = Icons.Default.OfflinePin,
                                contentDescription = "Reading offline copy",
                                tint = mutedColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = chapter.title,
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        color = inkColor
                    )

                    HorizontalDivider(
                        color = mutedColor.copy(alpha = 0.25f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    chapter.paragraphs.forEachIndexed { idx, paragraph ->
                        Text(
                            text = paragraph,
                            fontFamily = bodyFamily,
                            fontSize = bodySizeSp,
                            lineHeight = bodyLineHeightSp,
                            color = inkColor,
                            modifier = Modifier.padding(top = if (idx == 0) 2.dp else 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = mutedColor.copy(alpha = 0.25f))

                    // End-of-Chapter Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentChapterIndex > 0) {
                            OutlinedButton(
                                onClick = { currentChapterIndex -= 1 },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_prev_chapter")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Previous")
                            }
                        }

                        if (currentChapterIndex < effectiveChapters.lastIndex) {
                            Button(
                                onClick = {
                                    val next = currentChapterIndex + 1
                                    onSaveChapterProgress(next, chapter.estimatedMinutes.coerceAtMost(3), false)
                                    currentChapterIndex = next
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_next_chapter")
                            ) {
                                Text("Next Chapter")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    onSaveChapterProgress(currentChapterIndex, chapter.estimatedMinutes.coerceAtMost(3), true)
                                    onBack()
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reader_finish_book")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Finish Book")
                            }
                        }
                    }
                }
            }
        }

        // Top Distraction-Free Chrome Bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = bgColor.copy(alpha = 0.94f),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = inkColor
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = inkColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (book.isPdfUpload) {
                                    "Page ${currentChapterIndex + 1} of ${book.totalChapters}"
                                } else {
                                    chapter?.title ?: book.author
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val label = if (book.isPdfUpload) {
                                    "Page ${currentChapterIndex + 1}"
                                } else {
                                    chapter?.title ?: "Chapter ${currentChapterIndex + 1}"
                                }
                                onSaveBookmark(currentChapterIndex, "Saved at $label")
                            },
                            modifier = Modifier.testTag("reader_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isBookmarkedHere) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarkedHere) MaterialTheme.colorScheme.primary else inkColor
                            )
                        }
                        if (!book.isPdfUpload) {
                            IconButton(
                                onClick = { onShareBook(book) },
                                modifier = Modifier.testTag("reader_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Send book link to a friend",
                                    tint = inkColor
                                )
                            }
                        }
                        IconButton(
                            onClick = { showAppearanceSheet = true },
                            modifier = Modifier.testTag("reader_appearance_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = "Night mode & appearance settings",
                                tint = inkColor
                            )
                        }
                        IconButton(
                            onClick = { showChaptersSheet = true },
                            modifier = Modifier.testTag("reader_chapters_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = if (book.isPdfUpload) "Jump to page" else "Chapters",
                                tint = inkColor
                            )
                        }
                    }
                }
            }
        }

        // Subtle Bottom Progress Strip
        val totalUnits = if (book.isPdfUpload) book.totalChapters else effectiveChapters.size
        AnimatedVisibility(
            visible = showChrome && totalUnits > 0,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = bgColor.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val progress = ((currentChapterIndex + 1).toFloat() / totalUnits.coerceAtLeast(1)).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (book.isPdfUpload) {
                                "Page ${currentChapterIndex + 1} of $totalUnits"
                            } else {
                                "Chapter ${currentChapterIndex + 1} of $totalUnits"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                        Text(
                            text = if (book.isPdfUpload) {
                                "${(progress * 100).toInt()}% · Local PDF"
                            } else {
                                "${(progress * 100).toInt()}% · ${chapter?.estimatedMinutes ?: 4} min"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedColor
                        )
                    }
                }
            }
        }

        // Appearance & Night Mode Bottom Sheet
        if (showAppearanceSheet) {
            ModalBottomSheet(onDismissRequest = { showAppearanceSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Reading Appearance & Night Mode",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReaderThemeOption(
                            label = "Paper",
                            bg = ReaderPaperBg,
                            ink = ReaderPaperInk,
                            selected = preferences.readerTheme == ReaderThemeMode.PAPER,
                            onClick = { onSelectReaderTheme(ReaderThemeMode.PAPER) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_paper")
                        )
                        ReaderThemeOption(
                            label = "Sepia",
                            bg = ReaderSepiaBg,
                            ink = ReaderSepiaInk,
                            selected = preferences.readerTheme == ReaderThemeMode.SEPIA,
                            onClick = { onSelectReaderTheme(ReaderThemeMode.SEPIA) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_sepia")
                        )
                        ReaderThemeOption(
                            label = "Night",
                            bg = ReaderNightBg,
                            ink = ReaderNightInk,
                            selected = preferences.readerTheme == ReaderThemeMode.NIGHT,
                            onClick = { onSelectReaderTheme(ReaderThemeMode.NIGHT) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_night")
                        )
                        ReaderThemeOption(
                            label = "OLED",
                            bg = ReaderOledBg,
                            ink = ReaderOledInk,
                            selected = preferences.readerTheme == ReaderThemeMode.OLED,
                            onClick = { onSelectReaderTheme(ReaderThemeMode.OLED) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_oled")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null)
                            Column {
                                Text("App Night Mode", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (book.isPdfUpload) "Also inverts PDF pages for night reading" else "Darken library and dashboard too",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = preferences.nightMode,
                            onCheckedChange = { onToggleAppNightMode() }
                        )
                    }

                    if (!book.isPdfUpload) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size", style = MaterialTheme.typography.titleSmall)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onAdjustFontSize(-1) },
                                    enabled = preferences.fontSizeSp > 14,
                                    modifier = Modifier.testTag("font_size_decrease")
                                ) {
                                    Text("A-")
                                }
                                Text(
                                    text = "${preferences.fontSizeSp}sp",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                OutlinedButton(
                                    onClick = { onAdjustFontSize(1) },
                                    enabled = preferences.fontSizeSp < 28,
                                    modifier = Modifier.testTag("font_size_increase")
                                ) {
                                    Text("A+")
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { onSetUseSerif(true) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (preferences.useSerifFont) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Literata Serif", fontFamily = LiterataFontFamily)
                            }
                            FilledTonalButton(
                                onClick = { onSetUseSerif(false) },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (!preferences.useSerifFont) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Jakarta Sans", fontFamily = PlusJakartaFontFamily)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0 to "Compact", 1 to "Comfort", 2 to "Airy").forEach { (level, label) ->
                                OutlinedButton(
                                    onClick = { onSetLineSpacing(level) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (preferences.lineSpacingLevel == level) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // Chapters / PDF Page Scrubber Bottom Sheet
        if (showChaptersSheet) {
            ModalBottomSheet(onDismissRequest = { showChaptersSheet = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (book.isPdfUpload) {
                        Text(
                            text = "Jump to Page",
                            style = MaterialTheme.typography.titleLarge
                        )
                        var sliderPage by remember(currentChapterIndex) {
                            mutableFloatStateOf(currentChapterIndex.toFloat())
                        }
                        Text(
                            text = "Page ${sliderPage.roundToInt() + 1} of ${book.totalChapters}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (book.totalChapters > 1) {
                            Slider(
                                value = sliderPage,
                                onValueChange = { sliderPage = it },
                                valueRange = 0f..(book.totalChapters - 1).toFloat()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (bookItem.libraryState != null) {
                                OutlinedButton(
                                    onClick = {
                                        currentChapterIndex = bookItem.bookmarkChapter.coerceIn(0, maxUnits)
                                        showChaptersSheet = false
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Go to Bookmark (p.${bookItem.bookmarkChapter + 1})")
                                }
                            }
                            Button(
                                onClick = {
                                    currentChapterIndex = sliderPage.roundToInt().coerceIn(0, maxUnits)
                                    showChaptersSheet = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Go to Page")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Table of Contents",
                                style = MaterialTheme.typography.titleLarge
                            )
                            TextButton(
                                onClick = { onToggleOffline(book, bookItem.isOfflineReady) }
                            ) {
                                Text(if (bookItem.isOfflineReady) "Offline Saved" else "Save Offline")
                            }
                        }

                        effectiveChapters.forEach { ch ->
                            val isCurrent = ch.index == currentChapterIndex
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCurrent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        scope.launch {
                                            currentChapterIndex = ch.index
                                            showChaptersSheet = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isCurrent) {
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = "~${ch.estimatedMinutes} min read",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (bookItem.bookmarkChapter == ch.index && bookItem.libraryState != null) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Bookmarked",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun PdfPageReaderContent(
    filePath: String,
    pageIndex: Int,
    totalPages: Int,
    readerTheme: ReaderThemeMode,
    inkColor: Color,
    mutedColor: Color,
    onTapCenter: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onFinishPdf: () -> Unit
) {
    var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
    var offsetX by remember(pageIndex) { mutableFloatStateOf(0f) }
    var offsetY by remember(pageIndex) { mutableFloatStateOf(0f) }

    val colorFilter = remember(readerTheme) {
        when (readerTheme) {
            ReaderThemeMode.NIGHT, ReaderThemeMode.OLED -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        -0.90f, 0f, 0f, 0f, 240f,
                        0f, -0.90f, 0f, 0f, 242f,
                        0f, 0f, -0.88f, 0f, 248f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            ReaderThemeMode.SEPIA -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.96f, 0f, 0f, 0f, 0f,
                        0f, 0.91f, 0f, 0f, 0f,
                        0f, 0f, 0.80f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            ReaderThemeMode.PAPER -> null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 64.dp, bottom = 76.dp, start = 12.dp, end = 12.dp)
    ) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.toPx().toInt().coerceAtLeast(720) }

        val bitmapState by produceState<Bitmap?>(initialValue = null, filePath, pageIndex, targetWidthPx) {
            value = PdfBookImporter.renderPageBitmap(filePath, pageIndex, targetWidthPx)
        }

        val bitmap = bitmapState
        if (bitmap == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = inkColor)
                    Text(
                        text = "Rendering PDF page ${pageIndex + 1}…",
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(pageIndex) {
                            detectTapGestures(
                                onTap = { onTapCenter() },
                                onDoubleTap = {
                                    if (scale > 1.05f) {
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    } else {
                                        scale = 1.85f
                                    }
                                }
                            )
                        }
                        .pointerInput(pageIndex) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val nextScale = (scale * zoom).coerceIn(1f, 3.2f)
                                scale = nextScale
                                if (nextScale > 1.02f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF page ${pageIndex + 1} of $totalPages",
                        contentScale = ContentScale.Fit,
                        colorFilter = colorFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 560.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onPreviousPage,
                        enabled = pageIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pdf_prev_page")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Prev Page")
                    }

                    if (pageIndex < totalPages - 1) {
                        Button(
                            onClick = onNextPage,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_next_page")
                        ) {
                            Text("Next Page")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = onFinishPdf,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_finish_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Finish PDF")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderThemeOption(
    label: String,
    bg: Color,
    ink: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .background(bg)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aa",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ink
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = ink
        )
    }
}
