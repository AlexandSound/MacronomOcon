package com.example.macronomocon.di

import android.content.Context
import android.content.Intent
import com.example.macronomocon.BuildConfig
import com.example.macronomocon.data.api.CachingRecipeApiClient
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.data.scanner.GoogleCodeScanner
import com.example.macronomocon.data.supabase.SpoonacularApiClient
import com.example.macronomocon.data.supabase.SpoonacularIngredientApiFactory
import com.example.macronomocon.data.supabase.SupabaseAccountRepository
import com.example.macronomocon.data.supabase.SupabaseConnectionFactory
import com.example.macronomocon.data.supabase.SupabaseRecipeUserData
import io.github.jan.supabase.auth.handleDeeplinks

/**
 * Production composition root: Supabase accounts and bookmarks, Spoonacular recipes and
 * ingredients through the `spoonacular` Edge Function, and Google's barcode scanner. Pantry and
 * comments belong to the signed-in User for the current session.
 */
class AuthAppContainer(private val context: Context) : AppContainer {
    override val liveAuth = true
    override val authConfigured = BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private val connectionFactory = SupabaseConnectionFactory(
        BuildConfig.SUPABASE_URL,
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.ifBlank { "not-configured" }
    )
    private val connection by lazy { connectionFactory.createConnection() }
    private val client get() = connection.client
    private val spoonacular by lazy { SpoonacularApiClient(client) }
    override val accounts: AccountRepository by lazy { SupabaseAccountRepository(connection) }
    override val ingredients by lazy {
        IngredientRepository(SpoonacularIngredientApiFactory(connection))
    }
    override val recipes by lazy {
        RecipeRepository(CachingRecipeApiClient(spoonacular), SupabaseRecipeUserData(connection))
    }

    override fun barcodeScanner(): BarcodeScanner = GoogleCodeScanner(context)

    override fun handleAuthRedirect(intent: Intent) {
        if (authConfigured && intent.data?.scheme == REDIRECT_SCHEME) client.handleDeeplinks(intent)
    }

    companion object {
        const val REDIRECT_SCHEME = "macronomocon"
        const val REDIRECT_HOST = "login-callback"
    }
}
