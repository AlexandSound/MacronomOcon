package com.example.macronomocon.data.api

import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Recipe

/** Implement with the backend's Spoonacular proxy; API credentials must not be embedded in the APK. */
interface RecipeApiClient {
    suspend fun findByIngredients(items: List<Ingredient>): List<Recipe>
    suspend fun searchRecipes(query: String): List<Recipe>
    suspend fun getRecipe(recipeId: String): Recipe

    /** Recipes for [recipeIds]; unknown IDs are omitted. Transports may batch this into one call. */
    suspend fun getRecipes(recipeIds: List<String>): List<Recipe> = recipeIds.map { getRecipe(it) }
}
