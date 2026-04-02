package com.markduenas.visischeduler.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.Query
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.where
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cross-platform Firestore database wrapper using GitLive Firebase KMP SDK.
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
     * Create a document from a map (filters null values).
     */
    suspend fun createFromMap(collection: String, data: Map<String, Any?>): String {
        val filteredData = data.filterValues { it != null }
        val docRef = firestore.collection(collection).add(filteredData)
        return docRef.id
    }

    /**
     * Create a document with a specific ID from a map (filters null values).
     */
    suspend fun createWithIdFromMap(collection: String, id: String, data: Map<String, Any?>) {
        val filteredData = data.filterValues { it != null }
        firestore.collection(collection).document(id).set(filteredData)
    }

    /**
     * Get a document by ID.
     */
    suspend fun getById(collection: String, id: String): DocumentSnapshot? {
        val doc = firestore.collection(collection).document(id).get()
        return if (doc.exists) doc else null
    }

    /**
     * Update a document.
     */
    suspend fun update(collection: String, id: String, updates: Map<String, Any>) {
        firestore.collection(collection).document(id).update(updates)
    }

    /**
     * Update a document from a map (filters null values).
     */
    suspend fun updateFromMap(collection: String, id: String, updates: Map<String, Any?>) {
        val filteredUpdates = updates.filterValues { it != null }.mapValues { it.value!! }
        if (filteredUpdates.isNotEmpty()) {
            firestore.collection(collection).document(id).update(filteredUpdates)
        }
    }

    /**
     * Delete a document.
     */
    suspend fun delete(collection: String, id: String) {
        firestore.collection(collection).document(id).delete()
    }

    /**
     * Get all documents in a collection.
     */
    suspend fun getAll(collection: String): List<DocumentSnapshot> {
        return firestore.collection(collection).get().documents
    }

    /**
     * Query documents with equality condition.
     */
    suspend fun query(
        collection: String,
        field: String,
        value: Any,
        operator: QueryOperator = QueryOperator.EQUAL
    ): List<DocumentSnapshot> {
        val query = when (operator) {
            QueryOperator.EQUAL -> firestore.collection(collection).where { field equalTo value }
            QueryOperator.NOT_EQUAL -> firestore.collection(collection).where { field notEqualTo value }
            QueryOperator.LESS_THAN -> firestore.collection(collection).where { field lessThan value }
            QueryOperator.LESS_THAN_OR_EQUAL -> firestore.collection(collection).where { field lessThanOrEqualTo value }
            QueryOperator.GREATER_THAN -> firestore.collection(collection).where { field greaterThan value }
            QueryOperator.GREATER_THAN_OR_EQUAL -> firestore.collection(collection).where { field greaterThanOrEqualTo value }
            QueryOperator.ARRAY_CONTAINS -> firestore.collection(collection).where { field contains value }
        }
        return query.get().documents
    }

    /**
     * Listen to a document in real-time.
     */
    fun listenToDocument(collection: String, id: String): Flow<DocumentSnapshot?> {
        return firestore.collection(collection).document(id).snapshots.map { doc ->
            if (doc.exists) doc else null
        }
    }

    /**
     * Listen to a collection in real-time.
     */
    fun listenToCollection(collection: String): Flow<List<DocumentSnapshot>> {
        return firestore.collection(collection).snapshots.map { it.documents }
    }

    /**
     * Listen to a query in real-time.
     */
    fun listenToQuery(
        collection: String,
        field: String,
        value: Any,
        orderBy: String? = null,
        limit: Int? = null
    ): Flow<List<DocumentSnapshot>> {
        var query: Query = firestore.collection(collection).where { field equalTo value }

        orderBy?.let { query = query.orderBy(it) }
        limit?.let { query = query.limit(it) }

        return query.snapshots.map { it.documents }
    }

    // ==================== User Operations ====================

    suspend fun createUser(userId: String, userData: Map<String, Any?>) {
        createWithIdFromMap(COLLECTION_USERS, userId, userData)
    }

    suspend fun getUser(userId: String): DocumentSnapshot? {
        return getById(COLLECTION_USERS, userId)
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_USERS, userId, updates)
    }

    fun listenToUser(userId: String): Flow<DocumentSnapshot?> {
        return listenToDocument(COLLECTION_USERS, userId)
    }

    // ==================== Visit Operations ====================

    suspend fun createVisit(visitData: Map<String, Any?>): String {
        return createFromMap(COLLECTION_VISITS, visitData)
    }

    suspend fun getVisit(visitId: String): DocumentSnapshot? {
        return getById(COLLECTION_VISITS, visitId)
    }

    suspend fun updateVisit(visitId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_VISITS, visitId, updates)
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

    suspend fun updateBeneficiary(beneficiaryId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_BENEFICIARIES, beneficiaryId, updates)
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

    suspend fun getRestrictionsForVisitor(visitorId: String): List<DocumentSnapshot> {
        return query(COLLECTION_RESTRICTIONS, "visitorId", visitorId)
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

    suspend fun getConversation(conversationId: String): DocumentSnapshot? {
        return getById(COLLECTION_CONVERSATIONS, conversationId)
    }

    suspend fun sendMessage(conversationId: String, messageData: Map<String, Any?>): String {
        val filteredData = messageData.filterValues { it != null }
        val docRef = firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .add(filteredData)

        // Update conversation's last message
        val updates = mutableMapOf<String, Any>()
        messageData["timestamp"]?.let { updates["lastMessageAt"] = it }
        messageData["content"]?.let { updates["lastMessagePreview"] = it }
        if (updates.isNotEmpty()) {
            firestore.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(updates)
        }

        return docRef.id
    }

    fun listenToConversations(userId: String): Flow<List<DocumentSnapshot>> {
        return firestore.collection(COLLECTION_CONVERSATIONS)
            .where { "participantIds" contains userId }
            .orderBy("lastMessageAt", Direction.DESCENDING)
            .snapshots
            .map { it.documents }
    }

    fun listenToMessages(conversationId: String): Flow<List<DocumentSnapshot>> {
        return firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .orderBy("timestamp", Direction.ASCENDING)
            .snapshots
            .map { it.documents }
    }

    suspend fun deleteMessage(conversationId: String, messageId: String) {
        firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .document(messageId)
            .delete()
    }

    suspend fun editMessage(conversationId: String, messageId: String, newContent: String) {
        firestore.collection(COLLECTION_CONVERSATIONS)
            .document(conversationId)
            .collection(COLLECTION_MESSAGES)
            .document(messageId)
            .update(mapOf(
                "content" to newContent,
                "editedAt" to serverTimestamp()
            ))
    }

    // ==================== Check-in Operations ====================

    suspend fun createCheckIn(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_CHECK_INS, data)
    }

    suspend fun getCheckIn(checkInId: String): DocumentSnapshot? {
        return getById(COLLECTION_CHECK_INS, checkInId)
    }

    suspend fun updateCheckIn(checkInId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_CHECK_INS, checkInId, updates)
    }

    suspend fun getActiveCheckIn(visitId: String): DocumentSnapshot? {
        val docs = firestore.collection(COLLECTION_CHECK_INS)
            .where { "visitId" equalTo visitId }
            .where { "checkOutTime" equalTo null }
            .get()
            .documents
        return docs.firstOrNull()
    }

    suspend fun getCheckInsForVisit(visitId: String): List<DocumentSnapshot> {
        return query(COLLECTION_CHECK_INS, "visitId", visitId)
    }

    // ==================== Notification Operations ====================

    suspend fun createNotification(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_NOTIFICATIONS, data)
    }

    suspend fun markNotificationRead(notificationId: String) {
        update(COLLECTION_NOTIFICATIONS, notificationId, mapOf("isRead" to true))
    }

    suspend fun deleteNotification(notificationId: String) {
        delete(COLLECTION_NOTIFICATIONS, notificationId)
    }

    fun listenToNotifications(userId: String): Flow<List<DocumentSnapshot>> {
        return listenToQuery(COLLECTION_NOTIFICATIONS, "userId", userId, "timestamp", 50)
    }

    // ==================== Time Slot Operations ====================

    suspend fun createTimeSlot(data: Map<String, Any?>): String {
        return createFromMap(COLLECTION_TIME_SLOTS, data)
    }

    suspend fun getTimeSlot(slotId: String): DocumentSnapshot? {
        return getById(COLLECTION_TIME_SLOTS, slotId)
    }

    suspend fun updateTimeSlot(slotId: String, updates: Map<String, Any?>) {
        updateFromMap(COLLECTION_TIME_SLOTS, slotId, updates)
    }

    suspend fun deleteTimeSlot(slotId: String) {
        delete(COLLECTION_TIME_SLOTS, slotId)
    }

    suspend fun getTimeSlotsForDate(facilityId: String, date: String): List<DocumentSnapshot> {
        return firestore.collection(COLLECTION_TIME_SLOTS)
            .where { "facilityId" equalTo facilityId }
            .where { "date" equalTo date }
            .get()
            .documents
    }

    suspend fun getAvailableTimeSlotsForDate(date: String): List<DocumentSnapshot> {
        return firestore.collection(COLLECTION_TIME_SLOTS)
            .where { "date" equalTo date }
            .where { "isAvailable" equalTo true }
            .get()
            .documents
    }

    suspend fun getTimeSlotsForDateRange(startDate: String, endDate: String): List<DocumentSnapshot> {
        return firestore.collection(COLLECTION_TIME_SLOTS)
            .where { "date" greaterThanOrEqualTo startDate }
            .where { "date" lessThanOrEqualTo endDate }
            .get()
            .documents
    }

    fun listenToTimeSlotsForDate(facilityId: String, date: String): Flow<List<DocumentSnapshot>> {
        return firestore.collection(COLLECTION_TIME_SLOTS)
            .where { "facilityId" equalTo facilityId }
            .where { "date" equalTo date }
            .snapshots
            .map { it.documents }
    }

    fun listenToAvailableTimeSlots(date: String): Flow<List<DocumentSnapshot>> {
        return firestore.collection(COLLECTION_TIME_SLOTS)
            .where { "date" equalTo date }
            .where { "isAvailable" equalTo true }
            .snapshots
            .map { it.documents }
    }

    // ==================== Utility ====================

    /**
     * Get server timestamp for use in documents.
     */
    fun serverTimestamp(): FieldValue = FieldValue.serverTimestamp

    /**
     * Increment a field value.
     */
    fun increment(value: Int): FieldValue = FieldValue.increment(value)
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
