package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.database.DatabaseConnection
import io.github.jan.supabase.SupabaseClient

/** Wraps the authenticated HTTP client, not a raw SQL socket or a pooled connection. */
class SupabaseConnection(val client: SupabaseClient) : DatabaseConnection() {
    override fun createQuery(table: String): SupabaseQuery = SupabaseQuery(this, table)
}
