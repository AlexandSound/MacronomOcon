package com.example.macronomocon.data.supabase

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentMapperTest {
    private fun row(extra: String = "", text: String = "Useful recipe") = Json.parseToJsonElement(
        """
        {
          "comment_id": "comment-1", "recipe_id": "716429", "user_id": "author-1",
          "author_name": "Brady", "text": "$text",
          "created_at": "2026-11-12T03:04:05.123456+02:00"$extra
        }
        """.trimIndent()
    ).jsonObject

    @Test
    fun mapsSharedAuthorsImagesAndDistinctLikes() {
        val comment = CommentMapper.comment(
            row(
                """,
                "image_url": "https://example.com/photo.jpg",
                "comment_likes": [{"user_id": "reader-1"}, {"user_id": "reader-2"},
                                  {"user_id": "reader-1"}]
                """.trimIndent()
            )
        )
        assertEquals("comment-1", comment.commentId)
        assertEquals("716429", comment.recipeId)
        assertEquals("author-1", comment.authorId)
        assertEquals("Brady", comment.authorName)
        assertEquals("Useful recipe", comment.text)
        assertEquals("https://example.com/photo.jpg", comment.imageUrl)
        assertEquals(Instant.parse("2026-11-12T01:04:05.123456Z"), comment.timestamp)
        assertEquals(setOf("reader-1", "reader-2"), comment.likedByUserIds)
        assertEquals(2, comment.likeCount())
    }

    @Test
    fun mapsInsertResponseWithoutEmbeddedLikesOrImage() {
        val comment = CommentMapper.comment(row())
        assertNull(comment.imageUrl)
        assertTrue(comment.likedByUserIds.isEmpty())
    }

    @Test
    fun mapsExplicitNullImageAndEmptyLikes() {
        val comment = CommentMapper.comment(row(""", "image_url": null, "comment_likes": []"""))
        assertNull(comment.imageUrl)
        assertEquals(0, comment.likeCount())
    }

    @Test
    fun allowsImageOnlyCommentsForPhotoUploadIntegration() {
        val comment = CommentMapper.comment(
            row(""", "image_url": "https://example.com/photo.jpg"""", text = "")
        )
        assertEquals("", comment.text)
        assertEquals("https://example.com/photo.jpg", comment.imageUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCommentsWithoutTextOrImage() {
        CommentMapper.comment(row(text = "  "))
    }
}
