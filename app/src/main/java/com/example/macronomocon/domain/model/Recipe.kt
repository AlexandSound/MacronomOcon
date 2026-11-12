package com.example.macronomocon.domain.model

data class Recipe(
    val recipeId: String,
    val name: String,
    val instructions: List<String>,
    val prepTime: Int,
    val cookTime: Int,
    val difficulty: Difficulty,
    val mealType: MealType,
    val cuisine: String,
    val toolsNeeded: List<String>,
    val requirements: List<RecipeIngredient> = emptyList()
) {
    companion object {
        /** Recipes with a total time under this many minutes count as quick. */
        const val QUICK_MAX_MINUTES = 20

        // Postgres generates UUIDs for custom recipes; provider recipes keep their own IDs.
        private val CUSTOM_ID = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )

        fun isCustomId(recipeId: String): Boolean = CUSTOM_ID.matches(recipeId)
    }

    init {
        require(prepTime >= 0 && cookTime >= 0)
        require(prepTime.toLong() + cookTime <= Int.MAX_VALUE)
    }

    fun totalTime(): Int = prepTime + cookTime

    fun getIngredients(): List<Ingredient> = requirements.map {
        it.ingredient
    }.distinctBy { it.ingredientId }

    fun matchCount(items: List<PantryItem>): Int {
        val available = items.filter { it.quantity > 0 }.map { it.ingredient.ingredientId }.toSet()
        return getIngredients().count { it.ingredientId in available }
    }

    fun isQuick(): Boolean = totalTime() < QUICK_MAX_MINUTES

    fun isCustom(): Boolean = isCustomId(recipeId)

    /** A [new] recipe has no ID until the backend assigns one on insert. */
    fun hasRequiredInfo(new: Boolean = false): Boolean = (new || recipeId.isNotBlank()) &&
        name.isNotBlank() &&
        instructions.isNotEmpty() &&
        instructions.all { it.isNotBlank() } &&
        requirements.isNotEmpty()
}
