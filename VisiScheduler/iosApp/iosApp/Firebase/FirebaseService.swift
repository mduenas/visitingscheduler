import Foundation
import FirebaseCore
import FirebaseAnalytics
import FirebaseCrashlytics
import FirebaseFirestore
import FirebaseAuth

/// Firebase service for iOS - mirrors the Android FirebaseService functionality
/// Provides Analytics, Crashlytics, and Firestore operations
@objc public class FirebaseService: NSObject {

    // MARK: - Singleton
    @objc public static let shared = FirebaseService()

    // MARK: - Properties
    private let db = Firestore.firestore()
    private let analytics = Analytics.self
    private let crashlytics = Crashlytics.crashlytics()

    // MARK: - Collection Names
    public struct Collections {
        public static let users = "users"
        public static let beneficiaries = "beneficiaries"
        public static let visits = "visits"
        public static let timeSlots = "timeSlots"
        public static let restrictions = "restrictions"
        public static let messages = "messages"
        public static let conversations = "conversations"
        public static let checkIns = "checkIns"
        public static let notifications = "notifications"
    }

    // MARK: - Event Names
    public struct Events {
        public static let visitScheduled = "visit_scheduled"
        public static let visitApproved = "visit_approved"
        public static let visitDenied = "visit_denied"
        public static let visitCancelled = "visit_cancelled"
        public static let visitCheckedIn = "visit_checked_in"
        public static let visitCheckedOut = "visit_checked_out"
        public static let visitorAdded = "visitor_added"
        public static let visitorBlocked = "visitor_blocked"
        public static let messageSent = "message_sent"
        public static let restrictionCreated = "restriction_created"
        public static let timeSlotSelected = "time_slot_selected"
        public static let slotAvailabilityCheck = "slot_availability_check"
    }

    private override init() {
        super.init()
    }

    // MARK: - Analytics

    /// Log a custom event to Firebase Analytics
    @objc public func logEvent(_ name: String, parameters: [String: Any]? = nil) {
        analytics.logEvent(name, parameters: parameters)
        print("Firebase Analytics: Logged event '\(name)' with params: \(parameters ?? [:])")
    }

    /// Log visit scheduled event
    @objc public func logVisitScheduled(visitId: String, beneficiaryId: String, duration: Int) {
        logEvent(Events.visitScheduled, parameters: [
            "visit_id": visitId,
            "beneficiary_id": beneficiaryId,
            "duration_minutes": duration
        ])
    }

    /// Log visit approved event
    @objc public func logVisitApproved(visitId: String, coordinatorId: String) {
        logEvent(Events.visitApproved, parameters: [
            "visit_id": visitId,
            "coordinator_id": coordinatorId
        ])
    }

    /// Log check-in event
    @objc public func logCheckIn(visitId: String, method: String) {
        logEvent(Events.visitCheckedIn, parameters: [
            "visit_id": visitId,
            "method": method
        ])
    }

    /// Log check-out event
    @objc public func logCheckOut(visitId: String, durationMinutes: Int, rating: Int) {
        logEvent(Events.visitCheckedOut, parameters: [
            "visit_id": visitId,
            "duration_minutes": durationMinutes,
            "rating": rating
        ])
    }

    /// Log screen view
    @objc public func logScreenView(screenName: String, screenClass: String) {
        analytics.logEvent(AnalyticsEventScreenView, parameters: [
            AnalyticsParameterScreenName: screenName,
            AnalyticsParameterScreenClass: screenClass
        ])
    }

    /// Set user ID for analytics
    @objc public func setUserId(_ userId: String?) {
        analytics.setUserID(userId)
        if let userId = userId {
            crashlytics.setUserID(userId)
        }
    }

    /// Set user property
    @objc public func setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(value, forName: name)
    }

    // MARK: - Crashlytics

    /// Log a non-fatal exception
    @objc public func logException(_ error: Error) {
        crashlytics.record(error: error)
        print("Firebase Crashlytics: Recorded exception - \(error.localizedDescription)")
    }

    /// Log a message
    @objc public func log(_ message: String) {
        crashlytics.log(message)
    }

    /// Set a custom key
    @objc public func setCustomKey(_ key: String, value: Any) {
        if let stringValue = value as? String {
            crashlytics.setCustomValue(stringValue, forKey: key)
        } else if let intValue = value as? Int {
            crashlytics.setCustomValue(intValue, forKey: key)
        } else if let boolValue = value as? Bool {
            crashlytics.setCustomValue(boolValue, forKey: key)
        }
    }

    // MARK: - Firestore Operations

    /// Get Firestore database instance
    @objc public func getFirestore() -> Firestore {
        return db
    }

    /// Create a document
    public func createDocument(
        collection: String,
        data: [String: Any],
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        var ref: DocumentReference? = nil
        ref = db.collection(collection).addDocument(data: data) { error in
            if let error = error {
                completion(.failure(error))
            } else if let documentId = ref?.documentID {
                completion(.success(documentId))
            }
        }
    }

    /// Create a document with specific ID
    public func createDocumentWithId(
        collection: String,
        documentId: String,
        data: [String: Any],
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        db.collection(collection).document(documentId).setData(data) { error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(()))
            }
        }
    }

    /// Get a document by ID
    public func getDocument(
        collection: String,
        documentId: String,
        completion: @escaping (Result<[String: Any]?, Error>) -> Void
    ) {
        db.collection(collection).document(documentId).getDocument { snapshot, error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(snapshot?.data()))
            }
        }
    }

    /// Update a document
    public func updateDocument(
        collection: String,
        documentId: String,
        data: [String: Any],
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        db.collection(collection).document(documentId).updateData(data) { error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(()))
            }
        }
    }

    /// Delete a document
    public func deleteDocument(
        collection: String,
        documentId: String,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        db.collection(collection).document(documentId).delete { error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(()))
            }
        }
    }

    /// Query documents
    public func queryDocuments(
        collection: String,
        field: String,
        isEqualTo value: Any,
        completion: @escaping (Result<[[String: Any]], Error>) -> Void
    ) {
        db.collection(collection)
            .whereField(field, isEqualTo: value)
            .getDocuments { snapshot, error in
                if let error = error {
                    completion(.failure(error))
                } else {
                    let documents = snapshot?.documents.compactMap { $0.data() } ?? []
                    completion(.success(documents))
                }
            }
    }

    /// Listen to a document
    public func listenToDocument(
        collection: String,
        documentId: String,
        listener: @escaping ([String: Any]?) -> Void
    ) -> ListenerRegistration {
        return db.collection(collection).document(documentId)
            .addSnapshotListener { snapshot, error in
                if let error = error {
                    print("Error listening to document: \(error)")
                    listener(nil)
                } else {
                    listener(snapshot?.data())
                }
            }
    }

    /// Listen to a collection query
    public func listenToQuery(
        collection: String,
        field: String,
        isEqualTo value: Any,
        orderBy: String? = nil,
        listener: @escaping ([[String: Any]]) -> Void
    ) -> ListenerRegistration {
        var query: Query = db.collection(collection).whereField(field, isEqualTo: value)

        if let orderBy = orderBy {
            query = query.order(by: orderBy)
        }

        return query.addSnapshotListener { snapshot, error in
            if let error = error {
                print("Error listening to query: \(error)")
                listener([])
            } else {
                let documents = snapshot?.documents.compactMap { $0.data() } ?? []
                listener(documents)
            }
        }
    }

    // MARK: - Time Slot Operations

    /// Get available time slots for a date
    public func getAvailableTimeSlots(
        date: String,
        completion: @escaping (Result<[[String: Any]], Error>) -> Void
    ) {
        db.collection(Collections.timeSlots)
            .whereField("date", isEqualTo: date)
            .whereField("isAvailable", isEqualTo: true)
            .order(by: "startTime")
            .getDocuments { snapshot, error in
                if let error = error {
                    completion(.failure(error))
                } else {
                    let slots = snapshot?.documents.compactMap { doc -> [String: Any]? in
                        var data = doc.data()
                        data["id"] = doc.documentID
                        return data
                    } ?? []
                    completion(.success(slots))
                }
            }
    }

    /// Reserve a time slot
    public func reserveTimeSlot(
        slotId: String,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let slotRef = db.collection(Collections.timeSlots).document(slotId)

        db.runTransaction { transaction, errorPointer in
            let slotDoc: DocumentSnapshot
            do {
                slotDoc = try transaction.getDocument(slotRef)
            } catch let error as NSError {
                errorPointer?.pointee = error
                return nil
            }

            guard let currentBookings = slotDoc.data()?["currentBookings"] as? Int,
                  let maxCapacity = slotDoc.data()?["maxCapacity"] as? Int else {
                let error = NSError(domain: "FirebaseService", code: -1,
                                    userInfo: [NSLocalizedDescriptionKey: "Invalid slot data"])
                errorPointer?.pointee = error
                return nil
            }

            if currentBookings >= maxCapacity {
                let error = NSError(domain: "FirebaseService", code: -2,
                                    userInfo: [NSLocalizedDescriptionKey: "Time slot is fully booked"])
                errorPointer?.pointee = error
                return nil
            }

            transaction.updateData([
                "currentBookings": FieldValue.increment(Int64(1)),
                "updatedAt": Timestamp()
            ], forDocument: slotRef)

            if currentBookings + 1 >= maxCapacity {
                transaction.updateData(["isAvailable": false], forDocument: slotRef)
            }

            return nil
        } completion: { _, error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(()))
            }
        }
    }

    /// Release a time slot
    public func releaseTimeSlot(
        slotId: String,
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        let slotRef = db.collection(Collections.timeSlots).document(slotId)

        db.runTransaction { transaction, errorPointer in
            let slotDoc: DocumentSnapshot
            do {
                slotDoc = try transaction.getDocument(slotRef)
            } catch let error as NSError {
                errorPointer?.pointee = error
                return nil
            }

            guard let currentBookings = slotDoc.data()?["currentBookings"] as? Int else {
                let error = NSError(domain: "FirebaseService", code: -1,
                                    userInfo: [NSLocalizedDescriptionKey: "Invalid slot data"])
                errorPointer?.pointee = error
                return nil
            }

            if currentBookings <= 0 {
                let error = NSError(domain: "FirebaseService", code: -3,
                                    userInfo: [NSLocalizedDescriptionKey: "No bookings to release"])
                errorPointer?.pointee = error
                return nil
            }

            transaction.updateData([
                "currentBookings": FieldValue.increment(Int64(-1)),
                "isAvailable": true,
                "updatedAt": Timestamp()
            ], forDocument: slotRef)

            return nil
        } completion: { _, error in
            if let error = error {
                completion(.failure(error))
            } else {
                completion(.success(()))
            }
        }
    }

    // MARK: - Visit Operations

    /// Create a visit
    public func createVisit(
        data: [String: Any],
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        var visitData = data
        visitData["createdAt"] = Timestamp()
        visitData["updatedAt"] = Timestamp()

        createDocument(collection: Collections.visits, data: visitData, completion: completion)
    }

    /// Get visits for a user
    public func getVisitsForUser(
        userId: String,
        completion: @escaping (Result<[[String: Any]], Error>) -> Void
    ) {
        db.collection(Collections.visits)
            .whereField("visitorId", isEqualTo: userId)
            .order(by: "scheduledDate", descending: true)
            .getDocuments { snapshot, error in
                if let error = error {
                    completion(.failure(error))
                } else {
                    let visits = snapshot?.documents.compactMap { doc -> [String: Any]? in
                        var data = doc.data()
                        data["id"] = doc.documentID
                        return data
                    } ?? []
                    completion(.success(visits))
                }
            }
    }

    /// Update visit status
    public func updateVisitStatus(
        visitId: String,
        status: String,
        additionalData: [String: Any] = [:],
        completion: @escaping (Result<Void, Error>) -> Void
    ) {
        var updates: [String: Any] = [
            "status": status,
            "updatedAt": Timestamp()
        ]
        updates.merge(additionalData) { _, new in new }

        updateDocument(
            collection: Collections.visits,
            documentId: visitId,
            data: updates,
            completion: completion
        )
    }
}
