package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.Comment

/** The server supplies author identity and enforces ownership and unique likes. */
interface CommentRepository {
    suspend fun findByRecipe(recipeId: String): List<Comment>
    suspend fun publish(recipeId: String, text: String, imageUrl: String?): Comment
    suspend fun delete(commentId: String)
    suspend fun like(commentId: String): Comment
}
