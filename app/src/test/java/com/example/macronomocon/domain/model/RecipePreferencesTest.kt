package com.example.macronomocon.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipePreferencesTest {
    @Test
    fun mapsDietsAndIntolerancesSeparatelyAndCanonicalizesAliases() {
        val filters = RecipePreferences.from(
            UserPreferences(
                dietaryRestrictions = listOf(
                    "Vegan",
                    "gluten-free",
                    "Dairy-free",
                    "peanut",
                    "vegan"
                ),
                preferredCuisines = listOf("Italian", " mexican ", "ITALIAN")
            )
        )
        assertEquals("gluten free,vegan", filters.diet)
        assertEquals("dairy,peanut", filters.intolerances)
        assertEquals("italian,mexican", filters.cuisine)
    }

    @Test
    fun cookingMethodsAndNotificationsDoNotChangeSearchFilters() {
        assertEquals(
            RecipePreferences(),
            RecipePreferences.from(
                UserPreferences(
                    cookingMethods = listOf("grilling"),
                    notificationsEnabled = false
                )
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedRestrictionCannotSilentlyProduceUnrestrictedResults() {
        RecipePreferences.from(UserPreferences(dietaryRestrictions = listOf("unrecognized")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedCuisineIsRejected() {
        RecipePreferences.from(UserPreferences(preferredCuisines = listOf("unrecognized")))
    }
}
