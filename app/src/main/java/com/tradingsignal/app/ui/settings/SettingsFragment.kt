package com.tradingsignal.app.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.tradingsignal.app.R
import com.tradingsignal.app.TradingSignalApplication
import com.tradingsignal.app.data.model.UserConfig
import com.tradingsignal.app.databinding.FragmentSettingsBinding
import com.tradingsignal.app.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceManager: PreferenceManager
    private var userConfig: UserConfig? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as TradingSignalApplication
        preferenceManager = app.preferenceManager

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        userConfig = preferenceManager.userConfig

        userConfig?.let { config ->
            // Scan settings
            binding.switchCrypto.isChecked = config.scanCrypto
            binding.switchForex.isChecked = config.scanForex
            binding.switchAutoScan.isChecked = config.autoScanEnabled

            // Strategy settings
            binding.switchSarSma.isChecked = config.activeStrategies.contains("SAR_SMA")
            binding.switchSupertrend.isChecked = config.activeStrategies.contains("SUPERTREND_MA")

            // Timeframe
            setupTimeframeSpinner(config.timeframe)

            // Scan interval
            binding.editScanInterval.setText(config.scanIntervalMinutes.toString())

            // Crypto coins
            binding.editCryptoCoins.setText(config.cryptoTopCoins.toString())

            // Notifications
            binding.switchNotifications.isChecked = config.notificationsEnabled
        }
    }

    private fun setupTimeframeSpinner(selectedTimeframe: String) {
        val timeframes = arrayOf("1m", "3m", "5m", "15m", "30m", "1h", "4h", "1d")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeframes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerTimeframe.adapter = adapter
        binding.spinnerTimeframe.setSelection(timeframes.indexOf(selectedTimeframe))
    }

    private fun setupClickListeners() {
        // Save button
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        // Reset to defaults
        binding.btnReset.setOnClickListener {
            resetToDefaults()
        }

        // Clear all signals
        binding.btnClearSignals.setOnClickListener {
            showClearSignalsDialog()
        }

        // Export data
        binding.btnExport.setOnClickListener {
            exportData()
        }

        // Theme selector
        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_light -> applyTheme("LIGHT")
                R.id.radio_dark -> applyTheme("DARK")
                R.id.radio_system -> applyTheme("SYSTEM")
            }
        }
    }

    private fun saveSettings() {
        val scanInterval = binding.editScanInterval.text.toString().toIntOrNull() ?: 5
        val cryptoCoins = binding.editCryptoCoins.text.toString().toIntOrNull() ?: 30

        val activeStrategies = mutableListOf<String>()
        if (binding.switchSarSma.isChecked) activeStrategies.add("SAR_SMA")
        if (binding.switchSupertrend.isChecked) activeStrategies.add("SUPERTREND_MA")

        val updatedConfig = UserConfig(
            scanIntervalMinutes = scanInterval,
            activeStrategies = activeStrategies,
            scanCrypto = binding.switchCrypto.isChecked,
            scanForex = binding.switchForex.isChecked,
            timeframe = binding.spinnerTimeframe.selectedItem.toString(),
            cryptoTopCoins = cryptoCoins,
            notificationsEnabled = binding.switchNotifications.isChecked,
            autoScanEnabled = binding.switchAutoScan.isChecked
        )

        preferenceManager.userConfig = updatedConfig
        preferenceManager.notificationsEnabled = updatedConfig.notificationsEnabled
        preferenceManager.autoScanEnabled = updatedConfig.autoScanEnabled

        Snackbar.make(binding.root, "Settings saved successfully!", Snackbar.LENGTH_SHORT).show()
    }

    private fun resetToDefaults() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset to Defaults")
            .setMessage("Are you sure you want to reset all settings to default values?")
            .setPositiveButton("Reset") { _, _ ->
                preferenceManager.userConfig = preferenceManager.getDefaultUserConfig()
                loadSettings()
                Snackbar.make(binding.root, "Settings reset to defaults", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearSignalsDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear All Signals")
            .setMessage("Are you sure you want to delete all saved signals? This cannot be undone.")
            .setPositiveButton("Clear") { _, _ ->
                clearAllSignals()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllSignals() {
        val app = requireActivity().application as TradingSignalApplication
        lifecycleScope.launch(Dispatchers.IO) {
            app.signalRepository.deleteAllSignals()

            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root, "All signals cleared", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun exportData() {
        // TODO: Implement data export functionality
        Snackbar.make(binding.root, "Export feature coming soon", Snackbar.LENGTH_SHORT).show()
    }

    private fun applyTheme(theme: String) {
        preferenceManager.themeMode = theme

        when (theme) {
            "LIGHT" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            )
            "DARK" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
            )
            "SYSTEM" -> androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}