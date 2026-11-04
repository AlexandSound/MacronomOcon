package com.example.macronomocon.domain.model

import org.junit.Assert.*
import org.junit.Test

class CommentTest {
    private val author = User("author", "author@example.com", "Author")
    private val reader = User("reader", "reader@example.com", "Reader")
    private val comment = Comment("comment", "recipe", author.userId, "Helpful recipe")

    @Test
    fun likeFromANewReaderIncrementsTheLikeCount() {
        assertEquals(1, comment.like(reader).likeCount())
    }

    @Test
    fun repeatedLikesFromTheSameReaderCountOnce() {
        assertEquals(1, comment.like(reader).like(reader).likeCount())
    }

    @Test
    fun likeLeavesTheOriginalCommentUnchanged() {
        comment.like(reader)
        assertEquals(0, comment.likeCount())
    }

    @Test
    fun isAuthorRecognizesTheCommentAuthor() {
        assertTrue(comment.isAuthor(author))
    }

    @Test
    fun isAuthorRejectsOtherUsers() {
        assertFalse(comment.isAuthor(reader))
    }

    @Test
    fun likingPreservesTheOriginalAuthor() {
        assertTrue(comment.like(reader).isAuthor(author))
    }
}
