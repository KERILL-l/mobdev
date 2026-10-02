package io.github.mobdev.ui

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.commit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import io.github.mobdev.R
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBackNavigation()
        observeViewModel()
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val imageDialog =
                    supportFragmentManager.findFragmentByTag(ImageViewerDialogFragment.TAG) as? DialogFragment
                if (imageDialog != null && imageDialog.dialog?.isShowing == true) {
                    imageDialog.dismiss()
                    return
                }

                val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                val selectedChannel = viewModel.uiState.value.selectedChannel

                if (isLandscape) {
                    if (selectedChannel != null) {
                        viewModel.clearSelectedChannel()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                } else {
                    if (selectedChannel != null) {
                        viewModel.clearSelectedChannel()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                var lastChannel: String? = null
                var lastLandscape: Boolean? = null

                viewModel.uiState.collect { state ->
                    val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                    val channel = state.selectedChannel

                    if (isLandscape != lastLandscape || channel != lastChannel) {
                        updateFragments(isLandscape, channel)
                        lastLandscape = isLandscape
                        lastChannel = channel
                    }

                    handleImageViewer(state.fullScreenImageUrl)
                }
            }
        }
    }

    private fun updateFragments(isLandscape: Boolean, selectedChannel: String?) {
        if (isLandscape) {
            updateLandscapeFragments(selectedChannel)
        } else {
            updatePortraitFragments(selectedChannel)
        }
    }

    private fun updateLandscapeFragments(selectedChannel: String?) {
        val channelsContainer = findViewById<View>(R.id.channels_container)
        val chatContainer = findViewById<View>(R.id.chat_container)
        if (channelsContainer == null || chatContainer == null) return

        val currentLeft = supportFragmentManager.findFragmentById(R.id.channels_container)
        if (currentLeft !is ChannelsFragment) {
            supportFragmentManager.commit {
                replace(R.id.channels_container, ChannelsFragment.newInstance())
            }
        }

        val currentRight = supportFragmentManager.findFragmentById(R.id.chat_container)
        if (selectedChannel != null) {
            if (currentRight !is ChatFragment) {
                supportFragmentManager.commit {
                    replace(R.id.chat_container, ChatFragment.newInstance())
                }
            }
        } else {
            if (currentRight !is PlaceholderFragment) {
                supportFragmentManager.commit {
                    replace(R.id.chat_container, PlaceholderFragment())
                }
            }
        }
    }

    private fun updatePortraitFragments(selectedChannel: String?) {
        val mainContainer = findViewById<View>(R.id.main_container) ?: return
        val current = supportFragmentManager.findFragmentById(R.id.main_container)

        if (selectedChannel != null) {
            if (current !is ChatFragment) {
                supportFragmentManager.commit {
                    replace(R.id.main_container, ChatFragment.newInstance())
                }
            }
        } else {
            if (current !is ChannelsFragment) {
                supportFragmentManager.commit {
                    replace(R.id.main_container, ChannelsFragment.newInstance())
                }
            }
        }
    }

    private fun handleImageViewer(imageUrl: String?) {
        val existingDialog =
            supportFragmentManager.findFragmentByTag(ImageViewerDialogFragment.TAG) as? DialogFragment

        if (!imageUrl.isNullOrBlank()) {
            if (existingDialog == null || !existingDialog.isAdded) {
                ImageViewerDialogFragment.newInstance()
                    .show(supportFragmentManager, ImageViewerDialogFragment.TAG)
            }
        } else {
            existingDialog?.dismissAllowingStateLoss()
        }
    }
}
