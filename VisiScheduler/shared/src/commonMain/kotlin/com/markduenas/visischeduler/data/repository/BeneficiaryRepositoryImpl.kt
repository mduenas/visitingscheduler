package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus
import com.markduenas.visischeduler.domain.entities.EmergencyContact
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Implementation of BeneficiaryRepository.
 */
class BeneficiaryRepositoryImpl(
    private val api: VisiSchedulerApi,
    private val database: VisiSchedulerDatabase,
    private val json: Json
) : BeneficiaryRepository {

    override fun getMyBeneficiaries(): Flow<List<Beneficiary>> = flow {
        // First emit cached data
        val cached = getCachedBeneficiaries()
        emit(cached)

        // Then fetch from API and update cache
        try {
            val beneficiaries = api.getMyBeneficiaries().map { it.toDomain() }
            beneficiaries.forEach { cacheBeneficiary(it) }
            emit(beneficiaries)
        } catch (e: Exception) {
            // If API fails and we have cached data, keep using it
            if (cached.isEmpty()) {
                throw e
            }
        }
    }

    override suspend fun getBeneficiaryById(beneficiaryId: String): Result<Beneficiary> {
        return withContext(Dispatchers.Default) {
            try {
                val response = api.getBeneficiaryById(beneficiaryId)
                val beneficiary = response.toDomain()
                cacheBeneficiary(beneficiary)
                Result.success(beneficiary)
            } catch (e: Exception) {
                // Try cache fallback
                val cached = getCachedBeneficiaryById(beneficiaryId)
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    override fun getAllBeneficiaries(): Flow<List<Beneficiary>> = flow {
        // First emit cached data
        val cached = getCachedBeneficiaries()
        emit(cached)

        // Then fetch from API and update cache
        try {
            val beneficiaries = api.getBeneficiaries().map { it.toDomain() }
            beneficiaries.forEach { cacheBeneficiary(it) }
            emit(beneficiaries)
        } catch (e: Exception) {
            // If API fails and we have cached data, keep using it
            if (cached.isEmpty()) {
                throw e
            }
        }
    }

    override suspend fun searchBeneficiaries(query: String): Result<List<Beneficiary>> {
        return withContext(Dispatchers.Default) {
            try {
                // For now, search locally from cache
                val cached = getCachedBeneficiaries()
                val filtered = cached.filter { beneficiary ->
                    beneficiary.firstName.contains(query, ignoreCase = true) ||
                    beneficiary.lastName.contains(query, ignoreCase = true) ||
                    beneficiary.roomNumber?.contains(query, ignoreCase = true) == true
                }
                Result.success(filtered)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun syncBeneficiaries(): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val beneficiaries = api.getBeneficiaries().map { it.toDomain() }
                beneficiaries.forEach { cacheBeneficiary(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun getCachedBeneficiaries(): List<Beneficiary> {
        return try {
            database.visiSchedulerQueries.selectAllBeneficiaries().executeAsList().map { entity ->
                mapEntityToDomain(entity)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCachedBeneficiaryById(beneficiaryId: String): Beneficiary? {
        return try {
            database.visiSchedulerQueries.selectBeneficiaryById(beneficiaryId).executeAsOneOrNull()?.let { entity ->
                mapEntityToDomain(entity)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mapEntityToDomain(entity: com.markduenas.visischeduler.data.local.BeneficiaryEntity): Beneficiary {
        return Beneficiary(
            id = entity.id,
            firstName = entity.firstName,
            lastName = entity.lastName,
            dateOfBirth = entity.dateOfBirth?.let {
                try { LocalDate.parse(it) } catch (e: Exception) { null }
            },
            facilityId = entity.facilityId,
            roomNumber = entity.roomNumber,
            status = try {
                BeneficiaryStatus.valueOf(entity.status)
            } catch (e: Exception) {
                BeneficiaryStatus.ACTIVE
            },
            specialInstructions = entity.specialInstructions,
            maxVisitorsPerSlot = entity.maxVisitorsPerSlot.toInt(),
            maxVisitsPerDay = entity.maxVisitsPerDay.toInt(),
            maxVisitsPerWeek = entity.maxVisitsPerWeek.toInt(),
            photoUrl = entity.photoUrl,
            emergencyContact = entity.emergencyContact?.let { parseEmergencyContact(it) },
            restrictions = parseRestrictions(entity.restrictions),
            createdAt = Instant.parse(entity.createdAt),
            updatedAt = Instant.parse(entity.updatedAt)
        )
    }

    private fun parseEmergencyContact(json: String): EmergencyContact? {
        return try {
            this.json.decodeFromString<EmergencyContact>(json)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRestrictions(json: String): List<String> {
        return try {
            this.json.decodeFromString<List<String>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun cacheBeneficiary(beneficiary: Beneficiary) {
        try {
            database.visiSchedulerQueries.insertBeneficiary(
                id = beneficiary.id,
                firstName = beneficiary.firstName,
                lastName = beneficiary.lastName,
                dateOfBirth = beneficiary.dateOfBirth?.toString(),
                facilityId = beneficiary.facilityId,
                roomNumber = beneficiary.roomNumber,
                status = beneficiary.status.name,
                specialInstructions = beneficiary.specialInstructions,
                maxVisitorsPerSlot = beneficiary.maxVisitorsPerSlot.toLong(),
                maxVisitsPerDay = beneficiary.maxVisitsPerDay.toLong(),
                maxVisitsPerWeek = beneficiary.maxVisitsPerWeek.toLong(),
                photoUrl = beneficiary.photoUrl,
                emergencyContact = beneficiary.emergencyContact?.let {
                    json.encodeToString(EmergencyContact.serializer(), it)
                },
                restrictions = json.encodeToString(ListSerializer(String.serializer()), beneficiary.restrictions),
                createdAt = beneficiary.createdAt.toString(),
                updatedAt = beneficiary.updatedAt.toString()
            )
        } catch (e: Exception) {
            // Ignore cache errors
        }
    }
}
