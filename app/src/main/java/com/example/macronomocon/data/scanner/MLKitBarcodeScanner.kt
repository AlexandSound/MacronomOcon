package com.example.macronomocon.data.scanner

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Adapts ML Kit's callback-based client to [BarcodeScanner]. The camera layer supplies frames and must
 * retain each one until [scan] returns. No images are uploaded.
 */
class MLKitBarcodeScanner(private val nextFrame: suspend () -> InputImage) : BarcodeScanner {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
    )

    override suspend fun scan(): String? = decode(nextFrame())

    private suspend fun decode(frame: InputImage): String? =
        suspendCancellableCoroutine { continuation ->
            scanner.process(frame)
                .addOnSuccessListener { results ->
                    if (continuation.isActive) {
                        continuation.resume(
                            results.firstNotNullOfOrNull {
                                it.rawValue?.takeIf(String::isNotBlank)
                            }
                        )
                    }
                }
                .addOnFailureListener { failure ->
                    if (continuation.isActive) continuation.resumeWithException(failure)
                }
                .addOnCanceledListener { continuation.cancel() }
        }

    override fun close() = scanner.close()
}
