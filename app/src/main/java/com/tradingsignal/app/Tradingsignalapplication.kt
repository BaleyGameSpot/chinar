package com.tradingsignal.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.tradingsignal.app.data.local.AppDatabase
import com.tradingsignal.app.data.remote.ApiClient
import com.tradingsignal.app.data.repository.SignalRepository
import com.tradingsignal.app.utils.PreferenceManager

class TradingSignalApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "trading_signals"
        const val NOTIFICATION_CHANNEL_NAME = "Trading Signals"

        lateinit var instance: TradingSignalApplication
            private set
    }

    // Lazy initialization of components
    val database by lazy { AppDatabase.getDatabase(this) }
    val apiClient by lazy { ApiClient.create() }
    val preferenceManager by lazy { PreferenceManager(this) }
    val signalRepository by lazy {
        SignalRepository(
            apiClient,
            database.signalDao(),
            database.marketDataDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize notification channel
        createNotificationChannel()

        // Configure WorkManager
        configureWorkManager()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Receive trading signal notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun configureWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

        WorkManager.initialize(this, config)
    }
}