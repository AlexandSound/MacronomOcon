package com.example.macronomocon.domain.model

/** Client profile only. Password verification, OTP, and lockouts belong to the authentication service. */
data class User(
    val userId: String,
    val email: String,
    val displayName: String,
    val twoFactorEnabled: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val favoriteRecipeIds: Set<String> = emptySet(),
    val blockedRecipeIds: Set<String> = emptySet()
) {
    init {
        require(userId.isNotBlank())
    }
    fun favorite(r: Recipe): User = copy(favoriteRecipeIds = favoriteRecipeIds + r.recipeId)

    fun unfavorite(r: Recipe): User = copy(favoriteRecipeIds = favoriteRecipeIds - r.recipeId)

    fun block(r: Recipe): User = copy(blockedRecipeIds = blockedRecipeIds + r.recipeId)

    fun isBlocked(r: Recipe): Boolean = r.recipeId in blockedRecipeIds
}
