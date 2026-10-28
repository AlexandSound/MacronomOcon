package com.example.macronomocon.ui.comment

import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.ui.common.StateViewModel
import com.example.macronomocon.ui.common.UiMessage

data class CommentState(val recipeId: String? = null, val comments: List<Comment> = emptyList())

class CommentViewModel(private val repository: CommentRepository) :
    StateViewModel<CommentState>(CommentState()) {
    fun loadComments(recipeId: String) = action(UiMessage.ERROR_LOAD_COMMENTS) {
        val comments = repository.findByRecipe(recipeId)
        updateData { CommentState(recipeId, comments) }
    }

    fun publishComment(text: String, imageUrl: String? = null) =
        action(UiMessage.ERROR_PUBLISH_THIS_COMMENT) {
            val recipeId = requireNotNull(data.recipeId)
            require(text.isNotBlank())
            val comment = repository.publish(recipeId, text.trim(), imageUrl)
            updateData { it.copy(comments = it.comments + comment) }
        }

    fun deleteComment(comment: Comment) = action(UiMessage.ERROR_DELETE_THIS_COMMENT) {
        repository.delete(comment.commentId)
        updateData {
            it.copy(
                comments = it.comments.filterNot { c ->
                    c.commentId ==
                        comment.commentId
                }
            )
        }
    }

    fun likeComment(comment: Comment) = action(UiMessage.ERROR_LIKE_THIS_COMMENT) {
        val saved = repository.like(comment.commentId)
        updateData {
            it.copy(
                comments = it.comments.map { c ->
                    if (c.commentId ==
                        saved.commentId
                    ) {
                        saved
                    } else {
                        c
                    }
                }
            )
        }
    }
}
