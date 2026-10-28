package com.example.macronomocon.data.api

import com.example.macronomocon.domain.model.Ingredient

/** Open Food Facts transport boundary. Empty results mean no matching product; failures throw. */
interface IngredientApiClient {
    suspend fun lookupBarcode(barcode: String): List<Ingredient>
    suspend fun searchIngredients(query: String): List<Ingredient>
}
