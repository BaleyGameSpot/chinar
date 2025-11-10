package com.tradingsignal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tradingsignal.app.data.model.FollowedSignal
import com.tradingsignal.app.data.model.MarketData
import com.tradingsignal.app.data.model.Signal

@Database(
    entities = [Signal::class, MarketData::class, FollowedSignal::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun signalDao(): SignalDao
    abstract fun marketDataDao(): MarketDataDao
    abstract fun followedSignalDao(): FollowedSignalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "trading_signal_db"

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}