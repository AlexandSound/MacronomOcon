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
    private val scanner = object : BarcodeScanner {
        override suspend fun scan(): String = ingredient.barcode
    }

    private fun viewModel(barcodeScanner: BarcodeScanner? = null) =
        PantryViewModel(repository, IngredientRepository(api), barcodeScanner)

    private fun stockItem() = PantryItem(ingredient, 100.0, "g", LocalDate.now().plusDays(2))

    /** [PantryViewModel.selectIngredient] only accepts ingredients in the current results. */
    private suspend fun viewModelWithSelection(
        barcodeScanner: BarcodeScanner? = null
    ): PantryViewModel = viewModel(barcodeScanner).also {
        it.searchIngredients("rice").join()
        it.selectIngredient(ingredient)
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun aFailedBarcodeLookupFallsBackToIngredientSearch() = runTest(dispatcher) {
        lookup = { throw IOException("offline") }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun aFailedBarcodeLookupReportsTheError() = runTest(dispatcher) {
        lookup = { throw IOException("offline") }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertEquals(UiMessage.ERROR_IDENTIFY_BARCODE, vm.uiState.value.errorMessage)
    }

    @Test
    fun aFailedBarcodeLookupStopsLoading() = runTest(dispatcher) {
        lookup = { throw IOException("offline") }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun aConfirmedItemIsAddedToThePantry() = runTest(dispatcher) {
        val item = stockItem()
        val vm = viewModelWithSelection()
        vm.confirmAdd(item).join()
        assertEquals(listOf(item), vm.uiState.value.data.items)
    }

    @Test
    fun confirmingAnItemClearsTheSelectedIngredient() = runTest(dispatcher) {
        val vm = viewModelWithSelection()
        vm.confirmAdd(stockItem()).join()
        assertNull(vm.uiState.value.data.selectedIngredient)
    }

    @Test
    fun confirmingAnItemClearsAnEarlierError() = runTest(dispatcher) {
        lookup = { throw IOException("offline") }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        vm.searchIngredients("rice").join()
        vm.selectIngredient(ingredient)
        vm.confirmAdd(stockItem()).join()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun aMissingBarcodeFallsBackToIngredientSearch() = runTest(dispatcher) {
        val vm = viewModel()
        vm.identifyBarcode(null).join()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun anUnmatchedBarcodeFallsBackToIngredientSearch() = runTest(dispatcher) {
        lookup = { emptyList() }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun anUnmatchedBarcodeIsNotReportedAsAnError() = runTest(dispatcher) {
        lookup = { emptyList() }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun aSuccessfulLookupShowsTheMatchedIngredients() = runTest(dispatcher) {
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertEquals(listOf(ingredient), vm.uiState.value.data.searchResults)
    }

    @Test
    fun aSuccessfulLookupDoesNotAddStockBeforeConfirmation() = runTest(dispatcher) {
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertTrue(pantry.items.isEmpty())
    }

    @Test
    fun aSuccessfulLookupDoesNotFallBackToIngredientSearch() = runTest(dispatcher) {
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertFalse(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun aCancelledLookupIsNotReportedAsAnError() = runTest(dispatcher) {
        lookup = { throw CancellationException() }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun aCancelledLookupDoesNotFallBackToIngredientSearch() = runTest(dispatcher) {
        lookup = { throw CancellationException() }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertFalse(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun aCancelledLookupStopsLoading() = runTest(dispatcher) {
        lookup = { throw CancellationException() }
        val vm = viewModel()
        vm.identifyBarcode(ingredient.barcode).join()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun anInjectedScannerFeedsTheBarcodeLookup() = runTest(dispatcher) {
        val vm = viewModel(scanner)
        vm.scanBarcode().join()
        assertEquals(listOf(ingredient), vm.uiState.value.data.searchResults)
    }

    @Test
    fun invalidStockStartsNoWork() = runTest(dispatcher) {
        assertNull(viewModelWithSelection().submitStock("0", "g", "7"))
    }

    @Test
    fun invalidStockIsFlaggedInState() = runTest(dispatcher) {
        val vm = viewModelWithSelection()
        vm.submitStock("0", "g", "7")
        assertTrue(vm.uiState.value.data.invalidStock)
    }

    @Test
    fun invalidStockIsNotAddedToThePantry() = runTest(dispatcher) {
        viewModelWithSelection().submitStock("0", "g", "7")
        assertTrue(pantry.items.isEmpty())
    }

    @Test
    fun validStockClearsTheInvalidStockFlag() = runTest(dispatcher) {
        val vm = viewModelWithSelection()
        vm.submitStock("0", "g", "7")
        vm.submitStock("100", "g", "7")?.join()
        assertFalse(vm.uiState.value.data.invalidStock)
    }

    @Test
    fun validStockIsAddedToThePantry() = runTest(dispatcher) {
        viewModelWithSelection().submitStock("100", "g", "7")?.join()
        assertEquals(100.0, pantry.items.single().quantity, 0.0)
    }

    @Test
    fun aMissingScannerFallsBackToIngredientSearch() = runTest(dispatcher) {
        val vm = viewModel()
        vm.scanBarcode().join()
        assertTrue(vm.uiState.value.data.showIngredientSearch)
    }

    @Test
    fun aMissingScannerIsNotReportedAsAnError() = runTest(dispatcher) {
        val vm = viewModel()
        vm.scanBarcode().join()
        assertNull(vm.uiState.value.errorMessage)
    }
}
