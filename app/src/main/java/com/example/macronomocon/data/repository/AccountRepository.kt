package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences

/** Authentication service boundary. The server determines the authenticated user and enforces OTP/lockouts. */
interface AccountRepository {
    suspend fun register(email: String, password: String)
    suspend fun authenticate(email: String, password: String): AuthenticationResult
    suspend fun verifyOtp(code: String): User
    suspend fun signOut()
    suspend fun findUser(): User?
    suspend fun saveUser(user: User): User
    suspend fun savePreferences(preferences: UserPreferences): User
    suspend fun deleteUser()
}
