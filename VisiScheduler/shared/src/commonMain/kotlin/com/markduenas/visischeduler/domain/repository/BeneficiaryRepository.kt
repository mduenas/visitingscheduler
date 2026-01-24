package com.markduenas.visischeduler.domain.repository

import com.markduenas.visischeduler.domain.entities.Beneficiary
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for beneficiary operations.
 */
interface BeneficiaryRepository {
    /**
     * Get all beneficiaries associated with the current user.
     */
    fun getMyBeneficiaries(): Flow<List<Beneficiary>>

    /**
     * Get a specific beneficiary by ID.
     */
    suspend fun getBeneficiaryById(beneficiaryId: String): Result<Beneficiary>

    /**
     * Get all beneficiaries (coordinator/admin access).
     */
    fun getAllBeneficiaries(): Flow<List<Beneficiary>>

    /**
     * Search beneficiaries by name.
     */
    suspend fun searchBeneficiaries(query: String): Result<List<Beneficiary>>

    /**
     * Sync beneficiaries from remote server.
     */
    suspend fun syncBeneficiaries(): Result<Unit>
}
