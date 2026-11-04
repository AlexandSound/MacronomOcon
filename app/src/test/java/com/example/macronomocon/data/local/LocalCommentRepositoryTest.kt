package com.example.macronomocon.data.local

import com.example.macronomocon.domain.model.User
import com.example.macronomocon.testing.assertThrows
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class LocalCommentRepositoryTest {
    private val recipeId = "716429"
    private val author = User("supabase-user", "a@example.com", "A")
    private val other = User("someone-else", "b@example.com", "B")
    private var user: User? = author
    private val comments = LocalCommentRepository { user }

    @Test
    fun publishedCommentsAreAuthoredBySignedInUser() = runTest {
        assertEquals(author.userId, comments.publish(recipeId, "Great", null).authorId)
    }

    @Test
    fun publishedCommentsAreFoundByRecipe() = runTest {
        val comment = comments.publish(recipeId, "Great", null)
        assertEquals(listOf(comment), comments.findByRecipe(recipeId))
    }

    @Test
    fun theAuthorCanDeleteTheirOwnComment() = runTest {
        val comment = comments.publish(recipeId, "Great", null)
        comments.delete(comment.commentId)
        assertTrue(comments.findByRecipe(recipeId).isEmpty())
    }

    @Test
    fun commentsAreNotVisibleToOtherAccounts() = runTest {
        comments.publish(recipeId, "Great", null)
        user = other
        assertTrue(comments.findByRecipe(recipeId).isEmpty())
    }

    @Test
    fun clearRemovesTheSignedInUsersComments() = runTest {
        comments.publish(recipeId, "Great", null)
        comments.clear()
        assertTrue(comments.findByRecipe(recipeId).isEmpty())
    }

    @Test
    fun otherAccountsCannotDeleteAComment() = runTest {
        val comment = comments.publish(recipeId, "Great", null)
        user = other
        assertThrows<IllegalArgumentException> { comments.delete(comment.commentId) }
    }
}
