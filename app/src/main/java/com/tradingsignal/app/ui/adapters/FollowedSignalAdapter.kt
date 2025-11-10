package com.tradingsignal.app.ui.adapters

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tradingsignal.app.R
import com.tradingsignal.app.data.model.FollowedSignal
import com.tradingsignal.app.databinding.ItemFollowedSignalBinding
import com.tradingsignal.app.utils.FormatUtils

class FollowedSignalAdapter(
    private val onItemClick: (FollowedSignal) -> Unit,
    private val onStopFollowingClick: (FollowedSignal) -> Unit
) : ListAdapter<FollowedSignal, FollowedSignalAdapter.FollowedSignalViewHolder>(FollowedSignalDiffCallback()) {

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            // Update all visible items
            notifyDataSetChanged()
            // Schedule next update in 1 second
            handler.postDelayed(this, 1000)
        }
    }

    init {
        // Start the timer
        handler.post(timerRunnable)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowedSignalViewHolder {
        val binding = ItemFollowedSignalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FollowedSignalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowedSignalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Stop the timer when adapter is detached
        handler.removeCallbacks(timerRunnable)
    }

    inner class FollowedSignalViewHolder(
        private val binding: ItemFollowedSignalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnStopFollowing.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onStopFollowingClick(getItem(position))
                }
            }
        }

        fun bind(followedSignal: FollowedSignal) {
            binding.apply {
                // Symbol
                val emoji = when (followedSignal.marketType) {
                    "CRYPTO" -> "💰"
                    "FOREX" -> "💱"
                    else -> "📊"
                }
                textSymbol.text = "$emoji ${followedSignal.symbol}"

                // Signal type badge
                textSignalType.text = followedSignal.signalType
                val signalBackground = when (followedSignal.signalType) {
                    "LONG" -> R.drawable.bg_signal_badge_long
                    "SHORT" -> R.drawable.bg_signal_badge_short
                    else -> R.drawable.bg_signal_badge_long
                }
                textSignalType.setBackgroundResource(signalBackground)

                // Strategy
                val strategyName = when (followedSignal.strategy) {
                    "SAR_SMA" -> "SAR + SMA"
                    "SUPERTREND_MA" -> "SuperTrend MA"
                    else -> followedSignal.strategy
                }
                textStrategy.text = strategyName

                // Timer - Update elapsed time
                textTimer.text = followedSignal.getFormattedElapsedTime()

                // Status
                textStatus.text = if (!followedSignal.oppositeSignalDetected) {
                    "✓ Following - Watching for signals..."
                } else {
                    followedSignal.getStatusDescription()
                }

                val statusColor = if (followedSignal.oppositeSignalDetected) {
                    ContextCompat.getColor(root.context, R.color.signal_short)
                } else {
                    ContextCompat.getColor(root.context, R.color.signal_long)
                }
                textStatus.setTextColor(statusColor)

                // Warning section (shown when opposite signal detected)
                if (followedSignal.oppositeSignalDetected && followedSignal.isActive) {
                    warningContainer.visibility = View.VISIBLE
                    textWarning.text = "⚠️ OPPOSITE ${followedSignal.getOppositeSignalType()} SIGNAL DETECTED"

                    val action = when (followedSignal.signalType) {
                        "LONG" -> "SELL NOW"
                        "SHORT" -> "BUY NOW"
                        else -> "EXIT NOW"
                    }
                    textAction.text = action
                } else {
                    warningContainer.visibility = View.GONE
                }

                // Entry price
                textEntryPrice.text = FormatUtils.formatPrice(followedSignal.entryPrice)

                // Take profit
                textTakeProfit.text = "TP: ${FormatUtils.formatPrice(followedSignal.takeProfit1)}"
                val tpDrawable = if (followedSignal.signalType == "LONG") {
                    R.drawable.ic_arrow_up
                } else {
                    R.drawable.ic_arrow_down
                }
                textTakeProfit.setCompoundDrawablesRelativeWithIntrinsicBounds(tpDrawable, 0, 0, 0)

                // Stop loss
                textStopLoss.text = "SL: ${FormatUtils.formatPrice(followedSignal.stopLoss)}"
                val slDrawable = if (followedSignal.signalType == "LONG") {
                    R.drawable.ic_arrow_down
                } else {
                    R.drawable.ic_arrow_up
                }
                textStopLoss.setCompoundDrawablesRelativeWithIntrinsicBounds(slDrawable, 0, 0, 0)

                // Active indicator color
                val indicatorColor = if (followedSignal.oppositeSignalDetected) {
                    ContextCompat.getColor(root.context, R.color.signal_short)
                } else {
                    ContextCompat.getColor(root.context, R.color.signal_long)
                }
                viewActiveIndicator.backgroundTintList = android.content.res.ColorStateList.valueOf(indicatorColor)

                // Card stroke color
                val strokeColor = if (followedSignal.oppositeSignalDetected) {
                    ContextCompat.getColor(root.context, R.color.signal_short)
                } else {
                    ContextCompat.getColor(root.context, R.color.accent)
                }
                cardView.strokeColor = strokeColor
            }
        }
    }

    private class FollowedSignalDiffCallback : DiffUtil.ItemCallback<FollowedSignal>() {
        override fun areItemsTheSame(oldItem: FollowedSignal, newItem: FollowedSignal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FollowedSignal, newItem: FollowedSignal): Boolean {
            return oldItem == newItem
        }
    }
}
