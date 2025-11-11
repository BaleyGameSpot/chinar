"""
Screener Wrapper - Imports and exposes the full CryptoForexScreener from screener.py
This provides the complete implementation with real TradingView and Binance API integration
"""

import sys
import os

# Import the full screener implementation
try:
    from screener import CryptoForexScreener
    print("✅ Successfully imported full CryptoForexScreener from screener.py")
except ImportError as e:
    print(f"❌ Error importing screener: {e}")
    print("⚠️  Make sure screener.py is in the same directory")
    raise

# Export the class for use by scanner_service
__all__ = ['CryptoForexScreener']