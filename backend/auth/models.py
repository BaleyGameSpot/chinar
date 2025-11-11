"""
Authentication Models - User and Token schemas
"""

from pydantic import BaseModel, EmailStr, Field
from typing import Optional
from datetime import datetime


class UserBase(BaseModel):
    """Base user model"""
    email: EmailStr
    username: str = Field(..., min_length=3, max_length=50)
    full_name: Optional[str] = None


class UserCreate(UserBase):
    """User creation model"""
    password: str = Field(..., min_length=6, max_length=100)


class UserLogin(BaseModel):
    """User login model"""
    email: EmailStr
    password: str


class User(UserBase):
    """User model with ID"""
    id: int
    is_active: bool = True
    is_admin: bool = False
    created_at: datetime

    class Config:
        from_attributes = True


class UserInDB(User):
    """User model as stored in database"""
    hashed_password: str


class Token(BaseModel):
    """Token response model"""
    access_token: str
    token_type: str = "bearer"
    user: User


class TokenData(BaseModel):
    """Token data for validation"""
    email: Optional[str] = None
    user_id: Optional[int] = None


class ChangePassword(BaseModel):
    """Change password model"""
    old_password: str
    new_password: str = Field(..., min_length=6, max_length=100)
