package com.example.macronomocon.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class Comment(
    val commentId: String,
    var text: String,
    var imageUrl: String,
    val timestamp: Instant
) {
    fun like(u: User): Boolean = TODO("Like the comment")

    fun likeCount(): Int = TODO("Return the number of likes")

    fun isAuthor(u: User): Boolean = TODO("Check whether the user authored the comment")
}
