package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Filter options for the visitor list.
 */
enum class VisitorFilter {
    ALL,
    APPROVED,
    PENDING,
    BLOCKED
}

/**
 * UI State for the visitor list screen.
 */
data class VisitorListUiState(
    val approvedVisitors: List<User> = emptyList(),
    val pendingVisitors: List<User> = emptyList(),
    val blockedVisitors: List<User> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: VisitorFilter = VisitorFilter.ALL,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppException? = null
) {
    val filteredVisitors: List<User>
        get() = when (selectedFilter) {
            VisitorFilter.ALL -> approvedVisitors + pendingVisitors + blockedVisitors
            VisitorFilter.APPROVED -> approvedVisitors
            VisitorFilter.PENDING -> pendingVisitors
            VisitorFilter.BLOCKED -> blockedVisitors
        }.filter { visitor ->
            searchQuery.isBlank() ||
                visitor.fullName.contains(searchQuery, ignoreCase = true) ||
                visitor.email.contains(searchQuery, ignoreCase = true) ||
                visitor.phoneNumber?.contains(searchQuery, ignoreCase = true) == true
        }

    val totalVisitorCount: Int
        get() = approvedVisitors.size + pendingVisitors.size + blockedVisitors.size

    val hasVisitors: Boolean
        get() = totalVisitorCount > 0

    val isEmptyForFilter: Boolean
        get() = filteredVisitors.isEmpty() && !isLoading
}

/**
 * ViewModel for managing the visitor list screen.
 *
 * Provides functionality to:
 * - View approved, pending, and blocked visitors
 * - Search visitors by name, email, or phone
 * - Filter visitors by status
 * - Quick approve/block actions
 */
class VisitorListViewModel(
    private val userRepository: UserRepository
) : BaseViewModel<VisitorListUiState>(VisitorListUiState()) {

    private val searchQueryFlow = MutableStateFlow("")

    init {
        observeVisitors()
        observeSearchQuery()
    }

    private fun observeVisitors() {
        updateState { copy(isLoading = true) }

        // Observe approved visitors
        userRepository.getUsersByRole(Role.APPROVED_VISITOR)
            .combine(userRepository.getAllUsers()) { approved, all ->
                // Get blocked users (inactive approved visitors)
                val blocked = all.filter { user ->
                    user.role == Role.APPROVED_VISITOR && !user.isActive
                }
                Triple(approved.filter { it.isActive }, blocked, all)
            }
            .onEach { (approved, blocked, _) ->
                updateState {
                    copy(
                        approvedVisitors = approved,
                        blockedVisitors = blocked,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)

        // Observe pending visitors
        userRepository.getPendingVisitors()
            .onEach { pending ->
                updateState { copy(pendingVisitors = pending) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSearchQuery() {
        searchQueryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { query ->
                updateState { copy(searchQuery = query) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Updates the search query.
     */
    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    /**
     * Updates the selected filter.
     */
    fun onFilterChange(filter: VisitorFilter) {
        updateState { copy(selectedFilter = filter) }
    }

    /**
     * Approves a pending visitor.
     */
    fun approveVisitor(userId: String) {
        launchSafe {
            updateState { copy(isLoading = true) }
            userRepository.approveVisitor(userId)
                .onSuccess {
                    showSnackbar("Visitor approved successfully")
                    updateState { copy(isLoading = false) }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to approve visitor")
                }
        }
    }

    /**
     * Blocks a visitor.
     */
    fun blockVisitor(userId: String, reason: String = "Blocked by coordinator") {
        launchSafe {
            updateState { copy(isLoading = true) }
            userRepository.deactivateUser(userId)
                .onSuccess {
                    showSnackbar("Visitor blocked successfully")
                    updateState { copy(isLoading = false) }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to block visitor")
                }
        }
    }

    /**
     * Unblocks a visitor.
     */
    fun unblockVisitor(userId: String) {
        launchSafe {
            updateState { copy(isLoading = true) }
            userRepository.reactivateUser(userId)
                .onSuccess {
                    showSnackbar("Visitor unblocked successfully")
                    updateState { copy(isLoading = false) }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to unblock visitor")
                }
        }
    }

    /**
     * Denies a pending visitor.
     */
    fun denyVisitor(userId: String, reason: String) {
        launchSafe {
            updateState { copy(isLoading = true) }
            userRepository.denyVisitor(userId, reason)
                .onSuccess {
                    showSnackbar("Visitor request denied")
                    updateState { copy(isLoading = false) }
                }
                .onFailure { error ->
                    updateState { copy(isLoading = false, error = error as? AppException) }
                    showSnackbar(error.message ?: "Failed to deny visitor")
                }
        }
    }

    /**
     * Refreshes the visitor list.
     */
    fun refresh() {
        launchSafe {
            updateState { copy(isRefreshing = true) }
            userRepository.syncUser()
            updateState { copy(isRefreshing = false) }
        }
    }

    /**
     * Navigates to visitor details.
     */
    fun onVisitorClick(userId: String) {
        navigate("visitors/details/$userId")
    }

    /**
     * Navigates to add new visitor screen.
     */
    fun onAddVisitorClick() {
        navigate("visitors/add")
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }
}
