package com.example.macronomocon.ui.pantry

import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.PantryRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.domain.factory.PantryItemFactory
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
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
    private val repository: PantryRepository,
    private val ingredients: IngredientRepository,
    private val scanner: BarcodeScanner? = null,
    private val itemFactory: PantryItemFactory = PantryItemFactory()
) : StateViewModel<PantryState>(PantryState()) {
    init {
        scanner?.let(::addCloseable)
    }

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
        val pantry = repository.findPantry()
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
        val pantry = repository.addItem(item)
        updateData {
            it.copy(items = pantry.items, selectedIngredient = null, searchResults = emptyList())
        }
    }

    fun removeSelected(itemIds: Set<String>) = action(UiMessage.ERROR_REMOVE_PANTRY_ITEMS) {
        val pantry = repository.removeItems(itemIds)
        updateData { it.copy(items = pantry.items) }
    }

    fun removeExpired() = action(UiMessage.ERROR_REMOVE_EXPIRED_ITEMS) {
        val current = repository.findPantry()
        val pantry = repository.removeItems(current.getExpired().map { it.pantryItemId }.toSet())
        updateData { it.copy(items = pantry.items) }
    }

    fun recordMeal(recipe: Recipe) = action(UiMessage.ERROR_RECORD_THIS_MEAL) {
        val pantry = repository.recordMeal(recipe)
        updateData { it.copy(items = pantry.items) }
    }
}
