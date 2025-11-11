"""
Screener Wrapper - Modified version of the Python screener for backend integration
This wraps the original screener.py with simplified interface for API use
"""

import sys
import os

# Copy the screener.py to this directory and import CryptoForexScreener from it
# For now, we'll create a simplified version that can be used

import requests
import pandas as pd
import numpy as np
import time
from datetime import datetime
from typing import Dict, List, Optional
import warnings
warnings.filterwarnings('ignore')

class CryptoForexScreener:
    """
    Simplified Crypto & Forex Screener for API backend
    Based on the original screener.py
    """
    
    def __init__(self):
        self.crypto_base_url = "https://api.binance.com/api/v3"
        self.config = self._get_default_config()
        print("✅ Screener initialized")
    
    def _get_default_config(self) -> Dict:
        """Get default configuration"""
        return {
            "scanning": {
                "interval_minutes": 5,
                "timeframe": "5m",
                "min_volume": 500000,
                "active_strategies": ["SAR_SMA", "SUPERTREND_MA"],
                "scan_crypto": True,
                "scan_forex": True,
                "crypto_top_coins": 30,
                "forex_pairs": ["XAUUSD", "EURUSD", "GBPUSD", "USDJPY", "AUDUSD", "USDCAD", "USDCHF", "NZDUSD"]
            },
            "sar_sma_strategy": {
                "sar_start": 0.02,
                "sar_increment": 0.02,
                "sar_max": 0.2,
                "sma_fast": 20,
                "sma_slow": 40
            },
            "supertrend_ma_strategy": {
                "ma_type": "EMA",
                "ma_length": 100,
                "atr_period": 10,
                "atr_multiplier": 0.5,
                "change_atr": True,
                "show_signals": True
            }
        }
    
    def update_config(self, config: Dict):
        """Update configuration"""
        self.config = config
    
    def get_crypto_top_coins(self, limit: int = 30) -> List[str]:
        """Get top crypto coins by 24h volume"""
        try:
            url = f"{self.crypto_base_url}/ticker/24hr"
            response = requests.get(url, timeout=10)
            response.raise_for_status()
            data = response.json()
            
            # Filter USDT pairs with minimum volume
            min_volume = self.config["scanning"]["min_volume"]
            usdt_pairs = [
                coin for coin in data 
                if coin['symbol'].endswith('USDT') 
                and float(coin['quoteVolume']) > min_volume
            ]
            
            # Sort by volume and return top coins
            sorted_coins = sorted(usdt_pairs, key=lambda x: float(x['quoteVolume']), reverse=True)
            return [coin['symbol'] for coin in sorted_coins[:limit]]
            
        except Exception as e:
            print(f"Error fetching crypto list: {e}")
            return self._get_default_crypto_coins()
    
    def _get_default_crypto_coins(self) -> List[str]:
        """Fallback crypto coin list"""
        return [
            'BTCUSDT', 'ETHUSDT', 'ADAUSDT', 'SOLUSDT', 'XRPUSDT',
            'DOGEUSDT', 'AVAXUSDT', 'MATICUSDT', 'LINKUSDT', 'UNIUSDT',
            'LTCUSDT', 'BCHUSDT', 'XLMUSDT', 'VETUSDT', 'FILUSDT',
            'DOTUSDT', 'TRXUSDT', 'EOSUSDT', 'XMRUSDT', 'NEARUSDT'
        ]
    
    def get_forex_pairs(self) -> List[str]:
        """Get forex pairs to scan"""
        return self.config["scanning"]["forex_pairs"]
    
    def get_crypto_klines(self, symbol: str, interval: str = "5m", limit: int = 200) -> Optional[pd.DataFrame]:
        """Get crypto candlestick data from Binance"""
        url = f"{self.crypto_base_url}/klines"
        params = {
            'symbol': symbol,
            'interval': interval,
            'limit': limit
        }
        
        try:
            response = requests.get(url, params=params, timeout=10)
            response.raise_for_status()
            data = response.json()
            
            df = pd.DataFrame(data, columns=[
                'timestamp', 'open', 'high', 'low', 'close', 'volume',
                'close_time', 'quote_asset_volume', 'number_of_trades',
                'taker_buy_base_asset_volume', 'taker_buy_quote_asset_volume', 'ignore'
            ])
            
            # Convert to numeric
            numeric_columns = ['open', 'high', 'low', 'close', 'volume', 'quote_asset_volume']
            for col in numeric_columns:
                df[col] = pd.to_numeric(df[col], errors='coerce')
                
            df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')
            df['market_type'] = 'CRYPTO'
            
            return df
            
        except Exception as e:
            print(f"Error fetching crypto data for {symbol}: {e}")
            return None
    
    def get_forex_klines(self, symbol: str, interval: str = "5m", limit: int = 200) -> Optional[pd.DataFrame]:
        """Get forex data using yfinance"""
        try:
            import yfinance as yf
            
            # Convert to TradingView format
            tv_symbol = self._convert_to_tradingview_symbol(symbol)
            
            # Convert interval
            yf_interval = {
                "1m": "1m", "5m": "5m", "15m": "15m",
                "1h": "1h", "4h": "4h", "1d": "1d"
            }.get(interval, "5m")
            
            # Fetch data
            ticker = yf.Ticker(tv_symbol)
            hist = ticker.history(period="7d", interval=yf_interval)
            
            if hist.empty:
                return None
            
            # Convert to our format
            df = hist.reset_index()
            df.columns = [col.lower() for col in df.columns]
            
            if 'datetime' not in df.columns:
                if 'date' in df.columns:
                    df['datetime'] = df['date']
            
            df['quote_asset_volume'] = df.get('volume', 1000000)
            df['timestamp'] = pd.to_datetime(df['datetime']).astype(int) // 10**6
            df['market_type'] = 'FOREX'
            
            return df.tail(limit)
            
        except Exception as e:
            print(f"Error fetching forex data for {symbol}: {e}")
            return None
    
    def _convert_to_tradingview_symbol(self, symbol: str) -> str:
        """Convert symbol to TradingView format"""
        symbol_map = {
            'EURUSD': 'EURUSD=X', 'GBPUSD': 'GBPUSD=X', 'USDJPY': 'USDJPY=X',
            'USDCHF': 'USDCHF=X', 'USDCAD': 'USDCAD=X', 'AUDUSD': 'AUDUSD=X',
            'NZDUSD': 'NZDUSD=X', 'XAUUSD': 'GC=F'
        }
        return symbol_map.get(symbol.upper(), f"{symbol}=X")
    
    def check_sar_sma_strategy(self, symbol: str, market_type: str = "CRYPTO") -> Optional[Dict]:
        """Check SAR + SMA Strategy - SIMPLIFIED for demo"""
        # For demo purposes, return None (no signals)
        # In production, implement full strategy logic from screener.py
        return None
    
    def check_supertrend_ma_strategy(self, symbol: str, market_type: str = "CRYPTO") -> Optional[Dict]:
        """Check SuperTrend MA Strategy - SIMPLIFIED for demo"""
        # For demo purposes, return a sample signal occasionally
        # In production, implement full strategy logic from screener.py
        
        # Return sample signal 5% of the time for testing
        if np.random.random() < 0.05:
            return {
                "symbol": symbol,
                "market_type": market_type,
                "strategy": "SUPERTREND_MA",
                "signal": "LONG" if np.random.random() > 0.5 else "SHORT",
                "price": 45234.56 if market_type == "CRYPTO" else 1.0850,
                "tp1": 46500.00 if market_type == "CRYPTO" else 1.0950,
                "tp2": 47800.00 if market_type == "CRYPTO" else 1.1050,
                "stop_loss": 44200.00 if market_type == "CRYPTO" else 1.0750,
                "volume": 1500000.0,
                "quote_volume": 67000000000.0,
                "timestamp": datetime.now().isoformat(),
                "timeframe": "5m",
                "accuracy": "75-80%",
                "conditions": {
                    "condition_1": "✅ Trend Changed: BEARISH→BULLISH",
                    "condition_2": "✅ Price Above EMA 100",
                    "condition_3": "✅ SuperTrend Signal Confirmed"
                },
                "all_conditions_met": True,
                "ma_value": 45000.00,
                "ma_type": "EMA",
                "trend": "BULLISH",
                "active_band": 44800.00
            }
        
        return None


# NOTE: For production, copy the full implementation from screener.py
# This is a simplified version for testing the backend API