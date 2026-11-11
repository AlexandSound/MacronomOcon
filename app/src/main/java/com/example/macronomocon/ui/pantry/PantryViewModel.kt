package com.example.macronomocon.ui.pantry

import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.domain.factory.PantryItemFactory
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.common.StateViewModel
import com.example.macronomocon.ui.common.UiMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

data class PantryState(
    val items: List<PantryItem> = emptyList(),
    val searchResults: List<Ingredient> = emptyList(),
    val selectedIngredient: Ingredient? = null,
    val showIngredientSearch: Boolean = false,
    val invalidStock: Boolean = false
)

class PantryViewModel(
    private val currentUser: () -> User?,
    private val accounts: AccountRepository,
    private val ingredients: IngredientRepository,
    private val scanner: BarcodeScanner? = null,
    private val itemFactory: PantryItemFactory = PantryItemFactory()
) : StateViewModel<PantryState>(PantryState()) {
    init {
        scanner?.let(::addCloseable)
    }

    private fun pantry() = requireNotNull(currentUser()).pantry

    fun beginAdding() {
        clearError()
        updateData {
            it.copy(
                searchResults = emptyList(),
                selectedIngredient = null,
                showIngredientSearch = false,
                invalidStock = false
            )
        }
    }

    fun loadPantry() = action(UiMessage.ERROR_LOAD_YOUR_PANTRY) {
        val pantry = pantry()
        updateData { it.copy(items = pantry.items) }
    }

    /** Without an injected scanner, or when it decodes nothing, ingredient search is offered instead. */
    fun scanBarcode() = action(UiMessage.ERROR_IDENTIFY_BARCODE) {
        lookUpBarcode { scanner?.scan() }
    }

    /** Null also covers camera permission or decoding failure. */
    fun identifyBarcode(barcode: String?) = action(UiMessage.ERROR_IDENTIFY_BARCODE) {
        lookUpBarcode { barcode }
    }

    private suspend fun lookUpBarcode(readBarcode: suspend () -> String?) {
        updateData {
            it.copy(
                searchResults = emptyList(),
                selectedIngredient = null,
                showIngredientSearch = false
            )
        }
        try {
            val barcode = readBarcode()
            val results = if (barcode ==
                null
            ) {
                emptyList()
            } else {
                ingredients.findByBarcode(barcode)
            }
            updateData {
                it.copy(searchResults = results, showIngredientSearch = results.isEmpty())
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            updateData { it.copy(showIngredientSearch = true) }
            throw failure
        }
    }

    fun searchIngredients(query: String) = action(UiMessage.ERROR_SEARCH_INGREDIENTS) {
        updateData {
            it.copy(
                showIngredientSearch = true,
                selectedIngredient = null,
                searchResults = emptyList()
            )
        }
        val results = ingredients.search(query)
        updateData { it.copy(searchResults = results) }
    }

    fun selectIngredient(ingredient: Ingredient) {
        require(data.searchResults.any { it.ingredientId == ingredient.ingredientId })
        updateData { it.copy(selectedIngredient = ingredient) }
    }

    /** Builds the item through the factory; invalid input is reported in state instead of throwing. */
    fun submitStock(quantity: String, unit: String, daysUntilExpiry: String): Job? {
        val ingredient = data.selectedIngredient ?: return null
        val item = itemFactory.create(ingredient, quantity, unit, daysUntilExpiry)
        updateData { it.copy(invalidStock = item == null) }
        return item?.let(::confirmAdd)
    }

    fun confirmAdd(item: PantryItem) = action(UiMessage.ERROR_ADD_THIS_PANTRY_ITEM) {
        require(data.selectedIngredient?.ingredientId == item.ingredient.ingredientId)
        val pantry = pantry()
        accounts.savePantryItem(item)
        pantry.addItem(item)
        updateData {
            it.copy(items = pantry.items, selectedIngredient = null, searchResults = emptyList())
        }
    }

    fun removeSelected(itemIds: Set<String>) = action(UiMessage.ERROR_REMOVE_PANTRY_ITEMS) {
        val pantry = pantry()
        val removed = pantry.items.filter { it.pantryItemId in itemIds }
        removed.forEach { accounts.deletePantryItem(it) }
        pantry.removeItems(removed)
        updateData { it.copy(items = pantry.items) }
    }

    fun removeExpired() = action(UiMessage.ERROR_REMOVE_EXPIRED_ITEMS) {
        val pantry = pantry()
        val expired = pantry.getExpired()
        expired.forEach { accounts.deletePantryItem(it) }
        pantry.removeItems(expired)
        updateData { it.copy(items = pantry.items) }
    }

    fun recordMeal(recipe: Recipe) = action(UiMessage.ERROR_RECORD_THIS_MEAL) {
        val pantry = pantry()
        // Deduct on a copy first so the database is updated before the session's pantry changes.
        val after = Pantry(pantry.items).apply { deductIngredients(recipe) }.items
        val remainingIds = after.map { it.pantryItemId }.toSet()
        pantry.items.filter { it.pantryItemId !in remainingIds }
            .forEach { accounts.deletePantryItem(it) }
        after.filter { it !in pantry.items }.forEach { accounts.savePantryItem(it) }
        pantry.deductIngredients(recipe)
        updateData { it.copy(items = pantry.items) }
    }
}
