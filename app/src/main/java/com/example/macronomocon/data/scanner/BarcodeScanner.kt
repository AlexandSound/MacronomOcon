package com.example.macronomocon.data.scanner

import java.io.Closeable

/** Adapter target: callers receive a decoded EAN/UPC string, or null, without knowing the decoder. */
interface BarcodeScanner : Closeable {
    suspend fun scan(): String?

    override fun close() = Unit
}
