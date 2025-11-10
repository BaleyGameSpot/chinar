package com.tradingsignal.app.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.tradingsignal.app.data.model.MarketData
import com.tradingsignal.app.data.model.Signal

@Dao
interface SignalDao {

    @Query("SELECT * FROM signals ORDER BY createdAt DESC")
    fun getAllSignals(): LiveData<List<Signal>>

    @Query("SELECT * FROM signals WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteSignals(): LiveData<List<Signal>>

    @Query("SELECT * FROM signals WHERE marketType = :marketType ORDER BY createdAt DESC")
    fun getSignalsByMarket(marketType: String): LiveData<List<Signal>>

    @Query("SELECT * FROM signals WHERE strategy = :strategy ORDER BY createdAt DESC")
    fun getSignalsByStrategy(strategy: String): LiveData<List<Signal>>

    @Query("SELECT * FROM signals WHERE signalType = :signalType ORDER BY createdAt DESC")
    fun getSignalsByType(signalType: String): LiveData<List<Signal>>

    @Query("SELECT * FROM signals WHERE id = :signalId")
    suspend fun getSignalById(signalId: Long): Signal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: Signal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<Signal>)

    @Update
    suspend fun updateSignal(signal: Signal)

    @Delete
    suspend fun deleteSignal(signal: Signal)

    @Query("DELETE FROM signals")
    suspend fun deleteAllSignals()

    @Query("UPDATE signals SET isRead = 1 WHERE id = :signalId")
    suspend fun markAsRead(signalId: Long)

    @Query("UPDATE signals SET isFavorite = :isFavorite WHERE id = :signalId")
    suspend fun toggleFavorite(signalId: Long, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM signals WHERE isRead = 0")
    fun getUnreadCount(): LiveData<Int>

    @Query("SELECT * FROM signals WHERE createdAt >= :timestamp ORDER BY createdAt DESC")
    fun getSignalsSince(timestamp: Long): LiveData<List<Signal>>

    @Query("""
        SELECT * FROM signals 
        WHERE symbol LIKE '%' || :query || '%' 
        OR strategy LIKE '%' || :query || '%'
        OR marketType LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchSignals(query: String): LiveData<List<Signal>>
}

@Dao
interface MarketDataDao {

    @Query("SELECT * FROM market_data ORDER BY symbol ASC")
    fun getAllMarketData(): LiveData<List<MarketData>>

    @Query("SELECT * FROM market_data WHERE marketType = :marketType ORDER BY symbol ASC")
    fun getMarketDataByType(marketType: String): LiveData<List<MarketData>>

    @Query("SELECT * FROM market_data WHERE symbol = :symbol")
    suspend fun getMarketDataBySymbol(symbol: String): MarketData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketData(marketData: MarketData)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketDataList(marketDataList: List<MarketData>)

    @Update
    suspend fun updateMarketData(marketData: MarketData)

    @Delete
    suspend fun deleteMarketData(marketData: MarketData)

    @Query("DELETE FROM market_data")
    suspend fun deleteAllMarketData()

    @Query("DELETE FROM market_data WHERE lastUpdateTime < :timestamp")
    suspend fun deleteOldMarketData(timestamp: Long)

    @Query("""
        SELECT * FROM market_data 
        WHERE symbol LIKE '%' || :query || '%'
        ORDER BY symbol ASC
    """)
    fun searchMarketData(query: String): LiveData<List<MarketData>>
}