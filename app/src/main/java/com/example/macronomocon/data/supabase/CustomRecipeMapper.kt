package com.example.macronomocon.data.supabase

import com.example.macronomocon.domain.model.Difficulty
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.MealType
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.RecipeIngredient
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Maps `custom_recipes` rows to domain models. `ingredients` is a jsonb array of
 * `{ingredient_id, name, category, quantity, unit}`; `instructions` and `tools_needed` are text[].
 */
object CustomRecipeMapper {
    /** Insert/update payload. `recipe_id` and `user_id` are left to Postgres and RLS. */
    fun row(recipe: Recipe): JsonObject = buildJsonObject {
        put("title", recipe.name.trim())
        put(
            "ingredients",
            buildJsonArray {
                recipe.requirements.forEach {
                    add(
                        buildJsonObject {
                            put("ingredient_id", it.ingredient.ingredientId)
                            put("name", it.ingredient.name)
                            put("category", it.ingredient.category)
                            put("quantity", it.quantity)
                            put("unit", it.unit)
                        }
                    )
                }
            }
        )
        put("instructions", strings(recipe.instructions.map { it.trim() }))
        put("prep_time", recipe.prepTime)
        put("cook_time", recipe.cookTime)
        put("difficulty", recipe.difficulty.name)
        put("meal_type", recipe.mealType.name)
        put("cuisine", recipe.cuisine.trim())
        put(
            "tools_needed",
            strings(
                recipe.toolsNeeded.map {
                    it.trim()
                }.filter { it.isNotEmpty() }
            )
        )
    }

    fun recipe(json: JsonObject): Recipe = Recipe(
        recipeId = requireNotNull(json.string("recipe_id")),
        name = requireNotNull(json.string("title")),
        instructions = json.array("instructions").mapNotNull { it.content() },
        prepTime = json.int("prep_time")?.coerceAtLeast(0) ?: 0,
        cookTime = json.int("cook_time")?.coerceAtLeast(0) ?: 0,
        difficulty = enumOrNull<Difficulty>(json.string("difficulty")) ?: Difficulty.EASY,
        mealType = enumOrNull<MealType>(json.string("meal_type")) ?: MealType.DINNER,
        cuisine = json.string("cuisine").orEmpty(),
        toolsNeeded = json.array("tools_needed").mapNotNull { it.content() },
        // Rows are user-edited, so one malformed ingredient is skipped instead of failing the recipe.
        requirements = json.array("ingredients").mapNotNull {
            runCatching { requirement(it.jsonObject) }.getOrNull()
        }
    )

    private fun requirement(json: JsonObject) = RecipeIngredient(
        Ingredient(
            requireNotNull(json.string("ingredient_id")),
            requireNotNull(json.string("name")),
            category = json.string("category").orEmpty()
        ),
        requireNotNull(json.double("quantity")),
        requireNotNull(json.string("unit"))
    )

    private fun strings(values: List<String>) = JsonArray(values.map(::JsonPrimitive))

    private inline fun <reified E : Enum<E>> enumOrNull(name: String?): E? =
        enumValues<E>().firstOrNull { it.name == name }

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
