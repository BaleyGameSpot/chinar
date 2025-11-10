package com.tradingsignal.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tradingsignal.app.R
import com.tradingsignal.app.TradingSignalApplication
import com.tradingsignal.app.ui.main.MainActivity
import com.tradingsignal.app.utils.NotificationUtils

class FirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Send token to server
        android.util.Log.d("FCM", "New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        // Check if user has notifications enabled
        val preferenceManager = (application as TradingSignalApplication).preferenceManager
        if (!preferenceManager.notificationsEnabled) {
            return
        }

        message.notification?.let {
            showNotification(
                title = it.title ?: "Trading Signal",
                body = it.body ?: "New signal available",
                data = message.data
            )
        }

        // Handle data payload
        if (message.data.isNotEmpty()) {
            handleDataPayload(message.data)
        }
    }

    private fun handleDataPayload(data: Map<String, String>) {
        val signalType = data["signal_type"]
        val symbol = data["symbol"]
        val price = data["price"]

        if (signalType != null && symbol != null) {
            val title = "$signalType Signal: $symbol"
            val body = "Entry: $$price"
            showNotification(title, body, data)
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String> = emptyMap()) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NotificationUtils.createNotificationId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(
            this,
            TradingSignalApplication.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationUtils.createNotificationId(), notificationBuilder.build())
    }
}