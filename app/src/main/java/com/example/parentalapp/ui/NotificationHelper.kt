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

fun showSosNotification(context: Context, childName: String) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notification = NotificationCompat.Builder(context, "geofence_alerts") // Korzystamy z utworzonego wcześniej kanału
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("🚨 ALARM SOS!")
        .setContentText("$childName potrzebuje natychmiastowej pomocy!")
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .build()
    manager.notify(("sos_$childName").hashCode(), notification)
}