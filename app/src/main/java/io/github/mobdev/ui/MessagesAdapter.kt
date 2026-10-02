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
    private val onImageClick: (String) -> Unit = {},
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
            OutgoingViewHolder(binding, onImageClick)
        } else {
            val binding = ItemMessageIncomingBinding.inflate(inflater, parent, false)
            IncomingViewHolder(binding, onImageClick)
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
        private val onImageClick: (String) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            val text = message.textContent
            if (!text.isNullOrBlank()) {
                binding.tvMessage.text = text
                binding.tvMessage.visibility = View.VISIBLE
            } else {
                binding.tvMessage.visibility = View.GONE
            }

            val thumbUrl = message.thumbnailUrl
            val fullUrl = message.fullImageUrl ?: thumbUrl
            if (!thumbUrl.isNullOrBlank()) {
                binding.ivImage.visibility = View.VISIBLE
                binding.ivImage.load(thumbUrl) {
                    crossfade(true)
                }
                binding.ivImage.setOnClickListener {
                    if (!fullUrl.isNullOrBlank()) {
                        onImageClick(fullUrl)
                    }
                }
            } else {
                binding.ivImage.visibility = View.GONE
                binding.ivImage.setOnClickListener(null)
            }

            binding.tvPending.visibility = if (message.isPending) View.VISIBLE else View.GONE
            if (message.isPending) {
                binding.tvTime.text = binding.root.context.getString(io.github.mobdev.R.string.status_pending)
            } else {
                binding.tvTime.text = message.formattedTime
            }
        }
    }

    class IncomingViewHolder(
        private val binding: ItemMessageIncomingBinding,
        private val onImageClick: (String) -> Unit,
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

            val thumbUrl = message.thumbnailUrl
            val fullUrl = message.fullImageUrl ?: thumbUrl
            if (!thumbUrl.isNullOrBlank()) {
                binding.ivImage.visibility = View.VISIBLE
                binding.ivImage.load(thumbUrl) {
                    crossfade(true)
                }
                binding.ivImage.setOnClickListener {
                    if (!fullUrl.isNullOrBlank()) {
                        onImageClick(fullUrl)
                    }
                }
            } else {
                binding.ivImage.visibility = View.GONE
                binding.ivImage.setOnClickListener(null)
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
