package com.example.macronomocon.data.local

import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.testing.assertThrows
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
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
    fun itemsAddedByOneAccountAreNotVisibleToAnother() = runTest {
        pantry.addItem(item("a-rice"))
        userId = "user-b"
        assertTrue(pantry.findPantry().items.isEmpty())
    }

    @Test
    fun anAccountKeepsItsItemsAcrossAccountSwitches() = runTest {
        pantry.addItem(item("a-rice"))
        userId = "user-b"
        pantry.findPantry()
        userId = "user-a"
        assertEquals(listOf("a-rice"), pantry.findPantry().items.map { it.pantryItemId })
    }

    @Test
    fun clearRemovesTheSignedInAccountsItems() = runTest {
        pantry.addItem(item("a-rice"))
        pantry.clear()
        assertTrue(pantry.findPantry().items.isEmpty())
    }

    @Test
    fun clearRemovesOtherAccountsItemsToo() = runTest {
        pantry.addItem(item("a-rice"))
        userId = "user-b"
        pantry.addItem(item("b-rice"))
        pantry.clear()
        userId = "user-a"
        assertTrue(pantry.findPantry().items.isEmpty())
    }

    @Test
    fun readingThePantryRequiresASignedInUser() = runTest {
        userId = null
        assertThrows<IllegalArgumentException> { pantry.findPantry() }
    }
}
