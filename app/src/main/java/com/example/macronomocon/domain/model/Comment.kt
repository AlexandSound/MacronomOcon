package com.example.macronomocon.domain.model

import java.time.Instant

data class Comment(
    val commentId: String,
    val recipeId: String,
    val authorId: String,
    val text: String,
    val imageUrl: String? = null,
    val timestamp: Instant = Instant.now(),
    val likedByUserIds: Set<String> = emptySet()
) {
    init {
        require(commentId.isNotBlank() && recipeId.isNotBlank() && authorId.isNotBlank())
        require(text.isNotBlank())
    }

    fun like(u: User): Comment = copy(likedByUserIds = likedByUserIds + u.userId)

    fun likeCount(): Int = likedByUserIds.size

    fun isAuthor(u: User): Boolean = authorId == u.userId
}
