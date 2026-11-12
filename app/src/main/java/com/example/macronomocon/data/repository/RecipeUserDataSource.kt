package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.Recipe

/** Authenticated backend storage for user-specific recipe actions. */
interface RecipeUserDataSource {
    suspend fun blockedRecipeIds(): Set<String>
    suspend fun setFavorite(recipeId: String, value: Boolean)
    suspend fun blockRecipe(recipeId: String)

    /** Inserts a new recipe; the returned copy carries the ID the backend assigned. */
    suspend fun saveCustom(recipe: Recipe): Recipe

    /** The signed-in user's own recipes, newest first. */
    suspend fun customRecipes(): List<Recipe>
    suspend fun findCustom(recipeId: String): Recipe
    suspend fun updateCustom(recipe: Recipe): Recipe
    suspend fun createShareLink(recipeId: String): String
}
