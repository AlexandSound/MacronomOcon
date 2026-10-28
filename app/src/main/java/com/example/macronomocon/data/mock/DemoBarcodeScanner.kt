package com.example.macronomocon.data.mock

import com.example.macronomocon.data.scanner.BarcodeScanner

/** Adapts a fixed fixture code to [BarcodeScanner] so the demo never touches the camera. */
class DemoBarcodeScanner(private val barcode: String) : BarcodeScanner {
    override suspend fun scan(): String = barcode
}
