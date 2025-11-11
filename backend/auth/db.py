"""
User Database Operations
"""

import aiosqlite
from typing import Optional, List
from datetime import datetime
from auth.models import UserCreate, UserInDB, User
from auth.utils import get_password_hash, verify_password

DATABASE_PATH = "trading_signals.db"


async def init_users_table():
    """Initialize users table in database"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                username TEXT UNIQUE NOT NULL,
                full_name TEXT,
                hashed_password TEXT NOT NULL,
                is_active BOOLEAN DEFAULT 1,
                is_admin BOOLEAN DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        await db.commit()
        print("✅ Users table initialized")


async def create_user(user: UserCreate) -> User:
    """Create a new user"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        # Check if email already exists
        async with db.execute("SELECT id FROM users WHERE email = ?", (user.email,)) as cursor:
            existing_user = await cursor.fetchone()
            if existing_user:
                raise ValueError("Email already registered")

        # Check if username already exists
        async with db.execute("SELECT id FROM users WHERE username = ?", (user.username,)) as cursor:
            existing_username = await cursor.fetchone()
            if existing_username:
                raise ValueError("Username already taken")

        # Hash password
        hashed_password = get_password_hash(user.password)

        # Insert user
        cursor = await db.execute("""
            INSERT INTO users (email, username, full_name, hashed_password, is_active, is_admin, created_at)
            VALUES (?, ?, ?, ?, 1, 0, ?)
        """, (user.email, user.username, user.full_name, hashed_password, datetime.now().isoformat()))

        await db.commit()
        user_id = cursor.lastrowid

        # Return created user
        return User(
            id=user_id,
            email=user.email,
            username=user.username,
            full_name=user.full_name,
            is_active=True,
            is_admin=False,
            created_at=datetime.now()
        )


async def get_user_by_email(email: str) -> Optional[UserInDB]:
    """Get user by email"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM users WHERE email = ?", (email,)) as cursor:
            row = await cursor.fetchone()
            if row:
                return UserInDB(
                    id=row['id'],
                    email=row['email'],
                    username=row['username'],
                    full_name=row['full_name'],
                    hashed_password=row['hashed_password'],
                    is_active=bool(row['is_active']),
                    is_admin=bool(row['is_admin']),
                    created_at=datetime.fromisoformat(row['created_at'])
                )
            return None


async def get_user_by_id(user_id: int) -> Optional[User]:
    """Get user by ID"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM users WHERE id = ?", (user_id,)) as cursor:
            row = await cursor.fetchone()
            if row:
                return User(
                    id=row['id'],
                    email=row['email'],
                    username=row['username'],
                    full_name=row['full_name'],
                    is_active=bool(row['is_active']),
                    is_admin=bool(row['is_admin']),
                    created_at=datetime.fromisoformat(row['created_at'])
                )
            return None


async def authenticate_user(email: str, password: str) -> Optional[UserInDB]:
    """Authenticate user by email and password"""
    user = await get_user_by_email(email)
    if not user:
        return None
    if not verify_password(password, user.hashed_password):
        return None
    return user


async def get_all_users(limit: int = 100, offset: int = 0) -> List[User]:
    """Get all users (admin only)"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute(
            "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?",
            (limit, offset)
        ) as cursor:
            rows = await cursor.fetchall()
            return [
                User(
                    id=row['id'],
                    email=row['email'],
                    username=row['username'],
                    full_name=row['full_name'],
                    is_active=bool(row['is_active']),
                    is_admin=bool(row['is_admin']),
                    created_at=datetime.fromisoformat(row['created_at'])
                )
                for row in rows
            ]


async def update_user_status(user_id: int, is_active: bool) -> bool:
    """Update user active status (admin only)"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute(
            "UPDATE users SET is_active = ? WHERE id = ?",
            (1 if is_active else 0, user_id)
        )
        await db.commit()
        return True


async def delete_user(user_id: int) -> bool:
    """Delete a user (admin only)"""
    async with aiosqlite.connect(DATABASE_PATH) as db:
        await db.execute("DELETE FROM users WHERE id = ?", (user_id,))
        await db.commit()
        return True


async def change_user_password(user_id: int, old_password: str, new_password: str) -> bool:
    """Change user password"""
    # Get user
    async with aiosqlite.connect(DATABASE_PATH) as db:
        db.row_factory = aiosqlite.Row
        async with db.execute("SELECT * FROM users WHERE id = ?", (user_id,)) as cursor:
            row = await cursor.fetchone()
            if not row:
                return False

            # Verify old password
            if not verify_password(old_password, row['hashed_password']):
                return False

            # Hash new password
            new_hashed = get_password_hash(new_password)

            # Update password
            await db.execute(
                "UPDATE users SET hashed_password = ? WHERE id = ?",
                (new_hashed, user_id)
            )
            await db.commit()
            return True
