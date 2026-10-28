package com.example.macronomocon.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.PantryRepository
import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.domain.factory.PantryItemFactory
import com.example.macronomocon.ui.account.AccountViewModel
import com.example.macronomocon.ui.comment.CommentViewModel
import com.example.macronomocon.ui.pantry.PantryViewModel
import com.example.macronomocon.ui.recipe.RecipeViewModel

/** Application-scoped composition root (one per process, so each repository is a singleton). Screens depend on contracts, never on mock implementations. */
interface AppContainer {
    val accounts: AccountRepository
    val pantry: PantryRepository
    val ingredients: IngredientRepository
    val recipes: RecipeRepository
    val comments: CommentRepository

    /** Factory method: each caller receives, owns, and closes its own scanner. */
    fun barcodeScanner(): BarcodeScanner
}

/** Factory pattern: the only place ViewModels are constructed, with dependencies injected. */
fun AppContainer.viewModelFactory(): ViewModelProvider.Factory = viewModelFactory {
    initializer { AccountViewModel(accounts) }
    initializer { PantryViewModel(pantry, ingredients, barcodeScanner(), PantryItemFactory()) }
    initializer { RecipeViewModel(recipes) }
    initializer { CommentViewModel(comments) }
}
