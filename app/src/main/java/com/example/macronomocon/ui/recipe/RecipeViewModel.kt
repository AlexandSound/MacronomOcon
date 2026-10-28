package com.example.macronomocon.ui.recipe

import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.ui.common.StateViewModel
import com.example.macronomocon.ui.common.UiMessage

data class RecipeState(
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipe: Recipe? = null,
    val shareLink: String? = null
)

class RecipeViewModel(private val repository: RecipeRepository) :
    StateViewModel<RecipeState>(RecipeState()) {
    private var allRecipes: List<Recipe> = emptyList()

    fun search(name: String) = action(UiMessage.ERROR_SEARCH_RECIPES) {
        allRecipes = repository.search(name)
        updateData { it.copy(recipes = allRecipes) }
    }

    fun findRecipes(ingredients: List<Ingredient>) = action(UiMessage.ERROR_RETRIEVE_RECIPES) {
        allRecipes = repository.findByIngredients(ingredients)
        updateData { it.copy(recipes = allRecipes) }
    }

    fun loadSaved(ids: Set<String>) = action(UiMessage.ERROR_RETRIEVE_RECIPES) {
        allRecipes = ids.map { repository.findById(it) }
        updateData { it.copy(recipes = allRecipes) }
    }

    fun applyFilters(
        mealType: MealType? = null,
        difficulty: Difficulty? = null,
        maxTime: Int? = null
    ) {
        require(maxTime == null || maxTime >= 0)
        updateData {
            it.copy(
                recipes = allRecipes.filter { r ->
                    (mealType == null || r.mealType == mealType) &&
                        (difficulty == null || r.difficulty == difficulty) &&
                        (maxTime == null || r.totalTime() <= maxTime)
                }
            )
        }
    }

    fun loadRecipe(recipeId: String) = action(UiMessage.ERROR_LOAD_THIS_RECIPE) {
        val recipe = repository.findById(recipeId)
        updateData { it.copy(selectedRecipe = recipe, shareLink = null) }
    }

    fun setFavorite(recipe: Recipe, value: Boolean) = action(UiMessage.ERROR_SAVE_THIS_FAVORITE) {
        repository.setFavorite(recipe, value)
    }

    fun blockRecipe(recipe: Recipe) = action(UiMessage.ERROR_BLOCK_THIS_RECIPE) {
        repository.blockRecipe(recipe)
        allRecipes = allRecipes.filterNot { it.recipeId == recipe.recipeId }
        updateData {
            it.copy(
                recipes = it.recipes.filterNot { r -> r.recipeId == recipe.recipeId },
                selectedRecipe = it.selectedRecipe?.takeUnless { r ->
                    r.recipeId == recipe.recipeId
                }
            )
        }
    }

    fun saveCustomRecipe(recipe: Recipe) = action(UiMessage.ERROR_SAVE_THIS_RECIPE) {
        val saved = repository.saveCustom(recipe)
        updateData { it.copy(selectedRecipe = saved) }
    }

    fun shareRecipe(recipe: Recipe) = action(UiMessage.ERROR_SHARE_THIS_RECIPE) {
        val link = repository.createShareLink(recipe)
        updateData { it.copy(shareLink = link) }
    }
}
