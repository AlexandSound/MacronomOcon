package com.example.macronomocon.data.mock

import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.Pantry
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
        val pantry = container.accounts.findUser()!!.pantry
        val before = Pantry(pantry.items)
        pantry.deductIngredients(recipe)
        assertEquals(150.0, before.items.single().quantity, 0.0)
        assertEquals(50.0, pantry.items.single().quantity, 0.0)
        try {
            pantry.deductIngredients(recipe)
            fail("Insufficient stock should fail")
        } catch (_: IllegalArgumentException) {
            assertEquals(50.0, pantry.items.single().quantity, 0.0)
        }
    }

    @Test
    fun commentsPersistAcrossReadsAndCanBeDeletedByAuthor() = runTest {
        val container = MockAppContainer(seed)
        val comment = container.accounts.publishComment(recipe.recipeId, " Tasty ", null)
        assertEquals("Tasty", comment.text)
        assertEquals(seed.user.displayName, comment.authorName)
        assertEquals(listOf(comment), container.accounts.loadComments(recipe.recipeId))
        try {
            container.accounts.likeComment(comment)
            fail("Authors cannot like their own comment")
        } catch (_: IllegalArgumentException) {
            assertEquals(0, container.accounts.loadComments(recipe.recipeId).single().likeCount())
        }
        container.accounts.deleteComment(comment)
        assertTrue(container.accounts.loadComments(recipe.recipeId).isEmpty())
    }

    @Test
    fun likesOnAnotherAuthorsCommentRemainUnique() = runTest {
        val comment = Comment("other-comment", recipe.recipeId, "other-author", "Tasty")
        val container = MockAppContainer(seed.copy(comments = listOf(comment)))
        container.accounts.likeComment(comment)
        container.accounts.likeComment(comment)
        assertEquals(1, container.accounts.loadComments(recipe.recipeId).single().likeCount())
    }

    @Test
    fun deletingAccountRemovesItsLikesOnOtherAuthorsComments() = runTest {
        val comment = Comment("other-comment", recipe.recipeId, "other-author", "Tasty")
        val container = MockAppContainer(seed.copy(comments = listOf(comment)))
        container.accounts.likeComment(comment)
        container.accounts.deleteUser()
        container.accounts.register("cook@example.com", "password")
        assertEquals(0, container.accounts.loadComments(recipe.recipeId).single().likeCount())
    }

    @Test
    fun anotherAuthorsCommentCannotBeDeleted() = runTest {
        val comment = Comment("other-comment", recipe.recipeId, "other-author", "Tasty")
        val container = MockAppContainer(seed.copy(comments = listOf(comment)))
        try {
            container.accounts.deleteComment(comment)
            fail("Only the author can delete a comment")
        } catch (_: IllegalArgumentException) {
            assertEquals(listOf(comment), container.accounts.loadComments(recipe.recipeId))
        }
    }

    @Test
    fun preferencesSurviveReadsWithinTheSession() = runTest {
        val container = MockAppContainer(seed)
        val preferences = seed.user.preferences.copy(notificationsEnabled = false)
        container.accounts.savePreferences(preferences)
        assertEquals(preferences, container.accounts.findUser()!!.preferences)
    }
}
