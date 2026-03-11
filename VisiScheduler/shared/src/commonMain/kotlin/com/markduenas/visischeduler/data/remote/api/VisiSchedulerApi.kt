package com.markduenas.visischeduler.data.remote.api

import com.markduenas.visischeduler.data.remote.dto.AuthResponseDto
import com.markduenas.visischeduler.data.remote.dto.BeneficiaryDto
import com.markduenas.visischeduler.data.remote.dto.CheckInDto
import com.markduenas.visischeduler.data.remote.dto.CheckInRequestDto
import com.markduenas.visischeduler.data.remote.dto.CheckInStatisticsDto
import com.markduenas.visischeduler.data.remote.dto.CheckOutRequestDto
import com.markduenas.visischeduler.data.remote.dto.ContactDto
import com.markduenas.visischeduler.data.remote.dto.ConversationDto
import com.markduenas.visischeduler.data.remote.dto.CreateConversationRequestDto
import com.markduenas.visischeduler.data.remote.dto.EditMessageRequestDto
import com.markduenas.visischeduler.data.remote.dto.ExpectedVisitorDto
import com.markduenas.visischeduler.data.remote.dto.LoginRequestDto
import com.markduenas.visischeduler.data.remote.dto.MessageDto
import com.markduenas.visischeduler.data.remote.dto.PaginatedMessagesResponseDto
import com.markduenas.visischeduler.data.remote.dto.QrCodeDataDto
import com.markduenas.visischeduler.data.remote.dto.QrValidationResponseDto
import com.markduenas.visischeduler.data.remote.dto.RegisterRequestDto
import com.markduenas.visischeduler.data.remote.dto.RestrictionDto
import com.markduenas.visischeduler.data.remote.dto.SendMessageRequestDto
import com.markduenas.visischeduler.data.remote.dto.TimeSlotDto
import com.markduenas.visischeduler.data.remote.dto.UpdateConversationRequestDto
import com.markduenas.visischeduler.data.remote.dto.UserDto
import com.markduenas.visischeduler.data.remote.dto.ValidateQrRequestDto
import com.markduenas.visischeduler.data.remote.dto.VisitDto
import com.markduenas.visischeduler.data.remote.dto.VisitorBadgeDto
import com.markduenas.visischeduler.data.remote.dto.VisitRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

import com.markduenas.visischeduler.config.AppConfig
...
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
            setBody(mapOf(
                "challengeId" to challengeId,
                "code" to code
            ))
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

    suspend fun getUserById(userId: String): UserDto {
        return client.get("$baseUrl/users/$userId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any?>): UserDto {
        return client.put("$baseUrl/users/$userId") {
            header("Authorization", "Bearer $authToken")
            setBody(updates)
        }.body()
    }

    suspend fun getAllUsers(): List<UserDto> {
        return client.get("$baseUrl/users") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun searchUsers(query: String): List<UserDto> {
        return client.get("$baseUrl/users/search") {
            header("Authorization", "Bearer $authToken")
            parameter("q", query)
        }.body()
    }

    suspend fun getPendingVisitors(): List<UserDto> {
        return client.get("$baseUrl/users/pending") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun approveVisitor(userId: String): UserDto {
        return client.post("$baseUrl/users/$userId/approve") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun denyVisitor(userId: String, reason: String) {
        client.post("$baseUrl/users/$userId/deny") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("reason" to reason))
        }
    }

    // ==================== VISITS ====================

    suspend fun getMyVisits(): List<VisitDto> {
        return client.get("$baseUrl/visits/my") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getVisitById(visitId: String): VisitDto {
        return client.get("$baseUrl/visits/$visitId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getVisitsForBeneficiary(beneficiaryId: String): List<VisitDto> {
        return client.get("$baseUrl/beneficiaries/$beneficiaryId/visits") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getPendingApprovalVisits(): List<VisitDto> {
        return client.get("$baseUrl/visits/pending") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun scheduleVisit(request: VisitRequestDto): VisitDto {
        return client.post("$baseUrl/visits") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun updateVisit(visitId: String, request: VisitRequestDto): VisitDto {
        return client.put("$baseUrl/visits/$visitId") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun cancelVisit(visitId: String, reason: String): VisitDto {
        return client.post("$baseUrl/visits/$visitId/cancel") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("reason" to reason))
        }.body()
    }

    suspend fun approveVisit(visitId: String, notes: String?): VisitDto {
        return client.post("$baseUrl/visits/$visitId/approve") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("notes" to notes))
        }.body()
    }

    suspend fun denyVisit(visitId: String, reason: String): VisitDto {
        return client.post("$baseUrl/visits/$visitId/deny") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("reason" to reason))
        }.body()
    }

    suspend fun checkInVisit(visitId: String): VisitDto {
        return client.post("$baseUrl/visits/$visitId/checkin") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun checkOutVisit(visitId: String): VisitDto {
        return client.post("$baseUrl/visits/$visitId/checkout") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    // ==================== BENEFICIARIES ====================

    suspend fun getBeneficiaries(): List<BeneficiaryDto> {
        return client.get("$baseUrl/beneficiaries") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getBeneficiaryById(beneficiaryId: String): BeneficiaryDto {
        return client.get("$baseUrl/beneficiaries/$beneficiaryId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getMyBeneficiaries(): List<BeneficiaryDto> {
        return client.get("$baseUrl/beneficiaries/my") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    // ==================== TIME SLOTS ====================

    suspend fun getAvailableSlots(
        beneficiaryId: String,
        startDate: String,
        endDate: String
    ): List<TimeSlotDto> {
        return client.get("$baseUrl/slots/available") {
            header("Authorization", "Bearer $authToken")
            parameter("beneficiaryId", beneficiaryId)
            parameter("startDate", startDate)
            parameter("endDate", endDate)
        }.body()
    }

    suspend fun getSlotById(slotId: String): TimeSlotDto {
        return client.get("$baseUrl/slots/$slotId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    // ==================== RESTRICTIONS ====================

    suspend fun getRestrictions(): List<RestrictionDto> {
        return client.get("$baseUrl/restrictions") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getRestrictionById(restrictionId: String): RestrictionDto {
        return client.get("$baseUrl/restrictions/$restrictionId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun createRestriction(restriction: RestrictionDto): RestrictionDto {
        return client.post("$baseUrl/restrictions") {
            header("Authorization", "Bearer $authToken")
            setBody(restriction)
        }.body()
    }

    suspend fun updateRestriction(restrictionId: String, restriction: RestrictionDto): RestrictionDto {
        return client.put("$baseUrl/restrictions/$restrictionId") {
            header("Authorization", "Bearer $authToken")
            setBody(restriction)
        }.body()
    }

    suspend fun deleteRestriction(restrictionId: String) {
        client.delete("$baseUrl/restrictions/$restrictionId") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun checkRestrictions(
        visitorId: String,
        beneficiaryId: String,
        visitDate: String,
        startTime: String,
        endTime: String,
        additionalVisitorCount: Int
    ): List<RestrictionDto> {
        return client.post("$baseUrl/restrictions/check") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf(
                "visitorId" to visitorId,
                "beneficiaryId" to beneficiaryId,
                "visitDate" to visitDate,
                "startTime" to startTime,
                "endTime" to endTime,
                "additionalVisitorCount" to additionalVisitorCount
            ))
        }.body()
    }

    // ==================== CHECK-IN ====================

    suspend fun checkInVisit(visitId: String, request: CheckInRequestDto): CheckInDto {
        return client.post("$baseUrl/visits/$visitId/check-in") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun checkOutFromCheckIn(checkInId: String, request: CheckOutRequestDto): CheckInDto {
        return client.post("$baseUrl/check-ins/$checkInId/check-out") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun getCheckInById(checkInId: String): CheckInDto {
        return client.get("$baseUrl/check-ins/$checkInId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getActiveCheckIn(visitId: String): CheckInDto? {
        return client.get("$baseUrl/visits/$visitId/active-check-in") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getCheckInsForVisit(visitId: String): List<CheckInDto> {
        return client.get("$baseUrl/visits/$visitId/check-ins") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getRecentCheckIns(date: String): List<CheckInDto> {
        return client.get("$baseUrl/check-ins/recent") {
            header("Authorization", "Bearer $authToken")
            parameter("date", date)
        }.body()
    }

    // ==================== QR CODE ====================

    suspend fun generateQrCode(visitId: String): QrCodeDataDto {
        return client.get("$baseUrl/visits/$visitId/qr-code") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun validateQrCode(request: ValidateQrRequestDto): QrValidationResponseDto {
        return client.post("$baseUrl/qr-code/validate") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    // ==================== EXPECTED VISITORS ====================

    suspend fun getExpectedVisitors(date: String): List<ExpectedVisitorDto> {
        return client.get("$baseUrl/visitors/expected") {
            header("Authorization", "Bearer $authToken")
            parameter("date", date)
        }.body()
    }

    // ==================== VISITOR BADGE ====================

    suspend fun generateVisitorBadge(checkInId: String): VisitorBadgeDto {
        return client.get("$baseUrl/check-ins/$checkInId/badge") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun verifyBadge(request: ValidateQrRequestDto): VisitorBadgeDto {
        return client.post("$baseUrl/badges/verify") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    // ==================== CHECK-IN STATISTICS ====================

    suspend fun getCheckInStatistics(startDate: String, endDate: String): CheckInStatisticsDto {
        return client.get("$baseUrl/check-ins/statistics") {
            header("Authorization", "Bearer $authToken")
            parameter("startDate", startDate)
            parameter("endDate", endDate)
        }.body()
    }

    // ==================== MESSAGING - CONVERSATIONS ====================

    suspend fun getConversations(): List<ConversationDto> {
        return client.get("$baseUrl/conversations") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getArchivedConversations(): List<ConversationDto> {
        return client.get("$baseUrl/conversations/archived") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getConversationById(conversationId: String): ConversationDto {
        return client.get("$baseUrl/conversations/$conversationId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getConversationByBeneficiaryId(beneficiaryId: String): ConversationDto? {
        return client.get("$baseUrl/conversations/beneficiary/$beneficiaryId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun createConversation(request: CreateConversationRequestDto): ConversationDto {
        return client.post("$baseUrl/conversations") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun updateConversation(
        conversationId: String,
        request: UpdateConversationRequestDto
    ): ConversationDto {
        return client.put("$baseUrl/conversations/$conversationId") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun archiveConversation(conversationId: String) {
        client.post("$baseUrl/conversations/$conversationId/archive") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun unarchiveConversation(conversationId: String) {
        client.post("$baseUrl/conversations/$conversationId/unarchive") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun deleteConversation(conversationId: String) {
        client.delete("$baseUrl/conversations/$conversationId") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun addConversationParticipants(
        conversationId: String,
        participantIds: List<String>
    ): ConversationDto {
        return client.post("$baseUrl/conversations/$conversationId/participants") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("participantIds" to participantIds))
        }.body()
    }

    suspend fun removeConversationParticipant(
        conversationId: String,
        participantId: String
    ): ConversationDto {
        return client.delete("$baseUrl/conversations/$conversationId/participants/$participantId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun leaveConversation(conversationId: String) {
        client.post("$baseUrl/conversations/$conversationId/leave") {
            header("Authorization", "Bearer $authToken")
        }
    }

    // ==================== MESSAGING - MESSAGES ====================

    suspend fun getMessages(conversationId: String): List<MessageDto> {
        return client.get("$baseUrl/conversations/$conversationId/messages") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun getMessagesPaginated(
        conversationId: String,
        limit: Int,
        beforeMessageId: String?
    ): PaginatedMessagesResponseDto {
        return client.get("$baseUrl/conversations/$conversationId/messages") {
            header("Authorization", "Bearer $authToken")
            parameter("limit", limit)
            beforeMessageId?.let { parameter("before", it) }
        }.body()
    }

    suspend fun getMessageById(messageId: String): MessageDto {
        return client.get("$baseUrl/messages/$messageId") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun sendMessage(
        conversationId: String,
        request: SendMessageRequestDto
    ): MessageDto {
        return client.post("$baseUrl/conversations/$conversationId/messages") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String?,
        attachmentPath: String,
        attachmentType: String
    ): MessageDto {
        // In a real implementation, this would use multipart form data
        return client.post("$baseUrl/conversations/$conversationId/messages/attachment") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf(
                "content" to content,
                "attachmentPath" to attachmentPath,
                "attachmentType" to attachmentType
            ))
        }.body()
    }

    suspend fun editMessage(messageId: String, request: EditMessageRequestDto): MessageDto {
        return client.put("$baseUrl/messages/$messageId") {
            header("Authorization", "Bearer $authToken")
            setBody(request)
        }.body()
    }

    suspend fun deleteMessage(messageId: String) {
        client.delete("$baseUrl/messages/$messageId") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun markConversationAsRead(conversationId: String) {
        client.post("$baseUrl/conversations/$conversationId/read") {
            header("Authorization", "Bearer $authToken")
        }
    }

    suspend fun markMessagesAsRead(messageIds: List<String>) {
        client.post("$baseUrl/messages/read") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("messageIds" to messageIds))
        }
    }

    suspend fun searchMessages(conversationId: String, query: String): List<MessageDto> {
        return client.get("$baseUrl/conversations/$conversationId/messages/search") {
            header("Authorization", "Bearer $authToken")
            parameter("q", query)
        }.body()
    }

    suspend fun searchAllMessages(query: String): List<MessageDto> {
        return client.get("$baseUrl/messages/search") {
            header("Authorization", "Bearer $authToken")
            parameter("q", query)
        }.body()
    }

    // ==================== MESSAGING - TYPING INDICATORS ====================

    suspend fun sendTypingIndicator(conversationId: String, isTyping: Boolean) {
        client.post("$baseUrl/conversations/$conversationId/typing") {
            header("Authorization", "Bearer $authToken")
            setBody(mapOf("isTyping" to isTyping))
        }
    }

    // ==================== MESSAGING - CONTACTS ====================

    suspend fun getContacts(): List<ContactDto> {
        return client.get("$baseUrl/contacts") {
            header("Authorization", "Bearer $authToken")
        }.body()
    }

    suspend fun searchContacts(query: String): List<ContactDto> {
        return client.get("$baseUrl/contacts/search") {
            header("Authorization", "Bearer $authToken")
            parameter("q", query)
        }.body()
    }

    suspend fun getRecentContacts(limit: Int): List<ContactDto> {
        return client.get("$baseUrl/contacts/recent") {
            header("Authorization", "Bearer $authToken")
            parameter("limit", limit)
        }.body()
    }

    // ==================== MESSAGING - PUSH NOTIFICATIONS ====================

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
}
