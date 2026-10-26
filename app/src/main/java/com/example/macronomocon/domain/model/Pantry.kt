package com.example.macronomocon.domain.model

class Pantry {
    fun addItem(item: PantryItem): Unit = TODO("Add a pantry item")

    fun removeItems(items: List<PantryItem>): Unit = TODO("Remove pantry items")

    fun getExpired(): List<PantryItem> = TODO("Return expired pantry items")

    fun getExpiringWithin(days: Int): List<PantryItem> =
        TODO("Return pantry items expiring within the given days")

    fun hasIngredients(r: Recipe): Boolean =
        TODO("Check whether the pantry has the recipe ingredients")

    fun deductIngredients(r: Recipe): Unit = TODO("Deduct ingredients used by the recipe")
}
