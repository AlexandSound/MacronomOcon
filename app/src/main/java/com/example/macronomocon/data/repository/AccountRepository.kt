package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.PantryItem
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

    /** The pantry is part of the User, so its items are persisted through this repository. */
    suspend fun loadPantryItems(): List<PantryItem>
    suspend fun savePantryItem(item: PantryItem)
    suspend fun deletePantryItem(item: PantryItem)

    /**
     * Comments are shared by every signed-in user. The server assigns comment IDs, author names,
     * and timestamps, and enforces that only authors delete and that likes are unique and never
     * the author's own.
     */
    suspend fun loadComments(recipeId: String): List<Comment>
    suspend fun publishComment(recipeId: String, text: String, imageUrl: String?): Comment
    suspend fun deleteComment(comment: Comment)

    /** Records the signed-in user's like and returns the comment with its current likes. */
    suspend fun likeComment(comment: Comment): Comment
}
