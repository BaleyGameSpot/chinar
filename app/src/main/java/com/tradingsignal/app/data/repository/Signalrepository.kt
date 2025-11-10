package com.tradingsignal.app.data.repository

import androidx.lifecycle.LiveData
import com.tradingsignal.app.data.local.FollowedSignalDao
import com.tradingsignal.app.data.local.MarketDataDao
import com.tradingsignal.app.data.local.SignalDao
import com.tradingsignal.app.data.model.*
import com.tradingsignal.app.data.remote.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SignalRepository(
    private val apiService: ApiService,
    private val signalDao: SignalDao,
    private val marketDataDao: MarketDataDao,
    private val followedSignalDao: FollowedSignalDao
) {

    // ========== SIGNAL OPERATIONS ==========

    // Get all signals from local database
    fun getAllSignals(): LiveData<List<Signal>> = signalDao.getAllSignals()

    // Get favorite signals
    fun getFavoriteSignals(): LiveData<List<Signal>> = signalDao.getFavoriteSignals()

    // Get signals by market type
    fun getSignalsByMarket(marketType: String): LiveData<List<Signal>> =
        signalDao.getSignalsByMarket(marketType)

    // Get signals by strategy
    fun getSignalsByStrategy(strategy: String): LiveData<List<Signal>> =
        signalDao.getSignalsByStrategy(strategy)

    // Get unread count
    fun getUnreadCount(): LiveData<Int> = signalDao.getUnreadCount()

    // Search signals
    fun searchSignals(query: String): LiveData<List<Signal>> =
        signalDao.searchSignals(query)

    // Mark signal as read
    suspend fun markSignalAsRead(signalId: Long) = withContext(Dispatchers.IO) {
        signalDao.markAsRead(signalId)
    }

    // Toggle favorite
    suspend fun toggleFavorite(signalId: Long, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        signalDao.toggleFavorite(signalId, isFavorite)
    }

    // Delete signal
    suspend fun deleteSignal(signal: Signal) = withContext(Dispatchers.IO) {
        signalDao.deleteSignal(signal)
    }

    // Delete all signals
    suspend fun deleteAllSignals() = withContext(Dispatchers.IO) {
        signalDao.deleteAllSignals()
    }

    // ========== FOLLOWED SIGNALS OPERATIONS ==========

    // Follow a signal
    suspend fun followSignal(signal: Signal): Result<FollowedSignal> = withContext(Dispatchers.IO) {
        try {
            // Check if already following this signal
            val existing = followedSignalDao.getActiveFollowedSignalBySignalId(signal.id)
            if (existing != null) {
                return@withContext Result.failure(Exception("Already following this signal"))
            }

            // Create followed signal entity
            val followedSignal = FollowedSignal(
                signalId = signal.id,
                symbol = signal.symbol,
                marketType = signal.marketType,
                strategy = signal.strategy,
                signalType = signal.signalType,
                entryPrice = signal.price,
                takeProfit1 = signal.takeProfit1,
                takeProfit2 = signal.takeProfit2,
                stopLoss = signal.stopLoss,
                timeframe = signal.timeframe
            )

            // Insert and return
            val id = followedSignalDao.insert(followedSignal)
            val inserted = followedSignalDao.getFollowedSignalById(id)
            if (inserted != null) {
                Result.success(inserted)
            } else {
                Result.failure(Exception("Failed to retrieve followed signal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Stop following a signal
    suspend fun stopFollowingSignal(followedSignal: FollowedSignal, reason: String) = withContext(Dispatchers.IO) {
        followedSignalDao.stopFollowing(
            id = followedSignal.id,
            exitedAt = System.currentTimeMillis(),
            exitReason = reason,
            exitPrice = null
        )
    }

    // Get all active followed signals
    fun getActiveFollowedSignals(): LiveData<List<FollowedSignal>> =
        followedSignalDao.getActiveFollowedSignals()

    // Get followed signals with opposite signal detected
    fun getFollowedSignalsWithOppositeSignal(): LiveData<List<FollowedSignal>> =
        followedSignalDao.getFollowedSignalsWithOppositeSignal()

    // Mark opposite signal detected
    suspend fun markOppositeSignalDetected(followedSignal: FollowedSignal, price: Double) = withContext(Dispatchers.IO) {
        followedSignalDao.markOppositeSignalDetected(
            id = followedSignal.id,
            price = price,
            time = System.currentTimeMillis()
        )
    }

    // ========== SCAN OPERATIONS ==========

    // Perform single scan
    suspend fun performScan(scanRequest: ScanRequest): Result<ScanResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.performSingleScan(scanRequest)
            if (response.isSuccessful && response.body() != null) {
                val scanResponse = response.body()!!

                // Save signals to local database
                if (scanResponse.signals.isNotEmpty()) {
                    signalDao.insertSignals(scanResponse.signals)
                }

                Result.success(scanResponse)
            } else {
                Result.failure(Exception("Scan failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get scan status
    suspend fun getScanStatus(): Result<Statistics> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getScanStatus()
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Unknown error"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sync signals from server
    suspend fun syncSignals(): Result<List<Signal>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRecentSignals(hours = 24)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    // Save to local database
                    signalDao.insertSignals(apiResponse.data)
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Sync failed"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== MARKET DATA OPERATIONS ==========

    // Get all market data
    fun getAllMarketData(): LiveData<List<MarketData>> = marketDataDao.getAllMarketData()

    // Get market data by type
    fun getMarketDataByType(marketType: String): LiveData<List<MarketData>> =
        marketDataDao.getMarketDataByType(marketType)

    // Fetch market data from API
    suspend fun fetchMarketData(symbols: String? = null): Result<List<MarketData>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMarketData(symbols)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    // Save to local database
                    marketDataDao.insertMarketDataList(apiResponse.data)
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Failed to fetch data"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get current price for a symbol
    suspend fun getCurrentPrice(symbol: String): Result<MarketData> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCurrentPrice(symbol)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    // Save to local database
                    marketDataDao.insertMarketData(apiResponse.data)
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Failed to fetch price"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== STATISTICS OPERATIONS ==========

    // Get statistics
    suspend fun getStatistics(): Result<Statistics> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStatistics()
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Failed to get statistics"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== CONFIGURATION OPERATIONS ==========

    // Get user configuration
    suspend fun getUserConfig(): Result<UserConfig> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserConfig()
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Failed to get config"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update user configuration
    suspend fun updateUserConfig(config: UserConfig): Result<UserConfig> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.updateUserConfig(config)
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                if (apiResponse.success && apiResponse.data != null) {
                    Result.success(apiResponse.data)
                } else {
                    Result.failure(Exception(apiResponse.error ?: "Failed to update config"))
                }
            } else {
                Result.failure(Exception("Request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== HEALTH CHECK ==========

    // Check API health
    suspend fun checkApiHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.healthCheck()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}