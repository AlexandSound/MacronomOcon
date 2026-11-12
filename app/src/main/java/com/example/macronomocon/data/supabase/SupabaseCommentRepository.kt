package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.domain.model.Comment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Shared comments; ownership and like restrictions are enforced by database RLS. */
class SupabaseCommentRepository(private val client: SupabaseClient) : CommentRepository {
    override suspend fun findByRecipe(recipeId: String): List<Comment> {
        require(recipeId.isNotBlank())
        return client.from(TABLE).select(Columns.raw("*,comment_likes(user_id)")) {
            filter { eq("recipe_id", recipeId) }
            order("created_at", Order.ASCENDING)
            order("comment_id", Order.ASCENDING)
        }.decodeList<JsonObject>().map(CommentMapper::comment)
    }

    override suspend fun publish(recipeId: String, text: String, imageUrl: String?): Comment {
        require(recipeId.isNotBlank())
        val image = imageUrl?.trim()?.takeIf { it.isNotEmpty() }
        require(text.isNotBlank() || image != null)
        return client.from(TABLE).insert(
            buildJsonObject {
                put("recipe_id", recipeId)
                put("user_id", userId())
                put("text", text.trim())
                put("image_url", image)
            }
        ) { select() }.decodeSingle<JsonObject>().let(CommentMapper::comment)
    }

    override suspend fun delete(commentId: String) {
        // Requiring a returned row prevents a silently filtered RLS delete appearing successful.
        client.from(TABLE).delete {
            filter {
                eq("comment_id", commentId)
                eq("user_id", userId())
            }
            select()
        }.decodeSingle<JsonObject>()
    }

    override suspend fun like(commentId: String): Comment {
        client.from(LIKES_TABLE).insert(
            buildJsonObject {
                put("comment_id", commentId)
                put("user_id", userId())
            }
        )
        return client.from(TABLE).select(Columns.raw("*,comment_likes(user_id)")) {
            filter { eq("comment_id", commentId) }
        }.decodeSingle<JsonObject>().let(CommentMapper::comment)
    }

    private fun userId(): String = requireNotNull(client.auth.currentUserOrNull()).id

    companion object {
        const val TABLE = "comments"
        const val LIKES_TABLE = "comment_likes"
    }
}
