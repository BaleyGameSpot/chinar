"""
Authentication API Routes
"""

from fastapi import APIRouter, HTTPException, status, Depends
from fastapi.security import OAuth2PasswordRequestForm
from typing import List

from auth.models import (
    User, UserCreate, UserLogin, Token, ChangePassword
)
from auth.db import (
    create_user, authenticate_user, get_user_by_id,
    get_all_users, update_user_status, delete_user,
    change_user_password
)
from auth.utils import create_access_token, get_current_user_from_token
from api.models import ApiResponse

router = APIRouter()


@router.post("/register", response_model=Token, status_code=status.HTTP_201_CREATED)
async def register(user: UserCreate):
    """
    Register a new user
    """
    try:
        # Create user in database
        db_user = await create_user(user)

        # Create access token
        access_token = create_access_token(
            data={"sub": db_user.email, "user_id": db_user.id}
        )

        return Token(
            access_token=access_token,
            token_type="bearer",
            user=db_user
        )

    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e)
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Registration failed: {str(e)}"
        )


@router.post("/login", response_model=Token)
async def login(form_data: OAuth2PasswordRequestForm = Depends()):
    """
    Login with email and password (OAuth2 compatible)
    """
    # Authenticate user
    user = await authenticate_user(form_data.username, form_data.password)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive"
        )

    # Create access token
    access_token = create_access_token(
        data={"sub": user.email, "user_id": user.id}
    )

    return Token(
        access_token=access_token,
        token_type="bearer",
        user=User(
            id=user.id,
            email=user.email,
            username=user.username,
            full_name=user.full_name,
            is_active=user.is_active,
            is_admin=user.is_admin,
            created_at=user.created_at
        )
    )


@router.post("/login/json", response_model=Token)
async def login_json(user_login: UserLogin):
    """
    Login with JSON body (alternative to OAuth2 form)
    """
    # Authenticate user
    user = await authenticate_user(user_login.email, user_login.password)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password"
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive"
        )

    # Create access token
    access_token = create_access_token(
        data={"sub": user.email, "user_id": user.id}
    )

    return Token(
        access_token=access_token,
        token_type="bearer",
        user=User(
            id=user.id,
            email=user.email,
            username=user.username,
            full_name=user.full_name,
            is_active=user.is_active,
            is_admin=user.is_admin,
            created_at=user.created_at
        )
    )


@router.get("/me", response_model=User)
async def get_current_user(current_user: dict = Depends(get_current_user_from_token)):
    """
    Get current authenticated user
    """
    user = await get_user_by_id(current_user["user_id"])

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found"
        )

    return user


@router.post("/change-password", response_model=ApiResponse)
async def change_password(
    password_data: ChangePassword,
    current_user: dict = Depends(get_current_user_from_token)
):
    """
    Change user password
    """
    success = await change_user_password(
        current_user["user_id"],
        password_data.old_password,
        password_data.new_password
    )

    if not success:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to change password. Check your old password."
        )

    return ApiResponse(
        success=True,
        message="Password changed successfully"
    )


# ==================== ADMIN ROUTES ====================

async def verify_admin(current_user: dict = Depends(get_current_user_from_token)):
    """Verify user is admin"""
    user = await get_user_by_id(current_user["user_id"])
    if not user or not user.is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required"
        )
    return user


@router.get("/users", response_model=List[User])
async def get_users(
    limit: int = 100,
    offset: int = 0,
    admin: User = Depends(verify_admin)
):
    """
    Get all users (admin only)
    """
    users = await get_all_users(limit, offset)
    return users


@router.put("/users/{user_id}/status", response_model=ApiResponse)
async def update_user_active_status(
    user_id: int,
    is_active: bool,
    admin: User = Depends(verify_admin)
):
    """
    Update user active status (admin only)
    """
    success = await update_user_status(user_id, is_active)

    if not success:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to update user status"
        )

    return ApiResponse(
        success=True,
        message=f"User {'activated' if is_active else 'deactivated'} successfully"
    )


@router.delete("/users/{user_id}", response_model=ApiResponse)
async def delete_user_endpoint(
    user_id: int,
    admin: User = Depends(verify_admin)
):
    """
    Delete a user (admin only)
    """
    # Prevent admin from deleting themselves
    if user_id == admin.id:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot delete your own account"
        )

    success = await delete_user(user_id)

    if not success:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Failed to delete user"
        )

    return ApiResponse(
        success=True,
        message="User deleted successfully"
    )
