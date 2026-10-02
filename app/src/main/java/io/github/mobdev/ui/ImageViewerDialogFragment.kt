package io.github.mobdev.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import io.github.mobdev.databinding.DialogImageViewerBinding
import kotlinx.coroutines.launch

class ImageViewerDialogFragment : DialogFragment() {

    private var _binding: DialogImageViewerBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.uiState.collect { state ->
                    val url = state.fullScreenImageUrl
                    if (url.isNullOrBlank()) {
                        dismissAllowingStateLoss()
                    } else {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.ivFullImage.load(url) {
                            crossfade(true)
                            listener(
                                onSuccess = { _, _ ->
                                    _binding?.progressBar?.visibility = View.GONE
                                },
                                onError = { _, _ ->
                                    _binding?.progressBar?.visibility = View.GONE
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mainViewModel.closeImage()
    }

    companion object {
        const val TAG = "ImageViewerDialogFragment"
        fun newInstance(): ImageViewerDialogFragment = ImageViewerDialogFragment()
    }
}
