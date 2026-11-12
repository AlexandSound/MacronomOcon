package com.example.macronomocon.di

import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory as buildViewModelFactory
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.domain.factory.PantryItemFactory
import com.example.macronomocon.domain.model.User
import com.example.macronomocon.ui.account.AccountViewModel
import com.example.macronomocon.ui.pantry.PantryViewModel
import com.example.macronomocon.ui.recipe.RecipeViewModel

/** Application-scoped composition root (one per process, so each repository is a singleton). Screens depend on contracts, never on mock implementations. */
interface AppContainer {
    val liveAuth: Boolean get() = false
    val authConfigured: Boolean get() = true
    val accounts: AccountRepository
    val ingredients: IngredientRepository
    val recipes: RecipeRepository

    /** Factory method: each caller receives, owns, and closes its own scanner. */
    fun barcodeScanner(): BarcodeScanner

    /** Completes an OAuth sign-in when the browser redirects back to the app. */
    fun handleAuthRedirect(intent: Intent) = Unit
}

/** Factory pattern: the only place ViewModels are constructed, with dependencies injected. */
fun AppContainer.viewModelFactory(currentUser: () -> User? = { null }): ViewModelProvider.Factory =
    buildViewModelFactory {
        initializer { AccountViewModel(accounts) }
        initializer {
            PantryViewModel(
                currentUser,
                accounts,
                ingredients,
                barcodeScanner(),
                PantryItemFactory()
            )
        }
        initializer { RecipeViewModel(recipes) }
    }
