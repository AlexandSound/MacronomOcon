package com.example.macronomocon.data.repository

import com.example.macronomocon.domain.model.PantryItem

interface PantryDataSource {

    suspend fun loadItems(): List<PantryItem>
    suspend fun saveItem(item: PantryItem)
    suspend fun deleteItem(itemId: PantryItem)
}
