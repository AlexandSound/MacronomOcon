package com.example.macronomocon.data.mock

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.RecipeIngredient
import com.example.macronomocon.domain.model.User
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MockAppContainerTest {
    private val rice = Ingredient("rice", "Rice")
    private val grams = "g"
    private val recipe = Recipe(
        "bowl", "Bowl", listOf("Cook"), 5, 10, Difficulty.EASY,
        MealType.LUNCH, "", emptyList(), listOf(RecipeIngredient(rice, 100.0, grams))
    )
    private val seed = MockSeed(
        User("cook", "cook@example.com", "Cook"),
        listOf(rice),
        listOf(PantryItem(rice, 150.0, grams, LocalDate.now().plusDays(3))),
        listOf(recipe)
    )

    @Test
    fun favoriteWritesAreSharedWithAccountRepositoryButNotOtherContainers() = runTest {
        val container = MockAppContainer(seed)
        container.recipes.setFavorite(recipe, true)
        assertTrue(recipe.recipeId in container.accounts.findUser()!!.favoriteRecipeIds)
        assertTrue(MockAppContainer(seed).accounts.findUser()!!.favoriteRecipeIds.isEmpty())
        container.recipes.setFavorite(recipe, false)
        assertTrue(container.accounts.findUser()!!.favoriteRecipeIds.isEmpty())
    }

    @Test
    fun mealDeductionIsAtomicAndReturnedPantriesAreSnapshots() = runTest {
        val container = MockAppContainer(seed)
        val before = container.pantry.findPantry()
        container.pantry.recordMeal(recipe)
        assertEquals(150.0, before.items.single().quantity, 0.0)
        assertEquals(50.0, container.pantry.findPantry().items.single().quantity, 0.0)
        try {
            container.pantry.recordMeal(recipe)
            fail("Insufficient stock should fail")
        } catch (_: IllegalArgumentException) {
            assertEquals(50.0, container.pantry.findPantry().items.single().quantity, 0.0)
        }
    }

    @Test
    fun commentsPersistAcrossReadsAndLikesRemainUnique() = runTest {
        val container = MockAppContainer(seed)
        val comment = container.comments.publish(recipe.recipeId, "Tasty", null)
        container.comments.like(comment.commentId)
        container.comments.like(comment.commentId)
        assertEquals(1, container.comments.findByRecipe(recipe.recipeId).single().likeCount())
        container.comments.delete(comment.commentId)
        assertTrue(container.comments.findByRecipe(recipe.recipeId).isEmpty())
    }

    @Test
    fun preferencesSurviveReadsWithinTheSession() = runTest {
        val container = MockAppContainer(seed)
        val preferences = seed.user.preferences.copy(notificationsEnabled = false)
        container.accounts.savePreferences(preferences)
        assertEquals(preferences, container.accounts.findUser()!!.preferences)
    }
}
