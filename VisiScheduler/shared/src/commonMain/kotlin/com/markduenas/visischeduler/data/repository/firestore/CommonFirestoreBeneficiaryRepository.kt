package com.markduenas.visischeduler.data.repository.firestore

import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus
import com.markduenas.visischeduler.domain.entities.EmergencyContact
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Cross-platform Firestore implementation of BeneficiaryRepository.
 */
class CommonFirestoreBeneficiaryRepository(
    private val firestore: FirestoreDatabase,
    private val auth: FirebaseAuth
) : BeneficiaryRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    override fun getMyBeneficiaries(): Flow<List<Beneficiary>> {
        val userId = currentUserId ?: return flowOf(emptyList())

        // Get user's associated beneficiary IDs first, then fetch beneficiaries
        return firestore.listenToUser(userId).map { userDoc ->
            val beneficiaryIds = userDoc?.get<List<String>>("associatedBeneficiaryIds") ?: emptyList()
            if (beneficiaryIds.isEmpty()) {
                emptyList()
            } else {
                beneficiaryIds.mapNotNull { id ->
                    firestore.getBeneficiary(id)?.toBeneficiary()
                }
            }
        }
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
            .filter { beneficiary ->
                beneficiary.firstName.contains(query, ignoreCase = true) ||
                beneficiary.lastName.contains(query, ignoreCase = true) ||
                beneficiary.fullName.contains(query, ignoreCase = true)
            }
    }

    override suspend fun syncBeneficiaries(): Result<Unit> = runCatching {
        // Firestore handles sync automatically
    }

    // ==================== Mapping Functions ====================

    @Suppress("UNCHECKED_CAST")
    private fun DocumentSnapshot.toBeneficiary(): Beneficiary? {
        return try {
            val emergencyContactMap = get<Map<String, Any?>?>("emergencyContact")

            Beneficiary(
                id = id,
                firstName = get("firstName") ?: return null,
                lastName = get("lastName") ?: return null,
                dateOfBirth = get<String?>("dateOfBirth")?.let { LocalDate.parse(it) },
                facilityId = get("facilityId") ?: return null,
                roomNumber = get("roomNumber"),
                status = try {
                    BeneficiaryStatus.valueOf(get("status") ?: "ACTIVE")
                } catch (e: Exception) {
                    BeneficiaryStatus.ACTIVE
                },
                specialInstructions = get("specialInstructions"),
                maxVisitorsPerSlot = get<Long?>("maxVisitorsPerSlot")?.toInt() ?: 2,
                maxVisitsPerDay = get<Long?>("maxVisitsPerDay")?.toInt() ?: 2,
                maxVisitsPerWeek = get<Long?>("maxVisitsPerWeek")?.toInt() ?: 7,
                photoUrl = get("photoUrl"),
                emergencyContact = emergencyContactMap?.let {
                    EmergencyContact(
                        name = it["name"] as? String ?: "",
                        relationship = it["relationship"] as? String ?: "",
                        phoneNumber = it["phoneNumber"] as? String ?: "",
                        email = it["email"] as? String,
                        isPrimaryContact = it["isPrimaryContact"] as? Boolean ?: true
                    )
                },
                restrictions = get("restrictions") ?: emptyList(),
                createdAt = get<Long?>("createdAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0),
                updatedAt = get<Long?>("updatedAt")?.let { Instant.fromEpochMilliseconds(it) }
                    ?: Instant.fromEpochMilliseconds(0)
            )
        } catch (e: Exception) {
            null
        }
    }
}
