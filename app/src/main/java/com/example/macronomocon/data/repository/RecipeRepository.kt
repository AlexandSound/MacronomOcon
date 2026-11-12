package com.example.macronomocon.data.repository

import com.example.macronomocon.data.api.RecipeApiClient
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Recipe

class RecipeRepository(
    private val api: RecipeApiClient,
    private val userData: RecipeUserDataSource
) {
    suspend fun search(name: String): List<Recipe> =
        if (name.isBlank()) emptyList() else visible(api.searchRecipes(name.trim()))

    suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> = if (items.isEmpty()) {
        emptyList()
    } else {
        visible(
            api.findByIngredients(
                items.distinctBy {
                    it.ingredientId
                }
            )
        )
    }

    suspend fun findById(recipeId: String): Recipe = if (Recipe.isCustomId(recipeId)) {
        userData.findCustom(recipeId)
    } else {
        api.getRecipe(recipeId)
    }

    /** Custom recipes come from the user's own table; the rest from the recipe API. */
    suspend fun findByIds(recipeIds: Set<String>): List<Recipe> {
        val (custom, provider) = recipeIds.partition { Recipe.isCustomId(it) }
        val own = if (custom.isEmpty()) {
            emptyList()
        } else {
            userData.customRecipes().filter { it.recipeId in custom }
        }
        return own + if (provider.isEmpty()) emptyList() else api.getRecipes(provider)
    }

    suspend fun customRecipes(): List<Recipe> = userData.customRecipes()

    suspend fun setFavorite(r: Recipe, value: Boolean) = userData.setFavorite(r.recipeId, value)

    suspend fun blockRecipe(r: Recipe) = userData.blockRecipe(r.recipeId)

    suspend fun saveCustom(r: Recipe): Recipe {
        require(r.hasRequiredInfo(new = true)) { "Recipe information is incomplete" }
        return userData.saveCustom(r)
    }

    suspend fun updateCustom(r: Recipe): Recipe {
        require(r.isCustom()) { "Only custom recipes can be edited" }
        require(r.hasRequiredInfo()) { "Recipe information is incomplete" }
        return userData.updateCustom(r)
    }

    suspend fun createShareLink(r: Recipe): String = userData.createShareLink(r.recipeId)

    private suspend fun visible(recipes: List<Recipe>): List<Recipe> {
        val blocked = userData.blockedRecipeIds()
        return recipes.distinctBy { it.recipeId }.filterNot { it.recipeId in blocked }
    }
}
