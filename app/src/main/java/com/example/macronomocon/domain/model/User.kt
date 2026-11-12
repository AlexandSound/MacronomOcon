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

    /** The held discussion for [recipeId], oldest first. */
    fun loadComments(recipeId: String): List<Comment> =
        comments.filter { it.recipeId == recipeId }.sortedBy { it.timestamp }

    /** Replaces the held discussion for [recipeId] with [shared], e.g. every user's comments. */
    fun showComments(recipeId: String, shared: List<Comment>): User {
        require(shared.all { it.recipeId == recipeId })
        return copy(comments = comments.filterNot { it.recipeId == recipeId } + shared)
    }

    /** Authors a comment with a local ID; server-saved comments use the [Comment] overload. */
    fun publishComment(recipeId: String, text: String, imageUrl: String? = null): User {
        require(recipeId.isNotBlank() && (text.isNotBlank() || !imageUrl.isNullOrBlank()))
        return publishComment(
            Comment(
                UUID.randomUUID().toString(),
                recipeId,
                userId,
                text.trim(),
                imageUrl,
                authorName = displayName
            )
        )
    }

    /** Adds a comment this user published, such as the row the server saved. */
    fun publishComment(published: Comment): User {
        require(published.isAuthor(this))
        val others = comments.filterNot { it.commentId == published.commentId }
        return copy(comments = others + published)
    }

    fun deleteComment(commentId: String): User {
        val comment = requireNotNull(comments.find { it.commentId == commentId })
        require(comment.isAuthor(this))
        return copy(comments = comments.filterNot { it.commentId == commentId })
    }

    /** Likes another author's comment; liking twice has no further effect. */
    fun likeComment(commentId: String): User {
        val liked = requireNotNull(comments.find { it.commentId == commentId }).like(this)
        return replaceComment(liked)
    }

    /** Applies a like the server recorded, keeping the likes it returned from other users. */
    fun likeComment(liked: Comment): User {
        require(!liked.isAuthor(this) && userId in liked.likedByUserIds)
        requireNotNull(comments.find { it.commentId == liked.commentId })
        return replaceComment(liked)
    }

    private fun replaceComment(comment: Comment): User =
        copy(comments = comments.map { if (it.commentId == comment.commentId) comment else it })

    fun favorite(r: Recipe): User = copy(favoriteRecipeIds = favoriteRecipeIds + r.recipeId)

    fun unfavorite(r: Recipe): User = copy(favoriteRecipeIds = favoriteRecipeIds - r.recipeId)

    fun block(r: Recipe): User = copy(blockedRecipeIds = blockedRecipeIds + r.recipeId)

    fun isBlocked(r: Recipe): Boolean = r.recipeId in blockedRecipeIds
}
