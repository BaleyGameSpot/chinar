"""
Pydantic Models for API Request/Response Validation
Matches the Android app data models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

# ==================== SIGNAL MODELS ====================

class Conditions(BaseModel):
    condition_1: str
    condition_2: str
    condition_3: str

class Signal(BaseModel):
    id: Optional[int] = None
    symbol: str
    market_type: str  # CRYPTO or FOREX
    strategy: str  # SAR_SMA or SUPERTREND_MA
    signal: str  # LONG or SHORT
    price: float
    tp1: float = Field(..., alias="takeProfit1")
    tp2: float = Field(..., alias="takeProfit2")
    stop_loss: float
    volume: float
    quote_volume: float
    timestamp: str
    timeframe: str
    accuracy: str
    conditions: Conditions
    all_conditions_met: bool
    
    # Optional fields for SAR_SMA strategy
    sma_20: Optional[float] = None
    sma_40: Optional[float] = None
    sar: Optional[float] = None
    
    # Optional fields for SUPERTREND_MA strategy
    ma_value: Optional[float] = None
    ma_type: Optional[str] = None
    trend: Optional[str] = None
    active_band: Optional[float] = None
    
    class Config:
        populate_by_name = True
        json_schema_extra = {
            "example": {
                "symbol": "BTCUSDT",
                "market_type": "CRYPTO",
                "strategy": "SUPERTREND_MA",
                "signal": "LONG",
                "price": 45234.56,
                "tp1": 46500.00,
                "tp2": 47800.00,
                "stop_loss": 44200.00,
                "volume": 1500000.0,
                "quote_volume": 67000000000.0,
                "timestamp": "2024-01-15T10:30:00.000000",
                "timeframe": "5m",
                "accuracy": "75-80%",
                "conditions": {
                    "condition_1": "✅ Trend Changed: BEARISH→BULLISH",
                    "condition_2": "✅ Price Above EMA 100",
                    "condition_3": "✅ SuperTrend Signal Confirmed"
                },
                "all_conditions_met": True
            }
        }

# ==================== SCAN MODELS ====================

class ScanRequest(BaseModel):
    market_types: List[str] = Field(..., description="List of markets to scan: CRYPTO, FOREX")
    strategies: List[str] = Field(..., description="Strategies: SAR_SMA, SUPERTREND_MA")
    timeframe: str = Field("5m", description="Timeframe: 1m, 5m, 15m, 1h, 4h")
    crypto_limit: int = Field(30, ge=10, le=100, description="Number of top crypto coins")
    forex_pairs: Optional[List[str]] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "market_types": ["CRYPTO", "FOREX"],
                "strategies": ["SAR_SMA", "SUPERTREND_MA"],
                "timeframe": "5m",
                "crypto_limit": 30,
                "forex_pairs": ["EURUSD", "GBPUSD", "XAUUSD"]
            }
        }

class ScanResponse(BaseModel):
    success: bool
    signals: List[Signal]
    scan_time: float
    total_symbols_scanned: int
    timestamp: str
    message: Optional[str] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "signals": [],
                "scan_time": 12.5,
                "total_symbols_scanned": 50,
                "timestamp": "2024-01-15T10:30:00.000000",
                "message": "Scan completed successfully"
            }
        }

# ==================== MARKET DATA MODELS ====================

class MarketData(BaseModel):
    symbol: str
    market_type: str
    price: float
    price_change_24h: float
    price_change_percent_24h: float
    volume_24h: float
    high_24h: float
    low_24h: float
    last_update_time: int
    
    class Config:
        json_schema_extra = {
            "example": {
                "symbol": "BTCUSDT",
                "market_type": "CRYPTO",
                "price": 45234.56,
                "price_change_24h": 1234.56,
                "price_change_percent_24h": 2.8,
                "volume_24h": 1500000000.0,
                "high_24h": 46000.00,
                "low_24h": 43500.00,
                "last_update_time": 1705315800
            }
        }

# ==================== CONFIGURATION MODELS ====================

class UserConfig(BaseModel):
    scan_interval_minutes: int = 5
    active_strategies: List[str] = ["SAR_SMA", "SUPERTREND_MA"]
    scan_crypto: bool = True
    scan_forex: bool = True
    timeframe: str = "5m"
    crypto_top_coins: int = 30
    forex_pairs: List[str] = [
        "EURUSD", "GBPUSD", "USDJPY", "XAUUSD",
        "AUDUSD", "USDCAD", "USDCHF", "NZDUSD"
    ]
    notifications_enabled: bool = True
    auto_scan_enabled: bool = False

# ==================== STATISTICS MODELS ====================

class Statistics(BaseModel):
    total_signals: int
    crypto_signals: int
    forex_signals: int
    long_signals: int
    short_signals: int
    sar_sma_signals: int
    supertrend_ma_signals: int
    last_scan_time: Optional[str] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "total_signals": 125,
                "crypto_signals": 80,
                "forex_signals": 45,
                "long_signals": 70,
                "short_signals": 55,
                "sar_sma_signals": 50,
                "supertrend_ma_signals": 75,
                "last_scan_time": "2024-01-15T10:30:00.000000"
            }
        }

# ==================== API RESPONSE WRAPPER ====================

class ApiResponse(BaseModel):
    success: bool
    data: Optional[Any] = None
    message: Optional[str] = None
    error: Optional[str] = None
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "data": {},
                "message": "Operation successful",
                "error": None
            }
        }