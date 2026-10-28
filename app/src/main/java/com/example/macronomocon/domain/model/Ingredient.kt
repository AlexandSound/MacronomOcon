package com.example.macronomocon.domain.model

data class Ingredient(
    val ingredientId: String,
    val name: String,
    val barcode: String = "",
    val category: String = ""
) {
    init {
        require(ingredientId.isNotBlank()) { "Ingredient ID is required" }
        require(name.isNotBlank()) { "Ingredient name is required" }
    }
}
