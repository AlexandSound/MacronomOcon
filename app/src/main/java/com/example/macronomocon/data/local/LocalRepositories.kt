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
 * In-memory pantry for the running process, starting empty. Replace with a `pantry_items` adapter
 * once meal deductions are atomic on the server.
 */
class LocalPantryRepository : PantryRepository {
    private val lock = Mutex()
    private val inventory = Pantry()

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

/** In-memory comments authored as the signed-in [currentUser] until a comments table exists. */
class LocalCommentRepository(private val currentUser: () -> User?) : CommentRepository {
    private val lock = Mutex()
    private val discussion = mutableMapOf<String, Comment>()

    override suspend fun findByRecipe(recipeId: String): List<Comment> = lock.withLock {
        discussion.values.filter { it.recipeId == recipeId }.sortedBy { it.timestamp }
    }

    override suspend fun publish(recipeId: String, text: String, imageUrl: String?): Comment =
        lock.withLock {
            val author = requireNotNull(currentUser())
            Comment(UUID.randomUUID().toString(), recipeId, author.userId, text, imageUrl)
                .also { discussion[it.commentId] = it }
        }

    override suspend fun delete(commentId: String) = lock.withLock {
        require(requireNotNull(discussion[commentId]).isAuthor(requireNotNull(currentUser())))
        discussion.remove(commentId)
        Unit
    }

    override suspend fun like(commentId: String): Comment = lock.withLock {
        requireNotNull(discussion[commentId]).like(requireNotNull(currentUser()))
            .also { discussion[commentId] = it }
    }
}
