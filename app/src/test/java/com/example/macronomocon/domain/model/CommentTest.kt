package com.example.macronomocon.domain.model

import org.junit.Assert.*
import org.junit.Test

class CommentTest {
    @Test(expected = IllegalArgumentException::class)
    fun authorCannotLikeOwnComment() {
        val author = User("author", "author@example.com", "Author")
        Comment("comment", "recipe", author.userId, "Helpful recipe").like(author)
    }

    @Test
    fun sameUserCannotIncreaseLikeCountTwice() {
        val author = User("author", "author@example.com", "Author")
        val reader = User("reader", "reader@example.com", "Reader")
        val comment =
            Comment("comment", "recipe", author.userId, "Helpful recipe")
        val liked = comment.like(reader).like(reader)
        assertEquals(1, liked.likeCount())
        assertEquals(0, comment.likeCount())
        assertTrue(liked.isAuthor(author))
        assertFalse(liked.isAuthor(reader))
    }
}
