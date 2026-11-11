package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.database.DatabaseQuery
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject

class SupabaseQuery(private val supabase: SupabaseConnection, override val table: String) :
    DatabaseQuery(supabase) {
    init {
        require(table.isNotBlank())
    }

    override suspend fun select(filters: Map<String, String>): List<JsonObject> =
        supabase.client.from(table).select {
            filter { filters.forEach { (column, value) -> eq(column, value) } }
        }.decodeList<JsonObject>()

    override suspend fun upsert(values: JsonObject, onConflict: String?) {
        supabase.client.from(table).upsert(values) {
            if (onConflict != null) this.onConflict = onConflict
        }
    }
}
