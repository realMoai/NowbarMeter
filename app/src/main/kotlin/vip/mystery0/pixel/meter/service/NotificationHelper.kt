package com.kakao.taxi.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.kakao.taxi.MainActivity
import com.kakao.taxi.R
import com.kakao.taxi.data.repository.NetworkRepository
import com.kakao.taxi.data.source.NetSpeedData
import kotlin.math.roundToInt

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "net_monitor_silent"
        const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val group = NotificationChannelGroup(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name)
            )
            notificationManager.createNotificationChannelGroup(group)

            // Use IMPORTANCE_LOW for silent notification
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows real-time network speed in status bar"
                setShowBadge(false)
                setGroup(CHANNEL_ID)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    // Icon generation
    // On Pixel, small icon is typically 24dp. We render at higher res (e.g. 48px or 96px) for clarity
    private val size =
        (context.resources.displayMetrics.density * 24).roundToInt().coerceAtLeast(48)
    private val bitmap = createBitmap(size, size)
    private val canvas = Canvas(bitmap)

    // Paints
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.60f // Value text (Updated default)
    }

    private val unitPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.45f // Unit text (Updated default)
    }

    fun buildNotification(
        speed: NetSpeedData,
        isLiveUpdate: Boolean,
        isNotificationEnabled: Boolean,
        textUp: String,
        textDown: String,
        upFirst: Boolean,
        displayMode: Int,
        textSize: Float = 0.60f,
        unitSize: Float = 0.45f,
        threshold: Long = 0L,
        lowTrafficMode: Int = 0, // 0: Static, 1: Dynamic
        useCustomColor: Boolean = false,
        color: Int = 0,
        speedUnit: String = "0",
        compactMode: Boolean = false,
        isBlank: Boolean = false
    ): Notification {
        var shouldLiveUpdate = isLiveUpdate
        val intent = Intent().apply {
            setClassName(context, MainActivity::class.java.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Common Builder setup
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setSmallIcon(R.drawable.ic_speed)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (useCustomColor) {
            builder.setColor(color)
        }

        if (isBlank) {
            val blankView = RemoteViews(context.packageName, R.layout.notification_blank)
            builder.setCustomContentView(blankView)
                .setCustomBigContentView(blankView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } else {
            builder.setContentTitle(context.getString(R.string.notification_content_title))
        }

        if (!isNotificationEnabled) {
            // Notification Disabled (Static Mode)
            if (!isBlank) {
                builder.setContentText(context.getString(R.string.notification_content_text))
            }
            return builder.build()
        }

        // Logic for Threshold Check
        // If speed is below threshold AND user selected Static Mode (0), show static icon
        if (speed.totalSpeed < threshold) {
            if (lowTrafficMode == 0) {
                if (!isBlank) {
                    builder.setContentText(context.getString(R.string.notification_content_text_monitoring))
                }
                return builder.build()
            } else {
                shouldLiveUpdate = false
            }
        }

        // Notification Enabled (Dynamic Mode)
        if (shouldLiveUpdate) {
            // Live Update Mode
            val statusText =
                NetworkRepository.formatSpeedTextForLiveUpdate(speed.totalSpeed, speedUnit, compactMode)
            val upText = "$textUp${NetworkRepository.formatSpeedLine(speed.uploadSpeed, speedUnit, compactMode)}"
            val downText =
                "$textDown${NetworkRepository.formatSpeedLine(speed.downloadSpeed, speedUnit, compactMode)}"

            val contentText = when (displayMode) {
                1 -> upText // Up Only
                2 -> downText // Down Only
                else -> if (upFirst) "$upText  $downText" else "$downText  $upText"
            }

            if (!isBlank) {
                builder.setContentText(contentText)
            }
            builder.setShortCriticalText(statusText)
                .setRequestPromotedOngoing(true)
        } else {
            // Standard Mode
            val (valueStr, unitStr) = NetworkRepository.formatSpeedText(speed.totalSpeed, speedUnit, compactMode)

            // Draw Bitmap with speed
            bitmap.eraseColor(Color.TRANSPARENT)
            val cx = size / 2f
            val cyValue =
                size * 0.5f // Ideally this offset might need adjustment based on size, but keeping simple for now
            val cyUnit = size * 0.95f

            textPaint.textSize = size * textSize
            unitPaint.textSize = size * unitSize

            canvas.drawText(valueStr, cx, cyValue, textPaint)
            canvas.drawText(unitStr, cx, cyUnit, unitPaint)

            val smallIcon = IconCompat.createWithBitmap(bitmap)

            val upText = "$textUp${NetworkRepository.formatSpeedLine(speed.uploadSpeed, speedUnit, compactMode)}"
            val downText =
                "$textDown${NetworkRepository.formatSpeedLine(speed.downloadSpeed, speedUnit, compactMode)}"

            val contentText = when (displayMode) {
                1 -> upText // Up Only
                2 -> downText // Down Only
                else -> if (upFirst) "$upText  $downText" else "$downText  $upText"
            }

            if (!isBlank) {
                builder.setContentText(contentText)
            }
            builder.setSmallIcon(smallIcon)
        }

        return builder.build()
    }
}
