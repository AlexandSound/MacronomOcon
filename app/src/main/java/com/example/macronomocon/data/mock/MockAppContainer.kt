package com.example.macronomocon.data.mock

import android.content.res.Resources
import androidx.annotation.VisibleForTesting
import com.example.macronomocon.data.api.IngredientApiClient
import com.example.macronomocon.data.api.RecipeApiClient
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.PantryRepository
import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.data.repository.RecipeUserDataSource
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.di.AppContainer
import com.example.macronomocon.domain.model.AuthenticationResult
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.domain.model.UserPreferences
import java.util.UUID
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
    private var user: User? = seed.user
    private val inventory = Pantry(seed.pantryItems)
    private val catalog = seed.recipes.associateBy { it.recipeId }.toMutableMap()
    private val discussion = seed.comments.associateBy { it.commentId }.toMutableMap()

    override val accounts = object : AccountRepository {
        override suspend fun register(email: String, password: String) = lock.withLock {
            user = seed.user.copy(email = email)
        }
        override suspend fun authenticate(email: String, password: String) = lock.withLock {
            val profile = (user ?: seed.user).copy(email = email)
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
            inventory.removeItems(inventory.items)
            discussion.clear()
        }
    }
    override val pantry = object : PantryRepository {
        override suspend fun findPantry(): Pantry = lock.withLock { Pantry(inventory.items) }
        override suspend fun addItem(item: PantryItem): Pantry = lock.withLock {
            inventory.addItem(item)
            Pantry(inventory.items)
        }
        override suspend fun removeItems(itemIds: Set<String>): Pantry = lock.withLock {
            inventory.removeItems(inventory.items.filter { it.pantryItemId in itemIds })
            Pantry(inventory.items)
        }
        override suspend fun recordMeal(recipe: Recipe): Pantry = lock.withLock {
            inventory.deductIngredients(recipe)
            Pantry(inventory.items)
        }
    }
    override val ingredients = IngredientRepository(object : IngredientApiClient {
        override suspend fun lookupBarcode(barcode: String): List<Ingredient> =
            seed.ingredients.filter { it.barcode == barcode }
        override suspend fun searchIngredients(query: String): List<Ingredient> =
            seed.ingredients.filter { it.name.contains(query, ignoreCase = true) }
    })
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
            override suspend fun setFavorite(recipeId: String, value: Boolean) = lock.withLock {
                val profile = requireNotNull(user)
                val recipe = requireNotNull(catalog[recipeId])
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
    override val comments = object : CommentRepository {
        override suspend fun findByRecipe(recipeId: String): List<Comment> = lock.withLock {
            discussion.values.filter { it.recipeId == recipeId }.sortedBy { it.timestamp }
        }
        override suspend fun publish(recipeId: String, text: String, imageUrl: String?): Comment =
            lock.withLock {
                require(catalog.containsKey(recipeId))
                val comment =
                    Comment(
                        UUID.randomUUID().toString(),
                        recipeId,
                        requireNotNull(user).userId,
                        text,
                        imageUrl
                    )
                discussion[comment.commentId] = comment
                comment
            }
        override suspend fun delete(commentId: String) = lock.withLock {
            require(requireNotNull(discussion[commentId]).isAuthor(requireNotNull(user)))
            discussion.remove(commentId)
            Unit
        }
        override suspend fun like(commentId: String): Comment = lock.withLock {
            requireNotNull(discussion[commentId]).like(requireNotNull(user)).also {
                discussion[commentId] =
                    it
            }
        }
    }

    override fun barcodeScanner(): BarcodeScanner = DemoBarcodeScanner(MockSeed.DEMO_BARCODE)
}
