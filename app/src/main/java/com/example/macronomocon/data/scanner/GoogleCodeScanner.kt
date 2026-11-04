package com.example.macronomocon.data.scanner

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Adapts Google's code scanner to [BarcodeScanner]. Google Play services shows the camera UI, so
 * the app needs no camera permission. Cancelling the scan returns null.
 */
class GoogleCodeScanner(context: Context) : BarcodeScanner {
    private val scanner = GmsBarcodeScanning.getClient(
        context,
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
    )

    override suspend fun scan(): String? = suspendCancellableCoroutine { continuation ->
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                if (continuation.isActive) {
                    continuation.resume(barcode.rawValue?.takeIf(String::isNotBlank))
                }
            }
            .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
            .addOnFailureListener { failure ->
                if (continuation.isActive) continuation.resumeWithException(failure)
            }
    }
}
