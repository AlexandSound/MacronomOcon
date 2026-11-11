package com.example.macronomocon.data.mock

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.example.macronomocon.data.api.RecipeApiClient
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.data.repository.RecipeUserDataSource
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.di.AppContainer
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * One in-memory session per container. No network, credentials, or disk writes. The app uses the
 * process-wide [getInstance]; tests construct isolated containers directly.
 */
class MockAppContainer @VisibleForTesting internal constructor(private val seed: MockSeed) :
    AppContainer {
    companion object {
        const val SHARE_BASE_URL = "https://example.com/recipes/"

        @Volatile
        private var instance: MockAppContainer? = null

        /** Repository Singleton: every screen in the process shares one set of repositories. */
        fun getInstance(resources: Resources): MockAppContainer = instance ?: synchronized(this) {
            instance ?: MockAppContainer(mockSeed(resources)).also { instance = it }
        }
    }

    private val lock = Mutex()
    private fun initialUser() =
        seed.user.copy(pantry = Pantry(seed.pantryItems), comments = seed.comments.toList())
    private var user: User? = initialUser()
    private val catalog = seed.recipes.associateBy { it.recipeId }.toMutableMap()

    override val accounts = object : AccountRepository {
        override suspend fun register(email: String, password: String) = lock.withLock {
            user = initialUser().copy(email = email)
        }
        override suspend fun authenticate(email: String, password: String) = lock.withLock {
            val profile = (user ?: initialUser()).copy(email = email)
            user = profile
            AuthenticationResult.SignedIn(profile)
        }
        override suspend fun verifyOtp(code: String): User = lock.withLock { requireNotNull(user) }
        override suspend fun signOut() = lock.withLock { user = null }
        override suspend fun findUser(): User? = lock.withLock { user }
        override suspend fun saveUser(user: User): User = lock.withLock {
            require(user.userId == this@MockAppContainer.user?.userId)
            user.also { this@MockAppContainer.user = it }
        }
        override suspend fun savePreferences(preferences: UserPreferences): User = lock.withLock {
            requireNotNull(user).copy(preferences = preferences).also { user = it }
        }
        override suspend fun deleteUser() = lock.withLock {
            user = null
        }

        // The pantry already lives on the in-memory User, so there is nothing else to persist.
        override suspend fun loadPantryItems(): List<PantryItem> = lock.withLock {
            user?.pantry?.items.orEmpty()
        }
        override suspend fun savePantryItem(item: PantryItem) = Unit
        override suspend fun deletePantryItem(item: PantryItem) = Unit
    }
    override val ingredients = IngredientRepository(MockIngredientApiFactory(seed.ingredients))
    override val recipes = RecipeRepository(
        object : RecipeApiClient {
            override suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> =
                lock.withLock {
                    val ids = items.map { it.ingredientId }.toSet()
                    catalog.values.filter { r -> r.getIngredients().any { it.ingredientId in ids } }
                        .sortedByDescending { r ->
                            r.getIngredients().count { it.ingredientId in ids }
                        }
                }
            override suspend fun searchRecipes(query: String): List<Recipe> = lock.withLock {
                catalog.values.filter { it.name.contains(query, ignoreCase = true) }
            }
            override suspend fun getRecipe(recipeId: String): Recipe = lock.withLock {
                requireNotNull(catalog[recipeId])
            }
        },
        object : RecipeUserDataSource {
            override suspend fun blockedRecipeIds(): Set<String> = lock.withLock {
                user?.blockedRecipeIds.orEmpty()
            }
            override suspend fun setFavorite(recipe: Recipe, value: Boolean) = lock.withLock {
                val profile = requireNotNull(user)
                user = if (value) profile.favorite(recipe) else profile.unfavorite(recipe)
            }
            override suspend fun blockRecipe(recipeId: String) = lock.withLock {
                user = requireNotNull(user).block(requireNotNull(catalog[recipeId]))
            }
            override suspend fun saveCustom(recipe: Recipe): Recipe = lock.withLock {
                catalog[recipe.recipeId] = recipe
                recipe
            }
            override suspend fun createShareLink(recipeId: String): String =
                SHARE_BASE_URL + recipeId
        }
    )
    override fun barcodeScanner(): BarcodeScanner = DemoBarcodeScanner(MockSeed.DEMO_BARCODE)
}
