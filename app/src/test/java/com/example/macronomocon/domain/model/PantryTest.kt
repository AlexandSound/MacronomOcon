package com.example.macronomocon.domain.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PantryTest {
    private val today = LocalDate.of(2026, 10, 28)
    private val rice = Ingredient("rice", "Rice")

    private fun recipe(vararg quantities: Double): Recipe = Recipe(
        "recipe", "Rice bowl", listOf("Cook rice"), 5, 10, Difficulty.EASY,
        MealType.DINNER, "", emptyList(),
        quantities.map {
            RecipeIngredient(rice, it, "g")
        }
    )

    private fun item(id: String, amount: Double, days: Long, unit: String = "g") = PantryItem(
        rice,
        amount,
        unit,
        today.plusDays(days),
        id
    )

    @Test
    fun expirationBoundariesIncludeTodayButExcludeAlreadyExpired() {
        val pantry =
            Pantry(
                listOf(
                    item("old", 1.0, -1),
                    item("today", 1.0, 0),
                    item("soon", 1.0, 2),
                    item("later", 1.0, 3)
                )
            )
        assertEquals(listOf("old"), pantry.getExpired(today).map { it.pantryItemId })
        assertEquals(
            listOf("today", "soon"),
            pantry.getExpiringWithin(2, today).map {
                it.pantryItemId
            }
        )
    }

    @Test
    fun deductsSoonestExpiringLotsFirst() {
        val pantry = Pantry(listOf(item("later", 100.0, 5), item("soon", 50.0, 1)))
        pantry.deductIngredients(recipe(80.0), today)
        assertEquals(listOf(item("later", 70.0, 5)), pantry.items)
    }

    @Test
    fun insufficientCombinedRequirementsNeverPartiallyDeduct() {
        val initial = listOf(item("rice", 100.0, 2))
        val pantry = Pantry(initial)
        assertFalse(pantry.hasIngredients(recipe(60.0, 60.0), today))
        assertThrows(IllegalArgumentException::class.java) {
            pantry.deductIngredients(recipe(60.0, 60.0), today)
        }
        assertEquals(initial, pantry.items)
    }

    @Test
    fun excludesExpiredStockAndDoesNotGuessUnitConversions() {
        val pantry = Pantry(listOf(item("expired", 500.0, -1), item("cups", 3.0, 5, "cup")))
        assertFalse(pantry.hasIngredients(recipe(100.0), today))
    }

    @Test
    fun quantitiesMustBeFiniteAndCannotBeOverdrawn() {
        assertThrows(IllegalArgumentException::class.java) { item("bad", Double.NaN, 1) }
        assertThrows(IllegalArgumentException::class.java) { item("bad", -1.0, 1) }
        assertThrows(IllegalArgumentException::class.java) {
            item("ok", 2.0, 1).reduceQuantity(3.0)
        }
    }

    @Test
    fun duplicateIdsDoNotOverwriteExistingStock() {
        val pantry = Pantry(listOf(item("same", 2.0, 1)))
        assertThrows(IllegalArgumentException::class.java) { pantry.addItem(item("same", 50.0, 1)) }
        assertEquals(2.0, pantry.items.single().quantity, 0.0)
    }

    @Test
    fun matchingCountsDistinctIngredientsInsteadOfLots() {
        assertEquals(
            1,
            recipe(50.0, 25.0).matchCount(listOf(item("one", 30.0, 1), item("two", 40.0, 2)))
        )
    }
}
