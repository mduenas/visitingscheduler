package com.markduenas.visischeduler.data.repository

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.domain.entities.Beneficiary
import com.markduenas.visischeduler.domain.entities.BeneficiaryStatus
import com.markduenas.visischeduler.domain.entities.EmergencyContact
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
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
        val cached = database.visiSchedulerQueries.selectAllBeneficiaries().executeAsList().map { mapEntityToBeneficiary(it) }
        emit(cached)

        try {
            val remoteVisits = api.getMyVisits()
            val beneficiaryIds = remoteVisits.map { it.beneficiaryId }.distinct()
            val remoteBeneficiaries = beneficiaryIds.map { id ->
                api.getBeneficiaryById(id).toDomain()
            }
            remoteBeneficiaries.forEach { cacheBeneficiary(it) }
            emit(remoteBeneficiaries)
        } catch (e: Exception) {
            // Keep cached
        }
    }

    override suspend fun getBeneficiaryById(beneficiaryId: String): Result<Beneficiary> {
        return try {
            val remote = api.getBeneficiaryById(beneficiaryId).toDomain()
            cacheBeneficiary(remote)
            Result.success(remote)
        } catch (e: Exception) {
            val cached = database.visiSchedulerQueries.selectBeneficiaryById(beneficiaryId).executeAsOneOrNull()
            if (cached != null) Result.success(mapEntityToBeneficiary(cached)) else Result.failure(e)
        }
    }

    override fun getAllBeneficiaries(): Flow<List<Beneficiary>> = flow {
        val cached = database.visiSchedulerQueries.selectAllBeneficiaries().executeAsList().map { mapEntityToBeneficiary(it) }
        emit(cached)
    }

    override suspend fun searchBeneficiaries(query: String): Result<List<Beneficiary>> {
        return Result.success(emptyList())
    }

    override suspend fun createBeneficiary(beneficiary: Beneficiary): Result<Beneficiary> {
        return Result.failure(UnsupportedOperationException("Not implemented"))
    }

    override suspend fun updateBeneficiary(beneficiary: Beneficiary): Result<Beneficiary> {
        return Result.failure(UnsupportedOperationException("Not implemented"))
    }

    override suspend fun syncBeneficiaries(): Result<Unit> {
        return Result.success(Unit)
    }

    private fun cacheBeneficiary(beneficiary: Beneficiary) {
        database.visiSchedulerQueries.insertBeneficiary(
            id = beneficiary.id,
            firstName = beneficiary.firstName,
            lastName = beneficiary.lastName,
            dateOfBirth = beneficiary.dateOfBirth.toString(),
            facilityId = beneficiary.facilityId,
            roomNumber = beneficiary.roomNumber,
            status = beneficiary.status.name,
            specialInstructions = beneficiary.specialInstructions,
            maxVisitorsPerSlot = beneficiary.maxVisitorsPerSlot.toLong(),
            maxVisitsPerDay = beneficiary.maxVisitsPerDay.toLong(),
            maxVisitsPerWeek = beneficiary.maxVisitsPerWeek.toLong(),
            photoUrl = beneficiary.photoUrl,
            emergencyContact = beneficiary.emergencyContact?.let { json.encodeToString(it) },
            restrictions = json.encodeToString(beneficiary.restrictions),
            createdAt = beneficiary.createdAt.toString(),
            updatedAt = beneficiary.updatedAt.toString()
        )
    }

    private fun mapEntityToBeneficiary(entity: com.markduenas.visischeduler.data.local.BeneficiaryEntity): Beneficiary {
        return Beneficiary(
            id = entity.id,
            firstName = entity.firstName,
            lastName = entity.lastName,
            dateOfBirth = entity.dateOfBirth?.let { kotlinx.datetime.LocalDate.parse(it) },
            facilityId = entity.facilityId,
            roomNumber = entity.roomNumber,
            status = BeneficiaryStatus.valueOf(entity.status),
            specialInstructions = entity.specialInstructions,
            maxVisitorsPerSlot = entity.maxVisitorsPerSlot.toInt(),
            maxVisitsPerDay = entity.maxVisitsPerDay.toInt(),
            maxVisitsPerWeek = entity.maxVisitsPerWeek.toInt(),
            photoUrl = entity.photoUrl,
            emergencyContact = entity.emergencyContact?.let { json.decodeFromString(it) },
            restrictions = json.decodeFromString(entity.restrictions),
            createdAt = kotlin.time.Instant.parse(entity.createdAt),
            updatedAt = kotlin.time.Instant.parse(entity.updatedAt)
        )
    }
}
