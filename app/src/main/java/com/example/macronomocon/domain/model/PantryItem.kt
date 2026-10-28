package com.example.macronomocon.domain.model

import java.time.LocalDate
import java.util.UUID

data class PantryItem(
    val ingredient: Ingredient,
    val quantity: Double,
    val unit: String,
    val expirationDate: LocalDate,
    val pantryItemId: String = UUID.randomUUID().toString()
) {
    companion object {
        /** Stock expiring within this many days is surfaced as "use soon". */
        const val EXPIRING_SOON_DAYS = 3
        const val UNIT_GRAMS = "g"
        const val UNIT_PIECE = "piece"
    }

    init {
        require(pantryItemId.isNotBlank())
        require(unit.isNotBlank()) { "Unit is required" }
        require(quantity.isFinite() && quantity >= 0) { "Quantity must be finite and nonnegative" }
    }

    fun isExpired(today: LocalDate = LocalDate.now()): Boolean = expirationDate.isBefore(today)

    fun isExpiringWithin(days: Int, today: LocalDate = LocalDate.now()): Boolean {
        require(days >= 0)
        return !isExpired(today) && !expirationDate.isAfter(today.plusDays(days.toLong()))
    }

    fun reduceQuantity(amount: Double): PantryItem {
        require(amount.isFinite() && amount >= 0 && amount <= quantity)
        return copy(quantity = quantity - amount)
    }
}
