package com.example.macronomocon.data.supabase

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.RecipeIngredient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class CustomRecipeMapperTest {
    private val id = "3f2b8c1e-6a4d-4e0f-9b7a-2c5d8e1f0a3b"
    private val recipe = Recipe(
        recipeId = id,
        name = "Lemon rice",
        instructions = listOf("Rinse the rice", "Simmer with lemon"),
        prepTime = 5,
        cookTime = 20,
        difficulty = Difficulty.MEDIUM,
        mealType = MealType.LUNCH,
        cuisine = "Indian",
        toolsNeeded = listOf("Pot"),
        requirements = listOf(
            RecipeIngredient(Ingredient("20444", "rice", category = "Grains"), 200.0, "g"),
            RecipeIngredient(Ingredient("9150", "lemon"), 1.0, "piece")
        )
    )

    /** What Postgres returns for an inserted row: the payload plus generated columns. */
    private fun stored(row: JsonObject) = JsonObject(
        row + mapOf(
            "recipe_id" to JsonPrimitive(id),
            "user_id" to JsonPrimitive("user-1"),
            "created_at" to JsonPrimitive("2026-11-12T10:00:00Z")
        )
    )

    @Test
    fun rowRoundTripsToTheSameRecipe() {
        assertEquals(recipe, CustomRecipeMapper.recipe(stored(CustomRecipeMapper.row(recipe))))
    }

    @Test
    fun rowLeavesIdsToPostgres() {
        val row = CustomRecipeMapper.row(recipe)
        assertFalse("recipe_id" in row)
        assertFalse("user_id" in row)
        assertEquals(JsonPrimitive("MEDIUM"), row["difficulty"])
        assertEquals(JsonPrimitive("LUNCH"), row["meal_type"])
    }

    @Test
    fun rowsFromBeforeTheDetailsMigrationUseDefaults() {
        val legacy = Json.parseToJsonElement(
            """
            {"recipe_id":"$id","title":"Toast","instructions":["Toast the bread"],
             "ingredients":[{"ingredient_id":"18064","name":"bread","quantity":2,"unit":"slice"}]}
            """
        ).jsonObject
        val mapped = CustomRecipeMapper.recipe(legacy)
        assertEquals(0, mapped.totalTime())
        assertEquals(Difficulty.EASY, mapped.difficulty)
        assertEquals(MealType.DINNER, mapped.mealType)
        assertEquals("", mapped.cuisine)
        assertEquals(1, mapped.requirements.size)
    }

    @Test
    fun malformedIngredientsAndUnknownEnumsAreSkipped() {
        val row = Json.parseToJsonElement(
            """
            {"recipe_id":"$id","title":"Soup","instructions":["Stir"," "],
             "difficulty":"EXTREME","meal_type":null,
             "ingredients":[{"name":"no id","quantity":1,"unit":"g"},
                            {"ingredient_id":"1","name":"salt","quantity":-1,"unit":"g"},
                            {"ingredient_id":"2","name":"water","quantity":500,"unit":"ml"}]}
            """
        ).jsonObject
        val mapped = CustomRecipeMapper.recipe(row)
        assertEquals(listOf("Stir"), mapped.instructions)
        assertEquals(Difficulty.EASY, mapped.difficulty)
        assertEquals(MealType.DINNER, mapped.mealType)
        assertEquals(listOf("water"), mapped.requirements.map { it.ingredient.name })
    }
}
