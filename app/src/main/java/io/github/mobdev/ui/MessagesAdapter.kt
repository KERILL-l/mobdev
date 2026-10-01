package io.github.mobdev.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import io.github.mobdev.data.ChatMessage
import io.github.mobdev.databinding.ItemMessageIncomingBinding
import io.github.mobdev.databinding.ItemMessageOutgoingBinding

class MessagesAdapter(
    private val currentUsername: String,
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        return if (msg.from.equals(currentUsername, ignoreCase = true)) {
            VIEW_TYPE_OUTGOING
        } else {
            VIEW_TYPE_INCOMING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_OUTGOING) {
            val binding = ItemMessageOutgoingBinding.inflate(inflater, parent, false)
            OutgoingViewHolder(binding)
        } else {
            val binding = ItemMessageIncomingBinding.inflate(inflater, parent, false)
            IncomingViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is OutgoingViewHolder) {
            holder.bind(message)
        } else if (holder is IncomingViewHolder) {
            holder.bind(message)
        }
    }

    class OutgoingViewHolder(
        private val binding: ItemMessageOutgoingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val text = message.textContent
            if (!text.isNullOrBlank()) {
                binding.tvMessage.text = text
                binding.tvMessage.visibility = View.VISIBLE
            } else {
                binding.tvMessage.visibility = View.GONE
            }

            val imgUrl = message.imageUrl
            if (!imgUrl.isNullOrBlank()) {
                binding.ivImage.visibility = View.VISIBLE
                binding.ivImage.load(imgUrl) {
                    crossfade(true)
                }
            } else {
                binding.ivImage.visibility = View.GONE
            }

            binding.tvTime.text = message.formattedTime
        }
    }

    class IncomingViewHolder(
        private val binding: ItemMessageIncomingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvAuthor.text = message.from

            val text = message.textContent
            if (!text.isNullOrBlank()) {
                binding.tvMessage.text = text
                binding.tvMessage.visibility = View.VISIBLE
            } else {
                binding.tvMessage.visibility = View.GONE
            }

            val imgUrl = message.imageUrl
            if (!imgUrl.isNullOrBlank()) {
                binding.ivImage.visibility = View.VISIBLE
                binding.ivImage.load(imgUrl) {
                    crossfade(true)
                }
            } else {
                binding.ivImage.visibility = View.GONE
            }

            binding.tvTime.text = message.formattedTime
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_INCOMING = 1
        private const val VIEW_TYPE_OUTGOING = 2
    }
}
