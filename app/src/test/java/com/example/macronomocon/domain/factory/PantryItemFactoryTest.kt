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
    fun parsesTheQuantityIgnoringSurroundingSpace() {
        assertEquals(150.0, factory.create(rice, " 150 ", " g ", "7")!!.quantity, 0.0)
    }

    @Test
    fun parsesTheUnitIgnoringSurroundingSpace() {
        assertEquals(PantryItem.UNIT_GRAMS, factory.create(rice, " 150 ", " g ", "7")!!.unit)
    }

    @Test
    fun datesExpirationShelfLifeDaysFromToday() {
        assertEquals(today.plusDays(7), factory.create(rice, "150", "g", "7")!!.expirationDate)
    }

    @Test
    fun rejectsANonPositiveQuantity() {
        assertNull(factory.create(rice, "0", "g", "7"))
    }

    @Test
    fun rejectsAQuantityThatIsNotANumber() {
        assertNull(factory.create(rice, "NaN", "g", "7"))
    }

    @Test
    fun rejectsABlankUnit() {
        assertNull(factory.create(rice, "1", " ", "7"))
    }

    @Test
    fun rejectsANegativeShelfLife() {
        assertNull(factory.create(rice, "1", "g", "-1"))
    }

    @Test
    fun rejectsAShelfLifeBeyondTheMaximum() {
        assertNull(
            factory.create(rice, "1", "g", (PantryItemFactory.MAX_SHELF_LIFE_DAYS + 1).toString())
        )
    }

    @Test
    fun acceptsAShelfLifeAtTheMaximum() {
        assertNotNull(
            factory.create(rice, "1", "g", PantryItemFactory.MAX_SHELF_LIFE_DAYS.toString())
        )
    }
}
