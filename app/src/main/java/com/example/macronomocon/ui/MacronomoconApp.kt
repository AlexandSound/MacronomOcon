package com.example.macronomocon.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.IntOffset
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
    val favoriteIds = accountState.data.user?.favoriteRecipeIds.orEmpty()
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
                AnimatedContent(
                    targetState = Screen(destination, detailKey),
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = { screenTransition() },
                    label = "screen"
                ) { screen ->
                    if (screen.recipeId != null) {
                        Column(Modifier.fillMaxSize()) {
                            StateFeedback(recipeState, recipes::clearError)
                            StateFeedback(pantryState, pantry::clearError)
                            val recipe = recipeState.data.selectedRecipe?.takeIf {
                                it.recipeId ==
                                    screen.recipeId
                            }
                            if (recipe != null) {
                                RecipeDetailScreen(
                                    recipe,
                                    pantryState.data.items,
                                    accountState.data.user,
                                    commentState,
                                    recipeState.isLoading || pantryState.isLoading,
                                    onFavorite = {
                                        scope.launch {
                                            recipes.setFavorite(
                                                recipe,
                                                recipe.recipeId !in favoriteIds
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
                        }
                    } else {
                        savedTabs.SaveableStateProvider(screen.destination) {
                            when (screen.destination) {
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

/** What the shell's content area shows; [AnimatedContent] animates between these states. */
private data class Screen(val destination: Destination, val recipeId: String? = null) {
    companion object {
        const val SLIDE_MILLIS = 320
        const val FADE_MILLIS = 220
        const val TOP_BAR_HEIGHT_DP = 56

        /** Tabs slide a quarter of the width; recipe details slide in from the full width. */
        const val TAB_SLIDE_DIVISOR = 4
    }
}

/** Details push in from the end and pop back out; tabs slide toward their position in the bar. */
private fun AnimatedContentTransitionScope<Screen>.screenTransition(): ContentTransform {
    val slide = tween<IntOffset>(Screen.SLIDE_MILLIS, easing = FastOutSlowInEasing)
    val fade = tween<Float>(Screen.FADE_MILLIS)
    val opening = initialState.recipeId == null && targetState.recipeId != null
    val closing = initialState.recipeId != null && targetState.recipeId == null
    val forward = when {
        opening -> true
        closing -> false
        else -> targetState.destination.ordinal >= initialState.destination.ordinal
    }
    val sign = if (forward) 1 else -1
    val divisor = if (opening || closing) 1 else Screen.TAB_SLIDE_DIVISOR
    // Every view fills the same area, so no size animation (which would shift the layout) is used.
    return (slideInHorizontally(slide) { sign * it / divisor } + fadeIn(fade)) togetherWith
        (slideOutHorizontally(slide) { -sign * it / divisor } + fadeOut(fade)) using null
}

/**
 * Child of the app shell. Its height is reserved on every view, so showing or hiding the back
 * button never moves the content below it.
 */
@Composable
private fun KitchenTopBar(showBack: Boolean, onBack: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(Screen.TOP_BAR_HEIGHT_DP.dp).padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        AnimatedVisibility(showBack, enter = fadeIn(), exit = fadeOut()) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
            }
        }
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
