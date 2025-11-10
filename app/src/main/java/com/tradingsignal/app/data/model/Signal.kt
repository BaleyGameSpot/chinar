package com.tradingsignal.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "signals")
data class Signal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("market_type")
    val marketType: String, // CRYPTO or FOREX

    @SerializedName("strategy")
    val strategy: String, // SAR_SMA or SUPERTREND_MA

    @SerializedName("signal")
    val signalType: String, // LONG or SHORT

    @SerializedName("price")
    val price: Double,

    @SerializedName("tp1")
    val takeProfit1: Double,

    @SerializedName("tp2")
    val takeProfit2: Double,

    @SerializedName("stop_loss")
    val stopLoss: Double,

    @SerializedName("volume")
    val volume: Double,

    @SerializedName("quote_volume")
    val quoteVolume: Double,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("timeframe")
    val timeframe: String,

    @SerializedName("accuracy")
    val accuracy: String,

    @SerializedName("conditions")
    val conditions: Conditions,

    @SerializedName("all_conditions_met")
    val allConditionsMet: Boolean,

    // Additional fields for SAR_SMA strategy
    @SerializedName("sma_20")
    val sma20: Double? = null,

    @SerializedName("sma_40")
    val sma40: Double? = null,

    @SerializedName("sar")
    val sar: Double? = null,

    // Additional fields for SUPERTREND_MA strategy
    @SerializedName("ma_value")
    val maValue: Double? = null,

    @SerializedName("ma_type")
    val maType: String? = null,

    @SerializedName("trend")
    val trend: String? = null,

    @SerializedName("active_band")
    val activeBand: Double? = null,

    // Local fields
    val isRead: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    fun getMarketEmoji(): String {
        return when (marketType) {
            "CRYPTO" -> "💰"
            "FOREX" -> "💱"
            else -> "📊"
        }
    }

    fun getSignalEmoji(): String {
        return when (signalType) {
            "LONG" -> "📈"
            "SHORT" -> "📉"
            else -> "➡️"
        }
    }

    fun getStrategyDisplayName(): String {
        return when (strategy) {
            "SAR_SMA" -> "SAR + SMA"
            "SUPERTREND_MA" -> "SuperTrend MA"
            else -> strategy
        }
    }

    fun getProfitPercentage(): Double {
        return when (signalType) {
            "LONG" -> ((takeProfit1 - price) / price) * 100
            "SHORT" -> ((price - takeProfit1) / price) * 100
            else -> 0.0
        }
    }

    fun getRiskRewardRatio(): Double {
        val profit = when (signalType) {
            "LONG" -> takeProfit1 - price
            "SHORT" -> price - takeProfit1
            else -> 0.0
        }

        val risk = when (signalType) {
            "LONG" -> price - stopLoss
            "SHORT" -> stopLoss - price
            else -> 1.0
        }

        return if (risk != 0.0) profit / risk else 0.0
    }
}

@Parcelize
data class Conditions(
    @SerializedName("condition_1")
    val condition1: String,

    @SerializedName("condition_2")
    val condition2: String,

    @SerializedName("condition_3")
    val condition3: String
) : Parcelable