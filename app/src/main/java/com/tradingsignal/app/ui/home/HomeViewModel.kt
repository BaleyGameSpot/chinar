package com.tradingsignal.app.ui.home

import androidx.lifecycle.*
import com.tradingsignal.app.data.model.ScanRequest
import com.tradingsignal.app.data.model.Signal
import com.tradingsignal.app.data.model.Statistics
import com.tradingsignal.app.data.repository.SignalRepository
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: SignalRepository
) : ViewModel() {

    // LiveData for signals
    private val _allSignals = repository.getAllSignals()
    private val _currentFilter = MutableLiveData<String?>(null)
    private val _showFavorites = MutableLiveData(false)

    val signals: LiveData<List<Signal>> = _currentFilter.switchMap { filter ->
        _showFavorites.switchMap { showFavorites ->
            when {
                showFavorites -> repository.getFavoriteSignals()
                filter != null -> repository.getSignalsByMarket(filter)
                else -> _allSignals
            }
        }
    }

    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Scanning state
    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    // Messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    // Statistics
    private val _statistics = MutableLiveData<Statistics?>()
    val statistics: LiveData<Statistics?> = _statistics

    init {
        loadStatistics()
    }

    // Filter signals
    fun filterSignals(marketType: String?) {
        _currentFilter.value = marketType
        _showFavorites.value = false
    }

    // Show favorites only
    fun showFavoritesOnly() {
        _showFavorites.value = true
        _currentFilter.value = null
    }

    // Perform quick scan (both markets, all strategies)
    fun performQuickScan() {
        val scanRequest = ScanRequest(
            marketTypes = listOf("CRYPTO", "FOREX"),
            strategies = listOf("SAR_SMA", "SUPERTREND_MA"),
            timeframe = "5m",
            cryptoLimit = 30
        )

        performScan(scanRequest)
    }

    // Perform crypto-only scan
    fun performCryptoScan() {
        val scanRequest = ScanRequest(
            marketTypes = listOf("CRYPTO"),
            strategies = listOf("SAR_SMA", "SUPERTREND_MA"),
            timeframe = "5m",
            cryptoLimit = 50
        )

        performScan(scanRequest)
    }

    // Perform forex-only scan
    fun performForexScan() {
        val scanRequest = ScanRequest(
            marketTypes = listOf("FOREX"),
            strategies = listOf("SAR_SMA", "SUPERTREND_MA"),
            timeframe = "5m"
        )

        performScan(scanRequest)
    }

    // Generic scan function
    private fun performScan(scanRequest: ScanRequest) {
        _isScanning.value = true

        viewModelScope.launch {
            val result = repository.performScan(scanRequest)

            result.onSuccess { scanResponse ->
                _successMessage.value = "Scan complete! Found ${scanResponse.signals.size} signals"
                loadStatistics()
            }.onFailure { exception ->
                _errorMessage.value = "Scan failed: ${exception.message}"
            }

            _isScanning.value = false
        }
    }

    // Sync signals from server
    fun syncSignals() {
        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.syncSignals()

            result.onSuccess { signals ->
                _successMessage.value = "Synced ${signals.size} signals"
                loadStatistics()
            }.onFailure { exception ->
                _errorMessage.value = "Sync failed: ${exception.message}"
            }

            _isLoading.value = false
        }
    }

    // Load statistics
    fun loadStatistics() {
        viewModelScope.launch {
            val result = repository.getStatistics()

            result.onSuccess { stats ->
                _statistics.value = stats
            }.onFailure {
                // Silently fail for statistics
            }
        }
    }

    // Toggle favorite
    fun toggleFavorite(signal: Signal) {
        viewModelScope.launch {
            repository.toggleFavorite(signal.id, !signal.isFavorite)
        }
    }

    // Mark signal as read
    fun markAsRead(signal: Signal) {
        viewModelScope.launch {
            repository.markSignalAsRead(signal.id)
        }
    }

    // Clear messages
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}

// ViewModel Factory
class HomeViewModelFactory(
    private val repository: SignalRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}