package io.github.mobdev

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.github.mobdev.databinding.ActivityContactDetailBinding

class ContactDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContactDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_CONTACT, Contact::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CONTACT) as? Contact
        }

        if (contact == null) {
            finish()
            return
        }

        bindContact(contact)
    }

    private fun bindContact(contact: Contact) {
        binding.tvDetailAvatarInitials.text = contact.initials
        binding.tvDetailName.text = contact.name

        // Phone
        val phone = contact.phoneNumber
        if (!phone.isNullOrBlank()) {
            binding.tvDetailPhone.text = phone
            binding.btnCall.setOnClickListener {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phone")
                }
                startActivity(dialIntent)
            }
        } else {
            binding.tvDetailPhone.text = getString(R.string.value_missing)
            binding.btnCall.visibility = View.GONE
        }

        // Email
        val email = contact.email
        if (!email.isNullOrBlank()) {
            binding.tvDetailEmail.text = email
            binding.btnSendEmail.setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                }
                startActivity(emailIntent)
            }
        } else {
            binding.tvDetailEmail.text = getString(R.string.value_missing)
            binding.btnSendEmail.visibility = View.GONE
        }
    }

    companion object {
        const val EXTRA_CONTACT = "extra_contact"
    }
}
