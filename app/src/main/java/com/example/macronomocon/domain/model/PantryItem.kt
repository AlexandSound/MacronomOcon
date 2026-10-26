package com.example.macronomocon.domain.model

import java.time.LocalDate

class PantryItem(var quantity: Double, var unit: String, var expirationDate: LocalDate) {
    fun isExpired(): Boolean = TODO("Check whether the item is expired")

    fun isExpiringWithin(days: Int): Boolean =
        TODO("Check whether the item expires within the given days")

    fun reduceQuantity(amount: Double): Unit = TODO("Reduce the item quantity")
}
