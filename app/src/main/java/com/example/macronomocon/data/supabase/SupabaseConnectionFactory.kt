package com.example.macronomocon.data.supabase

import com.example.macronomocon.data.database.ConnectionFactory
import com.example.macronomocon.data.database.DatabaseConnection
import com.example.macronomocon.data.database.DatabaseQuery
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.ExternalAuthAction
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

class SupabaseConnectionFactory(private val url: String, private val publishableKey: String) :
    ConnectionFactory {
    override fun createConnection(): SupabaseConnection = SupabaseConnection(
        createSupabaseClient(url, publishableKey) {
            install(Auth) {
                scheme = REDIRECT_SCHEME
                host = REDIRECT_HOST
                flowType = FlowType.PKCE
                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Postgrest)
            install(Functions)
        }
    )

    override fun createQuery(connection: DatabaseConnection, table: String): DatabaseQuery {
        require(connection is SupabaseConnection)
        return connection.createQuery(table)
    }

    companion object {
        const val REDIRECT_SCHEME = "macronomocon"
        const val REDIRECT_HOST = "login-callback"
    }
}
