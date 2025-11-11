package com.tradingsignal.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Authentication data models for API requests and responses
 */

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String?,

    @SerializedName("password")
    val password: String
)

data class User(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("username")
    val username: String,

    @SerializedName("full_name")
    val fullName: String?,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("is_admin")
    val isAdmin: Boolean,

    @SerializedName("created_at")
    val createdAt: String
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String,

    @SerializedName("user")
    val user: User
)

data class ChangePasswordRequest(
    @SerializedName("old_password")
    val oldPassword: String,

    @SerializedName("new_password")
    val newPassword: String
)

data class FollowSignalRequest(
    @SerializedName("signal_id")
    val signalId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("market_type")
    val marketType: String,

    @SerializedName("strategy")
    val strategy: String,

    @SerializedName("signal_type")
    val signalType: String,

    @SerializedName("entry_price")
    val entryPrice: Double,

    @SerializedName("take_profit1")
    val takeProfit1: Double,

    @SerializedName("take_profit2")
    val takeProfit2: Double,

    @SerializedName("stop_loss")
    val stopLoss: Double
)

data class FollowedSignal(
    @SerializedName("id")
    val id: Int,

    @SerializedName("signal_id")
    val signalId: Int,

    @SerializedName("user_id")
    val userId: Int,

    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("market_type")
    val marketType: String,

    @SerializedName("strategy")
    val strategy: String,

    @SerializedName("signal_type")
    val signalType: String,

    @SerializedName("entry_price")
    val entryPrice: Double,

    @SerializedName("take_profit1")
    val takeProfit1: Double,

    @SerializedName("take_profit2")
    val takeProfit2: Double,

    @SerializedName("stop_loss")
    val stopLoss: Double,

    @SerializedName("started_at")
    val startedAt: String,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("exited_at")
    val exitedAt: String?,

    @SerializedName("exit_reason")
    val exitReason: String?,

    @SerializedName("exit_price")
    val exitPrice: Double?,

    @SerializedName("opposite_signal_detected")
    val oppositeSignalDetected: Boolean,

    @SerializedName("opposite_signal_price")
    val oppositeSignalPrice: Double?,

    @SerializedName("opposite_signal_time")
    val oppositeSignalTime: String?
)

data class OppositeSignalDetection(
    @SerializedName("followed_signal_id")
    val followedSignalId: Int,

    @SerializedName("followed_signal")
    val followedSignal: FollowedSignal,

    @SerializedName("opposite_signal")
    val oppositeSignal: Signal
)
