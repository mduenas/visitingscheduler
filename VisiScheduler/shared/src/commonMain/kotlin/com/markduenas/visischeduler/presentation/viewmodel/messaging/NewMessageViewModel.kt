package com.markduenas.visischeduler.presentation.viewmodel.messaging

import com.markduenas.visischeduler.domain.entities.Contact
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the new message/compose screen.
 */
class NewMessageViewModel(
    private val messageRepository: MessageRepository,
    private val beneficiaryId: String? = null
) : BaseViewModel<NewMessageUiState>(NewMessageUiState(beneficiaryId = beneficiaryId)) {

    private var searchJob: Job? = null
    private var contactsJob: Job? = null

    init {
        loadContacts()
        loadRecentContacts()
    }

    /**
     * Load all available contacts.
     */
    private fun loadContacts() {
        contactsJob?.cancel()
        contactsJob = viewModelScope.launch {
            updateState { copy(isLoading = true) }

            try {
                messageRepository.getContacts().collectLatest { contacts ->
                    updateState {
                        copy(
                            allContacts = contacts,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load contacts"
                    )
                }
            }
        }
    }

    /**
     * Load recent contacts.
     */
    private fun loadRecentContacts() {
        viewModelScope.launch {
            messageRepository.getRecentContacts(RECENT_CONTACTS_LIMIT)
                .onSuccess { contacts ->
                    updateState { copy(recentContacts = contacts) }
                }
        }
    }

    /**
     * Search contacts by query.
     */
    fun onSearchQueryChanged(query: String) {
        updateState { copy(searchQuery = query) }

        if (query.isBlank()) {
            updateState { copy(searchResults = null, isSearching = false) }
            return
        }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchContacts(query)
        }
    }

    /**
     * Perform contact search.
     */
    private suspend fun searchContacts(query: String) {
        updateState { copy(isSearching = true) }

        messageRepository.searchContacts(query)
            .onSuccess { contacts ->
                // Filter out already selected contacts
                val filtered = contacts.filter { contact ->
                    !currentState.selectedRecipients.any { it.id == contact.user.id }
                }
                updateState {
                    copy(
                        searchResults = filtered,
                        isSearching = false
                    )
                }
            }
            .onFailure {
                updateState { copy(isSearching = false) }
            }
    }

    /**
     * Clear search query and results.
     */
    fun clearSearch() {
        updateState {
            copy(
                searchQuery = "",
                searchResults = null,
                isSearching = false
            )
        }
    }

    /**
     * Select a contact to add to recipients.
     */
    fun selectContact(contact: Contact) {
        val user = contact.user
        if (currentState.selectedRecipients.any { it.id == user.id }) return

        updateState {
            copy(
                selectedRecipients = selectedRecipients + user,
                searchQuery = "",
                searchResults = null
            )
        }
    }

    /**
     * Remove a recipient from the selected list.
     */
    fun removeRecipient(user: User) {
        updateState {
            copy(selectedRecipients = selectedRecipients.filter { it.id != user.id })
        }
    }

    /**
     * Clear all selected recipients.
     */
    fun clearRecipients() {
        updateState { copy(selectedRecipients = emptyList()) }
    }

    /**
     * Create a conversation with selected recipients and navigate to it.
     */
    fun createConversation() {
        if (!currentState.canProceed) return

        val recipientIds = currentState.selectedRecipients.map { it.id }
        val beneficiaryId = currentState.beneficiaryId

        if (beneficiaryId == null) {
            showSnackbar("Please select a beneficiary")
            return
        }

        viewModelScope.launch {
            updateState { copy(isCreating = true) }

            // First check if conversation already exists
            val existingContact = currentState.selectedRecipients.firstOrNull()?.let { user ->
                currentState.allContacts.find { it.user.id == user.id }
            }

            if (existingContact?.hasExistingConversation == true && existingContact.conversationId != null) {
                // Navigate to existing conversation
                navigate("chat/${existingContact.conversationId}")
                return@launch
            }

            // Create new conversation
            messageRepository.createConversation(
                participantIds = recipientIds,
                beneficiaryId = beneficiaryId
            ).onSuccess { conversation ->
                updateState { copy(isCreating = false) }
                // Navigate to the new conversation
                navigate("chat/${conversation.id}")
            }.onFailure { e ->
                updateState { copy(isCreating = false) }
                showSnackbar("Failed to create conversation: ${e.message}")
            }
        }
    }

    /**
     * Create a conversation and send an initial message.
     */
    fun createConversationWithMessage(initialMessage: String) {
        if (!currentState.canProceed || initialMessage.isBlank()) return

        val recipientIds = currentState.selectedRecipients.map { it.id }
        val beneficiaryId = currentState.beneficiaryId

        if (beneficiaryId == null) {
            showSnackbar("Please select a beneficiary")
            return
        }

        viewModelScope.launch {
            updateState { copy(isCreating = true) }

            messageRepository.createConversation(
                participantIds = recipientIds,
                beneficiaryId = beneficiaryId
            ).onSuccess { conversation ->
                // Send the initial message
                messageRepository.sendMessage(
                    conversationId = conversation.id,
                    content = initialMessage
                ).onSuccess {
                    updateState { copy(isCreating = false) }
                    navigate("chat/${conversation.id}")
                }.onFailure { e ->
                    updateState { copy(isCreating = false) }
                    // Still navigate to conversation even if message failed
                    showSnackbar("Conversation created but message failed: ${e.message}")
                    navigate("chat/${conversation.id}")
                }
            }.onFailure { e ->
                updateState { copy(isCreating = false) }
                showSnackbar("Failed to create conversation: ${e.message}")
            }
        }
    }

    /**
     * Set the beneficiary for the conversation.
     */
    fun setBeneficiary(beneficiaryId: String) {
        updateState { copy(beneficiaryId = beneficiaryId) }
    }

    /**
     * Navigate back.
     */
    fun goBack() {
        navigateBack()
    }

    override fun onCleared() {
        searchJob?.cancel()
        contactsJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
        private const val RECENT_CONTACTS_LIMIT = 10
    }
}

/**
 * UI State for the new message screen.
 */
data class NewMessageUiState(
    val allContacts: List<Contact> = emptyList(),
    val recentContacts: List<Contact> = emptyList(),
    val searchResults: List<Contact>? = null,
    val selectedRecipients: List<User> = emptyList(),
    val searchQuery: String = "",
    val beneficiaryId: String? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null
) {
    val hasSelectedRecipients: Boolean
        get() = selectedRecipients.isNotEmpty()

    val canProceed: Boolean
        get() = selectedRecipients.isNotEmpty() && beneficiaryId != null && !isCreating

    val displayContacts: List<Contact>
        get() = searchResults ?: if (searchQuery.isBlank()) {
            recentContacts.ifEmpty { allContacts }
        } else {
            emptyList()
        }

    val showRecentHeader: Boolean
        get() = searchQuery.isBlank() && recentContacts.isNotEmpty() && searchResults == null

    val showNoResults: Boolean
        get() = searchResults?.isEmpty() == true

    val recipientNames: String
        get() = selectedRecipients.joinToString(", ") { it.fullName }
}
