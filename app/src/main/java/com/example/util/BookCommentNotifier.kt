package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object BookCommentNotifier {
    private const val CHANNEL_ID = "folio_book_comments"
    private const val CHANNEL_NAME = "Shared Book Comments"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications when friends comment or reply on a shared book link."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun notifyNewComment(
        context: Context,
        circleId: String,
        bookTitle: String,
        authorName: String,
        commentBody: String,
        isReply: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannel(context)

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(BookLinkHelper.buildCircleAppLink(circleId)),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            circleId.hashCode(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isReply) {
            "$authorName replied in $bookTitle"
        } else {
            "$authorName commented on $bookTitle"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(commentBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(commentBody))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (circleId + commentBody).hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }
}

class CommentNotificationTracker(
    context: Context,
    userId: String
) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "folio_comments_${userId.take(32)}",
        Context.MODE_PRIVATE
    )

    fun getLastSeenMillis(circleId: String): Long =
        prefs.getLong("seen_$circleId", 0L)

    fun markSeen(circleId: String, timestampMillis: Long) {
        val current = getLastSeenMillis(circleId)
        if (timestampMillis > current) {
            prefs.edit().putLong("seen_$circleId", timestampMillis).apply()
        }
    }

    fun getLastNotifiedMillis(circleId: String): Long =
        prefs.getLong("notified_$circleId", 0L)

    fun markNotified(circleId: String, timestampMillis: Long) {
        val current = getLastNotifiedMillis(circleId)
        if (timestampMillis > current) {
            prefs.edit().putLong("notified_$circleId", timestampMillis).apply()
        }
    }
}
