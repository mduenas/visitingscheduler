package com.markduenas.visischeduler.di

import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreAuthRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreBeneficiaryRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreCheckInRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreMessageRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreNotificationRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreRestrictionRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreTimeSlotRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreUserRepository
import com.markduenas.visischeduler.data.repository.firestore.CommonFirestoreVisitRepository
import com.markduenas.visischeduler.domain.repository.AuthRepository
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.domain.repository.NotificationRepository
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.TimeSlotRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import com.markduenas.visischeduler.firebase.FirestoreDatabase
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import org.koin.dsl.module

/**
 * Firestore module providing cross-platform Firebase implementations.
 * Uses GitLive Firebase KMP SDK for both Android and iOS.
 *
 * Use this module instead of dataModule for Firebase-backed storage.
 */
val firestoreModule = module {

    // ==========================================
    // Firebase Core Services
    // ==========================================

    single { FirestoreDatabase() }

    single { Firebase.auth }

    // ==========================================
    // Authentication Repository
    // ==========================================
    single<AuthRepository> {
        CommonFirestoreAuthRepository(
            auth = get(),
            firestore = get()
        )
    }

    // ==========================================
    // User Repository
    // ==========================================
    single<UserRepository> {
        CommonFirestoreUserRepository(
            firestore = get(),
            auth = get()
        )
    }

    // ==========================================
    // Beneficiary Repository
    // ==========================================
    single<BeneficiaryRepository> {
        CommonFirestoreBeneficiaryRepository(
            firestore = get(),
            auth = get()
        )
    }

    // ==========================================
    // Visit/Schedule Repository
    // ==========================================
    single<VisitRepository> {
        CommonFirestoreVisitRepository(
            firestore = get(),
            auth = get()
        )
    }

    // ==========================================
    // Restriction/Rules Repository
    // ==========================================
    single<RestrictionRepository> {
        CommonFirestoreRestrictionRepository(
            firestore = get(),
            auth = get()
        )
    }

    // ==========================================
    // Time Slot Repository
    // ==========================================
    single<TimeSlotRepository> {
        CommonFirestoreTimeSlotRepository(
            firestore = get()
        )
    }

    // ==========================================
    // Check-In Repository
    // ==========================================
    single<CheckInRepository> {
        CommonFirestoreCheckInRepository(
            firestore = get(),
            auth = get()
        )
    }

    // ==========================================
    // Message/Conversation Repository
    // ==========================================
    single<MessageRepository> {
        CommonFirestoreMessageRepository(
            firestore = get(),
            auth = get()
        )
    }

    // ==================== Notification Repository ====================
    single { CommonFirestoreNotificationRepository(get()) }
    single<NotificationRepository> {
        com.markduenas.visischeduler.data.repository.NotificationRepositoryImpl(
            database = get(),
            firestoreRepository = get(),
            authRepository = get()
        )
    }
}
