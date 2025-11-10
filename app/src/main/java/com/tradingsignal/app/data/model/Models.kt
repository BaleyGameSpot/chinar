package com.tradingsignal.app.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// Scan Request Model
@Parcelize
data class ScanRequest(
    @SerializedName("market_types")
    val marketTypes: List<String>, // ["CRYPTO", "FOREX"]

    @SerializedName("strategies")
    val strategies: List<String>, // ["SAR_SMA", "SUPERTREND_MA"]

    @SerializedName("timeframe")
    val timeframe: String, // "5m", "15m", "1h", etc.

    @SerializedName("crypto_limit")
    val cryptoLimit: Int = 30,

    @SerializedName("forex_pairs")
    val forexPairs: List<String>? = null
) : Parcelable

// Scan Response Model
@Parcelize
data class ScanResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("signals")
    val signals: List<Signal>,

    @SerializedName("scan_time")
    val scanTime: Double,

    @SerializedName("total_symbols_scanned")
    val totalSymbolsScanned: Int,

    @SerializedName("timestamp")
    val timestamp: String,

    @SerializedName("message")
    val message: String? = null
) : Parcelable

// Market Data Model
@Parcelize
@Entity(tableName = "market_data")
data class MarketData(
    @PrimaryKey
    val symbol: String,

    val marketType: String,
    val price: Double,
    val priceChange24h: Double,
    val priceChangePercent24h: Double,
    val volume24h: Double,
    val high24h: Double,
    val low24h: Double,
    val lastUpdateTime: Long = System.currentTimeMillis()
) : Parcelable

// User Configuration Model
@Parcelize
data class UserConfig(
    @SerializedName("scan_interval_minutes")
    val scanIntervalMinutes: Int = 5,

    @SerializedName("active_strategies")
    val activeStrategies: List<String> = listOf("SAR_SMA", "SUPERTREND_MA"),

    @SerializedName("scan_crypto")
    val scanCrypto: Boolean = true,

    @SerializedName("scan_forex")
    val scanForex: Boolean = true,

    @SerializedName("timeframe")
    val timeframe: String = "5m",

    @SerializedName("crypto_top_coins")
    val cryptoTopCoins: Int = 30,

    @SerializedName("forex_pairs")
    val forexPairs: List<String> = listOf(
        "EURUSD", "GBPUSD", "USDJPY", "XAUUSD",
        "AUDUSD", "USDCAD", "USDCHF", "NZDUSD"
    ),

    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean = true,

    @SerializedName("auto_scan_enabled")
    val autoScanEnabled: Boolean = false
) : Parcelable

// API Response wrapper
@Parcelize
data class ApiResponse<T : Parcelable>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null
) : Parcelable

// Statistics Model
@Parcelize
data class Statistics(
    @SerializedName("total_signals")
    val totalSignals: Int,

    @SerializedName("crypto_signals")
    val cryptoSignals: Int,

    @SerializedName("forex_signals")
    val forexSignals: Int,

    @SerializedName("long_signals")
    val longSignals: Int,

    @SerializedName("short_signals")
    val shortSignals: Int,

    @SerializedName("sar_sma_signals")
    val sarSmaSignals: Int,

    @SerializedName("supertrend_ma_signals")
    val supertrendMaSignals: Int,

    @SerializedName("last_scan_time")
    val lastScanTime: String? = null
) : Parcelable

// Subscription Model (for future implementation)
@Parcelize
data class Subscription(
    val tier: String, // "FREE", "BASIC", "PREMIUM", "PROFESSIONAL"
    val isActive: Boolean,
    val expiryDate: Long? = null,
    val features: List<String>
) : Parcelable {

    companion object {
        fun getFreeSubscription() = Subscription(
            tier = "FREE",
            isActive = true,
            expiryDate = null,
            features = listOf(
                "5 scans per day",
                "Basic signals",
                "Limited history"
            )
        )

        fun getBasicSubscription() = Subscription(
            tier = "BASIC",
            isActive = true,
            expiryDate = null,
            features = listOf(
                "Unlimited scans",
                "All signals",
                "Full history",
                "Email notifications"
            )
        )

        fun getPremiumSubscription() = Subscription(
            tier = "PREMIUM",
            isActive = true,
            expiryDate = null,
            features = listOf(
                "Everything in Basic",
                "Auto scanning",
                "Priority support",
                "Advanced analytics",
                "Custom strategies"
            )
        )
    }
}