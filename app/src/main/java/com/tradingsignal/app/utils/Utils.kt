package com.tradingsignal.app.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object FormatUtils {

    private val priceFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    // Date/Time formatting
    fun formatTimestamp(timestamp: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
            val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US)
            val date = inputFormat.parse(timestamp)
            date?.let { outputFormat.format(it) } ?: timestamp
        } catch (e: Exception) {
            timestamp
        }
    }

    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> SimpleDateFormat("MMM dd", Locale.US).format(Date(timestamp))
        }
    }

    // Price formatting
    fun formatPrice(price: Double, decimals: Int = 6): String {
        return when {
            price >= 1000 -> String.format(Locale.US, "$%,.2f", price)
            price >= 1 -> String.format(Locale.US, "$%.4f", price)
            else -> String.format(Locale.US, "$%.${decimals}f", price)
        }
    }

    fun formatPriceChange(change: Double): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign${formatPrice(change)}"
    }

    // Percentage formatting
    fun formatPercent(value: Double): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign%.2f%%".format(value)
    }

    // Volume formatting
    fun formatVolume(volume: Double): String {
        return when {
            volume >= 1_000_000_000 -> "%.2fB".format(volume / 1_000_000_000)
            volume >= 1_000_000 -> "%.2fM".format(volume / 1_000_000)
            volume >= 1_000 -> "%.2fK".format(volume / 1_000)
            else -> "%.2f".format(volume)
        }
    }

    // Number formatting
    fun formatNumber(number: Double): String {
        return numberFormat.format(number)
    }

    fun formatRiskReward(ratio: Double): String {
        return "1:%.2f".format(ratio)
    }
}

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$".toRegex()
        return email.matches(emailRegex)
    }

    fun isValidPhoneNumber(phone: String): Boolean {
        val phoneRegex = "^\\+?[1-9]\\d{1,14}$".toRegex()
        return phone.matches(phoneRegex)
    }
}

object NetworkUtils {

    fun isNetworkAvailable(context: android.content.Context): Boolean {
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

object ColorUtils {

    fun getSignalColor(signalType: String): Int {
        return when (signalType) {
            "LONG" -> android.graphics.Color.parseColor("#4CAF50") // Green
            "SHORT" -> android.graphics.Color.parseColor("#F44336") // Red
            else -> android.graphics.Color.parseColor("#9E9E9E") // Grey
        }
    }

    fun getPercentageColor(percentage: Double): Int {
        return when {
            percentage > 0 -> android.graphics.Color.parseColor("#4CAF50") // Green
            percentage < 0 -> android.graphics.Color.parseColor("#F44336") // Red
            else -> android.graphics.Color.parseColor("#9E9E9E") // Grey
        }
    }

    fun getStrategyColor(strategy: String): Int {
        return when (strategy) {
            "SAR_SMA" -> android.graphics.Color.parseColor("#2196F3") // Blue
            "SUPERTREND_MA" -> android.graphics.Color.parseColor("#FF9800") // Orange
            else -> android.graphics.Color.parseColor("#9E9E9E") // Grey
        }
    }
}

object NotificationUtils {

    fun createNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }
}