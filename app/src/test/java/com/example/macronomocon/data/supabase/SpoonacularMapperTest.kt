package com.example.macronomocon.data.supabase

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.PantryItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpoonacularMapperTest {
    private fun json(text: String) = Json.parseToJsonElement(text).jsonObject

    private val recipe = """
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

    @Test
    fun mapsRecipeInformation() {
        val mapped = SpoonacularMapper.recipe(json(recipe))
        assertEquals("716429", mapped.recipeId)
        assertEquals("Pasta with Garlic", mapped.name)
        assertEquals(listOf("Boil the pasta.", "Add garlic."), mapped.instructions)
        assertEquals(0, mapped.prepTime)
        assertEquals(45, mapped.cookTime)
        assertEquals(Difficulty.MEDIUM, mapped.difficulty)
        assertEquals(MealType.LUNCH, mapped.mealType)
        assertEquals("Italian", mapped.cuisine)
        assertEquals(listOf("pot", "pan"), mapped.toolsNeeded)
    }

    @Test
    fun usesMetricMeasuresAndSkipsUnusableIngredients() {
        val requirements = SpoonacularMapper.recipe(json(recipe)).requirements
        assertEquals(listOf("11215", "20420"), requirements.map { it.ingredient.ingredientId })
        assertEquals(PantryItem.UNIT_PIECE, requirements[0].unit)
        assertEquals("g", requirements[1].unit)
        assertEquals(226.796, requirements[1].quantity, 0.0)
        assertEquals("Produce", requirements[0].ingredient.category)
    }

    @Test
    fun skipsMalformedSearchResults() {
        val results = SpoonacularMapper.recipes(
            json("""{"results": [{"id": 1, "title": "Toast"}, {"title": "No id"}]}""")
        )
        assertEquals(listOf("1"), results.map { it.recipeId })
        assertTrue(results.single().requirements.isEmpty())
    }

    @Test
    fun mapsIngredientSearchWithBarcode() {
        val results = SpoonacularMapper.ingredients(
            json("""{"results": [{"id": 9003, "name": "apple", "aisle": "Produce"}, {"id": 2}]}"""),
            "0123456789012"
        )
        assertEquals(1, results.size)
        assertEquals("9003", results[0].ingredientId)
        assertEquals("0123456789012", results[0].barcode)
    }

    @Test
    fun productSearchTermPrefersGenericBreadcrumb() {
        assertEquals(
            "brown rice",
            SpoonacularMapper.productSearchTerm(
                json("""{"title": "Brown Rice 2lb", "breadcrumbs": ["brown rice", "rice"]}""")
            )
        )
        assertEquals(
            "Oat Milk",
            SpoonacularMapper.productSearchTerm(
                json("""{"title": "Oat Milk", "breadcrumbs": []}""")
            )
        )
    }
}
