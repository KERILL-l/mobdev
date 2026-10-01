package io.github.mobdev

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.mobdev.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ContactsViewModel by viewModels()
    private lateinit var adapter: ContactsAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        checkPermissionAndLoad()
    }

    override fun onResume() {
        super.onResume()
        // If permission was granted in system settings while in background
        if (hasReadContactsPermission() && viewModel.uiState.value is ContactsUiState.PermissionRequired) {
            viewModel.loadContacts()
        }
    }

    private fun setupRecyclerView() {
        adapter = ContactsAdapter { contact ->
            val intent = Intent(this, ContactDetailActivity::class.java).apply {
                putExtra(ContactDetailActivity.EXTRA_CONTACT, contact)
            }
            startActivity(intent)
        }

        binding.rvContacts.layoutManager = LinearLayoutManager(this)
        binding.rvContacts.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            if (hasReadContactsPermission()) {
                viewModel.loadContacts()
            } else {
                binding.swipeRefresh.isRefreshing = false
                requestContactsPermission()
            }
        }

        binding.btnGrantPermission.setOnClickListener {
            requestContactsPermission()
        }

        binding.etSearch.doAfterTextChanged { text ->
            viewModel.onSearchQueryChanged(text?.toString().orEmpty())
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            binding.swipeRefresh.isRefreshing = false

            when (state) {
                is ContactsUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvContacts.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.GONE
                    binding.layoutPermissionRequired.visibility = View.GONE
                }
                is ContactsUiState.PermissionRequired -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvContacts.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.GONE
                    binding.layoutPermissionRequired.visibility = View.VISIBLE
                }
                is ContactsUiState.Empty -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvContacts.visibility = View.GONE
                    binding.layoutPermissionRequired.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = getString(R.string.empty_contacts)
                }
                is ContactsUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutPermissionRequired.visibility = View.GONE

                    if (state.contacts.isEmpty() && state.query.isNotBlank()) {
                        binding.rvContacts.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.tvEmptyMessage.text = getString(R.string.empty_search)
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvContacts.visibility = View.VISIBLE
                        adapter.submitList(state.contacts)
                    }
                }
            }
        }
    }

    private fun hasReadContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPermissionAndLoad() {
        if (hasReadContactsPermission()) {
            viewModel.loadContacts()
        } else {
            requestContactsPermission()
        }
    }

    private fun requestContactsPermission() {
        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
}
