package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.repository.RecipeUserDataSource
import com.example.macronomocon.domain.model.Recipe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Favorites and blocks in `recipe_bookmarks`; user-authored recipes in `custom_recipes`. Both are owner-only. */
class SupabaseRecipeUserData(private val client: SupabaseClient) : RecipeUserDataSource {
    companion object {
        const val TABLE = "recipe_bookmarks"
        const val CUSTOM_TABLE = "custom_recipes"
    }

    override suspend fun blockedRecipeIds(): Set<String> = bookmarkIds(client, "blocked")

    override suspend fun setFavorite(recipeId: String, value: Boolean) =
        upsert(recipeId) { put("favorite", value) }

    override suspend fun blockRecipe(recipeId: String) = upsert(recipeId) { put("blocked", true) }

    // Postgres assigns recipe_id and user_id; returning the row gives the new ID back.
    override suspend fun saveCustom(recipe: Recipe): Recipe = CustomRecipeMapper.recipe(
        client.from(CUSTOM_TABLE).insert(CustomRecipeMapper.row(recipe)) { select() }
            .decodeSingle<JsonObject>()
    )

    // RLS limits every custom_recipes query to the signed-in user's rows.
    override suspend fun customRecipes(): List<Recipe> = client.from(CUSTOM_TABLE).select {
        order("created_at", Order.DESCENDING)
    }.decodeList<JsonObject>().map(CustomRecipeMapper::recipe)

    override suspend fun findCustom(recipeId: String): Recipe = CustomRecipeMapper.recipe(
        client.from(CUSTOM_TABLE).select { filter { eq("recipe_id", recipeId) } }
            .decodeSingle<JsonObject>()
    )

    // Fails when no row matched, so editing someone else's recipe is reported, not ignored.
    override suspend fun updateCustom(recipe: Recipe): Recipe = CustomRecipeMapper.recipe(
        client.from(CUSTOM_TABLE).update(CustomRecipeMapper.row(recipe)) {
            select()
            filter { eq("recipe_id", recipe.recipeId) }
        }.decodeSingle<JsonObject>()
    )

    // Not exposed in the UI yet; share links need their own flow.
    override suspend fun createShareLink(recipeId: String): String =
        error("Recipe sharing is not available yet")

    private suspend fun upsert(recipeId: String, values: JsonObjectBuilder.() -> Unit) {
        val userId = requireNotNull(client.auth.currentUserOrNull()).id
        // Merges only the given columns, so favoriting never clears a block and vice versa.
        client.from(TABLE).upsert(
            buildJsonObject {
                put("user_id", userId)
                put("recipe_id", recipeId)
                values()
            }
        ) { onConflict = "user_id,recipe_id" }
    }
}

/** IDs of the signed-in user's bookmarks where [column] is true. */
internal suspend fun bookmarkIds(client: SupabaseClient, column: String): Set<String> =
    client.from(SupabaseRecipeUserData.TABLE).select {
        filter { eq(column, true) }
    }.decodeList<JsonObject>().mapNotNull {
        (it["recipe_id"] as? JsonPrimitive)?.content
    }.toSet()
