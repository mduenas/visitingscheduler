package com.markduenas.visischeduler.firebase

import com.google.firebase.auth.FirebaseAuth
import com.markduenas.visischeduler.data.repository.FirestoreBeneficiaryRepository
import com.markduenas.visischeduler.data.repository.FirestoreCheckInRepository
import com.markduenas.visischeduler.data.repository.FirestoreMessageRepository
import com.markduenas.visischeduler.data.repository.FirestoreRestrictionRepository
import com.markduenas.visischeduler.data.repository.FirestoreUserRepository
import com.markduenas.visischeduler.data.repository.FirestoreVisitRepository
import com.markduenas.visischeduler.domain.repository.BeneficiaryRepository
import com.markduenas.visischeduler.domain.repository.CheckInRepository
import com.markduenas.visischeduler.domain.repository.MessageRepository
import com.markduenas.visischeduler.domain.repository.RestrictionRepository
import com.markduenas.visischeduler.domain.repository.UserRepository
import com.markduenas.visischeduler.domain.repository.VisitRepository
import org.koin.dsl.module

/**
 * Koin module for Firebase dependencies.
 */
val firebaseModule = module {
    // Firebase services
    single { FirebaseService() }
    single { FirestoreDatabase() }
    single { FirebaseAuth.getInstance() }

    // Auth state provider
    single<() -> String?> { { get<FirebaseAuthManager>().currentUserId } }
    single<() -> String?>(qualifier = org.koin.core.qualifier.named("userName")) {
        { get<FirebaseAuthManager>().currentUserName }
    }

    // Firebase Auth Manager
    single { FirebaseAuthManager() }

    // Firestore Repositories
    single<UserRepository> { FirestoreUserRepository(get(), get()) }
    single<VisitRepository> { FirestoreVisitRepository(get(), get()) }
    single<BeneficiaryRepository> { FirestoreBeneficiaryRepository(get(), get()) }
    single<RestrictionRepository> { FirestoreRestrictionRepository(get(), get()) }
    single<MessageRepository> {
        FirestoreMessageRepository(
            get(),
            get(),
            get(qualifier = org.koin.core.qualifier.named("userName"))
        )
    }
    single<CheckInRepository> { FirestoreCheckInRepository(get()) }
}
