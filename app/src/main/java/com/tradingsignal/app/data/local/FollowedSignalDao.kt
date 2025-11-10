package com.tradingsignal.app.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.tradingsignal.app.data.model.FollowedSignal

@Dao
interface FollowedSignalDao {

    /**
     * Insert a new followed signal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(followedSignal: FollowedSignal): Long

    /**
     * Update a followed signal
     */
    @Update
    suspend fun update(followedSignal: FollowedSignal)

    /**
     * Delete a followed signal
     */
    @Delete
    suspend fun delete(followedSignal: FollowedSignal)

    /**
     * Get all active followed signals
     */
    @Query("SELECT * FROM followed_signals WHERE isActive = 1 ORDER BY startedAt DESC")
    fun getActiveFollowedSignals(): LiveData<List<FollowedSignal>>

    /**
     * Get all followed signals (active and inactive)
     */
    @Query("SELECT * FROM followed_signals ORDER BY startedAt DESC")
    fun getAllFollowedSignals(): LiveData<List<FollowedSignal>>

    /**
     * Get a specific followed signal by ID
     */
    @Query("SELECT * FROM followed_signals WHERE id = :id")
    suspend fun getFollowedSignalById(id: Long): FollowedSignal?

    /**
     * Get followed signal by original signal ID
     */
    @Query("SELECT * FROM followed_signals WHERE signalId = :signalId AND isActive = 1 LIMIT 1")
    suspend fun getActiveFollowedSignalBySignalId(signalId: Long): FollowedSignal?

    /**
     * Check if a signal is already being followed
     */
    @Query("SELECT EXISTS(SELECT 1 FROM followed_signals WHERE signalId = :signalId AND isActive = 1)")
    suspend fun isSignalBeingFollowed(signalId: Long): Boolean

    /**
     * Get active followed signals for a specific symbol and strategy
     */
    @Query("SELECT * FROM followed_signals WHERE symbol = :symbol AND strategy = :strategy AND isActive = 1")
    suspend fun getActiveFollowedSignalsForSymbolAndStrategy(symbol: String, strategy: String): List<FollowedSignal>

    /**
     * Mark a followed signal as having opposite signal detected
     */
    @Query("""
        UPDATE followed_signals
        SET oppositeSignalDetected = 1,
            oppositeSignalPrice = :price,
            oppositeSignalTime = :time
        WHERE id = :id
    """)
    suspend fun markOppositeSignalDetected(id: Long, price: Double, time: Long)

    /**
     * Stop following a signal (mark as inactive)
     */
    @Query("""
        UPDATE followed_signals
        SET isActive = 0,
            exitedAt = :exitedAt,
            exitReason = :exitReason,
            exitPrice = :exitPrice
        WHERE id = :id
    """)
    suspend fun stopFollowing(id: Long, exitedAt: Long, exitReason: String, exitPrice: Double?)

    /**
     * Delete all inactive followed signals
     */
    @Query("DELETE FROM followed_signals WHERE isActive = 0")
    suspend fun deleteAllInactive()

    /**
     * Delete all followed signals
     */
    @Query("DELETE FROM followed_signals")
    suspend fun deleteAll()

    /**
     * Get count of active followed signals
     */
    @Query("SELECT COUNT(*) FROM followed_signals WHERE isActive = 1")
    fun getActiveFollowedSignalsCount(): LiveData<Int>

    /**
     * Get active followed signals with opposite signal detected
     */
    @Query("SELECT * FROM followed_signals WHERE isActive = 1 AND oppositeSignalDetected = 1 ORDER BY oppositeSignalTime DESC")
    fun getFollowedSignalsWithOppositeSignal(): LiveData<List<FollowedSignal>>
}
