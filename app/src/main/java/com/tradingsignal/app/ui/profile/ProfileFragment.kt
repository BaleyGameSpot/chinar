package com.tradingsignal.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tradingsignal.app.TradingSignalApplication
import com.tradingsignal.app.data.model.Subscription
import com.tradingsignal.app.databinding.FragmentProfileBinding
import com.tradingsignal.app.utils.PreferenceManager

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferenceManager: PreferenceManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as TradingSignalApplication
        preferenceManager = app.preferenceManager

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Display subscription info
        val subscription = getCurrentSubscription()

        binding.textSubscriptionTier.text = subscription.tier
        binding.textSubscriptionStatus.text = if (subscription.isActive) {
            "Active"
        } else {
            "Inactive"
        }

        // Display features
        val featuresText = subscription.features.joinToString("\n• ", "• ")
        binding.textFeatures.text = featuresText

        // Display scan count
        binding.textScanCount.text = "${preferenceManager.scanCount} scans performed"
    }

    private fun setupClickListeners() {
        // Upgrade subscription
        binding.btnUpgrade.setOnClickListener {
            showSubscriptionOptions()
        }

        // About section
        binding.cardAbout.setOnClickListener {
            showAboutDialog()
        }

        // Privacy policy
        binding.cardPrivacy.setOnClickListener {
            // TODO: Open privacy policy
        }

        // Terms of service
        binding.cardTerms.setOnClickListener {
            // TODO: Open terms of service
        }
    }

    private fun getCurrentSubscription(): Subscription {
        return when (preferenceManager.subscriptionTier) {
            "BASIC" -> Subscription.getBasicSubscription()
            "PREMIUM" -> Subscription.getPremiumSubscription()
            else -> Subscription.getFreeSubscription()
        }
    }

    private fun showSubscriptionOptions() {
        val options = arrayOf("Basic Plan", "Premium Plan")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Choose Subscription")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> purchaseBasicSubscription()
                    1 -> purchasePremiumSubscription()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun purchaseBasicSubscription() {
        // TODO: Implement payment flow
        preferenceManager.subscriptionTier = "BASIC"
        setupUI()
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Basic subscription activated!",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun purchasePremiumSubscription() {
        // TODO: Implement payment flow
        preferenceManager.subscriptionTier = "PREMIUM"
        setupUI()
        com.google.android.material.snackbar.Snackbar.make(
            binding.root,
            "Premium subscription activated!",
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("About Trading Signal App")
            .setMessage("""
                Version: 1.0.0
                
                Professional crypto & forex trading signal scanner with 2 advanced strategies:
                • SAR + SMA Strategy (60-65% accuracy)
                • SuperTrend MA Strategy (75-80% accuracy)
                
                Supports 100+ crypto pairs and major forex markets.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}