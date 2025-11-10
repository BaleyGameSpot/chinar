package com.tradingsignal.app.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.tradingsignal.app.R
import com.tradingsignal.app.databinding.ActivitySplashBinding
import com.tradingsignal.app.ui.main.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDelay = 2500L // 2.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start animations
        startAnimations()

        // Navigate to MainActivity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            // Add smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, splashDelay)
    }

    private fun startAnimations() {
        // Fade in animation for logo container
        binding.logoContainer.alpha = 0f
        binding.logoContainer.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(200)
            .start()

        // Fade in and slide up for app name
        binding.appName.alpha = 0f
        binding.appName.translationY = 50f
        binding.appName.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(400)
            .start()

        // Fade in for tagline
        binding.tagline.alpha = 0f
        binding.tagline.animate()
            .alpha(0.95f)
            .setDuration(800)
            .setStartDelay(600)
            .start()

        // Fade in for features
        binding.featuresContainer.alpha = 0f
        binding.featuresContainer.animate()
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(800)
            .start()

        // Pulse animation for progress bar
        binding.progressBar.alpha = 0f
        binding.progressBar.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(1000)
            .start()

        // Fade in for loading text
        binding.loadingText.alpha = 0f
        binding.loadingText.animate()
            .alpha(0.8f)
            .setDuration(600)
            .setStartDelay(1000)
            .start()
    }
}
