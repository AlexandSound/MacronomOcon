package com.example.macronomocon.data.repository

import com.example.macronomocon.data.api.RecipeApiClient
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.UserPreferences

class RecipeRepository(
    private val api: RecipeApiClient,
    private val userData: RecipeUserDataSource
) {
    suspend fun search(
        name: String,
        preferences: UserPreferences = UserPreferences()
    ): List<Recipe> =
        if (name.isBlank()) emptyList() else visible(api.searchRecipes(name.trim(), preferences))

    suspend fun findByIngredients(
        items: List<Ingredient>,
        preferences: UserPreferences = UserPreferences()
    ): List<Recipe> = if (items.isEmpty()) {
        emptyList()
    } else {
        visible(
            api.findByIngredients(
                items.distinctBy {
                    it.ingredientId
                },
                preferences
            )
        )
    }

    suspend fun findById(recipeId: String): Recipe = api.getRecipe(recipeId)

    suspend fun findByIds(recipeIds: Set<String>): List<Recipe> =
        if (recipeIds.isEmpty()) emptyList() else api.getRecipes(recipeIds.toList())

    suspend fun setFavorite(r: Recipe, value: Boolean) = userData.setFavorite(r, value)

    suspend fun blockRecipe(r: Recipe) = userData.blockRecipe(r.recipeId)

    suspend fun saveCustom(r: Recipe): Recipe {
        require(r.hasRequiredInfo()) { "Recipe information is incomplete" }
        return userData.saveCustom(r)
    }

    suspend fun createShareLink(r: Recipe): String = userData.createShareLink(r.recipeId)

    private suspend fun visible(recipes: List<Recipe>): List<Recipe> {
        val blocked = userData.blockedRecipeIds()
        return recipes.distinctBy { it.recipeId }.filterNot { it.recipeId in blocked }
    }
}
