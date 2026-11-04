package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.api.IngredientApiClient
import com.example.macronomocon.data.api.RecipeApiClient
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Recipe
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Adapter for the `spoonacular` Edge Function; the API key stays on the server. */
class SpoonacularApiClient(private val client: SupabaseClient) :
    RecipeApiClient,
    IngredientApiClient {
    companion object {
        const val FUNCTION = "spoonacular"
    }

    override suspend fun findByIngredients(items: List<Ingredient>): List<Recipe> =
        SpoonacularMapper.recipes(
            call("findByIngredients") {
                put("ingredients", JsonArray(items.map { JsonPrimitive(it.name) }))
            }
        )

    override suspend fun searchRecipes(query: String): List<Recipe> =
        SpoonacularMapper.recipes(call("search") { put("query", query) })

    override suspend fun getRecipe(recipeId: String): Recipe =
        SpoonacularMapper.recipe(call("recipe") { put("id", recipeId) })

    override suspend fun searchIngredients(query: String): List<Ingredient> =
        SpoonacularMapper.ingredients(call("ingredients") { put("query", query) })

    override suspend fun lookupBarcode(barcode: String): List<Ingredient> {
        val product = try {
            call("upc") { put("upc", barcode) }
        } catch (_: NotFoundRestException) {
            return emptyList()
        }
        val term = SpoonacularMapper.productSearchTerm(product) ?: return emptyList()
        return SpoonacularMapper.ingredients(
            call("ingredients") { put("query", term) },
            barcode
        )
    }

    private suspend fun call(action: String, params: JsonObjectBuilder.() -> Unit): JsonObject {
        val body = buildJsonObject {
            put("action", action)
            params()
        }
        val response = client.functions.invoke(FUNCTION, body)
        return Json.parseToJsonElement(response.bodyAsText()).jsonObject
    }
}
