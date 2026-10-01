package io.github.mobdev

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface ContactsUiState {
    object Loading : ContactsUiState
    object PermissionRequired : ContactsUiState
    object Empty : ContactsUiState
    data class Success(
        val contacts: List<Contact>,
        val query: String = "",
    ) : ContactsUiState
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContactsRepository(application)

    private val _uiState = MutableLiveData<ContactsUiState>(ContactsUiState.Loading)
    val uiState: LiveData<ContactsUiState> = _uiState

    private var allContacts: List<Contact> = emptyList()
    private var currentQuery: String = ""

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            loadContacts()
        } else {
            _uiState.value = ContactsUiState.PermissionRequired
        }
    }

    fun loadContacts() {
        _uiState.value = ContactsUiState.Loading
        viewModelScope.launch {
            try {
                val list = repository.getContacts()
                allContacts = list
                applyFilter(currentQuery)
            } catch (e: Exception) {
                _uiState.value = ContactsUiState.PermissionRequired
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        currentQuery = query
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        if (allContacts.isEmpty()) {
            _uiState.value = ContactsUiState.Empty
            return
        }

        val filtered = if (query.isBlank()) {
            allContacts
        } else {
            val q = query.trim().lowercase()
            allContacts.filter { contact ->
                contact.name.lowercase().contains(q) ||
                    (contact.phoneNumber != null && contact.phoneNumber.contains(q))
            }
        }

        _uiState.value = if (filtered.isEmpty() && query.isNotBlank()) {
            ContactsUiState.Success(emptyList(), query)
        } else if (filtered.isEmpty()) {
            ContactsUiState.Empty
        } else {
            ContactsUiState.Success(filtered, query)
        }
    }
}
