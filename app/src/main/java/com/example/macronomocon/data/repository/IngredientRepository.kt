package com.example.macronomocon.data.repository

import com.example.macronomocon.data.api.IngredientApiClient
import com.example.macronomocon.domain.model.Ingredient

class IngredientRepository(private val api: IngredientApiClient) {
    suspend fun findByBarcode(barcode: String): List<Ingredient> {
        val code = barcode.trim()
        if (code.isEmpty()) return emptyList()
        require(code.all { it in '0'..'9' }) { "A product barcode must contain digits" }
        return api.lookupBarcode(code).distinctBy { it.ingredientId }
    }

    suspend fun search(query: String): List<Ingredient> {
        val term = query.trim()
        return if (term.isEmpty()) {
            emptyList()
        } else {
            api.searchIngredients(term).distinctBy {
                it.ingredientId
            }
        }
    }
}
