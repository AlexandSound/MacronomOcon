package com.example.macronomocon.data.database

import kotlinx.serialization.json.JsonObject

/** Queries retain the session that created them; credentials never appear in query text. */
abstract class DatabaseQuery(protected val connection: DatabaseConnection) {
    abstract val table: String
    abstract suspend fun select(filters: Map<String, String> = emptyMap()): List<JsonObject>
    abstract suspend fun upsert(values: JsonObject, onConflict: String? = null)
    abstract suspend fun delete(filters: Map<String, String>)
}
