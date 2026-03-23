package com.markduenas.visischeduler.domain.usecase

import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.VisitStatus
import com.markduenas.visischeduler.domain.repository.VisitRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Assesses fatigue level for a beneficiary based on recent visit frequency.
 */
class GetBeneficiaryFatigueUseCase(
    private val visitRepository: VisitRepository
) {
    suspend operator fun invoke(beneficiaryId: String): FatigueAssessment {
        val visits = visitRepository.getVisitsForBeneficiary(beneficiaryId).first()
        val completedOrActive = visits.filter {
            it.status == VisitStatus.COMPLETED || it.status == VisitStatus.APPROVED
        }

        val now = Clock.System.now()
        val sevenDaysAgo = now - 7.days
        val thirtyDaysAgo = now - 30.days
        val tz = TimeZone.currentSystemDefault()

        val visitsLast7Days = completedOrActive.count { visit ->
            val visitInstant = LocalDateTime(visit.scheduledDate, visit.startTime).toInstant(tz)
            visitInstant >= sevenDaysAgo
        }
        val visitsLast30Days = completedOrActive.count { visit ->
            val visitInstant = LocalDateTime(visit.scheduledDate, visit.startTime).toInstant(tz)
            visitInstant >= thirtyDaysAgo
        }

        val level = when {
            visitsLast7Days >= 7 -> FatigueLevel.CRITICAL
            visitsLast7Days >= 5 -> FatigueLevel.HIGH
            visitsLast7Days >= 3 -> FatigueLevel.ELEVATED
            else -> FatigueLevel.NORMAL
        }

        return FatigueAssessment(
            beneficiaryId = beneficiaryId,
            level = level,
            visitsLast7Days = visitsLast7Days,
            visitsLast30Days = visitsLast30Days
        )
    }
}

/**
 * Fatigue severity levels for a beneficiary.
 */
enum class FatigueLevel {
    NORMAL,
    ELEVATED,
    HIGH,
    CRITICAL;

    val isWarning: Boolean get() = this != NORMAL
}

/**
 * Result of a beneficiary fatigue assessment.
 */
data class FatigueAssessment(
    val beneficiaryId: String,
    val level: FatigueLevel,
    val visitsLast7Days: Int,
    val visitsLast30Days: Int
) {
    val summaryText: String
        get() = "$visitsLast7Days visit${if (visitsLast7Days != 1) "s" else ""} in the last 7 days"
}
