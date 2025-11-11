"""
Database Module - SQLite database for storing signals
Simple and portable, no external database server needed
"""

import sqlite3
import json
from typing import List, Optional
from datetime import datetime
import aiosqlite
from api.models import Signal, Statistics

# Database file
DB_FILE = "trading_signals.db"

# Global connection pool
_connection = None

async def init_db():
    """Initialize database and create tables"""
    async with aiosqlite.connect(DB_FILE) as db:
        # Create signals table
        await db.execute("""
            CREATE TABLE IF NOT EXISTS signals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                symbol TEXT NOT NULL,
                market_type TEXT NOT NULL,
                strategy TEXT NOT NULL,
                signal_type TEXT NOT NULL,
                price REAL NOT NULL,
                tp1 REAL NOT NULL,
                tp2 REAL NOT NULL,
                stop_loss REAL NOT NULL,
                volume REAL,
                quote_volume REAL,
                timestamp TEXT NOT NULL,
                timeframe TEXT,
                accuracy TEXT,
                conditions TEXT,
                all_conditions_met INTEGER,
                sma_20 REAL,
                sma_40 REAL,
                sar REAL,
                ma_value REAL,
                ma_type TEXT,
                trend TEXT,
                active_band REAL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
        """)
        
        # Create index on timestamp for faster queries
        await db.execute("""
            CREATE INDEX IF NOT EXISTS idx_timestamp 
            ON signals(timestamp DESC)
        """)
        
        # Create index on market_type
        await db.execute("""
            CREATE INDEX IF NOT EXISTS idx_market_type 
            ON signals(market_type)
        """)
        
        # Create index on strategy
        await db.execute("""
            CREATE INDEX IF NOT EXISTS idx_strategy 
            ON signals(strategy)
        """)
        
        await db.commit()
        print("✅ Database initialized successfully")

async def close_db():
    """Close database connection"""
    global _connection
    if _connection:
        await _connection.close()
        _connection = None

async def save_signals(signals: List[Signal]):
    """Save signals to database"""
    if not signals:
        return
    
    async with aiosqlite.connect(DB_FILE) as db:
        for signal in signals:
            # Convert conditions to JSON string
            conditions_json = json.dumps({
                "condition_1": signal.conditions.condition_1,
                "condition_2": signal.conditions.condition_2,
                "condition_3": signal.conditions.condition_3
            })
            
            await db.execute("""
                INSERT INTO signals (
                    symbol, market_type, strategy, signal_type,
                    price, tp1, tp2, stop_loss,
                    volume, quote_volume, timestamp, timeframe,
                    accuracy, conditions, all_conditions_met,
                    sma_20, sma_40, sar,
                    ma_value, ma_type, trend, active_band
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """, (
                signal.symbol, signal.market_type, signal.strategy, signal.signal,
                signal.price, signal.tp1, signal.tp2, signal.stop_loss,
                signal.volume, signal.quote_volume, signal.timestamp, signal.timeframe,
                signal.accuracy, conditions_json, 1 if signal.all_conditions_met else 0,
                signal.sma_20, signal.sma_40, signal.sar,
                signal.ma_value, signal.ma_type, signal.trend, signal.active_band
            ))
        
        await db.commit()
        print(f"✅ Saved {len(signals)} signals to database")

async def get_db_signals(limit: int = 100, offset: int = 0) -> List[dict]:
    """Get signals from database"""
    async with aiosqlite.connect(DB_FILE) as db:
        db.row_factory = aiosqlite.Row
        
        async with db.execute("""
            SELECT * FROM signals
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """, (limit, offset)) as cursor:
            rows = await cursor.fetchall()
            
            signals = []
            for row in rows:
                signal_dict = dict(row)
                # Parse conditions JSON
                if signal_dict.get('conditions'):
                    signal_dict['conditions'] = json.loads(signal_dict['conditions'])
                signals.append(signal_dict)
            
            return signals

async def get_statistics() -> Statistics:
    """Get statistics about signals"""
    async with aiosqlite.connect(DB_FILE) as db:
        # Total signals
        async with db.execute("SELECT COUNT(*) FROM signals") as cursor:
            total = (await cursor.fetchone())[0]
        
        # Crypto signals
        async with db.execute(
            "SELECT COUNT(*) FROM signals WHERE market_type = 'CRYPTO'"
        ) as cursor:
            crypto = (await cursor.fetchone())[0]
        
        # Forex signals
        async with db.execute(
            "SELECT COUNT(*) FROM signals WHERE market_type = 'FOREX'"
        ) as cursor:
            forex = (await cursor.fetchone())[0]
        
        # Long signals
        async with db.execute(
            "SELECT COUNT(*) FROM signals WHERE signal_type = 'LONG'"
        ) as cursor:
            long_signals = (await cursor.fetchone())[0]
        
        # Short signals
        async with db.execute(
            "SELECT COUNT(*) FROM signals WHERE signal_type = 'SHORT'"
        ) as cursor:
            short_signals = (await cursor.fetchone())[0]
        
        # SAR_SMA signals
        async with db.execute(
            "SELECT COUNT(*) FROM signals WHERE strategy = 'SAR_SMA'"
        ) as cursor:
            sar_sma = (await cursor.fetchone())[0]
        
        # SUPERTREND_MA signals
        async with db.execute(
            "SELECT COUNT(*) FROM signals WHERE strategy = 'SUPERTREND_MA'"
        ) as cursor:
            supertrend = (await cursor.fetchone())[0]
        
        # Last scan time
        async with db.execute(
            "SELECT timestamp FROM signals ORDER BY created_at DESC LIMIT 1"
        ) as cursor:
            result = await cursor.fetchone()
            last_scan = result[0] if result else None
        
        return Statistics(
            total_signals=total,
            crypto_signals=crypto,
            forex_signals=forex,
            long_signals=long_signals,
            short_signals=short_signals,
            sar_sma_signals=sar_sma,
            supertrend_ma_signals=supertrend,
            last_scan_time=last_scan
        )