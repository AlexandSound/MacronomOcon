package com.example.macronomocon.data.local

import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.data.repository.PantryRepository
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.User
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory pantry for the running process, kept separately for each signed-in user so accounts
 * sharing a device never see each other's items. Replace with a `pantry_items` adapter once meal
 * deductions are atomic on the server.
 */
class LocalPantryRepository(private val currentUserId: () -> String?) : PantryRepository {
    private val lock = Mutex()
    private val pantries = mutableMapOf<String, Pantry>()

    private fun inventory(): Pantry = pantries.getOrPut(requireNotNull(currentUserId())) {
        Pantry()
    }

    override suspend fun findPantry(): Pantry = lock.withLock { Pantry(inventory().items) }

    override suspend fun addItem(item: PantryItem): Pantry = lock.withLock {
        inventory().addItem(item)
        Pantry(inventory().items)
    }

    override suspend fun removeItems(itemIds: Set<String>): Pantry = lock.withLock {
        val inventory = inventory()
        inventory.removeItems(inventory.items.filter { it.pantryItemId in itemIds })
        Pantry(inventory.items)
    }

    override suspend fun recordMeal(recipe: Recipe): Pantry = lock.withLock {
        inventory().deductIngredients(recipe)
        Pantry(inventory().items)
    }

    /** Discards every user's items, e.g. on sign-out. */
    suspend fun clear() = lock.withLock { pantries.clear() }
}

/**
 * In-memory comments authored as the signed-in [currentUser] until a comments table exists. Each
 * user only sees their own, since nothing is shared through a server yet.
 */
class LocalCommentRepository(private val currentUser: () -> User?) : CommentRepository {
    private val lock = Mutex()
    private val discussions = mutableMapOf<String, MutableMap<String, Comment>>()

    private fun user(): User = requireNotNull(currentUser())

    private fun discussion(): MutableMap<String, Comment> =
        discussions.getOrPut(user().userId) { mutableMapOf() }

    override suspend fun findByRecipe(recipeId: String): List<Comment> = lock.withLock {
        discussion().values.filter { it.recipeId == recipeId }.sortedBy { it.timestamp }
    }

    override suspend fun publish(recipeId: String, text: String, imageUrl: String?): Comment =
        lock.withLock {
            Comment(UUID.randomUUID().toString(), recipeId, user().userId, text, imageUrl)
                .also { discussion()[it.commentId] = it }
        }

    override suspend fun delete(commentId: String) = lock.withLock {
        require(requireNotNull(discussion()[commentId]).isAuthor(user()))
        discussion().remove(commentId)
        Unit
    }

    override suspend fun like(commentId: String): Comment = lock.withLock {
        requireNotNull(discussion()[commentId]).like(user()).also { discussion()[commentId] = it }
    }

    /** Discards every user's comments, e.g. on sign-out. */
    suspend fun clear() = lock.withLock { discussions.clear() }
}
