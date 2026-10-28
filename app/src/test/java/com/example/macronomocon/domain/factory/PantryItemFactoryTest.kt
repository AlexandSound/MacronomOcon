package com.example.macronomocon.domain.factory

import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.PantryItem
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PantryItemFactoryTest {
    private val today = LocalDate.of(2026, 1, 1)
    private val factory = PantryItemFactory { today }
    private val rice = Ingredient("rice", "Rice")

    @Test
    fun validInputIsTrimmedAndDated() {
        val item = factory.create(rice, " 150 ", " g ", "7")!!
        assertEquals(150.0, item.quantity, 0.0)
        assertEquals(PantryItem.UNIT_GRAMS, item.unit)
        assertEquals(today.plusDays(7), item.expirationDate)
    }

    @Test
    fun invalidInputIsRejected() {
        assertNull(factory.create(rice, "0", "g", "7"))
        assertNull(factory.create(rice, "NaN", "g", "7"))
        assertNull(factory.create(rice, "1", " ", "7"))
        assertNull(factory.create(rice, "1", "g", "-1"))
        assertNull(
            factory.create(rice, "1", "g", (PantryItemFactory.MAX_SHELF_LIFE_DAYS + 1).toString())
        )
        assertNotNull(
            factory.create(rice, "1", "g", PantryItemFactory.MAX_SHELF_LIFE_DAYS.toString())
        )
    }
}
