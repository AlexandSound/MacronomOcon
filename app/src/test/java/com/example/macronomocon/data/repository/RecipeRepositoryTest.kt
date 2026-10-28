package com.example.macronomocon.data.repository

import com.example.macronomocon.data.api.RecipeApiClient
import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.RecipeIngredient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RecipeRepositoryTest {
    private val rice = Ingredient("rice", "Rice")
    private val allowed = Recipe(
        "allowed", "Rice", listOf("Cook"), 5, 10, Difficulty.EASY,
        MealType.DINNER, "", emptyList(),
        listOf(
            RecipeIngredient(rice, 100.0, "g")
        )
    )
    private val blocked = allowed.copy(recipeId = "blocked")
    private var received = emptyList<Ingredient>()
    private var requestCount = 0
    private val api = object : RecipeApiClient {
        override suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> {
            requestCount++
            received = items
            return listOf(allowed, blocked, allowed)
        }
        override suspend fun searchRecipes(query: String): List<Recipe> = listOf(allowed, blocked)
        override suspend fun getRecipe(recipeId: String): Recipe = allowed
    }
    private val storage = object : RecipeUserDataSource {
        override suspend fun blockedRecipeIds(): Set<String> = setOf(blocked.recipeId)
        override suspend fun setFavorite(recipeId: String, value: Boolean) = Unit
        override suspend fun blockRecipe(recipeId: String) = Unit
        override suspend fun saveCustom(recipe: Recipe): Recipe = recipe
        override suspend fun createShareLink(recipeId: String): String =
            "https://example.com/recipes/$recipeId"
    }

    @Test
    fun ingredientRecommendationsDeduplicateInputAndExcludeBlockedRecipes() = runTest {
        val repository = RecipeRepository(api, storage)
        assertEquals(listOf(allowed), repository.findByIngredients(listOf(rice, rice)))
        assertEquals(listOf(rice), received)
    }

    @Test
    fun emptyIngredientSelectionDoesNotCallRecipeApi() = runTest {
        val repository = RecipeRepository(api, storage)
        assertTrue(repository.findByIngredients(emptyList()).isEmpty())
        assertEquals(0, requestCount)
    }
}
