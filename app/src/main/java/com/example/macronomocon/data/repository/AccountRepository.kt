package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences

/** Authentication service boundary. The server determines the authenticated user and enforces OTP/lockouts. */
interface AccountRepository {
    val signedOutEvents: kotlinx.coroutines.flow.Flow<Unit>
        get() = kotlinx.coroutines.flow.emptyFlow()

    /** Emits when a session arrives from outside the app, such as an OAuth browser redirect. */
    val signedInEvents: kotlinx.coroutines.flow.Flow<Unit>
        get() = kotlinx.coroutines.flow.emptyFlow()
    suspend fun register(email: String, password: String)
    suspend fun authenticate(email: String, password: String): AuthenticationResult
    suspend fun verifyOtp(code: String): User

    /** Starts Google OAuth; the session arrives later through [signedInEvents]. */
    suspend fun signInWithGoogle(): Unit = error("Google sign-in is not available")
    suspend fun signOut()
    suspend fun findUser(): User?
    suspend fun saveUser(user: User): User
    suspend fun savePreferences(preferences: UserPreferences): User
    suspend fun deleteUser()
}
