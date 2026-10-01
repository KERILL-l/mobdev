package io.github.mobdev

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.mobdev.databinding.ItemContactBinding

class ContactsAdapter(
    private val onContactClick: (Contact) -> Unit,
) : ListAdapter<Contact, ContactsAdapter.ContactViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return ContactViewHolder(binding, onContactClick)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContactViewHolder(
        private val binding: ItemContactBinding,
        private val onContactClick: (Contact) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact) {
            binding.tvAvatarInitials.text = contact.initials
            binding.tvContactName.text = contact.name

            val phone = contact.phoneNumber
            if (!phone.isNullOrBlank()) {
                binding.tvContactPhone.text = phone
                binding.tvContactPhone.visibility = android.view.View.VISIBLE
            } else {
                binding.tvContactPhone.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onContactClick(contact)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
        }
    }
}
