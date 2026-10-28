package com.example.macronomocon.domain.factory

import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.PantryItem
import java.time.LocalDate

/** Factory: builds a valid [PantryItem] from raw form input, or returns null when any field is invalid. */
class PantryItemFactory(private val today: () -> LocalDate = LocalDate::now) {
    companion object {
        const val MAX_SHELF_LIFE_DAYS = 3650
    }

    fun create(
        ingredient: Ingredient,
        quantity: String,
        unit: String,
        daysUntilExpiry: String
    ): PantryItem? {
        val amount = quantity.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 }
        val days = daysUntilExpiry.trim().toIntOrNull()?.takeIf { it in 0..MAX_SHELF_LIFE_DAYS }
        val cleanUnit = unit.trim()
        if (amount == null || days == null || cleanUnit.isEmpty()) return null
        return PantryItem(ingredient, amount, cleanUnit, today().plusDays(days.toLong()))
    }
}
