package com.example.macronomocon.domain.model

import java.util.UUID

/** Client profile only. Password verification, OTP, and lockouts belong to the authentication service. */
data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val twoFactorEnabled: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val favoriteRecipeIds: Set<String> = emptySet(),
    val blockedRecipeIds: Set<String> = emptySet(),
    val pantry: Pantry = Pantry(),
    val comments: List<Comment> = emptyList()
) {
    init {
        require(userId.isNotBlank())
    }
    fun loadComments(recipeId: String): List<Comment> =
        comments.filter { it.recipeId == recipeId }.sortedBy { it.timestamp }

    fun publishComment(recipeId: String, text: String, imageUrl: String? = null): User {
        require(recipeId.isNotBlank() && text.isNotBlank())
        val comment = Comment(UUID.randomUUID().toString(), recipeId, userId, text.trim(), imageUrl)
        return copy(comments = comments + comment)
    }

    fun deleteComment(commentId: String): User {
        val comment = requireNotNull(comments.find { it.commentId == commentId })
        require(comment.isAuthor(this))
        return copy(comments = comments.filterNot { it.commentId == commentId })
    }

    fun likeComment(commentId: String): User {
        val comment = requireNotNull(comments.find { it.commentId == commentId })
        return copy(
            comments = comments.map {
                if (it.commentId ==
                    commentId
                ) {
                    comment.like(this)
                } else {
                    it
                }
            }
        )
    }

    fun favorite(r: Recipe): User = copy(favoriteRecipeIds = favoriteRecipeIds + r.recipeId)

    fun unfavorite(r: Recipe): User = copy(favoriteRecipeIds = favoriteRecipeIds - r.recipeId)

    fun block(r: Recipe): User = copy(blockedRecipeIds = blockedRecipeIds + r.recipeId)

    fun isBlocked(r: Recipe): Boolean = r.recipeId in blockedRecipeIds
}
