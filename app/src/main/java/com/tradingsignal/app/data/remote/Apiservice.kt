package com.tradingsignal.app.data.remote

import com.tradingsignal.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Scan endpoints
    @POST("scan/single")
    suspend fun performSingleScan(
        @Body scanRequest: ScanRequest
    ): Response<ScanResponse>

    @GET("scan/status")
    suspend fun getScanStatus(): Response<ApiResponse<Statistics>>

    // Signal endpoints
    @GET("signals")
    suspend fun getAllSignals(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<ApiResponse<List<Signal>>>

    @GET("signals/{id}")
    suspend fun getSignalById(
        @Path("id") signalId: Long
    ): Response<ApiResponse<Signal>>

    @GET("signals/recent")
    suspend fun getRecentSignals(
        @Query("hours") hours: Int = 24
    ): Response<ApiResponse<List<Signal>>>

    @GET("signals/market/{marketType}")
    suspend fun getSignalsByMarket(
        @Path("marketType") marketType: String
    ): Response<ApiResponse<List<Signal>>>

    @GET("signals/strategy/{strategy}")
    suspend fun getSignalsByStrategy(
        @Path("strategy") strategy: String
    ): Response<ApiResponse<List<Signal>>>

    // Market data endpoints
    @GET("market/data")
    suspend fun getMarketData(
        @Query("symbols") symbols: String? = null
    ): Response<ApiResponse<List<MarketData>>>

    @GET("market/price/{symbol}")
    suspend fun getCurrentPrice(
        @Path("symbol") symbol: String
    ): Response<ApiResponse<MarketData>>

    // Configuration endpoints
    @GET("config")
    suspend fun getUserConfig(): Response<ApiResponse<UserConfig>>

    @PUT("config")
    suspend fun updateUserConfig(
        @Body config: UserConfig
    ): Response<ApiResponse<UserConfig>>

    // Statistics
    @GET("statistics")
    suspend fun getStatistics(): Response<ApiResponse<Statistics>>

    // Health check
    @GET("health")
    suspend fun healthCheck(): Response<ApiResponse<String>>
}