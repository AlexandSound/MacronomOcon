package com.example.macronomocon.data.api

/** Factory method for provider-specific ingredient API connections. */
interface IngredientApiFactory {
    fun createClient(): IngredientApiClient
}
