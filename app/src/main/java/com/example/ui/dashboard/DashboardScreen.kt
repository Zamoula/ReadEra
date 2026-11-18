package com.example.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BookDefinition
import com.example.ui.BookWithProgress
import com.example.ui.CircleWithMembers
import com.example.ui.DailyActivityPoint
import com.example.ui.DashboardUiData
import com.example.ui.components.BookCoverArt
import com.example.ui.components.ConnectivityBadge
import com.example.ui.components.DailyGoalDialog
import com.example.ui.components.OfflineReadyChip
import java.util.Calendar

@Composable
fun DashboardScreen(
    data: DashboardUiData,
    displayName: String,
    currentUserId: String,
    onOpenBook: (BookDefinition, Int) -> Unit,
    onShareBook: (BookDefinition) -> Unit,
    onOpenBookLinkClick: () -> Unit,
    onToggleNightMode: () -> Unit,
    onUpdateDailyGoal: (Int) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToCircles: () -> Unit,
    onUploadPdfClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoalDialog by remember { mutableStateOf(false) }

    val dailyGoal = (data.profile?.dailyGoalMinutes?.toInt() ?: 30).coerceIn(5, 240)
    val streakDays = (data.profile?.streakDays?.toInt() ?: 1).coerceAtLeast(1)
    val totalMinutes = data.profile?.totalMinutesRead?.toInt() ?: 0
    val booksCompleted = data.profile?.booksCompleted?.toInt() ?: 0
    val offlineCount = data.books.count { it.isOfflineReady }
    val uploadedPdfCount = remember(data.books) { data.books.count { it.book.isPdfUpload } }
    val hasUploadedBooks = uploadedPdfCount > 0

    val featuredBook = remember(data.books) {
        data.books.firstOrNull { it.book.isPdfUpload && it.progressPercent in 1..99 }
            ?: data.books.firstOrNull { it.book.isPdfUpload }
            ?: data.books.firstOrNull { it.progressPercent in 1..99 }
            ?: data.books.firstOrNull { it.isOfflineReady }
            ?: data.books.first()
    }

    val firstName = remember(displayName) {
        displayName.trim().substringBefore(" ").ifBlank { "Reader" }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    if (showGoalDialog) {
        DailyGoalDialog(
            currentGoalMinutes = dailyGoal,
            onDismiss = { showGoalDialog = false },
            onSaveGoal = onUpdateDailyGoal
        )
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Header
            item {
                DashboardHeader(
                    greeting = greeting,
                    firstName = firstName,
                    isOnline = data.isOnline,
                    subtitleText = if (hasUploadedBooks) {
                        "$uploadedPdfCount uploaded · $offlineCount offline ready"
                    } else {
                        "Upload a PDF to start your shelf"
                    },
                    nightMode = data.preferences.nightMode,
                    onToggleNightMode = onToggleNightMode,
                    onSignOut = onSignOut
                )
            }

            if (!hasUploadedBooks) {
                item {
                    EmptyUploadedBooksDashboard(
                        onUploadPdfClick = onUploadPdfClick,
                        onBrowseLibrary = onNavigateToLibrary,
                        onOpenBookLinkClick = onOpenBookLinkClick
                    )
                }
            } else {
                // Personalized Reading Goal & Streak Card
                item {
                    DailyGoalCard(
                        todayMinutes = data.todayMinutes,
                        dailyGoalMinutes = dailyGoal,
                        streakDays = streakDays,
                        totalMinutes = totalMinutes,
                        booksCompleted = booksCompleted,
                        onEditGoal = { showGoalDialog = true }
                    )
                }

                // Continue Reading Hero Card
                item {
                    ContinueReadingCard(
                        item = featuredBook,
                        onResume = { onOpenBook(featuredBook.book, featuredBook.currentChapter) },
                        onShare = { onShareBook(featuredBook.book) }
                    )
                }

                // 7-Day Reading Activity Chart
                item {
                    WeeklyReadingChartCard(
                        points = data.weeklyActivity,
                        dailyGoal = dailyGoal
                    )
                }

                // Reading With Friends Section
                item {
                    FriendsDashboardSection(
                        circles = data.circles,
                        currentUserId = currentUserId,
                        onOpenBook = onOpenBook,
                        onOpenBookLinkClick = onOpenBookLinkClick,
                        onSeeAllCircles = onNavigateToCircles
                    )
                }

                // Offline Shelf Quick Strip
                item {
                    OfflineQuickStrip(
                        offlineBooks = data.books.filter { it.isOfflineReady },
                        onOpenBook = onOpenBook,
                        onSeeLibrary = onNavigateToLibrary,
                        onUploadPdfClick = onUploadPdfClick
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun DashboardHeader(
    greeting: String,
    firstName: String,
    isOnline: Boolean,
    subtitleText: String,
    nightMode: Boolean,
    onToggleNightMode: () -> Unit,
    onSignOut: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting, $firstName",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConnectivityBadge(isOnline = isOnline)
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalIconButton(
                onClick = onToggleNightMode,
                modifier = Modifier.testTag("night_mode_toggle")
            ) {
                Icon(
                    imageVector = if (nightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (nightMode) "Switch to Day Mode" else "Switch to Night Mode"
                )
            }
            IconButton(
                onClick = onSignOut,
                modifier = Modifier.testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign out"
                )
            }
        }
    }
}

@Composable
private fun EmptyUploadedBooksDashboard(
    onUploadPdfClick: () -> Unit,
    onBrowseLibrary: () -> Unit,
    onOpenBookLinkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_uploaded_books_dashboard"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_reading_sanctuary_1795027274102),
                        contentDescription = "Your personal reading sanctuary",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x22090B10),
                                        Color(0xCC090B10)
                                    )
                                )
                            )
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "PERSONAL PDF LIBRARY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Upload your first book",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Text(
                            text = "No uploaded PDFs on your shelf yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add any PDF book from your phone to unlock your reading dashboard, track your daily streak, bookmark pages, and read offline in Day or Night mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onUploadPdfClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("empty_dashboard_upload_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Upload a PDF Book",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Why read your PDFs in Folio",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                UploadBenefitRow(
                    icon = Icons.Default.OfflinePin,
                    title = "Instant Offline Access",
                    description = "Saved privately on your device so your books open even without signal."
                )
                UploadBenefitRow(
                    icon = Icons.Default.DarkMode,
                    title = "Night & Sepia Comfort",
                    description = "Read late at night with smart Night/OLED inversion and warm Sepia tint."
                )
                UploadBenefitRow(
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Page Tracking & Bookmarks",
                    description = "Pinch to zoom, jump via page scrubber, and save your place automatically."
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBrowseLibrary,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("empty_dashboard_browse_library_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Explore Library")
            }
            OutlinedButton(
                onClick = onOpenBookLinkClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("open_book_link_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Friend Link")
            }
        }
    }
}

@Composable
private fun UploadBenefitRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .padding(9.dp)
                    .size(18.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyGoalCard(
    todayMinutes: Int,
    dailyGoalMinutes: Int,
    streakDays: Int,
    totalMinutes: Int,
    booksCompleted: Int,
    onEditGoal: () -> Unit
) {
    val progress = (todayMinutes.toFloat() / dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val ringColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${todayMinutes}m",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of ${dailyGoalMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "$streakDays-day streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    IconButton(
                        onClick = onEditGoal,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("edit_goal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Customize daily reading goal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = if (todayMinutes >= dailyGoalMinutes) {
                        "Daily reading goal reached!"
                    } else {
                        "${(dailyGoalMinutes - todayMinutes).coerceAtLeast(0)} min left to hit today's goal"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatMiniItem(label = "Total Read", value = "${totalMinutes}m")
                    StatMiniItem(label = "Finished", value = "$booksCompleted books")
                }
            }
        }
    }
}

@Composable
private fun StatMiniItem(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContinueReadingCard(
    item: BookWithProgress,
    onResume: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCoverArt(
                book = item.book,
                modifier = Modifier.width(92.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (item.progressPercent > 0) "CONTINUE READING" else "PICKED FOR YOU",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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

                val chapterText = if (item.book.isPdfUpload) {
                    "Page ${item.currentChapter + 1} of ${item.book.totalChapters} · ${item.progressPercent}%"
                } else {
                    "Chapter ${item.currentChapter + 1} of ${item.book.totalChapters} · ${item.progressPercent}%"
                }
                Text(
                    text = chapterText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LinearProgressIndicator(
                    progress = { (item.progressPercent / 100f).coerceIn(0.04f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onResume,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("resume_reading_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (item.progressPercent > 0) "Resume" else "Read Now")
                    }
                    if (!item.book.isPdfUpload) {
                        FilledTonalIconButton(
                            onClick = onShare,
                            modifier = Modifier.testTag("hero_share_book_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share book link with friend"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyReadingChartCard(
    points: List<DailyActivityPoint>,
    dailyGoal: Int
) {
    val maxVal = (points.maxOfOrNull { it.minutesRead } ?: 0).coerceAtLeast(dailyGoal).coerceAtLeast(15)
    val barColor = MaterialTheme.colorScheme.secondary
    val todayColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "This Week's Rhythm",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${points.sumOf { it.minutesRead }} min total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(108.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = points.size.coerceAtLeast(1)
                    val slotWidth = size.width / count
                    val barWidth = (slotWidth * 0.44f).coerceAtMost(28.dp.toPx())
                    val chartHeight = size.height - 8.dp.toPx()

                    points.forEachIndexed { index, point ->
                        val x = slotWidth * index + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(x, 4.dp.toPx()),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                        val ratio = (point.minutesRead.toFloat() / maxVal).coerceIn(0f, 1f)
                        val activeHeight = (chartHeight * ratio).coerceAtLeast(if (point.minutesRead > 0) 8.dp.toPx() else 0f)
                        if (activeHeight > 0f) {
                            drawRoundRect(
                                color = if (index == points.lastIndex) todayColor else barColor,
                                topLeft = Offset(x, 4.dp.toPx() + (chartHeight - activeHeight)),
                                size = Size(barWidth, activeHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                points.forEachIndexed { idx, pt ->
                    Text(
                        text = pt.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (idx == points.lastIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (idx == points.lastIndex) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendsDashboardSection(
    circles: List<CircleWithMembers>,
    currentUserId: String,
    onOpenBook: (BookDefinition, Int) -> Unit,
    onOpenBookLinkClick: () -> Unit,
    onSeeAllCircles: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reading With Friends",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onOpenBookLinkClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_book_link_button")
                ) {
                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open Link", style = MaterialTheme.typography.labelMedium)
                }
                if (circles.isNotEmpty()) {
                    TextButton(onClick = onSeeAllCircles) {
                        Text("All")
                    }
                }
            }
        }

        if (circles.isEmpty()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Read the same book together",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap Share on any book to send a friend link, or tap Open Link to join theirs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            circles.take(2).forEach { cwm ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            cwm.book?.let { book ->
                                val myChapter = cwm.members.firstOrNull { it.memberUid == currentUserId }
                                    ?.currentChapter?.toInt() ?: 0
                                onOpenBook(book, myChapter)
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cwm.circle.bookTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${cwm.circle.memberUids.size} reader(s) · ${cwm.comments.size} comment(s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (cwm.unreadCommentCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "${cwm.unreadCommentCount} new",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Read",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        cwm.members.take(3).forEach { member ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (member.memberUid == currentUserId) "You" else member.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.width(74.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LinearProgressIndicator(
                                    progress = { (member.progressPercent / 100f).coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Text(
                                    text = "${member.progressPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineQuickStrip(
    offlineBooks: List<BookWithProgress>,
    onOpenBook: (BookDefinition, Int) -> Unit,
    onSeeLibrary: () -> Unit,
    onUploadPdfClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Offline Shelf",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onUploadPdfClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("dashboard_upload_pdf_button")
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Upload PDF", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onSeeLibrary) {
                    Text("Browse All")
                }
            }
        }

        if (offlineBooks.isEmpty()) {
            Text(
                text = "No books downloaded yet. Visit Library to save books or upload a PDF.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(offlineBooks, key = { it.book.id }) { item ->
                    Column(
                        modifier = Modifier
                            .width(114.dp)
                            .clickable { onOpenBook(item.book, item.currentChapter) },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BookCoverArt(
                            book = item.book,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = item.book.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${item.offlineSizeKb} KB · ${if (item.book.isPdfUpload) "PDF" else "Offline"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

