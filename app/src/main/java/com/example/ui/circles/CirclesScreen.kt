package com.example.ui.circles

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
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BookCatalog
import com.example.data.model.BookDefinition
import com.example.data.model.CircleComment
import com.example.data.model.ReadingCircle
import com.example.ui.CircleWithMembers
import com.example.ui.DashboardUiData
import com.example.ui.components.BookCommentsBottomSheet
import com.example.ui.components.BookCoverArt
import com.example.util.BookLinkHelper

@Composable
fun CirclesScreen(
    data: DashboardUiData,
    currentUserId: String,
    onOpenBook: (BookDefinition, Int) -> Unit,
    onShareBook: (BookDefinition) -> Unit,
    onJoinBookLink: (String) -> Unit,
    onUpdateNote: (ReadingCircle, String) -> Unit,
    onPostComment: (BookDefinition, Int, String, CircleComment?, String?) -> Unit,
    onDeleteComment: (circleId: String, commentId: String) -> Unit,
    onMarkCommentsRead: (circleId: String) -> Unit,
    onLeaveCircle: (String) -> Unit,
    onCopiedMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var linkInput by remember { mutableStateOf("") }
    var editingCircle by remember { mutableStateOf<ReadingCircle?>(null) }
    var activeCommentsCircleId by remember { mutableStateOf<String?>(null) }

    val activeCommentsItem = remember(data.circles, activeCommentsCircleId) {
        activeCommentsCircleId?.let { cid ->
            data.circles.firstOrNull { it.circle.id == cid }
        }
    }

    if (activeCommentsItem != null) {
        val myCh = activeCommentsItem.members.firstOrNull { it.memberUid == currentUserId }
            ?.currentChapter?.toInt() ?: 0
        BookCommentsBottomSheet(
            bookTitle = activeCommentsItem.circle.bookTitle,
            circleWithMembers = activeCommentsItem,
            currentUserId = currentUserId,
            currentChapterIndex = myCh,
            onDismiss = { activeCommentsCircleId = null },
            onPostComment = { body, parent ->
                activeCommentsItem.book?.let { book ->
                    onPostComment(book, myCh, body, parent, activeCommentsItem.circle.id)
                }
            },
            onDeleteComment = onDeleteComment,
            onShareBookLink = {
                activeCommentsItem.book?.let { onShareBook(it) }
            }
        )
    }

    if (editingCircle != null) {
        UpdateNoteDialog(
            circle = editingCircle!!,
            onDismiss = { editingCircle = null },
            onSave = { note ->
                onUpdateNote(editingCircle!!, note)
                editingCircle = null
            }
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Read With Friends",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Send a book link or paste a friend's code to read together and track chapter progress in real time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Join via Book Link Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddLink,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Open a Friend's Book Link",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedTextField(
                            value = linkInput,
                            onValueChange = { linkInput = it },
                            label = { Text("Paste link or circle code") },
                            placeholder = { Text("https://folio.read/circle/...") },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("circles_link_input")
                        )
                        Button(
                            onClick = {
                                if (linkInput.isNotBlank()) {
                                    onJoinBookLink(linkInput)
                                    linkInput = ""
                                }
                            },
                            enabled = linkInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("circles_join_button")
                        ) {
                            Text("Open Book & Track Progress")
                        }
                    }
                }
            }

            // Quick Create Link Picker
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Send a Book Link",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(BookCatalog.books, key = { it.id }) { book ->
                            FilledTonalButton(
                                onClick = { onShareBook(book) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("quick_share_${book.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(book.title, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Active Circles
            item {
                Text(
                    text = "Active Reading Circles (${data.circles.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (data.circles.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                text = "No shared reading circles yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap any book above to generate a shareable link for a friend, or paste a link you received.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(data.circles, key = { it.circle.id }) { cwm ->
                    CircleCard(
                        item = cwm,
                        currentUserId = currentUserId,
                        onOpenBook = {
                            cwm.book?.let { book ->
                                val myCh = cwm.members.firstOrNull { it.memberUid == currentUserId }
                                    ?.currentChapter?.toInt() ?: 0
                                onOpenBook(book, myCh)
                            }
                        },
                        onCopyLink = {
                            val link = BookLinkHelper.buildCircleWebLink(cwm.circle.id)
                            BookLinkHelper.copyToClipboard(context, "Folio Book Link", link)
                            onCopiedMessage("Copied link for \"${cwm.circle.bookTitle}\"")
                        },
                        onShareSystem = {
                            BookLinkHelper.launchShareSheet(
                                context = context,
                                bookTitle = cwm.circle.bookTitle,
                                bookAuthor = cwm.circle.bookAuthor,
                                circleId = cwm.circle.id
                            )
                        },
                        onEditNote = { editingCircle = cwm.circle },
                        onOpenComments = {
                            onMarkCommentsRead(cwm.circle.id)
                            activeCommentsCircleId = cwm.circle.id
                        },
                        onLeave = { onLeaveCircle(cwm.circle.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun CircleCard(
    item: CircleWithMembers,
    currentUserId: String,
    onOpenBook: () -> Unit,
    onCopyLink: () -> Unit,
    onShareSystem: () -> Unit,
    onEditNote: () -> Unit,
    onOpenComments: () -> Unit,
    onLeave: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item.book?.let {
                    BookCoverArt(book = it, modifier = Modifier.width(64.dp))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = item.circle.bookTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = item.circle.bookAuthor,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Link code: ${item.circle.shareCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onLeave) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = if (item.circle.creatorId == currentUserId) "Delete circle" else "Leave circle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Friend Progress List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item.members.forEach { member ->
                    val isMe = member.memberUid == currentUserId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isMe) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = member.displayName.take(1).uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isMe) "${member.displayName} (You)" else member.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "Ch ${member.currentChapter + 1}/${member.totalChapters} · ${member.progressPercent}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (member.progressPercent / 100f).coerceIn(0.04f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                            )
                            if (member.statusNote.isNotBlank()) {
                                Text(
                                    text = "\"${member.statusNote}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            val latestComment = item.comments.lastOrNull()
            if (latestComment != null || item.unreadCommentCount > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (item.unreadCommentCount > 0) {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            if (latestComment != null) {
                                Text(
                                    text = "${latestComment.authorName}: ${latestComment.body}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (item.unreadCommentCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "${item.unreadCommentCount} new",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenBook,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Read")
                }
                OutlinedButton(
                    onClick = onOpenComments,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("circle_comments_${item.circle.id}")
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${item.comments.size}")
                }
                IconButton(onClick = onEditNote) {
                    Icon(Icons.Default.EditNote, contentDescription = "Update reading status note")
                }
                IconButton(onClick = onCopyLink) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy book link")
                }
                IconButton(onClick = onShareSystem) {
                    Icon(Icons.Default.Share, contentDescription = "Share book link")
                }
            }
        }
    }
}

@Composable
private fun UpdateNoteDialog(
    circle: ReadingCircle,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Share Reading Thought",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Leave a quick note for friends reading \"${circle.bookTitle}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 140) noteText = it },
                    label = { Text("Your note (max 140 chars)") },
                    placeholder = { Text("Loved the opening reflection!") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(noteText.trim()) },
                enabled = noteText.isNotBlank()
            ) {
                Text("Post Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
