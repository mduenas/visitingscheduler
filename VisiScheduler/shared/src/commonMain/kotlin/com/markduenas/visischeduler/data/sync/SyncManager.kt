package com.markduenas.visischeduler.data.sync

import com.markduenas.visischeduler.data.local.VisiSchedulerDatabase
import com.markduenas.visischeduler.data.remote.api.VisiSchedulerApi
import com.markduenas.visischeduler.data.remote.dto.VisitDto
import com.markduenas.visischeduler.data.remote.dto.VisitRequestDto
import com.markduenas.visischeduler.data.remote.dto.SendMessageRequestDto
import com.markduenas.visischeduler.domain.entities.Visit
import com.markduenas.visischeduler.domain.entities.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Types of operations that can be queued for synchronization.
 */
enum class SyncOperationType {
    CREATE_VISIT,
    UPDATE_VISIT_STATUS,
    CANCEL_VISIT,
    SEND_MESSAGE,
    MARK_NOTIFICATION_READ
}

/**
 * Manager responsible for handling offline data synchronization.
 */
class SyncManager(
    private val database: VisiSchedulerDatabase,
    private val api: VisiSchedulerApi,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Add an operation to the sync queue.
     */
    fun enqueueOperation(
        operationType: SyncOperationType,
        entityType: String,
        entityId: String?,
        payload: String
    ) {
        scope.launch {
            database.visiSchedulerQueries.addToSyncQueue(
                operationType = operationType.name,
                entityType = entityType,
                entityId = entityId,
                payload = payload,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
            processSyncQueue()
        }
    }

    /**
     * Process all pending operations in the sync queue.
     */
    suspend fun processSyncQueue() {
        if (_isSyncing.value) return
        _isSyncing.value = true

        try {
            val pending = database.visiSchedulerQueries.selectPendingOperations().executeAsList()
            for (op in pending) {
                val success = try {
                    executeOperation(op)
                    true
                } catch (e: Exception) {
                    database.visiSchedulerQueries.updateRetryCount(
                        lastError = e.message,
                        id = op.id
                    )
                    false
                }

                if (success) {
                    database.visiSchedulerQueries.deleteFromSyncQueue(op.id)
                } else {
                    // Stop processing on first failure to maintain order for dependent operations
                    break
                }
            }
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun executeOperation(op: com.markduenas.visischeduler.data.local.SyncQueueEntity) {
        when (SyncOperationType.valueOf(op.operationType)) {
            SyncOperationType.CREATE_VISIT -> {
                val visit: Visit = json.decodeFromString(op.payload)
                val request = VisitRequestDto(
                    beneficiaryId = visit.beneficiaryId,
                    scheduledDate = visit.scheduledDate.toString(),
                    startTime = visit.startTime.toString(),
                    endTime = visit.endTime.toString(),
                    visitType = visit.visitType.name,
                    purpose = visit.purpose,
                    notes = visit.notes,
                    additionalVisitors = visit.additionalVisitors.map { 
                        com.markduenas.visischeduler.data.remote.dto.AdditionalVisitorDto.fromDomain(it) 
                    }
                )
                val remoteVisit = api.scheduleVisit(request).toDomain()
                // Update local ID if it was temporary
                if (visit.id.startsWith("temp_")) {
                    database.visiSchedulerQueries.deleteVisit(visit.id)
                }
            }
            SyncOperationType.UPDATE_VISIT_STATUS -> {
                val visit: Visit = json.decodeFromString(op.payload)
                api.updateVisit(visit.id, VisitRequestDto(
                    beneficiaryId = visit.beneficiaryId,
                    scheduledDate = visit.scheduledDate.toString(),
                    startTime = visit.startTime.toString(),
                    endTime = visit.endTime.toString(),
                    visitType = visit.visitType.name,
                    purpose = visit.purpose,
                    notes = visit.notes,
                    additionalVisitors = visit.additionalVisitors.map { 
                        com.markduenas.visischeduler.data.remote.dto.AdditionalVisitorDto.fromDomain(it) 
                    }
                ))
            }
            SyncOperationType.CANCEL_VISIT -> {
                val payloadMap: Map<String, String> = json.decodeFromString(op.payload)
                val reason = payloadMap["reason"] ?: ""
                op.entityId?.let { api.cancelVisit(it, reason) }
            }
            SyncOperationType.SEND_MESSAGE -> {
                val message: Message = json.decodeFromString(op.payload)
                val request = com.markduenas.visischeduler.data.remote.dto.SendMessageRequestDto(
                    content = message.content,
                    replyToMessageId = message.replyToMessageId
                )
                api.sendMessage(message.conversationId, request)
            }
            SyncOperationType.MARK_NOTIFICATION_READ -> {
                op.entityId?.let { api.markNotificationRead(it) }
            }
        }
    }

    /**
     * Starts a periodic sync background task.
     */
    fun startPeriodicSync(intervalMs: Long = 60_000) {
        scope.launch {
            while (true) {
                processSyncQueue()
                delay(intervalMs)
            }
        }
    }
}
