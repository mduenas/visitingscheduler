package com.markduenas.visischeduler.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore database wrapper for VisiScheduler.
 * Provides CRUD operations and real-time listeners for all collections.
 */
class FirestoreDatabase {

    private val firestore: FirebaseFirestore by lazy { Firebase.firestore }

    companion object {
        // Collection names
        const val COLLECTION_USERS = "users"
        const val COLLECTION_BENEFICIARIES = "beneficiaries"
        const val COLLECTION_VISITS = "visits"
        const val COLLECTION_TIME_SLOTS = "timeSlots"
        const val COLLECTION_RESTRICTIONS = "restrictions"
        const val COLLECTION_MESSAGES = "messages"
        const val COLLECTION_CONVERSATIONS = "conversations"
        const val COLLECTION_CHECK_INS = "checkIns"
        const val COLLECTION_NOTIFICATIONS = "notifications"

        // Subcollection names
        const val SUBCOLLECTION_VISITORS = "visitors"
        const val SUBCOLLECTION_COORDINATORS = "coordinators"
    }

    // ==================== Generic CRUD Operations ====================

    /**
     * Create a new document in a collection.
     */
    suspend fun <T : Any> create(collection: String, data: T): String {
        val docRef = firestore.collection(collection).add(data).await()
        return docRef.id
    }

    /**
     * Create a document from a map (filters null values).
     */
    suspend fun createFromMap(collection: String, data: Map<String, Any?>): String {
        val filteredData = data.filterValues { it != null }
        val docRef = firestore.collection(collection).add(filteredData).await()
        return docRef.id
    }

    /**
     * Create a document with a specific ID.
     */
    suspend fun <T : Any> createWithId(collection: String, id: String, data: T) {
        firestore.collection(collection).document(id).set(data).await()
    }

    /**
     * Create a document with a specific ID from a map (filters null values).
     */
    suspend fun createWithIdFromMap(collection: String, id: String, data: Map<String, Any?>) {
        val filteredData = data.filterValues { it != null }
        firestore.collection(collection).document(id).set(filteredData).await()
    }

    /**
     * Get a document by ID.
     */
    suspend fun getById(collection: String, id: String): DocumentSnapshot? {
        return firestore.collection(collection).document(id).get().await()
    }

    /**
     * Update a document.
     */
    suspend fun update(collection: String, id: String, updates: Map<String, Any>) {
        firestore.collection(collection).document(id).update(updates).await()
    }

    /**
     * Update a document from a map (filters null values).
     */
    suspend fun updateFromMap(collection: String, id: String, updates: Map<String, Any?>) {
        val filteredUpdates = updates.filterValues { it != null }.mapValues { it.value!! }
        if (filteredUpdates.isNotEmpty()) {
            firestore.collection(collection).document(id).update(filteredUpdates).await()
        }
    }

    /**
     * Delete a document.
     */
    suspend fun delete(collection: String, id: String) {
        firestore.collection(collection).document(id).delete().await()
    }

    /**
     * Get all documents in a collection.
     */
    suspend fun getAll(collection: String): List<DocumentSnapshot> {
        return firestore.collection(collection).get().await().documents
    }

    /**
     * Query documents with conditions.
     */
    suspend fun query(
        collection: String,
        field: String,
        value: Any,
        operator: QueryOperator = QueryOperator.EQUAL
    ): List<DocumentSnapshot> {
        val query = when (operator) {
            QueryOperator.EQUAL -> firestore.collection(collection).whereEqualTo(field, value)
            QueryOperator.NOT_EQUAL -> firestore.collection(collection).whereNotEqualTo(field, value)
            QueryOperator.LESS_THAN -> firestore.collection(collection).whereLessThan(field, value)
            QueryOperator.LESS_THAN_OR_EQUAL -> firestore.collection(collection).whereLessThanOrEqualTo(field, value)
            QueryOperator.GREATER_THAN -> firestore.collection(collection).whereGreaterThan(field, value)
            QueryOperator.GREATER_THAN_OR_EQUAL -> firestore.collection(collection).whereGreaterThanOrEqualTo(field, value)
            QueryOperator.ARRAY_CONTAINS -> firestore.collection(collection).whereArrayContains(field, value)
        }
        return query.get().await().documents
    }

    /**
     * Listen to a document in real-time.
     */
    fun listenToDocument(collection: String, id: String): Flow<DocumentSnapshot?> = callbackFlow {
        val listener = firestore.collection(collection).document(id)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Listen to a collection in real-time.
     */
    fun listenToCollection(collection: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = firestore.collection(collection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    /**
     * Listen to a query in real-time.
     */
    fun listenToQuery(
        collection: String,
        field: String,
        value: Any,
        orderBy: String? = null,
        limit: Long? = null
    ): Flow<List<DocumentSnapshot>> = callbackFlow {
        var query: Query = firestore.collection(collection).whereEqualTo(field, value)

        orderBy?.let { query = query.orderBy(it) }
        limit?.let { query = query.limit(it) }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.documents ?: emptyList())
        }
        awaitClose { listener.remove() }
    }

    // ==================== User Operations ====================

    suspend fun createUser(userId: String, userData: Map<String, Any?>) {
        val filteredData = userData.filterValues { it != null }
        firestore.collection(COLLECTION_USERS).document(userId).set(filteredData).await()
    }

    suspend fun getUser(userId: String): DocumentSnapshot? {
        return getById(COLLECTION_USERS, userId)
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any?>) {
        val filteredUpdates = updates.filterValues { it != null }.mapValues { it.value!! }
        if (filteredUpdates.isNotEmpty()) {
            update(COLLECTION_USERS, userId, filteredUpdates)
        }
    }

    fun listenToUser(userId: String): Flow<DocumentSnapshot?> {
        return listenToDocument(COLLECTION_USERS, userId)
    }

    // ==================== Visit Operations ====================

    suspend fun createVisit(visitData: Map<String, Any?>): String {
        val filteredData = visitData.filterValues { it != null }
        return createFromMap(COLLECTION_VISITS, filteredData)
    }

    suspend fun getVisit(visitId: String): DocumentSnapshot? {
        return getById(COLLECTION_VISITS, visitId)
    }

    suspend fun updateVisit(visitId: String, updates: Map<String, Any?>) {
        val filteredUpdates = updates.filterValues { it != null }.mapValues { it.value!! }
        if (filteredUpdates.isNotEmpty()) {
            update(COLLECTION_VISITS, visitId, filteredUpdates)
        }
    }

    suspend fun getVisitsForBeneficiary(beneficiaryId: String): List<DocumentSnapshot> {
        return query(COLLECTION_VISITS, "beneficiaryId", beneficiaryId)
    }

    suspend fun getVisitsForVisitor(visitorId: String): List<DocumentSnapshot> {
        return query(COLLECTION_VISITS, "visitorId", visitorId)
    }

    suspend fun getVisitsByStatus(status: String): List<DocumentSnapshot> {
        return query(COLLECTION_VISITS, "status", status)
    }

    fun listenToVisitsForBeneficiary(beneficiaryId: String): Flow<List<DocumentSnapshot>> {
        return listenToQuery(COLLECTION_VISITS, "beneficiaryId", beneficiaryId, "scheduledDate")
    }

    // ==================== Beneficiary Operations ====================

    suspend fun createBeneficiary(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_BENEFICIARIES, data)
    }

    suspend fun getBeneficiary(beneficiaryId: String): DocumentSnapshot? {
        return getById(COLLECTION_BENEFICIARIES, beneficiaryId)
    }

    suspend fun getBeneficiariesForCoordinator(coordinatorId: String): List<DocumentSnapshot> {
        return query(COLLECTION_BENEFICIARIES, "coordinatorIds", coordinatorId, QueryOperator.ARRAY_CONTAINS)
    }

    fun listenToBeneficiary(beneficiaryId: String): Flow<DocumentSnapshot?> {
        return listenToDocument(COLLECTION_BENEFICIARIES, beneficiaryId)
    }

    // ==================== Restriction Operations ====================

    suspend fun createRestriction(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_RESTRICTIONS, data)
    }

    suspend fun getRestrictionsForBeneficiary(beneficiaryId: String): List<DocumentSnapshot> {
        return query(COLLECTION_RESTRICTIONS, "beneficiaryId", beneficiaryId)
    }

    suspend fun updateRestriction(restrictionId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_RESTRICTIONS, restrictionId, updates)
    }

    suspend fun deleteRestriction(restrictionId: String) {
        delete(COLLECTION_RESTRICTIONS, restrictionId)
    }

    // ==================== Message Operations ====================

    suspend fun createConversation(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_CONVERSATIONS, data)
    }

    suspend fun sendMessage(conversationId: String, messageData: Map<String, Any?>): String {
        val filteredData = messageData.filterValues { it != null }
        val docRef = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .add(filteredData)
            .await()

        // Update conversation's last message
        val updates = mutableMapOf<String, Any>()
        messageData["timestamp"]?.let { updates["lastMessageAt"] = it }
        messageData["content"]?.let { updates["lastMessagePreview"] = it }
        if (updates.isNotEmpty()) {
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(updates).await()
        }

        return docRef.id
    }

    fun listenToConversations(userId: String): Flow<List<DocumentSnapshot>> {
        return listenToQuery(COLLECTION_CONVERSATIONS, "participantIds", userId, "lastMessageAt")
    }

    fun listenToMessages(conversationId: String): Flow<List<DocumentSnapshot>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ==================== Check-in Operations ====================

    suspend fun createCheckIn(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_CHECK_INS, data)
    }

    suspend fun updateCheckIn(checkInId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_CHECK_INS, checkInId, updates)
    }

    suspend fun getActiveCheckIn(visitId: String): DocumentSnapshot? {
        val docs = firestore.collection(COLLECTION_CHECK_INS)
            .whereEqualTo("visitId", visitId)
            .whereEqualTo("checkOutTime", null)
            .get()
            .await()
            .documents
        return docs.firstOrNull()
    }

    // ==================== Notification Operations ====================

    suspend fun createNotification(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_NOTIFICATIONS, data)
    }

    suspend fun markNotificationRead(notificationId: String) {
        update(COLLECTION_NOTIFICATIONS, notificationId, mapOf("isRead" to true))
    }

    fun listenToNotifications(userId: String): Flow<List<DocumentSnapshot>> {
        return listenToQuery(COLLECTION_NOTIFICATIONS, "userId", userId, "timestamp", 50)
    }

    // ==================== Batch Operations ====================

    /**
     * Perform multiple writes atomically.
     */
    suspend fun batchWrite(operations: List<BatchOperation>) {
        val batch = firestore.batch()

        operations.forEach { op ->
            val docRef = firestore.collection(op.collection).document(op.documentId)
            when (op.type) {
                BatchOperationType.SET -> batch.set(docRef, op.data!!)
                BatchOperationType.UPDATE -> batch.update(docRef, op.data!!)
                BatchOperationType.DELETE -> batch.delete(docRef)
            }
        }

        batch.commit().await()
    }

    /**
     * Run a transaction.
     */
    suspend fun <T> runTransaction(block: suspend (FirebaseFirestore) -> T): T {
        return firestore.runTransaction { transaction ->
            // Note: This is a simplified version. In practice, you'd pass the transaction object.
            kotlinx.coroutines.runBlocking { block(firestore) }
        }.await()
    }
}

enum class QueryOperator {
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    ARRAY_CONTAINS
}

data class BatchOperation(
    val type: BatchOperationType,
    val collection: String,
    val documentId: String,
    val data: Map<String, Any>? = null
)

enum class BatchOperationType {
    SET, UPDATE, DELETE
}
