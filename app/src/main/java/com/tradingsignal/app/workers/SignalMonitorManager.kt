package com.tradingsignal.app.workers

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Manager class for scheduling signal monitoring work
 */
object SignalMonitorManager {

    private const val MONITOR_INTERVAL_MINUTES = 5L // Check every 5 minutes

    /**
     * Schedule periodic signal monitoring
     */
    fun scheduleSignalMonitoring(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val monitorRequest = PeriodicWorkRequestBuilder<SignalMonitorWorker>(
            MONITOR_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // First run after 1 minute
            .addTag(SignalMonitorWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SignalMonitorWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            monitorRequest
        )
    }

    /**
     * Cancel signal monitoring
     */
    fun cancelSignalMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SignalMonitorWorker.WORK_NAME)
    }

    /**
     * Check if signal monitoring is scheduled
     */
    fun isScheduled(context: Context): Boolean {
        val workInfos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SignalMonitorWorker.WORK_NAME)
            .get()

        return workInfos.any { !it.state.isFinished }
    }

    /**
     * Trigger an immediate signal monitoring check
     */
    fun triggerImmediateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val monitorRequest = OneTimeWorkRequestBuilder<SignalMonitorWorker>()
            .setConstraints(constraints)
            .addTag("${SignalMonitorWorker.WORK_NAME}_immediate")
            .build()

        WorkManager.getInstance(context).enqueue(monitorRequest)
    }
}
