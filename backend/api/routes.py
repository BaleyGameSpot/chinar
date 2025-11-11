"""
API Routes for Trading Signal Backend
Handles all HTTP endpoints for the Android app
"""

from fastapi import APIRouter, HTTPException, Query, BackgroundTasks
from typing import List, Optional
from datetime import datetime, timedelta

from api.models import (
    ScanRequest, ScanResponse, Signal, ApiResponse,
    Statistics, UserConfig, MarketData
)
from services.scanner_service import ScannerService
from database.db import get_db_signals, save_signals, get_statistics

router = APIRouter()
scanner = ScannerService()

# ==================== SCAN ENDPOINTS ====================

@router.post("/scan/single", response_model=ScanResponse)
async def perform_single_scan(request: ScanRequest, background_tasks: BackgroundTasks):
    """
    Perform a single scan with specified parameters
    Returns signals found during the scan
    """
    try:
        print(f"📡 Starting scan: {request.market_types}, strategies: {request.strategies}")
        
        # Perform scan using the scanner service
        scan_response = await scanner.perform_scan(request)
        
        # Save signals to database in background
        if scan_response.signals:
            background_tasks.add_task(save_signals, scan_response.signals)
        
        print(f"✅ Scan complete: Found {len(scan_response.signals)} signals")
        return scan_response
        
    except Exception as e:
        print(f"❌ Scan error: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Scan failed: {str(e)}")

@router.get("/scan/status", response_model=ApiResponse)
async def get_scan_status():
    """
    Get current scanning status and statistics
    """
    try:
        stats = await get_statistics()
        return ApiResponse(
            success=True,
            data=stats,
            message="Statistics retrieved successfully"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==================== SIGNAL ENDPOINTS ====================

@router.get("/signals", response_model=ApiResponse)
async def get_all_signals(
    limit: Optional[int] = Query(100, ge=1, le=500),
    offset: Optional[int] = Query(0, ge=0)
):
    """
    Get all signals with pagination
    """
    try:
        signals = await get_db_signals(limit=limit, offset=offset)
        return ApiResponse(
            success=True,
            data=signals,
            message=f"Retrieved {len(signals)} signals"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/signals/{signal_id}", response_model=ApiResponse)
async def get_signal_by_id(signal_id: int):
    """
    Get a specific signal by ID
    """
    try:
        signal = await scanner.get_signal_by_id(signal_id)
        if not signal:
            raise HTTPException(status_code=404, detail="Signal not found")
        
        return ApiResponse(
            success=True,
            data=signal,
            message="Signal retrieved successfully"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/signals/recent", response_model=ApiResponse)
async def get_recent_signals(hours: int = Query(24, ge=1, le=168)):
    """
    Get signals from the last N hours
    """
    try:
        cutoff_time = datetime.now() - timedelta(hours=hours)
        signals = await scanner.get_signals_since(cutoff_time)
        
        return ApiResponse(
            success=True,
            data=signals,
            message=f"Retrieved {len(signals)} signals from last {hours} hours"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/signals/market/{market_type}", response_model=ApiResponse)
async def get_signals_by_market(market_type: str):
    """
    Get signals filtered by market type (CRYPTO or FOREX)
    """
    try:
        if market_type.upper() not in ["CRYPTO", "FOREX"]:
            raise HTTPException(status_code=400, detail="Market type must be CRYPTO or FOREX")
        
        signals = await scanner.get_signals_by_market(market_type.upper())
        
        return ApiResponse(
            success=True,
            data=signals,
            message=f"Retrieved {len(signals)} {market_type} signals"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/signals/strategy/{strategy}", response_model=ApiResponse)
async def get_signals_by_strategy(strategy: str):
    """
    Get signals filtered by strategy (SAR_SMA or SUPERTREND_MA)
    """
    try:
        if strategy.upper() not in ["SAR_SMA", "SUPERTREND_MA"]:
            raise HTTPException(
                status_code=400,
                detail="Strategy must be SAR_SMA or SUPERTREND_MA"
            )
        
        signals = await scanner.get_signals_by_strategy(strategy.upper())
        
        return ApiResponse(
            success=True,
            data=signals,
            message=f"Retrieved {len(signals)} {strategy} signals"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==================== MARKET DATA ENDPOINTS ====================

@router.get("/market/data", response_model=ApiResponse)
async def get_market_data(symbols: Optional[str] = None):
    """
    Get current market data for symbols
    symbols: Comma-separated list of symbols (e.g., "BTCUSDT,ETHUSDT")
    """
    try:
        symbol_list = symbols.split(",") if symbols else None
        market_data = await scanner.get_market_data(symbol_list)
        
        return ApiResponse(
            success=True,
            data=market_data,
            message=f"Retrieved market data for {len(market_data)} symbols"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.get("/market/price/{symbol}", response_model=ApiResponse)
async def get_current_price(symbol: str):
    """
    Get current price for a specific symbol
    """
    try:
        price_data = await scanner.get_current_price(symbol)
        if not price_data:
            raise HTTPException(status_code=404, detail=f"Price data not found for {symbol}")
        
        return ApiResponse(
            success=True,
            data=price_data,
            message=f"Retrieved price for {symbol}"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==================== CONFIGURATION ENDPOINTS ====================

@router.get("/config", response_model=ApiResponse)
async def get_user_config():
    """
    Get user configuration settings
    """
    try:
        config = await scanner.get_config()
        return ApiResponse(
            success=True,
            data=config,
            message="Configuration retrieved successfully"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.put("/config", response_model=ApiResponse)
async def update_user_config(config: UserConfig):
    """
    Update user configuration settings
    """
    try:
        updated_config = await scanner.update_config(config)
        return ApiResponse(
            success=True,
            data=updated_config,
            message="Configuration updated successfully"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==================== STATISTICS ENDPOINTS ====================

@router.get("/statistics", response_model=ApiResponse)
async def get_statistics_endpoint():
    """
    Get overall statistics about signals
    """
    try:
        stats = await get_statistics()
        return ApiResponse(
            success=True,
            data=stats,
            message="Statistics retrieved successfully"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ==================== FOLLOWED SIGNALS ENDPOINTS ====================

from database.followed_signals_db import (
    FollowedSignalCreate, FollowedSignal,
    create_followed_signal, get_user_followed_signals,
    get_followed_signal_by_id, stop_following_signal,
    check_for_opposite_signals
)
from auth.utils import get_current_user_from_token


@router.post("/signals/follow", response_model=ApiResponse)
async def follow_signal(
    followed: FollowedSignalCreate,
    current_user: dict = Depends(get_current_user_from_token)
):
    """
    Follow a trading signal
    """
    try:
        # Ensure user_id matches the authenticated user
        followed.user_id = current_user["user_id"]

        # Create followed signal
        result = await create_followed_signal(followed)

        return ApiResponse(
            success=True,
            data=result.dict(),
            message="Signal followed successfully"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/signals/followed", response_model=ApiResponse)
async def get_followed_signals(
    active_only: bool = False,
    current_user: dict = Depends(get_current_user_from_token)
):
    """
    Get user's followed signals
    """
    try:
        signals = await get_user_followed_signals(current_user["user_id"], active_only)

        return ApiResponse(
            success=True,
            data=[s.dict() for s in signals],
            message=f"Retrieved {len(signals)} followed signals"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/signals/followed/{followed_id}", response_model=ApiResponse)
async def get_followed_signal(
    followed_id: int,
    current_user: dict = Depends(get_current_user_from_token)
):
    """
    Get a specific followed signal
    """
    try:
        signal = await get_followed_signal_by_id(followed_id, current_user["user_id"])

        if not signal:
            raise HTTPException(status_code=404, detail="Followed signal not found")

        return ApiResponse(
            success=True,
            data=signal.dict(),
            message="Followed signal retrieved successfully"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/signals/followed/{followed_id}", response_model=ApiResponse)
async def unfollow_signal(
    followed_id: int,
    exit_reason: str = "MANUAL",
    exit_price: Optional[float] = None,
    current_user: dict = Depends(get_current_user_from_token)
):
    """
    Stop following a signal
    """
    try:
        success = await stop_following_signal(
            followed_id,
            current_user["user_id"],
            exit_reason,
            exit_price
        )

        if not success:
            raise HTTPException(status_code=400, detail="Failed to unfollow signal")

        return ApiResponse(
            success=True,
            message="Signal unfollowed successfully"
        )
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/signals/followed/check-opposite", response_model=ApiResponse)
async def check_opposite_signals(
    current_user: dict = Depends(get_current_user_from_token)
):
    """
    Check for opposite signals on user's followed positions
    """
    try:
        opposite_signals = await check_for_opposite_signals(current_user["user_id"])

        return ApiResponse(
            success=True,
            data=opposite_signals,
            message=f"Found {len(opposite_signals)} opposite signals"
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ==================== HEALTH CHECK ====================

@router.get("/health")
async def health_check():
    """
    Check if API is healthy
    """
    return ApiResponse(
        success=True,
        data="API is healthy",
        message="All systems operational"
    )