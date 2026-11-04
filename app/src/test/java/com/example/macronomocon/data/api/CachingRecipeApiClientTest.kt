package com.example.macronomocon.data.api

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.Recipe
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CachingRecipeApiClientTest {
    private fun recipe(id: String) = Recipe(
        id, "Recipe $id", emptyList(), 0, 10, Difficulty.EASY, MealType.DINNER, "", emptyList()
    )

    private val rice = Ingredient("1", "Rice")
    private val beans = Ingredient("2", "beans")

    private class CountingApi(private val recipes: Map<String, Recipe>) : RecipeApiClient {
        var searches = 0
        var single = 0
        val bulk = mutableListOf<List<String>>()

        override suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> {
            searches++
            delay(100)
            return recipes.values.toList()
        }
        override suspend fun searchRecipes(query: String): List<Recipe> {
            searches++
            return recipes.values.toList()
        }
        override suspend fun getRecipe(recipeId: String): Recipe {
            single++
            return requireNotNull(recipes[recipeId])
        }
        override suspend fun getRecipes(recipeIds: List<String>): List<Recipe> {
            bulk += recipeIds
            return recipeIds.mapNotNull { recipes[it] }
        }
    }

    private val api = CountingApi(listOf("10", "20", "30").associateWith(::recipe))
    private var clock = 0L
    private val cache = CachingRecipeApiClient(api, ttlMillis = 1_000) { clock }

    @Test
    fun concurrentSearchesForTheSameIngredientsMakeOneApiCall() = runTest {
        listOf(
            async { cache.findByIngredients(listOf(rice, beans)) },
            async { cache.findByIngredients(listOf(beans, rice)) }
        ).awaitAll()
        assertEquals(1, api.searches)
    }

    @Test
    fun concurrentSearchesForTheSameIngredientsShareOneResult() = runTest {
        val results = listOf(
            async { cache.findByIngredients(listOf(rice, beans)) },
            async { cache.findByIngredients(listOf(beans, rice)) }
        ).awaitAll()
        assertEquals(results[0], results[1])
    }

    @Test
    fun searchResultsPopulateTheRecipeDetailCache() = runTest {
        cache.searchRecipes("Pasta")
        assertEquals("10", cache.getRecipe("10").recipeId)
    }

    @Test
    fun cachedRecipeDetailsAvoidASingleRecipeApiCall() = runTest {
        cache.searchRecipes("Pasta")
        cache.getRecipe("10")
        assertEquals(0, api.single)
    }

    @Test
    fun searchQueriesMatchIgnoringCaseAndSurroundingSpace() = runTest {
        cache.searchRecipes("Pasta")
        cache.searchRecipes(" pasta ")
        assertEquals(1, api.searches)
    }

    @Test
    fun entriesExpireAfterTheTimeToLive() = runTest {
        cache.findByIngredients(listOf(rice))
        clock = 1_000
        cache.findByIngredients(listOf(rice))
        assertEquals(2, api.searches)
    }

    @Test
    fun bulkFetchReturnsKnownRecipesInRequestOrder() = runTest {
        cache.getRecipe("10")
        val saved = cache.getRecipes(listOf("30", "10", "20", "99"))
        assertEquals(listOf("30", "10", "20"), saved.map { it.recipeId })
    }

    @Test
    fun bulkFetchRequestsOnlyUncachedRecipesInOneCall() = runTest {
        cache.getRecipe("10")
        cache.getRecipes(listOf("30", "10", "20", "99"))
        assertEquals(listOf(listOf("30", "20", "99")), api.bulk)
    }

    @Test
    fun aRepeatedBulkFetchIsServedEntirelyFromTheCache() = runTest {
        cache.getRecipe("10")
        cache.getRecipes(listOf("30", "10", "20", "99"))
        cache.getRecipes(listOf("10", "20", "30"))
        assertEquals(1, api.bulk.size)
    }
}
