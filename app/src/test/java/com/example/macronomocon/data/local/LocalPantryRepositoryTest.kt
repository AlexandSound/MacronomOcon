package com.example.macronomocon.data.local

import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.PantryItem
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalPantryRepositoryTest {
    private var userId: String? = "user-a"
    private val pantry = LocalPantryRepository { userId }

    private fun item(id: String) = PantryItem(
        Ingredient("20444", "rice"),
        500.0,
        PantryItem.UNIT_GRAMS,
        LocalDate.now().plusDays(30),
        pantryItemId = id
    )

    @Test
    fun eachAccountHasItsOwnPantry() = runTest {
        pantry.addItem(item("a-rice"))
        userId = "user-b"
        assertTrue(pantry.findPantry().items.isEmpty())
        userId = "user-a"
        assertEquals(listOf("a-rice"), pantry.findPantry().items.map { it.pantryItemId })
    }

    @Test
    fun clearRemovesEveryAccountsItems() = runTest {
        pantry.addItem(item("a-rice"))
        pantry.clear()
        assertTrue(pantry.findPantry().items.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun requiresSignedInUser() = runTest {
        userId = null
        pantry.findPantry()
    }
}
