package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat

class NotificationHelper(private val context: Context) {

    private val channelId = "network_refresh_channel"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Network Refresh Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications posted whenever network telemetry refreshes"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showRefreshNotification(title: String, message: String) {
        try {
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(1001, notification)
        } catch (_: Exception) {}

        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}
    }
}
