package io.github.mobdev.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.R
import io.github.mobdev.databinding.ActivityChannelsBinding

class ChannelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelsBinding
    private val viewModel: ChannelsViewModel by viewModels()
    private lateinit var adapter: ChannelsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        val user = viewModel.currentUsername
        if (user.isNotBlank()) {
            supportActionBar?.subtitle = "@$user"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_LOGOUT, Menu.NONE, R.string.btn_logout).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == MENU_LOGOUT) {
            viewModel.logout()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = ChannelsAdapter { channelName ->
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_CHANNEL, channelName)
            }
            startActivity(intent)
        }
        binding.rvChannels.layoutManager = LinearLayoutManager(this)
        binding.rvChannels.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadChannels()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false
            binding.progressBar.visibility = if (state.isLoading && state.channels.isEmpty()) View.VISIBLE else View.GONE

            if (state.errorMessage != null && state.channels.isEmpty()) {
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_SHORT).show()
            }

            if (state.channels.isEmpty() && !state.isLoading) {
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
                adapter.submitList(state.channels)
            }

            if (state.isLoggedOut) {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    companion object {
        private const val MENU_LOGOUT = 1001
    }
}
