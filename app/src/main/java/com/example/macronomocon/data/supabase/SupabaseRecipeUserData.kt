package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.repository.RecipeUserDataSource
import com.example.macronomocon.domain.model.Recipe
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Favorites and blocks in the owner-only `recipe_bookmarks` table. */
class SupabaseRecipeUserData(private val connection: SupabaseConnection) : RecipeUserDataSource {
    companion object {
        const val TABLE = "recipe_bookmarks"
    }

    override suspend fun blockedRecipeIds(): Set<String> = bookmarkIds(connection, "blocked")

    override suspend fun setFavorite(recipeId: String, value: Boolean) =
        upsert(recipeId) { put("favorite", value) }

    override suspend fun blockRecipe(recipeId: String) = upsert(recipeId) { put("blocked", true) }

    // Not exposed in the UI yet; custom_recipes and share links need their own flows.
    override suspend fun saveCustom(recipe: Recipe): Recipe =
        error("Custom recipes are not available yet")

    override suspend fun createShareLink(recipeId: String): String =
        error("Recipe sharing is not available yet")

    private suspend fun upsert(recipeId: String, values: JsonObjectBuilder.() -> Unit) {
        val userId = requireNotNull(connection.client.auth.currentUserOrNull()).id
        // Merges only the given columns, so favoriting never clears a block and vice versa.
        connection.createQuery(TABLE).upsert(
            buildJsonObject {
                put("user_id", userId)
                put("recipe_id", recipeId)
                values()
            },
            onConflict = "user_id,recipe_id"
        )
    }
}

/** IDs of the signed-in user's bookmarks where [column] is true. */
internal suspend fun bookmarkIds(connection: SupabaseConnection, column: String): Set<String> =
    connection.createQuery(
        SupabaseRecipeUserData.TABLE
    ).select(mapOf(column to "true")).mapNotNull {
        (it["recipe_id"] as? JsonPrimitive)?.content
    }.toSet()
