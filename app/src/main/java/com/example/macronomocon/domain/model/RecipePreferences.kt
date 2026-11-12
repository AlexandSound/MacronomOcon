package com.example.macronomocon.domain.model

/** Only supported Spoonacular filters are sent; all selected diets must match (comma/AND). */
data class RecipePreferences(
    val diet: String = "",
    val intolerances: String = "",
    val cuisine: String = ""
) {
    companion object {
        val diets = setOf(
            "gluten free", "ketogenic", "vegetarian", "lacto-vegetarian", "ovo-vegetarian",
            "vegan", "pescetarian", "paleo", "primal", "low fodmap", "whole30"
        )
        val intolerances = setOf(
            "dairy", "egg", "gluten", "grain", "peanut", "seafood", "sesame", "shellfish",
            "soy", "sulfite", "tree nut", "wheat"
        )
        val cuisines = setOf(
            "african", "asian", "american", "british", "cajun", "caribbean", "chinese",
            "eastern european", "european", "french", "german", "greek", "indian", "irish",
            "italian", "japanese", "jewish", "korean", "latin american", "mediterranean",
            "mexican", "middle eastern", "nordic", "southern", "spanish", "thai", "vietnamese"
        )
        private val aliases = mapOf(
            "dairy free" to "dairy", "lactose free" to "dairy", "peanut free" to "peanut",
            "tree nut free" to "tree nut", "egg free" to "egg",
            "soy free" to "soy", "shellfish free" to "shellfish", "keto" to "ketogenic",
            "pescatarian" to "pescetarian"
        )

        fun from(preferences: UserPreferences): RecipePreferences {
            val restrictions = preferences.dietaryRestrictions.map {
                val value = it.trim().lowercase().replace("-free", " free")
                aliases[value] ?: value
            }.filter { it.isNotBlank() }.distinct().sorted()
            val cuisines = preferences.preferredCuisines.map { it.trim().lowercase() }
                .filter { it.isNotBlank() }.distinct().sorted()
            require(restrictions.all { it in diets || it in intolerances })
            require(cuisines.all { it in this.cuisines })
            return RecipePreferences(
                diet = restrictions.filter { it in diets }.joinToString(","),
                intolerances = restrictions.filter { it in intolerances }.joinToString(","),
                cuisine = cuisines.joinToString(",")
            )
        }
    }
}
