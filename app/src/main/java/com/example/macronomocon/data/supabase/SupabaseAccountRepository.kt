package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AccountException
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAccountRepository(private val client: SupabaseClient) : AccountRepository {
    override val signedOutEvents = client.auth.sessionStatus
        .filterIsInstance<SessionStatus.NotAuthenticated>().map { Unit }

    override val signedInEvents = client.auth.sessionStatus
        .filterIsInstance<SessionStatus.Authenticated>()
        .filter { it.source is SessionSource.External }
        .map { Unit }

    override suspend fun register(email: String, password: String) {
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: AuthRestException) {
            throw e.toAccountException() ?: e
        }
    }

    override suspend fun authenticate(email: String, password: String): AuthenticationResult {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return AuthenticationResult.SignedIn(requireNotNull(findUser()))
    }

    override suspend fun signInWithGoogle() {
        // Opens a Custom Tab; MainActivity passes the redirect back through handleAuthRedirect.
        client.auth.signInWith(Google)
    }

    override suspend fun findUser(): User? {
        client.auth.awaitInitialization()
        val session = client.auth.currentUserOrNull() ?: return null
        val profile = client.from("profiles").select {
            filter { eq("user_id", session.id) }
        }.decodeList<JsonObject>().firstOrNull()
        fun strings(key: String) =
            profile?.get(key)?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()
        return User(
            userId = session.id,
            email = session.email.orEmpty(),
            displayName = profile?.get("display_name")?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() } ?: session.email.orEmpty(),
            preferences = UserPreferences(
                dietaryRestrictions = strings("dietary_restrictions"),
                preferredCuisines = strings("preferred_cuisines"),
                cookingMethods = strings("cooking_methods"),
                notificationsEnabled =
                profile?.get("notifications_enabled")?.jsonPrimitive?.booleanOrNull ?: true
            ),
            favoriteRecipeIds = bookmarkIds(client, "favorite"),
            blockedRecipeIds = bookmarkIds(client, "blocked")
        )
    }

    override suspend fun saveUser(user: User): User {
        val id = requireNotNull(client.auth.currentUserOrNull()).id
        require(id == user.userId)
        client.from("profiles").upsert(
            buildJsonObject {
                put("user_id", id)
                put("display_name", user.displayName)
                put(
                    "dietary_restrictions",
                    JsonArray(user.preferences.dietaryRestrictions.map(::JsonPrimitive))
                )
                put(
                    "preferred_cuisines",
                    JsonArray(user.preferences.preferredCuisines.map(::JsonPrimitive))
                )
                put(
                    "cooking_methods",
                    JsonArray(user.preferences.cookingMethods.map(::JsonPrimitive))
                )
                put("notifications_enabled", user.preferences.notificationsEnabled)
            }
        )
        return requireNotNull(findUser())
    }

    override suspend fun savePreferences(preferences: UserPreferences): User =
        saveUser(requireNotNull(findUser()).copy(preferences = preferences))

    override suspend fun signOut() {
        client.auth.signOut()
    }

    private fun AuthRestException.toAccountException() = when (errorCode) {
        AuthErrorCode.EmailExists,
        AuthErrorCode.UserAlreadyExists -> AccountException.Reason.EMAIL_IN_USE
        AuthErrorCode.EmailAddressInvalid -> AccountException.Reason.INVALID_EMAIL
        AuthErrorCode.WeakPassword -> AccountException.Reason.WEAK_PASSWORD
        // Supabase's built-in mailer only delivers to project team members, a few per hour.
        AuthErrorCode.EmailAddressNotAuthorized -> AccountException.Reason.EMAIL_NOT_AUTHORIZED
        AuthErrorCode.OverEmailSendRateLimit -> AccountException.Reason.EMAIL_RATE_LIMITED
        AuthErrorCode.SignupDisabled -> AccountException.Reason.SIGNUP_DISABLED
        else -> null
    }?.let(::AccountException)

    override suspend fun updateEmail(email: String): String {
        try {
            return client.auth.updateUser { this.email = email.trim() }.email.orEmpty()
        } catch (e: AuthRestException) {
            throw e.toAccountException() ?: e
        }
    }

    override suspend fun updatePassword(password: String) {
        try {
            client.auth.updateUser { this.password = password }
        } catch (e: AuthRestException) {
            throw e.toAccountException() ?: e
        }
    }

    override suspend fun deleteUser() {
        client.functions.invoke("delete-account", buildJsonObject {})
        // The auth user no longer exists. Clear persisted tokens without a second network call.
        client.auth.clearSession()
    }

    // MFA enrollment and verification require a dedicated flow.
    override suspend fun verifyOtp(code: String): User = error("MFA is not configured")
}
