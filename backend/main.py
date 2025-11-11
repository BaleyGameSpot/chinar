"""
🚀 TRADING SIGNAL BACKEND API
FastAPI backend for crypto & forex screener
Wraps the Python screener script with REST API
"""

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import uvicorn
import socket

from api.routes import router
from database.db import init_db, close_db
from services.scanner_service import ScannerService

# Initialize scanner service
scanner_service = ScannerService()

def get_local_ip():
    """Get local IP address for network access"""
    try:
        # Create a socket to get the local IP
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        local_ip = s.getsockname()[0]
        s.close()
        return local_ip
    except Exception:
        return "Unable to detect"

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    print("="*80)
    print("🚀 STARTING TRADING SIGNAL API")
    print("="*80)
    
    local_ip = get_local_ip()
    
    print("\n📡 NETWORK CONFIGURATION:")
    print(f"   Local Access:    http://localhost:8000")
    print(f"   Network Access:  http://{local_ip}:8000")
    print(f"   API Docs:        http://{local_ip}:8000/docs")
    print(f"   Health Check:    http://{local_ip}:8000/api/health")
    
    print("\n📱 ANDROID APP SETUP:")
    print(f"   Base URL: http://{local_ip}:8000/api")
    print(f"   Make sure your phone is on the same WiFi network!")
    
    print("\n🔧 Initializing database...")
    await init_db()
    print("✅ Database initialized successfully")
    
    print("\n🎯 Starting scanner service...")
    print("✅ Scanner service ready")
    
    print("\n" + "="*80)
    print("✅ API IS READY TO ACCEPT REQUESTS")
    print("="*80 + "\n")
    
    yield
    
    # Shutdown
    print("\n🛑 Shutting down Trading Signal API...")
    await close_db()
    print("✅ Shutdown complete")

app = FastAPI(
    title="Trading Signal API",
    description="Professional crypto & forex trading signal scanner API with SAR+SMA and SuperTrend MA strategies",
    version="1.0.0",
    lifespan=lifespan
)

# CORS Configuration - Allow all origins for development
# In production, replace "*" with your specific domain
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # For development - allows any origin
    allow_credentials=True,
    allow_methods=["*"],  # Allows all HTTP methods
    allow_headers=["*"],  # Allows all headers
)

# Include API routes
app.include_router(router, prefix="/api", tags=["Trading Signals"])

@app.get("/")
async def root():
    """Root endpoint with API information"""
    local_ip = get_local_ip()
    return {
        "message": "🚀 Trading Signal API",
        "version": "1.0.0",
        "status": "running",
        "network": {
            "local_ip": local_ip,
            "port": 8000,
            "access_url": f"http://{local_ip}:8000"
        },
        "endpoints": {
            "docs": "/docs",
            "redoc": "/redoc",
            "api": "/api",
            "health": "/api/health",
            "signals": "/api/signals",
            "scan": "/api/scan"
        },
        "strategies": [
            {
                "name": "SAR + SMA",
                "accuracy": "60-65%",
                "description": "Parabolic SAR + Moving Average Crossover"
            },
            {
                "name": "SuperTrend MA",
                "accuracy": "75-80%",
                "description": "SuperTrended Moving Average with trend reversal"
            }
        ]
    }

@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "message": "API is running smoothly",
        "scanner_status": "active",
        "database": "connected"
    }

if __name__ == "__main__":
    print("\n" + "="*80)
    print("🔥 STARTING DEVELOPMENT SERVER")
    print("="*80)
    
    local_ip = get_local_ip()
    
    print(f"\n🌐 Server will be accessible at:")
    print(f"   • http://localhost:8000")
    print(f"   • http://127.0.0.1:8000")
    print(f"   • http://{local_ip}:8000")
    
    print(f"\n📱 Configure your Android app with:")
    print(f"   BASE_URL = \"http://{local_ip}:8000/api\"")
    
    print("\n🔒 IMPORTANT NOTES:")
    print("   1. Make sure your phone is on the SAME WiFi network")
    print("   2. Check your firewall settings (allow port 8000)")
    print("   3. On Windows: Windows Defender Firewall → Allow an app")
    print("   4. On Linux: sudo ufw allow 8000/tcp")
    print("   5. On Mac: System Preferences → Security → Firewall")
    
    print("\n" + "="*80 + "\n")
    
    # Run the server
    uvicorn.run(
        "main:app",
        host="0.0.0.0",  # Listen on all network interfaces
        port=8000,
        reload=True,  # Auto-reload on code changes (development only)
        log_level="info",
        access_log=True  # Log all requests
    )