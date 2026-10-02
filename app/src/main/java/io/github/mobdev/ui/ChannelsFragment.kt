package io.github.mobdev.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.R
import io.github.mobdev.databinding.FragmentChannelsBinding
import kotlinx.coroutines.launch

class ChannelsFragment : Fragment() {

    private var _binding: FragmentChannelsBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: ChannelsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentChannelsBinding.inflate(inflater, container, false)
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
        val user = mainViewModel.currentUsername
        if (user.isNotBlank()) {
            binding.toolbar.subtitle = "@$user"
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.add(Menu.NONE, MENU_LOGOUT, Menu.NONE, R.string.btn_logout).apply {
                    setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return if (menuItem.itemId == MENU_LOGOUT) {
                    mainViewModel.logout()
                    true
                } else {
                    false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        adapter = ChannelsAdapter { channelName ->
            mainViewModel.selectChannel(channelName)
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChannels.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            mainViewModel.loadChannels(force = true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.uiState.collect { state ->
                    binding.tvOfflineBanner.visibility = if (!state.isOnline) View.VISIBLE else View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility =
                        if (state.isLoadingChannels && state.channels.isEmpty()) View.VISIBLE else View.GONE

                    if (state.channelsError != null && state.channels.isEmpty()) {
                        Toast.makeText(requireContext(), state.channelsError, Toast.LENGTH_SHORT).show()
                    }

                    if (state.channels.isEmpty() && !state.isLoadingChannels) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        adapter.submitList(state.channels)
                    }

                    adapter.selectedChannel = state.selectedChannel

                    if (state.isLoggedOut) {
                        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        requireActivity().finish()
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
        private const val MENU_LOGOUT = 1001
        fun newInstance(): ChannelsFragment = ChannelsFragment()
    }
}
