package com.example.macronomocon.data.repository

interface PantryDataSource {

    suspend fun loadItems()

    suspend fun saveItem()

    suspend fun deleteItem()
}
