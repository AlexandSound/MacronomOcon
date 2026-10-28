package com.example.macronomocon.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.ui.common.ViewState
import java.text.NumberFormat

/** Shared hover/press feedback retains Material focus, keyboard, touch, and accessibility behavior. */
@Composable
fun WarmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val color by animateColorAsState(
        if (hovered) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        label = "buttonColor"
    )
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "buttonScale")
    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaction,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            hoveredElevation = 5.dp
        ),
        modifier = modifier.heightIn(min = 52.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.hoverable(interaction, enabled)
    ) { Text(text) }
}

@Composable
fun ScreenHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            stringResource(R.string.brand_eyebrow),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun <T> StateFeedback(state: ViewState<T>, onDismiss: () -> Unit) {
    if (state.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    state.errorMessage?.let { message ->
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(message.resolve(), color = MaterialTheme.colorScheme.onErrorContainer)
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun RecipeCard(recipe: Recipe, pantry: List<PantryItem>, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hovered) 6.dp else 0.dp),
        modifier = Modifier.fillMaxWidth().hoverable(interaction)
    ) {
        RecipeSymbol(Modifier.fillMaxWidth().height(150.dp))
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(R.string.recipe_time, recipe.totalTime()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    difficultyLabel(recipe.difficulty),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(mealLabel(recipe.mealType), style = MaterialTheme.typography.labelLarge)
            }
            Text(recipe.name, style = MaterialTheme.typography.titleLarge)
            Text(
                pluralStringResource(
                    R.plurals.recipe_matches,
                    recipe.getIngredients().size,
                    recipe.matchCount(pantry),
                    recipe.getIngredients().size
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Placeholder until recipe photos exist: a decorative Android/Material symbol, never generated art. */
@Composable
fun RecipeSymbol(modifier: Modifier = Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(R.drawable.ic_restaurant),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
fun difficultyLabel(value: Difficulty): String = stringResource(
    when (value) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    }
)

@Composable
fun mealLabel(value: MealType): String = stringResource(
    when (value) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACK -> R.string.meal_snack
    }
)

@Composable
fun quantityLabel(quantity: Double, unit: String): String {
    val label = when (unit) {
        PantryItem.UNIT_GRAMS -> stringResource(R.string.unit_grams)
        PantryItem.UNIT_PIECE -> stringResource(R.string.unit_pieces)
        else -> unit
    }
    return stringResource(
        R.string.stock_amount,
        NumberFormat.getNumberInstance().format(quantity),
        label
    )
}
