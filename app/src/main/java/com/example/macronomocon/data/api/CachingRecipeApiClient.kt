package com.example.macronomocon.data.api

import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Recipe
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Decorator that saves API quota. Identical requests within [ttlMillis] are served from memory,
 * requests run one at a time so concurrent screens asking for the same results share one call,
 * and recipes returned by a search are reused when one is opened. Nothing is written to disk.
 */
class CachingRecipeApiClient(
    private val delegate: RecipeApiClient,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
    private val now: () -> Long = System::currentTimeMillis
) : RecipeApiClient {
    companion object {
        /** Spoonacular permits temporary caching only; keep this at an hour or less. */
        const val DEFAULT_TTL_MILLIS = 60 * 60 * 1000L
        const val MAX_SEARCHES = 50
        const val MAX_RECIPES = 300
    }

    private class Entry<T>(val value: T, val storedAt: Long)

    private val lock = Mutex()
    private val searches = lruMap<String, Entry<List<Recipe>>>(MAX_SEARCHES)
    private val recipes = lruMap<String, Entry<Recipe>>(MAX_RECIPES)

    override suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> {
        // Order-insensitive, so the same pantry always maps to the same entry.
        val names = items.map { it.name.trim().lowercase() }.distinct().sorted()
        return search("ingredients:" + names.joinToString(",")) {
            delegate.findByIngredients(items)
        }
    }

    override suspend fun searchRecipes(query: String): List<Recipe> =
        search("query:" + query.trim().lowercase()) { delegate.searchRecipes(query) }

    override suspend fun getRecipe(recipeId: String): Recipe = lock.withLock {
        fresh(recipes[recipeId]) ?: delegate.getRecipe(recipeId).also { store(it) }
    }

    override suspend fun getRecipes(recipeIds: List<String>): List<Recipe> = lock.withLock {
        val missing = recipeIds.distinct().filter { fresh(recipes[it]) == null }
        if (missing.isNotEmpty()) delegate.getRecipes(missing).forEach(::store)
        recipeIds.mapNotNull { fresh(recipes[it]) }
    }

    private suspend fun search(key: String, fetch: suspend () -> List<Recipe>): List<Recipe> =
        lock.withLock {
            fresh(searches[key]) ?: fetch().also { results ->
                searches[key] = Entry(results, now())
                results.forEach(::store)
            }
        }

    private fun store(recipe: Recipe) {
        recipes[recipe.recipeId] = Entry(recipe, now())
    }

    private fun <T> fresh(entry: Entry<T>?): T? =
        entry?.takeIf { now() - it.storedAt < ttlMillis }?.value

    private fun <K, V> lruMap(capacity: Int) = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>) = size > capacity
    }
}
