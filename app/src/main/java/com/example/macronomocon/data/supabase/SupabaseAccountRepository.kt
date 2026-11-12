package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.domain.model.AccountException
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import java.time.LocalDate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAccountRepository(private val connection: SupabaseConnection) : AccountRepository {
    companion object {
        const val PANTRY_TABLE = "pantry_items"
        const val COMMENTS_TABLE = "comments"
        const val COMMENT_LIKES_TABLE = "comment_likes"

        /** Each comment row embeds the IDs of the users who liked it. */
        const val COMMENT_COLUMNS = "*,comment_likes(user_id)"
    }

    private val client get() = connection.client
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
        val profile = connection.createQuery(
            "profiles"
        ).select(mapOf("user_id" to session.id)).firstOrNull()
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
            pantry = Pantry(loadPantryItems()),
            favoriteRecipeIds = bookmarkIds(connection, "favorite"),
            blockedRecipeIds = bookmarkIds(connection, "blocked")
        )
    }

    override suspend fun saveUser(user: User): User {
        val id = requireNotNull(client.auth.currentUserOrNull()).id
        require(id == user.userId)
        connection.createQuery("profiles").upsert(
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

    // Owner-only pantry_items rows; user_id defaults to auth.uid().
    override suspend fun loadPantryItems(): List<PantryItem> =
        connection.createQuery(PANTRY_TABLE).select().map { row ->
            PantryItem(
                ingredient = Ingredient(
                    ingredientId = row.text("ingredient_id"),
                    name = row.text("name"),
                    barcode = row.textOrNull("barcode").orEmpty(),
                    category = row.textOrNull("category").orEmpty()
                ),
                quantity = row.text("quantity").toDouble(),
                unit = row.text("unit"),
                expirationDate = LocalDate.parse(row.text("expiration_date")),
                pantryItemId = row.text("pantry_item_id")
            )
        }

    override suspend fun savePantryItem(item: PantryItem) {
        connection.createQuery(PANTRY_TABLE).upsert(
            buildJsonObject {
                put("pantry_item_id", item.pantryItemId)
                put("ingredient_id", item.ingredient.ingredientId)
                put("name", item.ingredient.name)
                put("barcode", item.ingredient.barcode)
                put("category", item.ingredient.category)
                put("quantity", item.quantity)
                put("unit", item.unit)
                put("expiration_date", item.expirationDate.toString())
            },
            onConflict = "pantry_item_id"
        )
    }

    override suspend fun deletePantryItem(item: PantryItem) {
        connection.createQuery(PANTRY_TABLE).delete(mapOf("pantry_item_id" to item.pantryItemId))
    }

    // Shared comments: every signed-in user reads them; RLS limits writes to the author.
    override suspend fun loadComments(recipeId: String): List<Comment> {
        require(recipeId.isNotBlank())
        return connection.createQuery(COMMENTS_TABLE).select(
            mapOf("recipe_id" to recipeId),
            COMMENT_COLUMNS,
            orderBy = listOf("created_at", "comment_id")
        ).map(CommentMapper::comment)
    }

    override suspend fun publishComment(
        recipeId: String,
        text: String,
        imageUrl: String?
    ): Comment {
        require(recipeId.isNotBlank())
        val image = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
        require(text.isNotBlank() || image != null)
        return connection.createQuery(COMMENTS_TABLE).insert(
            buildJsonObject {
                put("recipe_id", recipeId)
                put("user_id", signedInUserId())
                put("text", text.trim())
                put("image_url", image)
            }
        ).let(CommentMapper::comment)
    }

    override suspend fun deleteComment(comment: Comment) {
        // Requiring a returned row prevents a silently filtered RLS delete appearing successful.
        val deleted = connection.createQuery(COMMENTS_TABLE).delete(
            mapOf("comment_id" to comment.commentId, "user_id" to signedInUserId())
        )
        check(deleted.size == 1) { "Comment was not deleted" }
    }

    override suspend fun likeComment(comment: Comment): Comment {
        connection.createQuery(COMMENT_LIKES_TABLE).insert(
            buildJsonObject {
                put("comment_id", comment.commentId)
                put("user_id", signedInUserId())
            }
        )
        return connection.createQuery(COMMENTS_TABLE).select(
            mapOf("comment_id" to comment.commentId),
            COMMENT_COLUMNS
        ).single().let(CommentMapper::comment)
    }

    private fun signedInUserId(): String = requireNotNull(client.auth.currentUserOrNull()).id

    private fun JsonObject.text(key: String): String = getValue(key).jsonPrimitive.content

    private fun JsonObject.textOrNull(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

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
