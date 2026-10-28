package com.example.macronomocon.domain.model

/** Quantities use explicit units; conversion between units is not inferred. */
data class RecipeIngredient(val ingredient: Ingredient, val quantity: Double, val unit: String) {
    init {
        require(unit.isNotBlank())
        require(quantity.isFinite() && quantity > 0)
    }
}
