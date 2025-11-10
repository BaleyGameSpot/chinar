package com.tradingsignal.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tradingsignal.app.R
import com.tradingsignal.app.data.model.Signal
import com.tradingsignal.app.databinding.ItemSignalBinding
import com.tradingsignal.app.utils.ColorUtils
import com.tradingsignal.app.utils.FormatUtils

class SignalAdapter(
    private val onItemClick: (Signal) -> Unit,
    private val onFavoriteClick: (Signal) -> Unit,
    private val onFollowSignalClick: (Signal) -> Unit
) : ListAdapter<Signal, SignalAdapter.SignalViewHolder>(SignalDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SignalViewHolder {
        val binding = ItemSignalBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SignalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SignalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SignalViewHolder(
        private val binding: ItemSignalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }

            binding.btnFavorite.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFavoriteClick(getItem(position))
                }
            }

            binding.btnFollowSignal.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onFollowSignalClick(getItem(position))
                }
            }
        }

        fun bind(signal: Signal) {
            binding.apply {
                // Symbol and market type
                textSymbol.text = "${signal.getMarketEmoji()} ${signal.symbol}"

                // Signal type badge
                textSignalType.text = signal.signalType
                textSignalType.setBackgroundColor(ColorUtils.getSignalColor(signal.signalType))

                // Price
                textPrice.text = FormatUtils.formatPrice(signal.price)

                // Strategy
                textStrategy.text = signal.getStrategyDisplayName()
                chipAccuracy.text = signal.accuracy

                // TP and SL
                textTakeProfit.text = "TP1: ${FormatUtils.formatPrice(signal.takeProfit1)}"
                textStopLoss.text = "SL: ${FormatUtils.formatPrice(signal.stopLoss)}"

                // Profit percentage
                val profitPercent = signal.getProfitPercentage()
                textProfitPercent.text = FormatUtils.formatPercent(profitPercent)
                textProfitPercent.setTextColor(ColorUtils.getPercentageColor(profitPercent))

                // Risk-reward ratio
                textRiskReward.text = "R:R ${FormatUtils.formatRiskReward(signal.getRiskRewardRatio())}"

                // Timeframe
                chipTimeframe.text = signal.timeframe

                // Timestamp
                textTimestamp.text = FormatUtils.formatTimeAgo(signal.createdAt)

                // Favorite icon
                val favoriteIcon = if (signal.isFavorite) {
                    R.drawable.ic_favorite_filled
                } else {
                    R.drawable.ic_favorite_border
                }
                btnFavorite.setImageResource(favoriteIcon)

                // Unread indicator
                viewUnreadIndicator.visibility = if (signal.isRead) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }

                // Card background for signal type
                val cardColor = when (signal.signalType) {
                    "LONG" -> ContextCompat.getColor(root.context, R.color.signal_long_light)
                    "SHORT" -> ContextCompat.getColor(root.context, R.color.signal_short_light)
                    else -> ContextCompat.getColor(root.context, R.color.signal_neutral_light)
                }
                cardView.setCardBackgroundColor(cardColor)
            }
        }
    }

    private class SignalDiffCallback : DiffUtil.ItemCallback<Signal>() {
        override fun areItemsTheSame(oldItem: Signal, newItem: Signal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Signal, newItem: Signal): Boolean {
            return oldItem == newItem
        }
    }
}