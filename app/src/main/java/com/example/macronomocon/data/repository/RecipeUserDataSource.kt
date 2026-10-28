package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.Recipe

/** Authenticated backend storage for user-specific recipe actions. */
interface RecipeUserDataSource {
    suspend fun blockedRecipeIds(): Set<String>
    suspend fun setFavorite(recipeId: String, value: Boolean)
    suspend fun blockRecipe(recipeId: String)
    suspend fun saveCustom(recipe: Recipe): Recipe
    suspend fun createShareLink(recipeId: String): String
}
