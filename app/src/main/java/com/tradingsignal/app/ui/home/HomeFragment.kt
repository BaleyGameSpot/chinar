package com.tradingsignal.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tradingsignal.app.R
import com.tradingsignal.app.databinding.FragmentHomeBinding
import com.tradingsignal.app.ui.adapters.SignalAdapter
import com.tradingsignal.app.utils.FormatUtils

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((requireActivity().application as com.tradingsignal.app.TradingSignalApplication).signalRepository)
    }

    private lateinit var signalAdapter: SignalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        signalAdapter = SignalAdapter(
            onItemClick = { signal ->
                // Navigate to signal detail - will be implemented when navigation is set up
                // val action = HomeFragmentDirections.actionHomeToSignalDetail(signal)
                // findNavController().navigate(action)
                viewModel.markAsRead(signal)
            },
            onFavoriteClick = { signal ->
                viewModel.toggleFavorite(signal)
            }
        )

        binding.recyclerViewSignals.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = signalAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupClickListeners() {
        // Scan button
        binding.btnScan.setOnClickListener {
            showScanOptionsDialog()
        }

        // Filter buttons
        binding.chipAll.setOnClickListener {
            viewModel.filterSignals(null)
        }

        binding.chipCrypto.setOnClickListener {
            viewModel.filterSignals("CRYPTO")
        }

        binding.chipForex.setOnClickListener {
            viewModel.filterSignals("FOREX")
        }

        binding.chipFavorites.setOnClickListener {
            viewModel.showFavoritesOnly()
        }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.syncSignals()
        }
    }

    private fun observeViewModel() {
        // Observe signals
        viewModel.signals.observe(viewLifecycleOwner) { signals ->
            signalAdapter.submitList(signals)
            updateEmptyState(signals.isEmpty())
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observe scan state
        viewModel.isScanning.observe(viewLifecycleOwner) { isScanning ->
            binding.btnScan.isEnabled = !isScanning
            binding.btnScan.text = if (isScanning) "Scanning..." else "Start Scan"
        }

        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }

        // Observe success messages
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_SHORT).show()
                viewModel.clearSuccessMessage()
            }
        }

        // Observe statistics
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                binding.textTotalSignals.text = "${it.totalSignals}"
                binding.textCryptoSignals.text = "${it.cryptoSignals}"
                binding.textForexSignals.text = "${it.forexSignals}"

                it.lastScanTime?.let { time ->
                    binding.textLastScan.text = "Last scan: ${FormatUtils.formatTimestamp(time)}"
                }
            }
        }
    }

    private fun showScanOptionsDialog() {
        val options = arrayOf(
            "Quick Scan (Both Markets)",
            "Crypto Only",
            "Forex Only",
            "Custom Scan"
        )

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Scan Type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.performQuickScan()
                    1 -> viewModel.performCryptoScan()
                    2 -> viewModel.performForexScan()
                    3 -> showCustomScanDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCustomScanDialog() {
        // TODO: Show custom scan configuration dialog
        Snackbar.make(binding.root, "Custom scan coming soon", Snackbar.LENGTH_SHORT).show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerViewSignals.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.recyclerViewSignals.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}