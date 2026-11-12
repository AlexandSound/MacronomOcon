package com.example.macronomocon.data.database

import kotlinx.serialization.json.JsonObject

/** Queries retain the session that created them; credentials never appear in query text. */
abstract class DatabaseQuery(protected val connection: DatabaseConnection) {
    abstract val table: String

    /**
     * Rows matching every equality filter. [columns] may embed related rows, and results are
     * sorted ascending by each [orderBy] column in turn.
     */
    abstract suspend fun select(
        filters: Map<String, String> = emptyMap(),
        columns: String = ALL_COLUMNS,
        orderBy: List<String> = emptyList()
    ): List<JsonObject>

    /** Inserts one row and returns it as stored, including server-assigned columns. */
    abstract suspend fun insert(values: JsonObject): JsonObject
    abstract suspend fun upsert(values: JsonObject, onConflict: String? = null)

    /** Returns the deleted rows, so a delete that row-level security filtered out is visible. */
    abstract suspend fun delete(filters: Map<String, String>): List<JsonObject>

    companion object {
        const val ALL_COLUMNS = "*"
    }
}
