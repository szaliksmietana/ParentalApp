package com.example.parentalapp.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.parentalapp.MainActivity

object NotificationHelper {

    const val CHANNEL_LOCATION = "LOCATION_CHANNEL_ID"
    const val CHANNEL_MESSAGES = "MESSAGES_CHANNEL_ID"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_LOCATION,
                    "Śledzenie lokalizacji",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Trwałe powiadomienie gdy lokalizacja jest aktywna"
                    setShowBadge(false)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    "Wiadomości od rodzica",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Powiadomienia o nowych wiadomościach od rodzica"
                    enableVibration(true)
                }
            )
        }
    }

    fun buildLocationForegroundNotification(context: Context): Notification {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_LOCATION)
            .setContentTitle("Parental App")
            .setContentText("Śledzenie lokalizacji jest aktywne")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    fun showMessageNotification(context: Context, content: String) {
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Nowa wiadomość od rodzica")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}