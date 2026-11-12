package com.example.macronomocon.di

import android.content.Context
import android.content.Intent
import com.example.macronomocon.BuildConfig
import com.example.macronomocon.data.api.CachingRecipeApiClient
import com.example.macronomocon.data.local.LocalCommentRepository
import com.example.macronomocon.data.local.LocalPantryRepository
import com.example.macronomocon.data.repository.AccountRepository
import com.example.macronomocon.data.repository.CommentImageStore
import com.example.macronomocon.data.repository.CommentRepository
import com.example.macronomocon.data.repository.IngredientRepository
import com.example.macronomocon.data.repository.PantryRepository
import com.example.macronomocon.data.repository.RecipeRepository
import com.example.macronomocon.data.scanner.BarcodeScanner
import com.example.macronomocon.data.scanner.GoogleCodeScanner
import com.example.macronomocon.data.supabase.SpoonacularApiClient
import com.example.macronomocon.data.supabase.SupabaseAccountRepository
import com.example.macronomocon.data.supabase.SupabaseCommentImageStore
import com.example.macronomocon.data.supabase.SupabaseRecipeUserData
import com.example.macronomocon.domain.model.User
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Production composition root: Supabase accounts, bookmarks, custom recipes and comment photos,
 * Spoonacular recipes and ingredients through the `spoonacular` Edge Function, and Google's barcode
 * scanner. Pantry and comments stay in memory until their server tables are connected.
 */
class AuthAppContainer(private val context: Context) : AppContainer {
    override val liveAuth = true
    override val authConfigured = BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private val client by lazy {
        createSupabaseClient(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_PUBLISHABLE_KEY.ifBlank {
                "not-configured"
            }
        ) {
            install(Auth) {
                // OAuth returns to macronomocon://login-callback (see AndroidManifest.xml).
                scheme = REDIRECT_SCHEME
                host = REDIRECT_HOST
                flowType = FlowType.PKCE
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Postgrest)
            install(Functions)
            install(Storage)
        }
    }
    private val spoonacular by lazy { SpoonacularApiClient(client) }

    // Scoped to the signed-in user and cleared on sign-out, so accounts never share local data.
    private val localPantry = LocalPantryRepository { client.auth.currentUserOrNull()?.id }
    private val localComments = LocalCommentRepository {
        client.auth.currentUserOrNull()?.let { User(it.id, it.email.orEmpty(), it.email.orEmpty()) }
    }

    override val accounts: AccountRepository by lazy {
        val remote = SupabaseAccountRepository(client)
        object : AccountRepository by remote {
            override suspend fun signOut() {
                try {
                    remote.signOut()
                } finally {
                    localPantry.clear()
                    localComments.clear()
                }
            }
        }
    }
    override val ingredients by lazy { IngredientRepository(spoonacular) }
    override val recipes by lazy {
        RecipeRepository(CachingRecipeApiClient(spoonacular), SupabaseRecipeUserData(client))
    }
    override val pantry: PantryRepository = localPantry
    override val comments: CommentRepository = localComments
    override val commentImages: CommentImageStore by lazy {
        SupabaseCommentImageStore(client, context.contentResolver)
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
