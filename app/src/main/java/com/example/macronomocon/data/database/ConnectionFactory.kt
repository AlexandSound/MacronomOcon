package com.example.macronomocon.data.database

interface ConnectionFactory {
    fun createConnection(): DatabaseConnection
    fun createQuery(connection: DatabaseConnection, table: String): DatabaseQuery
}
