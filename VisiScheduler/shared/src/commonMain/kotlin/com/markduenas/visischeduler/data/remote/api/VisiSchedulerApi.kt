package com.markduenas.visischeduler.data.remote.api

import com.markduenas.visischeduler.data.remote.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import com.markduenas.visischeduler.config.AppConfig

/**
 * API interface for VisiScheduler backend.
 */
class VisiSchedulerApi(
    private val client: HttpClient,
    private val baseUrl: String = AppConfig.apiBaseUrl
) {
    private var authToken: String? = null

    fun setAuthToken(token: String?) {
        authToken = token
    }

    // ==================== AUTH ====================

    suspend fun login(request: LoginRequestDto): AuthResponseDto {
        return client.post("$baseUrl/auth/login") {
            setBody(request)
        }.body()
    }

    suspend fun register(request: RegisterRequestDto): AuthResponseDto {
        return client.post("$baseUrl/auth/register") {
            setBody(request)
        }.body()
    }

    suspend fun refreshToken(refreshToken: String): AuthResponseDto {
        return client.post("$baseUrl/auth/refresh") {
            setBody(mapOf("refreshToken" to refreshToken))
        }.body()
    }

    suspend fun logout() {
        client.post("$baseUrl/auth/logout") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun requestPasswordReset(email: String) {
        client.post("$baseUrl/auth/password/reset") {
            setBody(mapOf("email" to email))
        }
    }

    suspend fun resetPassword(token: String, newPassword: String) {
        client.post("$baseUrl/auth/password/reset/confirm") {
            setBody(mapOf("token" to token, "newPassword" to newPassword))
        }
    }

    suspend fun verifyEmail(token: String) {
        client.post("$baseUrl/auth/email/verify") {
            setBody(mapOf("token" to token))
        }
    }

    suspend fun verifyMfa(challengeId: String, code: String): AuthResponseDto {
        return client.post("$baseUrl/auth/mfa/verify") {
            setBody(mapOf("challengeId" to challengeId, "code" to code))
        }.body()
    }

    suspend fun resendMfaCode(challengeId: String) {
        client.post("$baseUrl/auth/mfa/resend") {
            setBody(mapOf("challengeId" to challengeId))
        }
    }


    // ==================== USERS ====================

    suspend fun getCurrentUser(): UserDto {
        return client.get("$baseUrl/users/me") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun updateProfile(request: UserDto): UserDto {
        return client.put("$baseUrl/users/me") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    // ==================== VISITS ====================

    suspend fun getMyVisits(): List<VisitDto> {
        return client.get("$baseUrl/visits") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getVisitById(id: String): VisitDto {
        return client.get("$baseUrl/visits/$id") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun scheduleVisit(request: VisitRequestDto): VisitDto {
        return client.post("$baseUrl/visits") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun updateVisit(id: String, request: VisitRequestDto): VisitDto {
        return client.put("$baseUrl/visits/$id") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun cancelVisit(id: String, reason: String): VisitDto {
        return client.post("$baseUrl/visits/$id/cancel") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("reason" to reason))
        }.body()
    }

    suspend fun approveVisit(id: String, notes: String?): VisitDto {
        return client.post("$baseUrl/visits/$id/approve") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("notes" to notes))
        }.body()
    }

    suspend fun denyVisit(id: String, reason: String): VisitDto {
        return client.post("$baseUrl/visits/$id/deny") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("reason" to reason))
        }.body()
    }

    suspend fun checkInVisit(id: String): VisitDto {
        return client.post("$baseUrl/visits/$id/check-in") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun checkOutVisit(id: String): VisitDto {
        return client.post("$baseUrl/visits/$id/check-out") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getPendingApprovalVisits(): List<VisitDto> {
        return client.get("$baseUrl/visits/pending") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getVisitsForBeneficiary(beneficiaryId: String): List<VisitDto> {
        return client.get("$baseUrl/beneficiaries/$beneficiaryId/visits") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getBeneficiaryById(id: String): BeneficiaryDto {
        return client.get("$baseUrl/beneficiaries/$id") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }


    // ==================== MESSAGING ====================

    suspend fun getConversations(): List<ConversationDto> {
        return client.get("$baseUrl/messages/conversations") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getMessages(conversationId: String): List<MessageDto> {
        return client.get("$baseUrl/messages/conversations/$conversationId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun sendMessage(conversationId: String, request: SendMessageRequestDto): MessageDto {
        return client.post("$baseUrl/messages/conversations/$conversationId") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    // ==================== NOTIFICATIONS ====================

    suspend fun registerPushToken(token: String) {
        client.post("$baseUrl/notifications/push/register") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("token" to token))
        }
    }

    suspend fun unregisterPushToken() {
        client.post("$baseUrl/notifications/push/unregister") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun markNotificationRead(notificationId: String) {
        client.post("$baseUrl/notifications/$notificationId/read") {
            header("Authorization", "Bearer $authToken")
        }
    }
}
