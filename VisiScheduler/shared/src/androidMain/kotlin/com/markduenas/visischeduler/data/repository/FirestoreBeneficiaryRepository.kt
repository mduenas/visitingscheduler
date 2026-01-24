package com.markduenas.visischeduler.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus
import com.markduenas.visischeduler.domain.entities.EmergencyContact
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Firestore implementation of BeneficiaryRepository.
 */
class FirestoreBeneficiaryRepository(
    private val firestore: FirestoreDatabase,
    private val currentUserId: () -> String?
) : BeneficiaryRepository {

    override fun getMyBeneficiaries(): Flow<List<Beneficiary>> {
        val userId = currentUserId() ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return firestore.listenToQuery(
            FirestoreDatabase.COLLECTION_BENEFICIARIES,
            "coordinatorIds",
            userId
        ).map { docs -> docs.mapNotNull { it.toBeneficiary() } }
    }

    override suspend fun getBeneficiaryById(beneficiaryId: String): Result<Beneficiary> = runCatching {
        firestore.getBeneficiary(beneficiaryId)?.toBeneficiary()
            ?: throw Exception("Beneficiary not found")
    }

    override fun getAllBeneficiaries(): Flow<List<Beneficiary>> {
        return firestore.listenToCollection(FirestoreDatabase.COLLECTION_BENEFICIARIES)
            .map { docs -> docs.mapNotNull { it.toBeneficiary() } }
    }

    override suspend fun searchBeneficiaries(query: String): Result<List<Beneficiary>> = runCatching {
        val allBeneficiaries = firestore.getAll(FirestoreDatabase.COLLECTION_BENEFICIARIES)
        allBeneficiaries.mapNotNull { it.toBeneficiary() }
            .filter {
                it.fullName.contains(query, ignoreCase = true) ||
                it.roomNumber?.contains(query, ignoreCase = true) == true
            }
    }

    override suspend fun syncBeneficiaries(): Result<Unit> = runCatching {
        // Firestore handles sync automatically with real-time listeners
    }

    // ==================== Mapping Functions ====================

    private fun DocumentSnapshot.toBeneficiary(): Beneficiary? {
        return try {
            val emergencyContact = (get("emergencyContact") as? Map<*, *>)?.let { ec ->
                EmergencyContact(
                    name = ec["name"] as? String ?: "",
                    relationship = ec["relationship"] as? String ?: "",
                    phoneNumber = ec["phoneNumber"] as? String ?: "",
                    email = ec["email"] as? String,
                    isPrimaryContact = ec["isPrimaryContact"] as? Boolean ?: true
                )
            }

            Beneficiary(
                id = id,
                firstName = getString("firstName") ?: return null,
                lastName = getString("lastName") ?: return null,
                dateOfBirth = getString("dateOfBirth")?.let { LocalDate.parse(it) },
                facilityId = getString("facilityId") ?: return null,
                roomNumber = getString("roomNumber"),
                status = BeneficiaryStatus.valueOf(getString("status") ?: "ACTIVE"),
                specialInstructions = getString("specialInstructions"),
                maxVisitorsPerSlot = getLong("maxVisitorsPerSlot")?.toInt() ?: 2,
                maxVisitsPerDay = getLong("maxVisitsPerDay")?.toInt() ?: 2,
                maxVisitsPerWeek = getLong("maxVisitsPerWeek")?.toInt() ?: 7,
                photoUrl = getString("photoUrl"),
                emergencyContact = emergencyContact,
                restrictions = (get("restrictions") as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList(),
                createdAt = getTimestamp("createdAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0),
                updatedAt = getTimestamp("updatedAt")?.let {
                    Instant.fromEpochMilliseconds(it.toDate().time)
                } ?: Instant.fromEpochMilliseconds(0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
