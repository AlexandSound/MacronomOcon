package com.example.macronomocon.data.database

/** A database session that manufactures queries for its own provider. No connection pool. */
abstract class DatabaseConnection {
    abstract fun createQuery(table: String): DatabaseQuery
}
