package com.markduenas.visischeduler.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.markduenas.visischeduler.domain.entities.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication manager for VisiScheduler.
 * Handles user authentication, registration, and session management.
 */
class FirebaseAuthManager {

    private val auth: FirebaseAuth by lazy { Firebase.auth }

    /**
     * Get the current user's ID or null if not authenticated.
     */
    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Get the current user's display name.
     */
    val currentUserName: String?
        get() = auth.currentUser?.displayName ?: auth.currentUser?.email?.substringBefore("@")

    /**
     * Get the current user's email.
     */
    val currentUserEmail: String?
        get() = auth.currentUser?.email

    /**
     * Check if a user is currently authenticated.
     */
    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    /**
     * Observe authentication state changes.
     */
    fun observeAuthState(): Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Sign in with email and password.
     */
    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        result.user?.uid ?: throw Exception("Sign in failed: no user returned")
    }

    /**
     * Create a new account with email and password.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<String> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Sign up failed: no user returned")

        // Update display name if provided
        displayName?.let {
            val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                this.displayName = it
            }
            user.updateProfile(profileUpdates).await()
        }

        user.uid
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    /**
     * Send email verification to current user.
     */
    suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await()
            ?: throw Exception("No user signed in")
    }

    /**
     * Reload current user to get updated state.
     */
    suspend fun reloadUser(): Result<Unit> = runCatching {
        auth.currentUser?.reload()?.await()
            ?: throw Exception("No user signed in")
    }

    /**
     * Check if current user's email is verified.
     */
    val isEmailVerified: Boolean
        get() = auth.currentUser?.isEmailVerified ?: false

    /**
     * Update user's display name.
     */
    suspend fun updateDisplayName(displayName: String): Result<Unit> = runCatching {
        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            this.displayName = displayName
        }
        auth.currentUser?.updateProfile(profileUpdates)?.await()
            ?: throw Exception("No user signed in")
    }

    /**
     * Update user's email.
     */
    suspend fun updateEmail(newEmail: String): Result<Unit> = runCatching {
        auth.currentUser?.verifyBeforeUpdateEmail(newEmail)?.await()
            ?: throw Exception("No user signed in")
    }

    /**
     * Update user's password.
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        auth.currentUser?.updatePassword(newPassword)?.await()
            ?: throw Exception("No user signed in")
    }

    /**
     * Delete current user account.
     */
    suspend fun deleteAccount(): Result<Unit> = runCatching {
        auth.currentUser?.delete()?.await()
            ?: throw Exception("No user signed in")
    }

    /**
     * Get Firebase ID token for API authentication.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String> = runCatching {
        auth.currentUser?.getIdToken(forceRefresh)?.await()?.token
            ?: throw Exception("No user signed in")
    }
}
