package com.example.util

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BarcodeScannerHelper {
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_ALL_FORMATS
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    suspend fun scanBitmap(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val rawValue = barcodes.firstOrNull()?.rawValue
                    continuation.resume(rawValue)
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeScanner", "Failed to scan barcode", e)
                    continuation.resume(null)
                }
        } catch (e: Exception) {
            Log.e("BarcodeScanner", "Error creating input image", e)
            continuation.resume(null)
        }
    }
}
