package com.example.macronomocon.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macronomocon.R
import com.example.macronomocon.di.AppContainer
import com.example.macronomocon.di.viewModelFactory
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.ui.account.AccountViewModel
import com.example.macronomocon.ui.comment.CommentViewModel
import com.example.macronomocon.ui.components.StateFeedback
import com.example.macronomocon.ui.pantry.PantryViewModel
import com.example.macronomocon.ui.recipe.RecipeViewModel
import com.example.macronomocon.ui.screens.AddIngredientDialog
import com.example.macronomocon.ui.screens.KitchenScreen
import com.example.macronomocon.ui.screens.PantryScreen
import com.example.macronomocon.ui.screens.ProfileScreen
import com.example.macronomocon.ui.screens.RecipeDetailScreen
import com.example.macronomocon.ui.screens.RecipeFilter
import com.example.macronomocon.ui.screens.RecipesScreen
import kotlinx.coroutines.launch

enum class Destination(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    KITCHEN(R.string.nav_kitchen, R.drawable.ic_kitchen),
    PANTRY(R.string.nav_pantry, R.drawable.ic_pantry),
    RECIPES(R.string.nav_recipes, R.drawable.ic_recipes),
    PROFILE(R.string.nav_profile, R.drawable.ic_profile)
}

@Composable
fun MacronomoconApp(container: AppContainer) {
    val factory = remember(container) { container.viewModelFactory() }
    val pantry: PantryViewModel = viewModel(factory = factory)
    val recipes: RecipeViewModel = viewModel(factory = factory)
    val kitchenRecipes: RecipeViewModel =
        viewModel(key = Destination.KITCHEN.name, factory = factory)
    val account: AccountViewModel = viewModel(factory = factory)
    val comments: CommentViewModel = viewModel(factory = factory)
    val pantryState by pantry.uiState.collectAsStateWithLifecycle()
    val recipeState by recipes.uiState.collectAsStateWithLifecycle()
    val kitchenRecipeState by kitchenRecipes.uiState.collectAsStateWithLifecycle()
    val accountState by account.uiState.collectAsStateWithLifecycle()
    val commentState by comments.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(Destination.KITCHEN) }
    var filter by rememberSaveable { mutableStateOf(RecipeFilter.ALL) }
    // Keep the selected recipe ID across recreation.
    var detailKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val savedTabs = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mealRecordedMessage = stringResource(R.string.meal_recorded)
    fun recommendations() {
        val ingredients = pantry.uiState.value.data.items
            .filter { !it.isExpired() && it.quantity > 0 }.map { it.ingredient }
        kitchenRecipes.findRecipes(ingredients)
        if (filter == RecipeFilter.SAVED) {
            recipes.loadSaved(account.uiState.value.data.user?.favoriteRecipeIds.orEmpty())
        } else {
            recipes.findRecipes(ingredients)
        }
    }
    fun navigate(next: Destination) {
        detailKey = null
        destination = next
    }
    fun openRecipe(recipe: Recipe) {
        detailKey = recipe.recipeId
    }

    LaunchedEffect(container) {
        account.loadAccount().join()
        pantry.loadPantry().join()
        recommendations()
    }
    LaunchedEffect(detailKey) {
        detailKey?.let {
            recipes.loadRecipe(it)
            comments.loadComments(it)
        }
    }
    BackHandler(enabled = detailKey != null || destination != Destination.KITCHEN) {
        if (detailKey != null) detailKey = null else destination = Destination.KITCHEN
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = { KitchenBottomBar(destination) { navigate(it) } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                KitchenTopBar(showBack = detailKey != null, onBack = { detailKey = null })
                if (detailKey != null) {
                    StateFeedback(recipeState, recipes::clearError)
                    StateFeedback(pantryState, pantry::clearError)
                    val recipe = recipeState.data.selectedRecipe?.takeIf {
                        it.recipeId ==
                            detailKey
                    }
                    if (recipe != null) {
                        RecipeDetailScreen(
                            recipe, pantryState.data.items, accountState.data.user, commentState,
                            recipeState.isLoading || pantryState.isLoading,
                            onFavorite = {
                                scope.launch {
                                    recipes.setFavorite(
                                        recipe,
                                        recipe.recipeId !in
                                            accountState.data.user?.favoriteRecipeIds.orEmpty()
                                    ).join()
                                    account.loadAccount()
                                }
                            },
                            onCook = {
                                scope.launch {
                                    pantry.recordMeal(recipe).join()
                                    if (pantry.uiState.value.errorMessage == null) {
                                        recommendations()
                                        snackbar.showSnackbar(
                                            mealRecordedMessage
                                        )
                                    }
                                }
                            },
                            onPublish = {
                                comments.publishComment(it).join()
                                comments.uiState.value.errorMessage == null
                            }, onLike = { comments.likeComment(it) },
                            onDelete = {
                                comments.deleteComment(it)
                            }, onDismissError = comments::clearError
                        )
                    }
                } else {
                    savedTabs.SaveableStateProvider(destination) {
                        when (destination) {
                            Destination.KITCHEN -> KitchenScreen(
                                pantryState.data.items,
                                kitchenRecipeState,
                                kitchenRecipes::clearError,
                                { navigate(Destination.PANTRY) },
                                { navigate(Destination.RECIPES) },
                                ::openRecipe
                            )
                            Destination.PANTRY -> PantryScreen(pantryState, {
                                pantry.beginAdding()
                                showAdd = true
                            }, { id ->
                                scope.launch {
                                    pantry.removeSelected(setOf(id)).join()
                                    recommendations()
                                }
                            }, pantry::clearError)
                            Destination.RECIPES -> RecipesScreen(
                                recipeState,
                                pantryState.data.items,
                                accountState.data.user?.favoriteRecipeIds.orEmpty(),
                                filter,
                                {
                                    filter = it
                                    if (it == RecipeFilter.SAVED) {
                                        recipes.loadSaved(
                                            accountState.data.user?.favoriteRecipeIds.orEmpty()
                                        )
                                    } else {
                                        recommendations()
                                    }
                                },
                                {
                                    if (it.isBlank()) recommendations() else recipes.search(it)
                                },
                                ::openRecipe,
                                recipes::clearError
                            )
                            Destination.PROFILE -> ProfileScreen(accountState, { enabled ->
                                accountState.data.user?.let {
                                    account.updatePreferences(
                                        it.preferences.copy(notificationsEnabled = enabled)
                                    )
                                }
                            }, {
                                filter = RecipeFilter.SAVED
                                navigate(Destination.RECIPES)
                                recipes.loadSaved(
                                    accountState.data.user?.favoriteRecipeIds.orEmpty()
                                )
                            }, account::clearError)
                        }
                    }
                }
            }
        }
    }
    if (showAdd) {
        AddIngredientDialog(
            pantryState,
            {
                pantry.searchIngredients(it)
            },
            pantry::scanBarcode,
            pantry::selectIngredient,
            onAdd = { quantity, unit, days ->
                val adding = pantry.submitStock(quantity, unit, days)
                if (adding != null) {
                    scope.launch {
                        adding.join()
                        if (pantry.uiState.value.errorMessage == null) {
                            showAdd = false
                            recommendations()
                        }
                    }
                }
            },
            onDismissError = pantry::clearError,
            onDismiss = { showAdd = false }
        )
    }
}

/** Child of the app shell: shows back navigation only when the shell says a detail view is open. */
@Composable
private fun KitchenTopBar(showBack: Boolean, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showBack) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
            }
        }
        Text(
            stringResource(R.string.demo_badge),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(vertical = 10.dp)
        )
    }
}

@Composable
private fun KitchenBottomBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelect(destination) },
                icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                label = { Text(stringResource(destination.label)) }
            )
        }
    }
}
