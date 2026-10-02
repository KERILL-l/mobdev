package io.github.mobdev.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.databinding.FragmentChatBinding
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: MessagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (isLandscape) {
            binding.toolbar.navigationIcon = null
        } else {
            binding.toolbar.setNavigationOnClickListener {
                mainViewModel.clearSelectedChannel()
            }
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        adapter = MessagesAdapter(
            currentUsername = mainViewModel.currentUsername,
            onImageClick = { fullImageUrl ->
                mainViewModel.openImage(fullImageUrl)
            },
        )
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (text.isNotBlank()) {
                mainViewModel.sendMessage(text)
                binding.etMessage.text?.clear()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            val channel = mainViewModel.uiState.value.selectedChannel
            if (!channel.isNullOrBlank()) {
                mainViewModel.loadMessages(channel = channel, force = true)
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.uiState.collect { state ->
                    val channel = state.selectedChannel
                    binding.toolbar.title = if (channel != null) "#$channel" else ""

                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility =
                        if (state.isLoadingMessages && state.currentMessages.isEmpty()) View.VISIBLE else View.GONE
                    binding.btnSend.isEnabled = !state.isSendingMessage

                    if (state.messagesError != null) {
                        Toast.makeText(requireContext(), state.messagesError, Toast.LENGTH_SHORT).show()
                    }

                    if (state.currentMessages.isEmpty() && !state.isLoadingMessages) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        val isAtBottom = !binding.rvMessages.canScrollVertically(1)
                        adapter.submitList(state.currentMessages) {
                            if (isAtBottom && state.currentMessages.isNotEmpty()) {
                                binding.rvMessages.scrollToPosition(state.currentMessages.size - 1)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): ChatFragment = ChatFragment()
    }
}
