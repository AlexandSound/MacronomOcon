package com.example.macronomocon.domain.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class UserActivityTest {
    @Test
    fun usersDoNotSharePantriesOrComments() {
        val first = User("a", "a@example.com", "A")
        val second = User("b", "b@example.com", "B")
        first.pantry.addItem(PantryItem(Ingredient("rice", "Rice"), 100.0, "g", LocalDate.now()))
        val updated = first.publishComment("recipe", "Great")
        assertTrue(second.pantry.items.isEmpty())
        assertTrue(second.comments.isEmpty())
        assertTrue(first.comments.isEmpty())
        assertEquals("a", updated.comments.single().authorId)
    }

    @Test
    fun deletingAnotherAuthorsCommentIsRejectedAndLikesAreUnique() {
        val comment = Comment("comment", "recipe", "other", "Tip")
        val user = User("a", "a@example.com", "A", comments = listOf(comment))
        assertThrows(IllegalArgumentException::class.java) { user.deleteComment("comment") }
        val liked = user.likeComment("comment").likeComment("comment")
        assertEquals(1, liked.comments.single().likeCount())
        assertEquals(0, user.comments.single().likeCount())
    }
}
