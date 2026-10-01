package io.github.mobdev.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var adapter: MessagesAdapter
    private var channelName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        channelName = intent.getStringExtra(EXTRA_CHANNEL).orEmpty()
        if (channelName.isBlank()) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.initChannel(channelName)
    }

    private fun setupToolbar() {
        binding.toolbar.title = "#$channelName"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        adapter = MessagesAdapter(currentUsername = viewModel.currentUsername)

        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                viewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMessages()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = if (state.isLoading && state.messages.isEmpty()) View.VISIBLE else View.GONE
            binding.btnSend.isEnabled = !state.isSending

            if (state.errorMessage != null) {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_SHORT).show()
            }

            if (state.messages.isEmpty() && !state.isLoading) {
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
                val isAtBottom = !binding.rvMessages.canScrollVertically(1)
                adapter.submitList(state.messages) {
                    if (isAtBottom && state.messages.isNotEmpty()) {
                        binding.rvMessages.scrollToPosition(state.messages.size - 1)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_CHANNEL = "extra_channel"
    }
}
