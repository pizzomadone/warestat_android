package com.warestat.android.util

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BarcodeScannerUtil(
    private val context: Context,
    private val onBarcodeDetected: (String) -> Unit
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    fun startScanning(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onBarcodeDetected))
                }

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopScanning() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    private inner class BarcodeAnalyzer(
        private val onBarcodeDetected: (String) -> Unit
    ) : ImageAnalysis.Analyzer {
        private val scanner = BarcodeScanning.getClient()
        private var lastDetectedBarcode = ""
        private var lastDetectionTime = 0L

        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { it.rawValue != null }?.let { barcode ->
                        val value = barcode.rawValue!!
                        val now = System.currentTimeMillis()
                        // Debounce: avoid multiple detections
                        if (value != lastDetectedBarcode || now - lastDetectionTime > 2000L) {
                            lastDetectedBarcode = value
                            lastDetectionTime = now
                            onBarcodeDetected(value)
                        }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }
    }
}
