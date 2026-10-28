package com.example.macronomocon.domain.model

data class UserPreferences(
    val dietaryRestrictions: List<String> = emptyList(),
    val preferredCuisines: List<String> = emptyList(),
    val cookingMethods: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true
)
