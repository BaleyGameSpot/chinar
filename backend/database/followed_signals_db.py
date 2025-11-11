"""
Followed Signals Database Operations
"""

import aiosqlite
from typing import Optional, List
from datetime import datetime
from pydantic import BaseModel

DATABASE_PATH = "trading_signals.db"


class FollowedSignalCreate(BaseModel):
    """Model for creating a followed signal"""
    signal_id: int
    user_id: int
    symbol: str
    market_type: str
    strategy: str
    signal_type: str  # LONG or SHORT
    entry_price: float
    take_profit1: float
    take_profit2: float
    stop_loss: float


class FollowedSignal(BaseModel):
    """Model for followed signal"""
    id: int
    signal_id: int
    user_id: int
    symbol: str
    market_type: str
    strategy: str
    signal_type: str
    entry_price: float
    take_profit1: float
    take_profit2: float
    stop_loss: float
    started_at: str
    is_active: bool
    exited_at: Optional[str] = None
    exit_reason: Optional[str] = None
    exit_price: Optional[float] = None
    opposite_signal_detected: bool = False
    opposite_signal_price: Optional[float] = None
    opposite_signal_time: Optional[str] = None


async def init_followed_signals_table():
    """Initialize followed_signals table"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS followed_signals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                signal_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                symbol TEXT NOT NULL,
                market_type TEXT NOT NULL,
                strategy TEXT NOT NULL,
                signal_type TEXT NOT NULL,
                entry_price REAL NOT NULL,
                take_profit1 REAL NOT NULL,
                take_profit2 REAL NOT NULL,
                stop_loss REAL NOT NULL,
                started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_active BOOLEAN DEFAULT 1,
                exited_at TIMESTAMP,
                exit_reason TEXT,
                exit_price REAL,
                opposite_signal_detected BOOLEAN DEFAULT 0,
                opposite_signal_price REAL,
                opposite_signal_time TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
            )
        """)

        # Create indexes
        await db.execute("""
            CREATE INDEX IF NOT EXISTS idx_followed_user_id
            ON followed_signals(user_id)
        """)

        await db.execute("""
            CREATE INDEX IF NOT EXISTS idx_followed_is_active
            ON followed_signals(is_active)
        """)

        await db.commit()
        print("✅ Followed signals table initialized")


async def create_followed_signal(followed: FollowedSignalCreate) -> FollowedSignal:
    """Create a new followed signal"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        cursor = await db.execute("""
            INSERT INTO followed_signals (
                signal_id, user_id, symbol, market_type, strategy, signal_type,
                entry_price, take_profit1, take_profit2, stop_loss,
                started_at, is_active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
        """, (
            followed.signal_id, followed.user_id, followed.symbol,
            followed.market_type, followed.strategy, followed.signal_type,
            followed.entry_price, followed.take_profit1, followed.take_profit2,
            followed.stop_loss, datetime.now().isoformat()
        ))

        await db.commit()
        followed_id = cursor.lastrowid

        return FollowedSignal(
            id=followed_id,
            signal_id=followed.signal_id,
            user_id=followed.user_id,
            symbol=followed.symbol,
            market_type=followed.market_type,
            strategy=followed.strategy,
            signal_type=followed.signal_type,
            entry_price=followed.entry_price,
            take_profit1=followed.take_profit1,
            take_profit2=followed.take_profit2,
            stop_loss=followed.stop_loss,
            started_at=datetime.now().isoformat(),
            is_active=True,
            opposite_signal_detected=False
        )


async def get_user_followed_signals(user_id: int, active_only: bool = False) -> List[FollowedSignal]:
    """Get all followed signals for a user"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row

        if active_only:
            query = "SELECT * FROM followed_signals WHERE user_id = ? AND is_active = 1 ORDER BY started_at DESC"
        else:
            query = "SELECT * FROM followed_signals WHERE user_id = ? ORDER BY started_at DESC"

        async with db.execute(query, (user_id,)) as cursor:
            rows = await cursor.fetchall()
            return [
                FollowedSignal(
                    id=row['id'],
                    signal_id=row['signal_id'],
                    user_id=row['user_id'],
                    symbol=row['symbol'],
                    market_type=row['market_type'],
                    strategy=row['strategy'],
                    signal_type=row['signal_type'],
                    entry_price=row['entry_price'],
                    take_profit1=row['take_profit1'],
                    take_profit2=row['take_profit2'],
                    stop_loss=row['stop_loss'],
                    started_at=row['started_at'],
                    is_active=bool(row['is_active']),
                    exited_at=row['exited_at'],
                    exit_reason=row['exit_reason'],
                    exit_price=row['exit_price'],
                    opposite_signal_detected=bool(row['opposite_signal_detected']),
                    opposite_signal_price=row['opposite_signal_price'],
                    opposite_signal_time=row['opposite_signal_time']
                )
                for row in rows
            ]


async def get_followed_signal_by_id(followed_id: int, user_id: int) -> Optional[FollowedSignal]:
    """Get a specific followed signal"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT * FROM followed_signals WHERE id = ? AND user_id = ?",
            (followed_id, user_id)
        ) as cursor:
            row = await cursor.fetchone()
            if row:
                return FollowedSignal(
                    id=row['id'],
                    signal_id=row['signal_id'],
                    user_id=row['user_id'],
                    symbol=row['symbol'],
                    market_type=row['market_type'],
                    strategy=row['strategy'],
                    signal_type=row['signal_type'],
                    entry_price=row['entry_price'],
                    take_profit1=row['take_profit1'],
                    take_profit2=row['take_profit2'],
                    stop_loss=row['stop_loss'],
                    started_at=row['started_at'],
                    is_active=bool(row['is_active']),
                    exited_at=row['exited_at'],
                    exit_reason=row['exit_reason'],
                    exit_price=row['exit_price'],
                    opposite_signal_detected=bool(row['opposite_signal_detected']),
                    opposite_signal_price=row['opposite_signal_price'],
                    opposite_signal_time=row['opposite_signal_time']
                )
            return None


async def stop_following_signal(followed_id: int, user_id: int, exit_reason: str, exit_price: Optional[float] = None) -> bool:
    """Stop following a signal"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute("""
            UPDATE followed_signals
            SET is_active = 0, exited_at = ?, exit_reason = ?, exit_price = ?
            WHERE id = ? AND user_id = ?
        """, (datetime.now().isoformat(), exit_reason, exit_price, followed_id, user_id))

        await db.commit()
        return True


async def mark_opposite_signal_detected(followed_id: int, opposite_price: float) -> bool:
    """Mark that an opposite signal was detected"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute("""
            UPDATE followed_signals
            SET opposite_signal_detected = 1,
                opposite_signal_price = ?,
                opposite_signal_time = ?
            WHERE id = ?
        """, (opposite_price, datetime.now().isoformat(), followed_id))

        await db.commit()
        return True


async def check_for_opposite_signals(user_id: int) -> List[dict]:
    """
    Check for opposite signals on user's followed positions
    Returns list of followed signals that have opposite signals
    """
    from database.db import get_db_signals

    # Get user's active followed signals
    followed = await get_user_followed_signals(user_id, active_only=True)

    if not followed:
        return []

    # Get recent signals (last 24 hours)
    recent_signals = await get_db_signals(limit=1000)

    opposite_detections = []

    for fs in followed:
        # Look for opposite signals for the same symbol
        for signal in recent_signals:
            if signal.get('symbol') == fs.symbol:
                # Check if it's an opposite signal
                if fs.signal_type == "LONG" and signal.get('signal_type') == "SHORT":
                    opposite_detections.append({
                        "followed_signal_id": fs.id,
                        "followed_signal": fs,
                        "opposite_signal": signal
                    })
                    # Mark as detected
                    await mark_opposite_signal_detected(fs.id, signal.get('price', 0))
                    break
                elif fs.signal_type == "SHORT" and signal.get('signal_type') == "LONG":
                    opposite_detections.append({
                        "followed_signal_id": fs.id,
                        "followed_signal": fs,
                        "opposite_signal": signal
                    })
                    # Mark as detected
                    await mark_opposite_signal_detected(fs.id, signal.get('price', 0))
                    break

    return opposite_detections
