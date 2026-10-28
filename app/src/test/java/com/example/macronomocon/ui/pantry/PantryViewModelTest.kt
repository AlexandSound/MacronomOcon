package com.example.macronomocon.ui.pantry

import com.example.macronomocon.data.api.IngredientApiClient
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.PantryRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.domain.model.Ingredient
import com.example.macronomocon.domain.model.Pantry
import com.example.macronomocon.domain.model.PantryItem
import com.example.macronomocon.domain.model.Recipe
import com.example.macronomocon.ui.common.UiMessage
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PantryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val ingredient =
        Ingredient("rice", "Rice", "0123456789012")
    private var lookup: suspend () -> List<Ingredient> = { listOf(ingredient) }
    private val api = object : IngredientApiClient {
        override suspend fun lookupBarcode(barcode: String): List<Ingredient> = lookup()
        override suspend fun searchIngredients(query: String): List<Ingredient> = listOf(ingredient)
    }
    private val pantry = Pantry()
    private val repository = object : PantryRepository {
        override suspend fun findPantry(): Pantry = pantry
        override suspend fun addItem(item: PantryItem): Pantry = pantry.also { it.addItem(item) }
        override suspend fun removeItems(itemIds: Set<String>): Pantry = pantry.also {
            it.removeItems(it.items.filter { item -> item.pantryItemId in itemIds })
        }
        override suspend fun recordMeal(recipe: Recipe): Pantry =
            pantry.also { it.deductIngredients(recipe) }
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun failedBarcodeLookupAllowsSearchSelectionAndConfirmation() = runTest(dispatcher) {
        lookup = { throw IOException("offline") }
        val vm = PantryViewModel(repository, IngredientRepository(api))
        vm.identifyBarcode(ingredient.barcode)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
        assertEquals(UiMessage.ERROR_IDENTIFY_BARCODE, vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
        vm.searchIngredients("rice")
        advanceUntilIdle()
        vm.selectIngredient(ingredient)
        val item =
            PantryItem(ingredient, 100.0, "g", LocalDate.now().plusDays(2))
        vm.confirmAdd(item)
        advanceUntilIdle()
        assertEquals(listOf(item), vm.uiState.value.data.items)
        assertNull(vm.uiState.value.data.selectedIngredient)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun missingOrUnmatchedCodeEnablesFallback() = runTest(dispatcher) {
        val vm = PantryViewModel(repository, IngredientRepository(api))
        vm.identifyBarcode(null)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
        lookup = { emptyList() }
        vm.identifyBarcode(ingredient.barcode)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun successfulLookupWaitsForConfirmationBeforeAddingStock() = runTest(dispatcher) {
        val vm = PantryViewModel(repository, IngredientRepository(api))
        vm.identifyBarcode(ingredient.barcode)
        advanceUntilIdle()
        assertEquals(listOf(ingredient), vm.uiState.value.data.searchResults)
        assertTrue(pantry.items.isEmpty())
        assertFalse(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun cancellationIsNotReportedAsScanFailure() = runTest(dispatcher) {
        lookup = { throw CancellationException() }
        val vm = PantryViewModel(repository, IngredientRepository(api))
        vm.identifyBarcode(ingredient.barcode)
        advanceUntilIdle()
        assertNull(vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.data.showIngredientSearch)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun injectedScannerAdapterFeedsLookupAndInvalidStockIsReportedInState() = runTest(dispatcher) {
        val scanner = object : BarcodeScanner {
            override suspend fun scan(): String = ingredient.barcode
        }
        val vm = PantryViewModel(repository, IngredientRepository(api), scanner)
        vm.scanBarcode()
        advanceUntilIdle()
        assertEquals(listOf(ingredient), vm.uiState.value.data.searchResults)
        vm.selectIngredient(ingredient)
        assertNull(vm.submitStock("0", "g", "7"))
        assertTrue(vm.uiState.value.data.invalidStock)
        assertTrue(pantry.items.isEmpty())
        vm.submitStock("100", "g", "7")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.data.invalidStock)
        assertEquals(100.0, pantry.items.single().quantity, 0.0)
    }

    @Test
    fun missingScannerFallsBackToSearch() = runTest(dispatcher) {
        val vm = PantryViewModel(repository, IngredientRepository(api))
        vm.scanBarcode()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
        assertNull(vm.uiState.value.errorMessage)
    }
}
