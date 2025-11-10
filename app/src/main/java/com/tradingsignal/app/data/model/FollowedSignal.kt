package com.tradingsignal.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Entity representing a signal that the user is actively following
 */
@Parcelize
@Entity(tableName = "followed_signals")
data class FollowedSignal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Reference to the original signal
    val signalId: Long,

    // Signal details (denormalized for quick access)
    val symbol: String,
    val marketType: String,
    val strategy: String,
    val signalType: String, // LONG or SHORT
    val entryPrice: Double,
    val takeProfit1: Double,
    val takeProfit2: Double,
    val stopLoss: Double,
    val timeframe: String,

    // Following metadata
    val startedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,

    // Exit information
    val exitedAt: Long? = null,
    val exitReason: String? = null, // "OPPOSITE_SIGNAL", "MANUAL", "TAKE_PROFIT", "STOP_LOSS"
    val exitPrice: Double? = null,

    // Opposite signal detection
    val oppositeSignalDetected: Boolean = false,
    val oppositeSignalPrice: Double? = null,
    val oppositeSignalTime: Long? = null
) : Parcelable {

    /**
     * Get elapsed time in milliseconds since signal was followed
     */
    fun getElapsedTime(): Long {
        return if (isActive) {
            System.currentTimeMillis() - startedAt
        } else {
            (exitedAt ?: System.currentTimeMillis()) - startedAt
        }
    }

    /**
     * Get formatted elapsed time (e.g., "5m 30s", "1h 15m")
     */
    fun getFormattedElapsedTime(): String {
        val elapsed = getElapsedTime()
        val seconds = (elapsed / 1000) % 60
        val minutes = (elapsed / (1000 * 60)) % 60
        val hours = (elapsed / (1000 * 60 * 60)) % 24
        val days = elapsed / (1000 * 60 * 60 * 24)

        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /**
     * Get profit/loss percentage if exited
     */
    fun getProfitLossPercentage(): Double? {
        if (!isActive && exitPrice != null) {
            return when (signalType) {
                "LONG" -> ((exitPrice - entryPrice) / entryPrice) * 100
                "SHORT" -> ((entryPrice - exitPrice) / entryPrice) * 100
                else -> 0.0
            }
        }
        return null
    }

    /**
     * Get current status description
     */
    fun getStatusDescription(): String {
        return when {
            oppositeSignalDetected && isActive -> "⚠️ STOP - ${getOppositeAction()} NOW"
            !isActive && exitReason == "OPPOSITE_SIGNAL" -> "Closed - Opposite Signal"
            !isActive && exitReason == "MANUAL" -> "Closed - Manual"
            !isActive && exitReason == "TAKE_PROFIT" -> "Closed - Take Profit Hit"
            !isActive && exitReason == "STOP_LOSS" -> "Closed - Stop Loss Hit"
            isActive -> "✓ Following - ${getFormattedElapsedTime()}"
            else -> "Inactive"
        }
    }

    /**
     * Get opposite action (SELL for LONG, BUY for SHORT)
     */
    private fun getOppositeAction(): String {
        return when (signalType) {
            "LONG" -> "SELL"
            "SHORT" -> "BUY"
            else -> "EXIT"
        }
    }

    /**
     * Get opposite signal type
     */
    fun getOppositeSignalType(): String {
        return when (signalType) {
            "LONG" -> "SHORT"
            "SHORT" -> "LONG"
            else -> ""
        }
    }
}
