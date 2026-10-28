package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.comment.CommentState
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.RecipeSymbol
import com.example.macronomocon.ui.components.StateFeedback
import com.example.macronomocon.ui.components.WarmButton
import com.example.macronomocon.ui.components.quantityLabel
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    pantry: List<PantryItem>,
    user: User?,
    comments: ViewState<CommentState>,
    busy: Boolean,
    onFavorite: () -> Unit,
    onCook: () -> Unit,
    onPublish: suspend (String) -> Boolean,
    onLike: (Comment) -> Unit,
    onDelete: (Comment) -> Unit,
    onDismissError: () -> Unit
) {
    var confirmMeal by rememberSaveable(recipe.recipeId) { mutableStateOf(false) }
    var draft by rememberSaveable(recipe.recipeId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val canCook = Pantry(pantry).hasIngredients(recipe)
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { RecipeSymbol(Modifier.fillMaxWidth().height(190.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.headlineLarge)
                Text(
                    stringResource(R.string.recipe_time, recipe.totalTime()),
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onFavorite, enabled = !busy && user != null) {
                    Text(
                        stringResource(
                            if (recipe.recipeId in
                                user?.favoriteRecipeIds.orEmpty()
                            ) {
                                R.string.saved_recipe
                            } else {
                                R.string.save_recipe
                            }
                        )
                    )
                }
            }
        }
        item {
            Text(
                stringResource(R.string.ingredients_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        items(recipe.requirements) { required ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(required.ingredient.name, modifier = Modifier.weight(1f))
                Text(
                    quantityLabel(required.quantity, required.unit),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item { HorizontalDivider() }
        item {
            Text(stringResource(R.string.method_title), style = MaterialTheme.typography.titleLarge)
        }
        items(recipe.instructions.size) { index ->
            InstructionStep(index + 1, recipe.instructions[index])
        }
        item {
            WarmButton(
                stringResource(R.string.cook_recipe),
                {
                    confirmMeal = true
                },
                Modifier.fillMaxWidth(),
                canCook && !busy
            )
            if (!canCook) {
                Text(
                    stringResource(R.string.missing_stock),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item { HorizontalDivider() }
        item {
            Text(
                stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleLarge
            )
        }
        item { StateFeedback(comments, onDismissError) }
        if (comments.data.recipeId == recipe.recipeId) {
            items(comments.data.comments, key = {
                it.commentId
            }) { comment -> CommentRow(comment, user, !comments.isLoading, onLike, onDelete) }
            if (comments.data.comments.isEmpty() &&
                !comments.isLoading
            ) {
                item { Text(stringResource(R.string.no_comments)) }
            }
            item {
                CommentComposer(
                    draft,
                    { draft = it },
                    enabled = !comments.isLoading && user != null,
                    onPublish = {
                        val sent = draft
                        scope.launch { if (onPublish(sent) && draft == sent) draft = "" }
                    }
                )
            }
        }
    }
    if (confirmMeal) {
        AlertDialog(
            onDismissRequest = { confirmMeal = false },
            title = { Text(stringResource(R.string.cook_confirmation_title)) },
            text = { Text(stringResource(R.string.cook_confirmation_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmMeal = false
                    onCook()
                }, enabled = !busy) { Text(stringResource(R.string.confirm_meal)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmMeal = false
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun InstructionStep(number: Int, instruction: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.step_number, number),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(instruction, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    user: User?,
    enabled: Boolean,
    onLike: (Comment) -> Unit,
    onDelete: (Comment) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(
                if (comment.authorId ==
                    user?.userId
                ) {
                    R.string.comment_author_self
                } else {
                    R.string.comment_author_other
                }
            ),
            style = MaterialTheme.typography.labelLarge
        )
        Text(comment.text)
        Row {
            TextButton(
                onClick = { onLike(comment) },
                enabled =
                enabled && user != null && user.userId !in comment.likedByUserIds
            ) {
                Text(stringResource(R.string.like_count, comment.likeCount()))
            }
            if (comment.authorId ==
                user?.userId
            ) {
                TextButton(onClick = {
                    onDelete(comment)
                }, enabled = enabled) { Text(stringResource(R.string.delete_comment)) }
            }
        }
    }
}

@Composable
private fun CommentComposer(
    text: String,
    onTextChange: (String) -> Unit,
    enabled: Boolean,
    onPublish: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(text, onTextChange, modifier = Modifier.fillMaxWidth(), label = {
            Text(stringResource(R.string.comment_hint))
        }, minLines = 2)
        WarmButton(
            stringResource(R.string.publish_comment),
            onPublish,
            enabled =
            enabled && text.isNotBlank()
        )
    }
}
