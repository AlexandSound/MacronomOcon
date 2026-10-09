package com.example.macronomocon.domain.model

class UserPreferences(
    var dietaryRestrictions: List<String>,
    var preferredCuisines: List<String>,
    var cookingMethods: List<String>,
    var notificationsEnabled: Boolean
) {
    fun update(prefs: UserPreferences): Unit = TODO("Update user preferences")
}
