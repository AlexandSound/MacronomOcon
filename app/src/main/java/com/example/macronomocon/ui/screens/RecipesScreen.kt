package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.ui.common.ViewState
import com.example.macronomocon.ui.components.EmptyState
import com.example.macronomocon.ui.components.RecipeCard
import com.example.macronomocon.ui.components.ScreenHeading
import com.example.macronomocon.ui.components.StateFeedback
import com.example.macronomocon.ui.recipe.RecipeState

enum class RecipeFilter { ALL, SAVED, QUICK }

@Composable
fun RecipesScreen(
    state: ViewState<RecipeState>,
    pantry: List<PantryItem>,
    favorites: Set<String>,
    filter: RecipeFilter,
    onFilter: (RecipeFilter) -> Unit,
    onSearch: (String) -> Unit,
    onOpen: (Recipe) -> Unit,
    onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val visible = state.data.recipes.filter { recipe ->
        when (filter) {
            RecipeFilter.ALL -> true
            RecipeFilter.SAVED -> recipe.recipeId in favorites
            RecipeFilter.QUICK -> recipe.isQuick()
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeading(
                stringResource(R.string.recipes_title),
                stringResource(R.string.recipes_subtitle)
            )
        }
        item {
            OutlinedTextField(query, {
                query = it
            }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = {
                Text(stringResource(R.string.search_recipes))
            }, trailingIcon = {
                TextButton(onClick = {
                    onSearch(query)
                }, enabled = !state.isLoading) { Text(stringResource(R.string.search)) }
            })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeFilter.entries.forEach { option ->
                    FilterChip(selected = filter == option, onClick = {
                        onFilter(option)
                    }, label = {
                        Text(
                            when (option) {
                                RecipeFilter.ALL -> stringResource(R.string.for_you)
                                RecipeFilter.SAVED -> stringResource(R.string.saved_recipes)
                                RecipeFilter.QUICK -> stringResource(
                                    R.string.quick_recipes,
                                    Recipe.QUICK_MAX_MINUTES
                                )
                            }
                        )
                    })
                }
            }
        }
        item { StateFeedback(state, onDismiss) }
        if (visible.isEmpty() &&
            !state.isLoading
        ) {
            item {
                EmptyState(
                    stringResource(R.string.no_recipes),
                    stringResource(R.string.no_recipes_body)
                )
            }
        }
        items(visible, key = {
            it.recipeId
        }) { recipe -> RecipeCard(recipe, pantry) { onOpen(recipe) } }
    }
}
