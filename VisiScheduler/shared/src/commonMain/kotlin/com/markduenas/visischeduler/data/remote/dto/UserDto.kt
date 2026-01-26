package com.markduenas.visischeduler.data.remote.dto

import com.markduenas.visischeduler.domain.entities.NotificationPreferences
import com.markduenas.visischeduler.domain.entities.Role
import com.markduenas.visischeduler.domain.entities.User
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val isActive: Boolean = true,
    val isEmailVerified: Boolean = false,
    val createdAt: String,
    val updatedAt: String,
    val lastLoginAt: String? = null,
    val associatedBeneficiaryIds: List<String> = emptyList(),
    val notificationPreferences: NotificationPreferencesDto? = null
) {
    fun toDomain(): User {
        return User(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            role = Role.valueOf(role),
            phoneNumber = phoneNumber,
            profileImageUrl = profileImageUrl,
            isActive = isActive,
            isEmailVerified = isEmailVerified,
            createdAt = Instant.parse(createdAt),
            updatedAt = Instant.parse(updatedAt),
            lastLoginAt = lastLoginAt?.let { Instant.parse(it) },
            associatedBeneficiaryIds = associatedBeneficiaryIds,
            notificationPreferences = notificationPreferences?.toDomain() ?: NotificationPreferences()
        )
    }

    companion object {
        fun fromDomain(user: User): UserDto {
            return UserDto(
                id = user.id,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                role = user.role.name,
                phoneNumber = user.phoneNumber,
                profileImageUrl = user.profileImageUrl,
                isActive = user.isActive,
                isEmailVerified = user.isEmailVerified,
                createdAt = user.createdAt.toString(),
                updatedAt = user.updatedAt.toString(),
                lastLoginAt = user.lastLoginAt?.toString(),
                associatedBeneficiaryIds = user.associatedBeneficiaryIds,
                notificationPreferences = NotificationPreferencesDto.fromDomain(user.notificationPreferences)
            )
        }
    }
}

@Serializable
data class NotificationPreferencesDto(
    val emailNotifications: Boolean = true,
    val pushNotifications: Boolean = true,
    val smsNotifications: Boolean = false,
    val visitReminders: Boolean = true,
    val approvalNotifications: Boolean = true,
    val scheduleChanges: Boolean = true
) {
    fun toDomain(): NotificationPreferences {
        return NotificationPreferences(
            emailNotifications = emailNotifications,
            pushNotifications = pushNotifications,
            smsNotifications = smsNotifications,
            visitReminders = visitReminders,
            approvalNotifications = approvalNotifications,
            scheduleChanges = scheduleChanges
        )
    }

    companion object {
        fun fromDomain(prefs: NotificationPreferences): NotificationPreferencesDto {
            return NotificationPreferencesDto(
                emailNotifications = prefs.emailNotifications,
                pushNotifications = prefs.pushNotifications,
                smsNotifications = prefs.smsNotifications,
                visitReminders = prefs.visitReminders,
                approvalNotifications = prefs.approvalNotifications,
                scheduleChanges = prefs.scheduleChanges
            )
        }
    }
}
