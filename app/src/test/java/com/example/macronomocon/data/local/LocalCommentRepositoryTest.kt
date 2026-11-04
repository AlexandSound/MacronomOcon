package com.example.macronomocon.data.local

import com.example.macronomocon.domain.model.User
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCommentRepositoryTest {
    private var user: User? = User("supabase-user", "a@example.com", "A")
    private val comments = LocalCommentRepository { user }

    @Test
    fun commentsAreAuthoredBySignedInUserForAnyRecipe() = runTest {
        val comment = comments.publish("716429", "Great", null)
        assertEquals("supabase-user", comment.authorId)
        assertEquals(listOf(comment), comments.findByRecipe("716429"))
        comments.delete(comment.commentId)
        assertTrue(comments.findByRecipe("716429").isEmpty())
    }

    @Test
    fun commentsAreNotSharedBetweenAccountsAndClearRemovesThem() = runTest {
        comments.publish("716429", "Great", null)
        user = User("someone-else", "b@example.com", "B")
        assertTrue(comments.findByRecipe("716429").isEmpty())
        user = User("supabase-user", "a@example.com", "A")
        comments.clear()
        assertTrue(comments.findByRecipe("716429").isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun otherUsersCannotDelete() = runTest {
        val comment = comments.publish("716429", "Great", null)
        user = User("someone-else", "b@example.com", "B")
        comments.delete(comment.commentId)
    }
}
