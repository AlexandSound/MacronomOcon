package com.example.macronomocon.domain.model

class Recipe(
    val recipeId: String,
    var name: String,
    var instructions: List<String>,
    var prepTime: Int,
    var cookTime: Int,
    var difficulty: Difficulty,
    var mealType: MealType,
    var cuisine: String,
    var toolsNeeded: List<String>
) {
    fun totalTime(): Int = TODO("Calculate total preparation and cooking time")

    fun getIngredients(): List<Ingredient> = TODO("Return the recipe ingredients")

    fun matchCount(items: List<PantryItem>): Int = TODO("Count matching pantry items")

    fun hasRequiredInfo(): Boolean = TODO("Check required recipe information")
}
