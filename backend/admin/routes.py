"""
Web-Based Admin Panel Routes
"""

from fastapi import APIRouter, Request, HTTPException, Depends, Form
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from typing import Optional

from auth.models import User
from auth.db import get_all_users, get_user_by_id, update_user_status, delete_user
from auth.utils import get_current_user_from_token
from database.db import get_db_signals, get_statistics
from database.followed_signals_db import get_user_followed_signals

router = APIRouter()
templates = Jinja2Templates(directory="admin/templates")


async def verify_admin_user(current_user: dict = Depends(get_current_user_from_token)) -> User:
    """Verify user is admin"""
    user = await get_user_by_id(current_user["user_id"])
    if not user or not user.is_admin:
        raise HTTPException(status_code=403, detail="Admin access required")
    return user


@router.get("/", response_class=HTMLResponse)
async def admin_dashboard(request: Request):
    """
    Admin dashboard home page
    """
    try:
        # Get statistics
        stats = await get_statistics()

        # Get recent signals
        recent_signals = await get_db_signals(limit=10)

        # Get user count
        users = await get_all_users(limit=5)

        return templates.TemplateResponse("dashboard.html", {
            "request": request,
            "stats": stats,
            "recent_signals": recent_signals,
            "users": users,
            "page": "dashboard"
        })
    except Exception as e:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error": str(e)
        })


@router.get("/users", response_class=HTMLResponse)
async def admin_users(
    request: Request,
    limit: int = 50,
    offset: int = 0
):
    """
    User management page
    """
    try:
        users = await get_all_users(limit=limit, offset=offset)

        return templates.TemplateResponse("users.html", {
            "request": request,
            "users": users,
            "page": "users"
        })
    except Exception as e:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error": str(e)
        })


@router.get("/signals", response_class=HTMLResponse)
async def admin_signals(
    request: Request,
    limit: int = 100,
    offset: int = 0
):
    """
    Signal management page
    """
    try:
        signals = await get_db_signals(limit=limit, offset=offset)

        return templates.TemplateResponse("signals.html", {
            "request": request,
            "signals": signals,
            "page": "signals"
        })
    except Exception as e:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error": str(e)
        })


@router.get("/strategies", response_class=HTMLResponse)
async def admin_strategies(request: Request):
    """
    Strategy configuration page
    """
    try:
        return templates.TemplateResponse("strategies.html", {
            "request": request,
            "page": "strategies"
        })
    except Exception as e:
        return templates.TemplateResponse("error.html", {
            "request": request,
            "error": str(e)
        })


@router.post("/users/{user_id}/toggle-status")
async def toggle_user_status(
    user_id: int,
    is_active: bool = Form(...),
    admin: User = Depends(verify_admin_user)
):
    """
    Toggle user active status
    """
    try:
        await update_user_status(user_id, is_active)
        return RedirectResponse(url="/admin/users", status_code=303)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/users/{user_id}/delete")
async def delete_user_endpoint(
    user_id: int,
    admin: User = Depends(verify_admin_user)
):
    """
    Delete a user
    """
    try:
        if user_id == admin.id:
            raise HTTPException(status_code=400, detail="Cannot delete your own account")

        await delete_user(user_id)
        return RedirectResponse(url="/admin/users", status_code=303)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/login", response_class=HTMLResponse)
async def admin_login_page(request: Request):
    """
    Admin login page
    """
    return templates.TemplateResponse("login.html", {
        "request": request
    })
