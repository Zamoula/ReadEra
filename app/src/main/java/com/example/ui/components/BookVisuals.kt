package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookDefinition
import com.example.data.model.ReadingCircle
import com.example.ui.theme.NewsreaderFontFamily
import com.example.util.BookLinkHelper
import kotlin.math.roundToInt

@Composable
fun BookCoverArt(
    book: BookDefinition,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 12.dp, bottomEnd = 12.dp)
    Box(
        modifier = modifier
            .aspectRatio(0.70f)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
    ) {
        if (book.coverDrawableRes != null) {
            Image(
                painter = painterResource(id = book.coverDrawableRes),
                contentDescription = "${book.title} cover art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Subtle book spine shadow on left edge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0.0f to Color(0x55000000),
                                0.08f to Color(0x11000000),
                                0.14f to Color.Transparent
                            )
                        )
                    )
            )
        } else {
            val clothColor = Color(book.clothColorHex)
            val foilColor = Color(book.accentColorHex)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(clothColor, clothColor.copy(alpha = 0.85f))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Spine crease line
                    drawLine(
                        color = Color.Black.copy(alpha = 0.35f),
                        start = Offset(size.width * 0.09f, 0f),
                        end = Offset(size.width * 0.09f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Gold foil inner frame
                    val padX = size.width * 0.15f
                    val padY = size.height * 0.09f
                    drawRect(
                        color = foilColor.copy(alpha = 0.65f),
                        topLeft = Offset(padX, padY),
                        size = Size(size.width - padX * 1.45f, size.height - padY * 2f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    // Decorative geometric medallion
                    drawCircle(
                        color = foilColor.copy(alpha = 0.35f),
                        radius = size.width * 0.16f,
                        center = Offset(size.width * 0.54f, size.height * 0.72f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 12.dp, top = 20.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.category.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = foilColor,
                        letterSpacing = 1.4.sp,
                        maxLines = 1
                    )
                    Text(
                        text = book.title,
                        fontFamily = NewsreaderFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFFAF3E6),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = foilColor.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectivityBadge(isOnline: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (isOnline) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = if (isOnline) "Online" else "Offline",
                tint = if (isOnline) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                },
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (isOnline) "Synced" else "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        }
    }
}

@Composable
fun OfflineReadyChip(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.OfflinePin,
                contentDescription = "Available offline",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Offline",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun ShareBookLinkDialog(
    circle: ReadingCircle,
    onDismiss: () -> Unit,
    onCopied: (String) -> Unit
) {
    val context = LocalContext.current
    val webLink = remember(circle.id) { BookLinkHelper.buildCircleWebLink(circle.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Share \"${circle.bookTitle}\"",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Send this book link to a friend so they can open the same book and track chapter progress alongside you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = webLink,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Circle Code: ${circle.shareCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            BookLinkHelper.copyToClipboard(context, "Folio Book Link", webLink)
                            onCopied("Book link copied to clipboard")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_book_link_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Link")
                    }
                    Button(
                        onClick = {
                            BookLinkHelper.launchShareSheet(
                                context = context,
                                bookTitle = circle.bookTitle,
                                bookAuthor = circle.bookAuthor,
                                circleId = circle.id
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("send_book_link_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun OpenBookLinkDialog(
    onDismiss: () -> Unit,
    onSubmitLink: (String) -> Unit
) {
    var linkText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Open Friend's Book Link",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste a Folio link (e.g. https://folio.read/circle/...) or circle code sent by a friend to read together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = linkText,
                    onValueChange = { linkText = it },
                    label = { Text("Book link or circle code") },
                    placeholder = { Text("https://folio.read/circle/...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paste_book_link_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (linkText.isNotBlank()) {
                        onSubmitLink(linkText)
                        onDismiss()
                    }
                },
                enabled = linkText.isNotBlank(),
                modifier = Modifier.testTag("join_book_link_confirm")
            ) {
                Text("Open & Track")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DailyGoalDialog(
    currentGoalMinutes: Int,
    onDismiss: () -> Unit,
    onSaveGoal: (Int) -> Unit
) {
    var sliderValue by remember(currentGoalMinutes) {
        mutableFloatStateOf(currentGoalMinutes.coerceIn(5, 120).toFloat())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Personalize Daily Goal",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "${sliderValue.roundToInt()} minutes per day",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 5f..120f,
                    steps = 22
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(15, 30, 45, 60).forEach { preset ->
                        TextButton(onClick = { sliderValue = preset.toFloat() }) {
                            Text("${preset}m")
                        }
                    }
                }
            }
        },
                confirmButton = {
            Button(
                onClick = {
                    onSaveGoal(sliderValue.roundToInt())
                    onDismiss()
                },
                modifier = Modifier.testTag("save_daily_goal_button")
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmPdfUploadDialog(
    meta: com.example.util.ImportedPdfMetadata,
    onDismiss: () -> Unit,
    onConfirm: (title: String, author: String) -> Unit
) {
    var title by remember(meta.bookId) { mutableStateOf(meta.suggestedTitle) }
    var author by remember(meta.bookId) { mutableStateOf("Personal PDF") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Add PDF Book",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${meta.pageCount} pages · ${meta.fileSizeKb} KB · Stored locally for offline reading",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_title_input")
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author or Source") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_author_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.trim().ifBlank { meta.suggestedTitle },
                        author.trim().ifBlank { "Personal PDF" }
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_pdf_upload_button")
            ) {
                Text("Add & Read")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

