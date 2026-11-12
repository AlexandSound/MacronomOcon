package com.example.macronomocon.data.supabase

import com.example.macronomocon.domain.model.Comment
import java.time.OffsetDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object CommentMapper {
    fun comment(row: JsonObject): Comment = Comment(
        commentId = row.getValue("comment_id").jsonPrimitive.content,
        recipeId = row.getValue("recipe_id").jsonPrimitive.content,
        authorId = row.getValue("user_id").jsonPrimitive.content,
        text = row.getValue("text").jsonPrimitive.content,
        imageUrl = row["image_url"]?.jsonPrimitive?.contentOrNull,
        timestamp = OffsetDateTime.parse(
            row.getValue("created_at").jsonPrimitive.content
        ).toInstant(),
        likedByUserIds = row["comment_likes"]?.jsonArray?.map {
            it.jsonObject.getValue("user_id").jsonPrimitive.content
        }?.toSet().orEmpty(),
        authorName = row.getValue("author_name").jsonPrimitive.content
    )
}
