package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.api.IngredientApiClient
import com.example.macronomocon.data.api.IngredientApiFactory

class SpoonacularIngredientApiFactory(private val connection: SupabaseConnection) :
    IngredientApiFactory {
    override fun createClient(): IngredientApiClient = SpoonacularApiClient(connection.client)
}
