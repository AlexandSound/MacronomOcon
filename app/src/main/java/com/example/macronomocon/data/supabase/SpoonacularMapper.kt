package com.example.macronomocon.data.supabase

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.RecipeIngredient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Maps Spoonacular JSON to domain models. Ingredient IDs are Spoonacular's, so pantry items added
 * through ingredient search match recipe requirements. Metric measures keep units comparable.
 */
object SpoonacularMapper {
    const val EASY_MAX_MINUTES = 30
    const val MEDIUM_MAX_MINUTES = 60

    fun recipes(response: JsonObject): List<Recipe> =
        response.array("results").mapNotNull { runCatching { recipe(it.jsonObject) }.getOrNull() }

    fun recipe(json: JsonObject): Recipe {
        val ready = json.int("readyInMinutes")?.coerceAtLeast(0) ?: 0
        val prep = json.int("preparationMinutes")?.takeIf { it >= 0 } ?: 0
        val cook = json.int("cookingMinutes")?.takeIf { it >= 0 }
            ?: (ready - prep).coerceAtLeast(0)
        val steps = json.array("analyzedInstructions").flatMap { it.jsonObject.array("steps") }
            .map { it.jsonObject }
        return Recipe(
            recipeId = requireNotNull(json.int("id")).toString(),
            name = requireNotNull(json.string("title")),
            instructions = steps.mapNotNull { it.string("step") },
            prepTime = prep,
            cookTime = cook,
            difficulty = when {
                prep + cook <= EASY_MAX_MINUTES -> Difficulty.EASY
                prep + cook <= MEDIUM_MAX_MINUTES -> Difficulty.MEDIUM
                else -> Difficulty.HARD
            },
            mealType = mealType(json.array("dishTypes").mapNotNull { it.content() }),
            cuisine = json.array("cuisines").firstNotNullOfOrNull { it.content() }.orEmpty(),
            toolsNeeded = steps.flatMap { it.array("equipment") }
                .mapNotNull { it.jsonObject.string("name") }.distinct(),
            requirements = json.array("extendedIngredients")
                .mapNotNull { requirement(it.jsonObject) }
        )
    }

    fun ingredients(response: JsonObject, barcode: String = ""): List<Ingredient> =
        response.array("results").mapNotNull {
            val item = it.jsonObject
            val id = item.int("id") ?: return@mapNotNull null
            val name = item.string("name") ?: return@mapNotNull null
            Ingredient(id.toString(), name, barcode, item.string("aisle").orEmpty())
        }

    /** A packaged product's most generic name, used to find the matching ingredient. */
    fun productSearchTerm(product: JsonObject): String? =
        product.array("breadcrumbs").firstNotNullOfOrNull { it.content() }
            ?: product.string("title")

    private fun requirement(json: JsonObject): RecipeIngredient? {
        val id = json.int("id")?.takeIf { it > 0 } ?: return null
        val name = json.string("name") ?: return null
        val metric = json["measures"]?.jsonObject?.get("metric")?.jsonObject
        // Amount and unit come from the same system; a blank unit means a count.
        val amount = (if (metric != null) metric.double("amount") else json.double("amount"))
            ?.takeIf { it.isFinite() && it > 0 } ?: return null
        val unit = (if (metric != null) metric.string("unitShort") else json.string("unit"))
            ?: PantryItem.UNIT_PIECE
        return RecipeIngredient(
            Ingredient(id.toString(), name, category = json.string("aisle").orEmpty()),
            amount,
            unit
        )
    }

    private fun mealType(dishTypes: List<String>): MealType = when {
        dishTypes.any { it in setOf("breakfast", "brunch", "morning meal") } -> MealType.BREAKFAST
        dishTypes.any { it in setOf("snack", "appetizer", "fingerfood", "starter") } ->
            MealType.SNACK
        dishTypes.any { it in setOf("lunch", "salad", "soup") } -> MealType.LUNCH
        else -> MealType.DINNER
    }

    private fun JsonObject.array(key: String): JsonArray =
        (get(key) as? JsonArray) ?: JsonArray(emptyList())

    private fun JsonObject.string(key: String): String? = get(key)?.content()

    private fun JsonObject.int(key: String): Int? =
        runCatching { get(key)?.jsonPrimitive?.intOrNull }.getOrNull()

    private fun JsonObject.double(key: String): Double? =
        runCatching { get(key)?.jsonPrimitive?.doubleOrNull }.getOrNull()

    private fun JsonElement.content(): String? =
        runCatching { jsonPrimitive.contentOrNull }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}
