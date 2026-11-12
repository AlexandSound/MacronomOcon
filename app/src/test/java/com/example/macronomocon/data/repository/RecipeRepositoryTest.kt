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
    private val custom = allowed.copy(recipeId = "3f2b8c1e-6a4d-4e0f-9b7a-2c5d8e1f0a3b")
    private var received = emptyList<Ingredient>()
    private var requestCount = 0
    private val apiRequested = mutableListOf<String>()
    private val api = object : RecipeApiClient {
        override suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> {
            requestCount++
            received = items
            return listOf(allowed, blocked, allowed)
        }
        override suspend fun searchRecipes(query: String): List<Recipe> = listOf(allowed, blocked)
        override suspend fun getRecipe(recipeId: String): Recipe {
            apiRequested += recipeId
            return allowed
        }
    }
    private var inserted: Recipe? = null
    private var updated: Recipe? = null
    private val storage = object : RecipeUserDataSource {
        override suspend fun blockedRecipeIds(): Set<String> = setOf(blocked.recipeId)
        override suspend fun setFavorite(recipeId: String, value: Boolean) = Unit
        override suspend fun blockRecipe(recipeId: String) = Unit
        override suspend fun saveCustom(recipe: Recipe): Recipe {
            inserted = recipe
            return recipe.copy(recipeId = custom.recipeId)
        }
        override suspend fun customRecipes(): List<Recipe> = listOf(custom)
        override suspend fun findCustom(recipeId: String): Recipe =
            custom.takeIf { it.recipeId == recipeId } ?: throw NoSuchElementException()
        override suspend fun updateCustom(recipe: Recipe): Recipe = recipe.also { updated = it }
        override suspend fun createShareLink(recipeId: String): String =
            "https://example.com/recipes/$recipeId"
    }
    private val repository = RecipeRepository(api, storage)

    @Test
    fun ingredientRecommendationsDeduplicateInputAndExcludeBlockedRecipes() = runTest {
        assertEquals(listOf(allowed), repository.findByIngredients(listOf(rice, rice)))
        assertEquals(listOf(rice), received)
    }

    @Test
    fun emptyIngredientSelectionDoesNotCallRecipeApi() = runTest {
        assertTrue(repository.findByIngredients(emptyList()).isEmpty())
        assertEquals(0, requestCount)
    }

    @Test
    fun customRecipeIdsAreLoadedFromUserStorageNotTheRecipeApi() = runTest {
        assertEquals(custom, repository.findById(custom.recipeId))
        assertTrue(apiRequested.isEmpty())
        assertEquals(allowed, repository.findById("716429"))
        assertEquals(listOf("716429"), apiRequested)
    }

    @Test
    fun savedRecipesCombineCustomAndProviderRecipes() = runTest {
        val found = repository.findByIds(setOf(custom.recipeId, "716429"))
        assertEquals(listOf(custom, allowed), found)
        assertEquals(listOf("716429"), apiRequested)
    }

    @Test
    fun newCustomRecipeNeedsNoIdButMustBeComplete() = runTest {
        val saved = repository.saveCustom(allowed.copy(recipeId = ""))
        assertEquals("", inserted?.recipeId)
        assertEquals(custom.recipeId, saved.recipeId)
        val incomplete = runCatching {
            repository.saveCustom(allowed.copy(recipeId = "", instructions = emptyList()))
        }
        assertTrue(incomplete.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun onlyCustomRecipesCanBeUpdated() = runTest {
        assertEquals(custom, repository.updateCustom(custom))
        val provider = runCatching { repository.updateCustom(allowed) }
        assertTrue(provider.exceptionOrNull() is IllegalArgumentException)
        assertEquals(custom, updated)
    }

    @Test
    fun onlyUuidsAreTreatedAsCustomIds() {
        assertTrue(Recipe.isCustomId(custom.recipeId))
        assertFalse(Recipe.isCustomId("716429"))
        assertFalse(Recipe.isCustomId("harvest-bowl"))
        assertFalse(Recipe.isCustomId(""))
    }
}
