package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CircleComment
import com.example.ui.CircleWithMembers
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCommentsBottomSheet(
    bookTitle: String,
    circleWithMembers: CircleWithMembers?,
    currentUserId: String,
    currentChapterIndex: Int,
    pendingQuote: String? = null,
    focusedCommentId: String? = null,
    onClearPendingQuote: () -> Unit = {},
    onDismiss: () -> Unit,
    onPostComment: (body: String, quoteText: String, parentComment: CircleComment?) -> Unit,
    onDeleteComment: (circleId: String, commentId: String) -> Unit,
    onShareBookLink: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    var commentDraft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<CircleComment?>(null) }

    val comments = circleWithMembers?.comments.orEmpty()
    val circleCreatorId = circleWithMembers?.circle?.creatorId.orEmpty()

    val topLevelComments = remember(comments) {
        val topIds = comments.filter { it.parentCommentId.isBlank() }.map { it.id }.toSet()
        comments.filter { it.parentCommentId.isBlank() || it.parentCommentId !in topIds }
    }
    val repliesByParent = remember(comments) {
        comments.filter { it.parentCommentId.isNotBlank() }
            .groupBy { it.parentCommentId }
    }

    LaunchedEffect(focusedCommentId, topLevelComments) {
        if (!focusedCommentId.isNullOrBlank()) {
            val idx = topLevelComments.indexOfFirst { it.id == focusedCommentId }
            if (idx >= 0) {
                listState.animateScrollToItem(idx)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Book Comments",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (circleWithMembers != null) {
                            "$bookTitle · ${circleWithMembers.circle.memberUids.size} reader(s) with link"
                        } else {
                            "$bookTitle · Post a thought & share link with friends"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onShareBookLink != null) {
                    FilledTonalIconButton(
                        onClick = onShareBookLink,
                        modifier = Modifier.testTag("comments_sheet_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share book link"
                        )
                    }
                }
            }

            HorizontalDivider()

            if (topLevelComments.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "No comments yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Comment on this chapter below. Anyone with the same book link will be notified and can reply.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(topLevelComments, key = { it.id }) { comment ->
                        val replies = repliesByParent[comment.id].orEmpty()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CommentItemCard(
                                comment = comment,
                                isMe = comment.authorUid == currentUserId,
                                canDelete = comment.authorUid == currentUserId || circleCreatorId == currentUserId,
                                isReply = false,
                                isFocused = comment.id == focusedCommentId,
                                onReply = { replyTarget = comment },
                                onDelete = {
                                    circleWithMembers?.circle?.id?.let { cid ->
                                        onDeleteComment(cid, comment.id)
                                    }
                                }
                            )

                            replies.forEach { reply ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                    )
                                    CommentItemCard(
                                        comment = reply,
                                        isMe = reply.authorUid == currentUserId,
                                        canDelete = reply.authorUid == currentUserId || circleCreatorId == currentUserId,
                                        isReply = true,
                                        isFocused = false,
                                        onReply = {
                                            // Keep replies grouped under the parent thread while addressing the reply author
                                            replyTarget = comment.copy(authorName = reply.authorName)
                                        },
                                        onDelete = {
                                            circleWithMembers?.circle?.id?.let { cid ->
                                                onDeleteComment(cid, reply.id)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val activeQuote = if (replyTarget == null) pendingQuote?.trim().orEmpty() else ""
            if (activeQuote.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFE042).copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, Color(0xFFFFD400)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_quote_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Selected passage (will highlight in book)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "\"$activeQuote\"",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onClearPendingQuote,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear selected passage",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (replyTarget != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replying to ${replyTarget!!.authorName}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { replyTarget = null },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { if (it.length <= 500) commentDraft = it },
                    placeholder = {
                        Text(
                            when {
                                replyTarget != null -> "Reply to ${replyTarget!!.authorName}…"
                                activeQuote.isNotEmpty() -> "Add your comment on this sentence…"
                                else -> "Comment on Chapter ${currentChapterIndex + 1}…"
                            }
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                FilledTonalIconButton(
                    onClick = {
                        val text = commentDraft.trim()
                        if (text.isNotEmpty()) {
                            onPostComment(text, activeQuote, replyTarget)
                            commentDraft = ""
                            replyTarget = null
                            if (activeQuote.isNotEmpty()) {
                                onClearPendingQuote()
                            }
                        }
                    },
                    enabled = commentDraft.isNotBlank(),
                    modifier = Modifier
                        .size(50.dp)
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment"
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CommentItemCard(
    comment: CircleComment,
    isMe: Boolean,
    canDelete: Boolean,
    isReply: Boolean,
    isFocused: Boolean,
    onReply: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeLabel = remember(comment.createdAt) {
        val date = comment.createdAt?.toDate()
        if (date != null) {
            SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
        } else {
            "Just now"
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isMe) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = if (isFocused) BorderStroke(1.5.dp, Color(0xFFFFD400)) else null,
        modifier = modifier.fillMaxWidth()
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = comment.authorName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
                            )
                        }
                    }
                    Text(
                        text = if (isMe) "${comment.authorName} (You)" else comment.authorName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Ch.${comment.chapterIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isReply && comment.parentAuthorName.isNotBlank()) {
                Text(
                    text = "Replying to ${comment.parentAuthorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (comment.quoteText.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFE042).copy(alpha = 0.24f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(22.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFFFD400))
                        )
                        Text(
                            text = "\"${comment.quoteText}\"",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onReply,
                    modifier = Modifier.testTag("reply_comment_${comment.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Reply", style = MaterialTheme.typography.labelMedium)
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_comment_${comment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete comment",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
