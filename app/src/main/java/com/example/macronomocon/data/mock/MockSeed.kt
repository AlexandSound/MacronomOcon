package com.example.macronomocon.data.mock

import android.content.res.Resources
import com.example.macronomocon.R
import com.example.macronomocon.domain.model.Comment
import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.PantryItem.Companion.UNIT_GRAMS
import com.example.macronomocon.domain.model.PantryItem.Companion.UNIT_PIECE
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.RecipeIngredient
import com.example.macronomocon.domain.model.User
import java.time.LocalDate

/** Fixtures are injectable for tests; display content is loaded from XML by the Android composition root. */
data class MockSeed(
    val user: User,
    val ingredients: List<Ingredient>,
    val pantryItems: List<PantryItem>,
    val recipes: List<Recipe>,
    val comments: List<Comment> = emptyList()
) {
    companion object {
        const val DEMO_BARCODE = "0123456789012"
        const val DEMO_USER_ID = "demo-user"
        const val DEMO_EMAIL = "alex@example.com"
        const val DEMO_NEIGHBOR_ID = "demo-neighbor"
    }
}

fun mockSeed(resources: Resources, today: LocalDate = LocalDate.now()): MockSeed {
    fun text(id: Int) = resources.getString(id)
    val rice =
        Ingredient("rice", text(R.string.demo_rice), MockSeed.DEMO_BARCODE)
    val chickpeas = Ingredient("chickpeas", text(R.string.demo_chickpeas))
    val tomato = Ingredient("tomato", text(R.string.demo_tomato))
    val spinach = Ingredient("spinach", text(R.string.demo_spinach))
    val lemon = Ingredient("lemon", text(R.string.demo_lemon))
    val oats = Ingredient("oats", text(R.string.demo_oats))
    fun stock(ingredient: Ingredient, amount: Double, days: Long, unit: String = UNIT_GRAMS) =
        PantryItem(
            ingredient,
            amount,
            unit,
            today.plusDays(days),
            "stock-${ingredient.ingredientId}"
        )
    fun requirement(ingredient: Ingredient, amount: Double) =
        RecipeIngredient(ingredient, amount, UNIT_GRAMS)
    val bowl = Recipe(
        "harvest-bowl", text(R.string.demo_bowl),
        listOf(
            text(R.string.demo_step_rice),
            text(R.string.demo_step_roast),
            text(R.string.demo_step_bowl)
        ),
        10, 15, Difficulty.EASY, MealType.LUNCH, text(R.string.demo_cuisine), emptyList(),
        listOf(requirement(rice, 150.0), requirement(chickpeas, 200.0), requirement(tomato, 100.0))
    )
    val salad = Recipe(
        "sunshine-salad", text(R.string.demo_salad),
        listOf(text(R.string.demo_step_salad), text(R.string.demo_step_toss)),
        10, 0, Difficulty.EASY, MealType.LUNCH, text(R.string.demo_cuisine), emptyList(),
        listOf(
            requirement(chickpeas, 150.0),
            requirement(tomato, 100.0),
            requirement(spinach, 50.0),
            RecipeIngredient(lemon, 1.0, UNIT_PIECE)
        )
    )
    val skillet = Recipe(
        "garden-rice", text(R.string.demo_rice_recipe),
        listOf(text(R.string.demo_step_rice), text(R.string.demo_step_skillet)),
        5, 20, Difficulty.EASY, MealType.DINNER, text(R.string.demo_cuisine), emptyList(),
        listOf(requirement(rice, 150.0), requirement(tomato, 100.0), requirement(spinach, 50.0))
    )
    return MockSeed(
        User(
            MockSeed.DEMO_USER_ID,
            MockSeed.DEMO_EMAIL,
            text(R.string.demo_name),
            favoriteRecipeIds = setOf(salad.recipeId)
        ),
        listOf(rice, chickpeas, tomato, spinach, lemon, oats),
        listOf(
            stock(rice, 500.0, 60),
            stock(chickpeas, 400.0, 30),
            stock(tomato, 250.0, 3),
            stock(spinach, 150.0, 1),
            stock(lemon, 2.0, 7, UNIT_PIECE)
        ),
        listOf(bowl, salad, skillet),
        listOf(
            Comment(
                "demo-comment",
                bowl.recipeId,
                MockSeed.DEMO_NEIGHBOR_ID,
                text(R.string.demo_comment)
            )
        )
    )
}
