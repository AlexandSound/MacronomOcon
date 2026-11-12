package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.database.DatabaseQuery
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject

class SupabaseQuery(private val supabase: SupabaseConnection, override val table: String) :
    DatabaseQuery(supabase) {
    init {
        require(table.isNotBlank())
    }

    override suspend fun select(
        filters: Map<String, String>,
        columns: String,
        orderBy: List<String>
    ): List<JsonObject> = supabase.client.from(table).select(Columns.raw(columns)) {
        filter { filters.forEach { (column, value) -> eq(column, value) } }
        orderBy.forEach { order(it, Order.ASCENDING) }
    }.decodeList<JsonObject>()

    override suspend fun insert(values: JsonObject): JsonObject =
        supabase.client.from(table).insert(values) { select() }.decodeSingle<JsonObject>()

    override suspend fun upsert(values: JsonObject, onConflict: String?) {
        supabase.client.from(table).upsert(values) {
            if (onConflict != null) this.onConflict = onConflict
        }
    }

    override suspend fun delete(filters: Map<String, String>): List<JsonObject> =
        supabase.client.from(table).delete {
            filter {
                filters.forEach { (column, value) -> eq(column, value) }
            }
            select()
        }.decodeList<JsonObject>()
}
