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

    @Test
    fun sharedCommentsReplaceOnlyThatRecipesDiscussion() {
        val kept = Comment("kept", "other-recipe", "a", "Mine")
        val stale = Comment("stale", "recipe", "b", "Deleted elsewhere")
        val shared = listOf(Comment("shared", "recipe", "b", "From B"))
        val user = User("a", "a@example.com", "A", comments = listOf(kept, stale))
        val shown = user.showComments("recipe", shared)
        assertEquals(shared, shown.loadComments("recipe"))
        assertEquals(listOf(kept), shown.loadComments("other-recipe"))
        assertThrows(IllegalArgumentException::class.java) {
            user.showComments("recipe", listOf(kept))
        }
    }

    @Test
    fun serverSavedCommentsAreAddedOnlyForTheirAuthor() {
        val user = User("a", "a@example.com", "A")
        val published = Comment("saved", "recipe", "a", "", "https://example.com/photo.jpg")
        assertEquals(listOf(published), user.publishComment(published).loadComments("recipe"))
        assertThrows(IllegalArgumentException::class.java) {
            user.publishComment(Comment("other", "recipe", "b", "Not mine"))
        }
    }

    @Test
    fun selfLikesAreRejectedAndServerLikesKeepOtherReaders() {
        val own = Comment("own", "recipe", "a", "Mine")
        val other = Comment("other", "recipe", "b", "Tip", likedByUserIds = setOf("c"))
        val user = User("a", "a@example.com", "A", comments = listOf(own, other))
        assertThrows(IllegalArgumentException::class.java) { user.likeComment("own") }
        val saved = other.copy(likedByUserIds = setOf("a", "c"))
        val liked = user.likeComment(saved)
        assertEquals(2, liked.loadComments("recipe").single { it.commentId == "other" }.likeCount())
        assertThrows(IllegalArgumentException::class.java) { user.likeComment(other) }
    }
}
