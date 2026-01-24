package com.markduenas.visischeduler.presentation.viewmodel.visitors

import com.markduenas.visischeduler.common.error.AppException
import com.markduenas.visischeduler.domain.entities.Restriction
import com.markduenas.visischeduler.domain.entities.RestrictionScope
import com.markduenas.visischeduler.domain.entities.RestrictionType
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.presentation.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Grouped restrictions for display.
 */
data class GroupedRestrictions(
    val timeBasedRestrictions: List<Restriction> = emptyList(),
    val visitorBasedRestrictions: List<Restriction> = emptyList(),
    val capacityBasedRestrictions: List<Restriction> = emptyList(),
    val beneficiaryBasedRestrictions: List<Restriction> = emptyList(),
    val relationshipBasedRestrictions: List<Restriction> = emptyList()
) {
    val allRestrictions: List<Restriction>
        get() = timeBasedRestrictions +
                visitorBasedRestrictions +
                capacityBasedRestrictions +
                beneficiaryBasedRestrictions +
                relationshipBasedRestrictions

    val totalCount: Int
        get() = allRestrictions.size

    val activeCount: Int
        get() = allRestrictions.count { it.isActive }

    fun isEmpty(): Boolean = allRestrictions.isEmpty()
}

/**
 * UI State for the restrictions screen.
 */
data class RestrictionsUiState(
    val restrictions: GroupedRestrictions = GroupedRestrictions(),
    val selectedType: RestrictionType? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: AppException? = null,
    val showDeleteDialog: Boolean = false,
    val restrictionToDelete: Restriction? = null
) {
    val hasRestrictions: Boolean
        get() = !restrictions.isEmpty()

    val filteredRestrictions: List<Restriction>
        get() = if (selectedType == null) {
            restrictions.allRestrictions
        } else {
            restrictions.allRestrictions.filter { it.type == selectedType }
        }
}

/**
 * ViewModel for managing the restrictions screen.
 *
 * Provides functionality to:
 * - View all restrictions grouped by type
 * - Toggle restrictions on/off
 * - Delete restrictions
 * - Filter by restriction type
 */
class RestrictionsViewModel(
    private val restrictionRepository: RestrictionRepository
) : BaseViewModel<RestrictionsUiState>(RestrictionsUiState()) {

    init {
        observeRestrictions()
    }

    private fun observeRestrictions() {
        updateState { copy(isLoading = true) }

        restrictionRepository.getActiveRestrictions()
            .onEach { allRestrictions ->
                val grouped = groupRestrictions(allRestrictions)
                updateState {
                    copy(
                        restrictions = grouped,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun groupRestrictions(restrictions: List<Restriction>): GroupedRestrictions {
        return GroupedRestrictions(
            timeBasedRestrictions = restrictions.filter { it.type == RestrictionType.TIME_BASED },
            visitorBasedRestrictions = restrictions.filter { it.type == RestrictionType.VISITOR_BASED },
            capacityBasedRestrictions = restrictions.filter { it.type == RestrictionType.CAPACITY_BASED },
            beneficiaryBasedRestrictions = restrictions.filter { it.type == RestrictionType.BENEFICIARY_BASED },
            relationshipBasedRestrictions = restrictions.filter { it.type == RestrictionType.RELATIONSHIP_BASED }
        )
    }

    /**
     * Filters restrictions by type.
     */
    fun onFilterChange(type: RestrictionType?) {
        updateState { copy(selectedType = type) }
    }

    /**
     * Toggles a restriction's active status.
     */
    fun toggleRestriction(restriction: Restriction) {
        launchSafe {
            if (restriction.isActive) {
                restrictionRepository.deactivateRestriction(restriction.id)
                    .onSuccess {
                        showSnackbar("Restriction disabled")
                    }
                    .onFailure { error ->
                        showSnackbar(error.message ?: "Failed to disable restriction")
                    }
            } else {
                restrictionRepository.reactivateRestriction(restriction.id)
                    .onSuccess {
                        showSnackbar("Restriction enabled")
                    }
                    .onFailure { error ->
                        showSnackbar(error.message ?: "Failed to enable restriction")
                    }
            }
        }
    }

    /**
     * Shows delete confirmation dialog.
     */
    fun showDeleteDialog(restriction: Restriction) {
        updateState {
            copy(
                showDeleteDialog = true,
                restrictionToDelete = restriction
            )
        }
    }

    /**
     * Hides delete confirmation dialog.
     */
    fun hideDeleteDialog() {
        updateState {
            copy(
                showDeleteDialog = false,
                restrictionToDelete = null
            )
        }
    }

    /**
     * Deletes a restriction.
     */
    fun deleteRestriction() {
        val restriction = currentState.restrictionToDelete ?: return

        launchSafe {
            hideDeleteDialog()
            restrictionRepository.deleteRestriction(restriction.id)
                .onSuccess {
                    showSnackbar("Restriction deleted")
                }
                .onFailure { error ->
                    showSnackbar(error.message ?: "Failed to delete restriction")
                }
        }
    }

    /**
     * Navigates to add restriction screen.
     */
    fun onAddRestrictionClick() {
        navigate("restrictions/add")
    }

    /**
     * Navigates to restriction details.
     */
    fun onRestrictionClick(restrictionId: String) {
        navigate("restrictions/details/$restrictionId")
    }

    /**
     * Navigates to edit restriction screen.
     */
    fun onEditRestrictionClick(restrictionId: String) {
        navigate("restrictions/edit/$restrictionId")
    }

    /**
     * Refreshes the restrictions list.
     */
    fun refresh() {
        launchSafe {
            updateState { copy(isRefreshing = true) }
            restrictionRepository.syncRestrictions()
            updateState { copy(isRefreshing = false) }
        }
    }

    /**
     * Clears the current error.
     */
    fun clearError() {
        updateState { copy(error = null) }
    }

    /**
     * Gets the icon for a restriction type.
     */
    fun getRestrictionTypeIcon(type: RestrictionType): String {
        return when (type) {
            RestrictionType.TIME_BASED -> "schedule"
            RestrictionType.VISITOR_BASED -> "person"
            RestrictionType.CAPACITY_BASED -> "groups"
            RestrictionType.BENEFICIARY_BASED -> "elderly"
            RestrictionType.RELATIONSHIP_BASED -> "family_restroom"
            RestrictionType.COMBINED -> "tune"
        }
    }

    /**
     * Gets the display name for a restriction type.
     */
    fun getRestrictionTypeDisplayName(type: RestrictionType): String {
        return when (type) {
            RestrictionType.TIME_BASED -> "Time-Based"
            RestrictionType.VISITOR_BASED -> "Visitor-Based"
            RestrictionType.CAPACITY_BASED -> "Capacity"
            RestrictionType.BENEFICIARY_BASED -> "Beneficiary"
            RestrictionType.RELATIONSHIP_BASED -> "Relationship"
            RestrictionType.COMBINED -> "Combined"
        }
    }
}
