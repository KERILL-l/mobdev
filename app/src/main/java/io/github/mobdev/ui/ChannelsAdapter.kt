package io.github.mobdev.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.databinding.ItemChannelBinding

class ChannelsAdapter(
    private val onChannelClick: (String) -> Unit,
) : ListAdapter<String, ChannelsAdapter.ChannelViewHolder>(DiffCallback) {

    var selectedChannel: String? = null
        set(value) {
            val old = field
            field = value
            if (old != value) {
                notifyItemChangedPosition(old)
                notifyItemChangedPosition(value)
            }
        }

    private fun notifyItemChangedPosition(channelName: String?) {
        if (channelName == null) return
        val pos = currentList.indexOfFirst { it.equals(channelName, ignoreCase = true) }
        if (pos != -1) {
            notifyItemChanged(pos)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ItemChannelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ChannelViewHolder(binding, onChannelClick)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = getItem(position)
        val isSelected = channel.equals(selectedChannel, ignoreCase = true)
        holder.bind(channel, isSelected)
    }

    class ChannelViewHolder(
        private val binding: ItemChannelBinding,
        private val onChannelClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(channel: String, isSelected: Boolean) {
            val context = binding.root.context
            binding.tvChannelName.text = channel

            val density = context.resources.displayMetrics.density
            if (isSelected) {
                binding.cardChannel.strokeColor = androidx.core.content.ContextCompat.getColor(context, io.github.mobdev.R.color.primary)
                binding.cardChannel.strokeWidth = (2.5f * density).toInt()
                binding.cardChannel.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(context, io.github.mobdev.R.color.channel_selected_bg)
                )
            } else {
                binding.cardChannel.strokeColor = androidx.core.content.ContextCompat.getColor(context, io.github.mobdev.R.color.divider)
                binding.cardChannel.strokeWidth = (1f * density).toInt()
                binding.cardChannel.setCardBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(context, android.R.color.transparent)
                )
            }

            binding.root.setOnClickListener {
                onChannelClick(channel)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    }
}
