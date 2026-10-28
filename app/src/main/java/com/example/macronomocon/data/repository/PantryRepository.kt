package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe

/** Operations target the authenticated user's pantry. Writes and meal deductions must be atomic on the server. */
interface PantryRepository {
    suspend fun findPantry(): Pantry
    suspend fun addItem(item: PantryItem): Pantry
    suspend fun removeItems(itemIds: Set<String>): Pantry
    suspend fun recordMeal(recipe: Recipe): Pantry
}
