"""
Scanner Service - Wraps the Python screener logic
Provides async interface for the FastAPI backend
"""

import asyncio
from typing import List, Optional
from datetime import datetime
import time
import sys
import os

# Add the parent directory to path to import screener
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from api.models import ScanRequest, ScanResponse, Signal, MarketData, UserConfig
from screener_wrapper import CryptoForexScreener

class ScannerService:
    """
    Service layer that wraps the Python screener
    Provides async methods for the API
    """
    
    def __init__(self):
        self.screener = None
        self.config = None
        self._initialize_screener()
    
    def _initialize_screener(self):
        """Initialize the screener with default config"""
        try:
            self.screener = CryptoForexScreener()
            self.config = self.screener.config
            print("✅ Screener initialized successfully")
        except Exception as e:
            print(f"❌ Failed to initialize screener: {e}")
            raise
    
    async def perform_scan(self, request: ScanRequest) -> ScanResponse:
        """
        Perform a scan based on the request parameters
        Returns scan response with signals
        """
        start_time = time.time()
        
        try:
            # Update screener config based on request
            self._update_screener_config(request)
            
            # Run scan in executor to avoid blocking
            loop = asyncio.get_event_loop()
            signals = await loop.run_in_executor(
                None,
                self._run_scan,
                request
            )
            
            scan_time = time.time() - start_time
            
            # Convert to Signal models
            signal_models = [self._convert_to_signal_model(s) for s in signals]
            
            # Count total symbols scanned
            total_symbols = 0
            if "CRYPTO" in request.market_types:
                total_symbols += request.crypto_limit
            if "FOREX" in request.market_types:
                total_symbols += len(request.forex_pairs) if request.forex_pairs else 8
            
            return ScanResponse(
                success=True,
                signals=signal_models,
                scan_time=round(scan_time, 2),
                total_symbols_scanned=total_symbols,
                timestamp=datetime.now().isoformat(),
                message=f"Found {len(signal_models)} signals"
            )
            
        except Exception as e:
            print(f"❌ Scan error: {str(e)}")
            return ScanResponse(
                success=False,
                signals=[],
                scan_time=time.time() - start_time,
                total_symbols_scanned=0,
                timestamp=datetime.now().isoformat(),
                message=f"Scan failed: {str(e)}"
            )
    
    def _update_screener_config(self, request: ScanRequest):
        """Update screener configuration based on scan request"""
        self.screener.config["scanning"]["scan_crypto"] = "CRYPTO" in request.market_types
        self.screener.config["scanning"]["scan_forex"] = "FOREX" in request.market_types
        self.screener.config["scanning"]["active_strategies"] = request.strategies
        self.screener.config["scanning"]["timeframe"] = request.timeframe
        self.screener.config["scanning"]["crypto_top_coins"] = request.crypto_limit
        
        if request.forex_pairs:
            self.screener.config["scanning"]["forex_pairs"] = request.forex_pairs
    
    def _run_scan(self, request: ScanRequest) -> List[dict]:
        """
        Run the actual scan (synchronous)
        This runs in a separate thread via run_in_executor
        """
        all_signals = []
        
        # Get symbols to scan
        crypto_symbols = []
        forex_symbols = []
        
        if "CRYPTO" in request.market_types:
            crypto_symbols = self.screener.get_crypto_top_coins(request.crypto_limit)
        
        if "FOREX" in request.market_types:
            forex_symbols = request.forex_pairs if request.forex_pairs else self.screener.get_forex_pairs()
        
        # Scan crypto
        if crypto_symbols:
            for symbol in crypto_symbols:
                for strategy in request.strategies:
                    signal = None
                    
                    if strategy == "SAR_SMA":
                        signal = self.screener.check_sar_sma_strategy(symbol, "CRYPTO")
                    elif strategy == "SUPERTREND_MA":
                        signal = self.screener.check_supertrend_ma_strategy(symbol, "CRYPTO")
                    
                    if signal:
                        all_signals.append(signal)
                
                time.sleep(0.05)  # Rate limiting
        
        # Scan forex
        if forex_symbols:
            for symbol in forex_symbols:
                for strategy in request.strategies:
                    signal = None
                    
                    if strategy == "SAR_SMA":
                        signal = self.screener.check_sar_sma_strategy(symbol, "FOREX")
                    elif strategy == "SUPERTREND_MA":
                        signal = self.screener.check_supertrend_ma_strategy(symbol, "FOREX")
                    
                    if signal:
                        all_signals.append(signal)
                
                time.sleep(0.05)  # Rate limiting
        
        return all_signals
    
    def _convert_to_signal_model(self, signal_dict: dict) -> Signal:
        """Convert screener signal dict to Signal model"""
        from api.models import Conditions
        
        return Signal(
            symbol=signal_dict["symbol"],
            market_type=signal_dict["market_type"],
            strategy=signal_dict["strategy"],
            signal=signal_dict["signal"],
            price=signal_dict["price"],
            takeProfit1=signal_dict["tp1"],
            takeProfit2=signal_dict["tp2"],
            stop_loss=signal_dict["stop_loss"],
            volume=signal_dict["volume"],
            quote_volume=signal_dict["quote_volume"],
            timestamp=signal_dict["timestamp"],
            timeframe=signal_dict["timeframe"],
            accuracy=signal_dict["accuracy"],
            conditions=Conditions(**signal_dict["conditions"]),
            all_conditions_met=signal_dict["all_conditions_met"],
            # Optional fields
            sma_20=signal_dict.get("sma_20"),
            sma_40=signal_dict.get("sma_40"),
            sar=signal_dict.get("sar"),
            ma_value=signal_dict.get("ma_value"),
            ma_type=signal_dict.get("ma_type"),
            trend=signal_dict.get("trend"),
            active_band=signal_dict.get("active_band")
        )
    
    async def get_signal_by_id(self, signal_id: int) -> Optional[Signal]:
        """Get a signal by ID from database"""
        # TODO: Implement database query
        return None
    
    async def get_signals_since(self, cutoff_time: datetime) -> List[Signal]:
        """Get signals since a specific time"""
        # TODO: Implement database query
        return []
    
    async def get_signals_by_market(self, market_type: str) -> List[Signal]:
        """Get signals filtered by market type"""
        # TODO: Implement database query
        return []
    
    async def get_signals_by_strategy(self, strategy: str) -> List[Signal]:
        """Get signals filtered by strategy"""
        # TODO: Implement database query
        return []
    
    async def get_market_data(self, symbols: Optional[List[str]] = None) -> List[MarketData]:
        """Get current market data for symbols"""
        # TODO: Implement market data fetching
        return []
    
    async def get_current_price(self, symbol: str) -> Optional[MarketData]:
        """Get current price for a symbol"""
        # TODO: Implement price fetching
        return None
    
    async def get_config(self) -> UserConfig:
        """Get current user configuration"""
        return UserConfig(**{
            "scan_interval_minutes": self.config["scanning"]["interval_minutes"],
            "active_strategies": self.config["scanning"]["active_strategies"],
            "scan_crypto": self.config["scanning"]["scan_crypto"],
            "scan_forex": self.config["scanning"]["scan_forex"],
            "timeframe": self.config["scanning"]["timeframe"],
            "crypto_top_coins": self.config["scanning"]["crypto_top_coins"],
            "forex_pairs": self.config["scanning"]["forex_pairs"],
            "notifications_enabled": True,
            "auto_scan_enabled": False
        })
    
    async def update_config(self, config: UserConfig) -> UserConfig:
        """Update user configuration"""
        self.config["scanning"]["interval_minutes"] = config.scan_interval_minutes
        self.config["scanning"]["active_strategies"] = config.active_strategies
        self.config["scanning"]["scan_crypto"] = config.scan_crypto
        self.config["scanning"]["scan_forex"] = config.scan_forex
        self.config["scanning"]["timeframe"] = config.timeframe
        self.config["scanning"]["crypto_top_coins"] = config.crypto_top_coins
        self.config["scanning"]["forex_pairs"] = config.forex_pairs
        
        # Save to file
        self.screener.update_config(self.config)
        
        return config