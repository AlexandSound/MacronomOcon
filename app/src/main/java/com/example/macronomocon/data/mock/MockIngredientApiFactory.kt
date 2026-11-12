package com.example.macronomocon.data.mock

import com.example.macronomocon.data.api.IngredientApiClient
import com.example.macronomocon.data.api.IngredientApiFactory
import com.example.macronomocon.domain.model.Ingredient

class MockIngredientApiFactory(private val ingredients: List<Ingredient>) : IngredientApiFactory {
    override fun createClient(): IngredientApiClient = object : IngredientApiClient {
        override suspend fun lookupBarcode(barcode: String): List<Ingredient> =
            ingredients.filter { it.barcode == barcode }
        override suspend fun searchIngredients(query: String): List<Ingredient> =
            ingredients.filter { it.name.contains(query, ignoreCase = true) }
    }
}
