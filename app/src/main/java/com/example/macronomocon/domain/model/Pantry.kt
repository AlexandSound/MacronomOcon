package com.example.macronomocon.domain.model

import java.time.LocalDate
import java.util.Locale

class Pantry(items: List<PantryItem> = emptyList()) {

    companion object {
        private const val QUANTITY_TOLERANCE = 1e-9
    }

    private val inventory = linkedMapOf<String, PantryItem>()
    val items: List<PantryItem> get() = inventory.values.toList()

    init {
        require(
            items.map {
                it.pantryItemId
            }.distinct().size == items.size
        ) { "Duplicate pantry item ID" }
        items.forEach { inventory[it.pantryItemId] = it }
    }

    fun addItem(item: PantryItem) {
        require(item.pantryItemId !in inventory) { "Pantry item already exists" }
        inventory[item.pantryItemId] = item
    }

    fun removeItems(items: List<PantryItem>) {
        items.forEach { inventory.remove(it.pantryItemId) }
    }

    fun getExpired(today: LocalDate = LocalDate.now()): List<PantryItem> = items.filter {
        it.isExpired(today)
    }

    fun getExpiringWithin(days: Int, today: LocalDate = LocalDate.now()): List<PantryItem> {
        require(days >= 0)
        return items.filter { it.isExpiringWithin(days, today) }
    }

    fun hasIngredients(r: Recipe, today: LocalDate = LocalDate.now()): Boolean =
        allocation(r, today) != null

    /** Computes the entire deduction first so insufficient stock never partially changes the pantry. */
    fun deductIngredients(r: Recipe, today: LocalDate = LocalDate.now()) {
        val remaining =
            requireNotNull(allocation(r, today)) {
                "Insufficient unexpired ingredients in matching units"
            }
        inventory.clear()
        remaining.filter { it.quantity > 0 }.forEach { inventory[it.pantryItemId] = it }
    }

    private fun allocation(r: Recipe, today: LocalDate): List<PantryItem>? {
        if (r.requirements.isEmpty()) return null
        val remaining = items.toMutableList()
        for (required in r.requirements) {
            var needed = required.quantity
            val candidates = remaining.indices.filter {
                val item = remaining[it]
                item.ingredient.ingredientId == required.ingredient.ingredientId &&
                    item.unit.trim().lowercase(Locale.ROOT) ==
                    required.unit.trim().lowercase(Locale.ROOT) &&
                    !item.isExpired(today)
            }.sortedBy { remaining[it].expirationDate }
            for (index in candidates) {
                val item = remaining[index]
                val used = minOf(needed, item.quantity)
                remaining[index] = item.reduceQuantity(used)
                needed -= used
                if (needed <= QUANTITY_TOLERANCE) break
            }
            if (needed > QUANTITY_TOLERANCE) return null
        }
        return remaining
    }
}
