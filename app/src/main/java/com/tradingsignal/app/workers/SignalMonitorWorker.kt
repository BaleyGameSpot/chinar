package com.tradingsignal.app.workers

import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tradingsignal.app.R
import com.tradingsignal.app.TradingSignalApplication
import com.tradingsignal.app.data.model.ScanRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Background worker that monitors followed signals for opposite signals
 * Runs periodically to check if any opposite signals have been detected
 */
class SignalMonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = (applicationContext as TradingSignalApplication).signalRepository
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get all active followed signals
            val followedSignals = repository.getActiveFollowedSignals().value ?: emptyList()

            if (followedSignals.isEmpty()) {
                return@withContext Result.success()
            }

            // Perform a scan to get latest signals
            val scanRequest = ScanRequest(
                marketTypes = listOf("CRYPTO", "FOREX"),
                strategies = listOf("SAR_SMA", "SUPERTREND_MA"),
                timeframe = "5m",
                cryptoLimit = 50
            )

            val scanResult = repository.performScan(scanRequest)

            scanResult.onSuccess { scanResponse ->
                // Check each followed signal for opposite signals
                followedSignals.forEach { followedSignal ->
                    // Find opposite signals for this symbol and strategy
                    val oppositeSignals = scanResponse.signals.filter { signal ->
                        signal.symbol == followedSignal.symbol &&
                        signal.strategy == followedSignal.strategy &&
                        signal.signalType == followedSignal.getOppositeSignalType() &&
                        signal.allConditionsMet
                    }

                    if (oppositeSignals.isNotEmpty()) {
                        val latestOpposite = oppositeSignals.first()

                        // Mark opposite signal detected if not already marked
                        if (!followedSignal.oppositeSignalDetected) {
                            repository.markOppositeSignalDetected(
                                followedSignal = followedSignal,
                                price = latestOpposite.price
                            )

                            // Send notification to user
                            sendOppositeSignalNotification(
                                followedSignal = followedSignal,
                                oppositeSignal = latestOpposite
                            )
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * Send notification when opposite signal is detected
     */
    private fun sendOppositeSignalNotification(
        followedSignal: com.tradingsignal.app.data.model.FollowedSignal,
        oppositeSignal: com.tradingsignal.app.data.model.Signal
    ) {
        val action = when (followedSignal.signalType) {
            "LONG" -> "SELL"
            "SHORT" -> "BUY"
            else -> "EXIT"
        }

        val title = "⚠️ OPPOSITE SIGNAL: ${followedSignal.symbol}"
        val message = "$action NOW! ${oppositeSignal.signalType} signal detected at ${oppositeSignal.price}"

        val notification = NotificationCompat.Builder(applicationContext, TradingSignalApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(followedSignal.id.toInt(), notification)
    }

    companion object {
        const val WORK_NAME = "signal_monitor_work"
    }
}
