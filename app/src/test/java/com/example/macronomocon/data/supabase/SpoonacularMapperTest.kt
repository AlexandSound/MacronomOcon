package com.example.macronomocon.data.supabase

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.PantryItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class SpoonacularMapperTest {
    private fun json(text: String) = Json.parseToJsonElement(text).jsonObject

    private val recipeJson = """
        {
          "id": 716429, "title": "Pasta with Garlic", "readyInMinutes": 45,
          "preparationMinutes": -1, "cookingMinutes": null,
          "cuisines": ["Italian"], "dishTypes": ["lunch", "main course"],
          "analyzedInstructions": [{"steps": [
            {"step": "Boil the pasta.", "equipment": [{"name": "pot"}]},
            {"step": "Add garlic.", "equipment": [{"name": "pot"}, {"name": "pan"}]}
          ]}],
          "extendedIngredients": [
            {"id": 11215, "name": "garlic", "aisle": "Produce", "amount": 2, "unit": "cloves",
             "measures": {"metric": {"amount": 2.0, "unitShort": ""}}},
            {"id": 20420, "name": "pasta", "amount": 0.5, "unit": "lb",
             "measures": {"metric": {"amount": 226.796, "unitShort": "g"}}},
            {"id": -1, "name": "water", "amount": 1, "unit": "cup"},
            {"id": 1002030, "name": "pepper", "amount": 0, "unit": "pinch"}
          ]
        }
    """.trimIndent()

    private val recipe = SpoonacularMapper.recipe(json(recipeJson))

    @Test
    fun mapsTheRecipeId() {
        assertEquals("716429", recipe.recipeId)
    }

    @Test
    fun mapsTheRecipeTitle() {
        assertEquals("Pasta with Garlic", recipe.name)
    }

    @Test
    fun flattensAnalyzedInstructionStepsIntoInstructions() {
        assertEquals(listOf("Boil the pasta.", "Add garlic."), recipe.instructions)
    }

    @Test
    fun treatsNegativePreparationMinutesAsNoPrepTime() {
        assertEquals(0, recipe.prepTime)
    }

    @Test
    fun derivesCookTimeFromReadyMinutesWhenCookingMinutesIsMissing() {
        assertEquals(45, recipe.cookTime)
    }

    @Test
    fun derivesDifficultyFromTheTotalTime() {
        assertEquals(Difficulty.MEDIUM, recipe.difficulty)
    }

    @Test
    fun derivesMealTypeFromDishTypes() {
        assertEquals(MealType.LUNCH, recipe.mealType)
    }

    @Test
    fun usesTheFirstCuisineAsTheRecipeCuisine() {
        assertEquals("Italian", recipe.cuisine)
    }

    @Test
    fun collectsDistinctStepEquipmentAsToolsNeeded() {
        assertEquals(listOf("pot", "pan"), recipe.toolsNeeded)
    }

    @Test
    fun skipsIngredientsWithoutAPositiveId() {
        assertFalse("-1" in recipe.requirements.map { it.ingredient.ingredientId })
    }

    @Test
    fun skipsIngredientsWithoutAPositiveAmount() {
        assertFalse("1002030" in recipe.requirements.map { it.ingredient.ingredientId })
    }

    @Test
    fun keepsUsableIngredientsInOrder() {
        assertEquals(
            listOf("11215", "20420"),
            recipe.requirements.map { it.ingredient.ingredientId }
        )
    }

    @Test
    fun usesTheMetricAmountRatherThanTheOriginalAmount() {
        assertEquals(226.796, recipe.requirements[1].quantity, 0.0)
    }

    @Test
    fun usesTheMetricUnitRatherThanTheOriginalUnit() {
        assertEquals("g", recipe.requirements[1].unit)
    }

    @Test
    fun treatsABlankMetricUnitAsACount() {
        assertEquals(PantryItem.UNIT_PIECE, recipe.requirements[0].unit)
    }

    @Test
    fun mapsTheIngredientAisleToItsCategory() {
        assertEquals("Produce", recipe.requirements[0].ingredient.category)
    }

    @Test
    fun skipsSearchResultsThatCannotBeMapped() {
        val results = SpoonacularMapper.recipes(
            json("""{"results": [{"id": 1, "title": "Toast"}, {"title": "No id"}]}""")
        )
        assertEquals(listOf("1"), results.map { it.recipeId })
    }

    @Test
    fun searchResultsCarryNoIngredientRequirements() {
        val results = SpoonacularMapper.recipes(
            json("""{"results": [{"id": 1, "title": "Toast"}]}""")
        )
        assertTrue(results.single().requirements.isEmpty())
    }

    @Test
    fun skipsIngredientResultsWithoutAName() {
        val results = SpoonacularMapper.ingredients(
            json("""{"results": [{"id": 9003, "name": "apple", "aisle": "Produce"}, {"id": 2}]}"""),
            "0123456789012"
        )
        assertEquals(listOf("9003"), results.map { it.ingredientId })
    }

    @Test
    fun attachesTheScannedBarcodeToIngredientResults() {
        val results = SpoonacularMapper.ingredients(
            json("""{"results": [{"id": 9003, "name": "apple", "aisle": "Produce"}]}"""),
            "0123456789012"
        )
        assertEquals("0123456789012", results.single().barcode)
    }

    @Test
    fun productSearchTermPrefersTheMostGenericBreadcrumb() {
        assertEquals(
            "brown rice",
            SpoonacularMapper.productSearchTerm(
                json("""{"title": "Brown Rice 2lb", "breadcrumbs": ["brown rice", "rice"]}""")
            )
        )
    }

    @Test
    fun productSearchTermFallsBackToTheProductTitle() {
        assertEquals(
            "Oat Milk",
            SpoonacularMapper.productSearchTerm(
                json("""{"title": "Oat Milk", "breadcrumbs": []}""")
            )
        )
    }
}
