package com.example.parentalapp.ui

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

fun showMessageNotification(context: Context, senderName: String, content: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "geofence_alerts")
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setContentTitle("Nowa wiadomość od $senderName")
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    manager.notify(System.currentTimeMillis().toInt(), notification)
}