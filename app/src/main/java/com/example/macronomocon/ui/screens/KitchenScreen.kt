package com.example.macronomocon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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

private const val FEATURED_RECIPE_COUNT = 3

@Composable
fun KitchenScreen(
    items: List<PantryItem>,
    recipeState: ViewState<RecipeState>,
    onDismissError: () -> Unit,
    openPantry: () -> Unit,
    openRecipes: () -> Unit,
    openRecipe: (Recipe) -> Unit
) {
    val recipes = recipeState.data.recipes
    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ScreenHeading(
                stringResource(R.string.kitchen_title),
                stringResource(R.string.kitchen_subtitle)
            )
        }
        if (recipeState.isLoading || recipeState.errorMessage != null) {
            item { StateFeedback(recipeState, onDismissError) }
        }
        item { PantrySummary(items, openPantry) }
        item {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        stringResource(R.string.tonight_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = openRecipes) { Text(stringResource(R.string.view_all)) }
                }
                Text(
                    stringResource(R.string.tonight_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (recipes.isEmpty() && !recipeState.isLoading) {
            item {
                EmptyState(
                    stringResource(R.string.no_recipes),
                    stringResource(R.string.no_recipes_body)
                )
            }
        }
        items(recipes.take(FEATURED_RECIPE_COUNT), key = {
            it.recipeId
        }) { recipe -> RecipeCard(recipe, items) { openRecipe(recipe) } }
    }
}

@Composable
private fun PantrySummary(items: List<PantryItem>, openPantry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                pluralStringResource(R.plurals.pantry_count, items.size, items.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                stringResource(
                    R.string.expiring_count,
                    items.count {
                        it.isExpiringWithin(PantryItem.EXPIRING_SOON_DAYS)
                    }
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            TextButton(onClick = openPantry) { Text(stringResource(R.string.open_pantry)) }
        }
    }
}
